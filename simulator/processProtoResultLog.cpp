#include <stdio.h>
#include <fcntl.h>
#include <math.h>

#include <map>
#include <set>
#include <string>
#include <vector>

#include "trial.pb.h"
#include "ProtoResultLog.h"

#include <google/protobuf/io/zero_copy_stream_impl.h>

using google::protobuf::int32;
using google::protobuf::io::FileInputStream;

using std::vector;
using std::set;
using std::string;
using std::map;

/*
 Graphing
 X, Y, sets
 Y will most often be one of the compiled figures in a ResultList {mean_avg, voter_std_avg, gini_avg, mean_std}
 X will most often be something there are many steps over, {choices, error}
 
 */

class ResultList {
public:
	vector<float> mean_happiness;
	vector<float> voter_happiness_stddev;
	vector<float> gini_index;
	
	// average total happiness
	double mean_avg;
	// average voter variability
	double voter_std_avg;
	// average voter variability
	double gini_avg;
	// system reliability
	double mean_std;
	
	void process() {
		size_t i;
		double mean_sum = 0.0;
		double mean_vs = 0.0;
		double voter_std_sum = 0.0;
		double gini_sum = 0.0;
		for (i = 0; i < mean_happiness.size(); ++i) {
			mean_sum += mean_happiness[i];
		}
		mean_avg = mean_sum / mean_happiness.size();
		for (i = 0; i < voter_happiness_stddev.size(); ++i) {
			voter_std_sum += voter_happiness_stddev[i];
		}
		voter_std_avg = voter_std_sum / voter_happiness_stddev.size(); 
		for (i = 0; i < gini_index.size(); ++i) {
			gini_sum += gini_index[i];
		}
		gini_avg = gini_sum / voter_happiness_stddev.size(); 
		for (i = 0; i < mean_happiness.size(); ++i) {
			double v = mean_happiness[i] - mean_avg;
			mean_vs += v * v;
		}
		mean_std = sqrt(mean_vs / mean_happiness.size());
	}
};

class ResultHolder {
public:
	// Parameters
	int32 voters;
	int32 choices;
	float error;
	int32 system_index;
	VoterSim::PreferenceMode mode;
	int32 dimensions;
	
	// Results
	ResultList* results;
	ResultHolder()
	: voters(-1), choices(-1), error(-1.0), system_index(-1), mode(VoterSim::BOGUS_PREFERENCE_MODE), dimensions(-1), results(NULL) {
	}
	ResultHolder(int32 v, int32 c, float e, int32 i, VoterSim::PreferenceMode m, int32 d)
	: voters(v), choices(c), error(e), system_index(i), mode(m), dimensions(d), results(NULL) {
	}
	
	void addResult(double mean_happiness, double voter_happiness_stddev, double gini_index) {
		if (results == NULL) {
			results = new ResultList();
		}
		results->mean_happiness.push_back(mean_happiness);
		results->voter_happiness_stddev.push_back(voter_happiness_stddev);
		results->gini_index.push_back(gini_index);
	}
};

class ResultHolderCompare {
public:
	int operator()(const ResultHolder& a, const ResultHolder& b) {
		// is a less than b?
		if (a.voters < b.voters) {
			return 1;
		}
		if (a.voters > b.voters) {
			return 0;
		}
		if (a.choices < b.choices) {
			return 1;
		}
		if (a.choices > b.choices) {
			return 0;
		}
		if (a.error < b.error) {
			return 1;
		}
		if (a.error > b.error) {
			return 0;
		}
		if (a.system_index < b.system_index) {
			return 1;
		}
		if (a.system_index > b.system_index) {
			return 0;
		}
		if (a.mode < b.mode) {
			return 1;
		}
		if (a.mode > b.mode) {
			return 0;
		}
		if (a.dimensions < b.dimensions) {
			return 1;
		}
		if (a.dimensions > b.dimensions) {
			return 0;
		}
		return 0;
	}
};

typedef map<ResultHolder, ResultHolder, ResultHolderCompare> ResultsMap;
ResultsMap results;
set<int> vsteps;
set<int> csteps;
set<double> esteps;
set<int> sisteps;
set<VoterSim::PreferenceMode> modesteps;
set<int> dimsteps;

template<class T> void printIterable(T i, const T& end, const char* format) {
	for (; i != end; i++) {
		printf(format, *i);
	}
}

int main(int argc, char** argv) {
	int count = 0;
	int voters, choices, systemIndex, dimensions;
	double error, happiness, voterHappinessStd, gini;
	VoterSim::PreferenceMode mode;
	const char* rlogname = argv[1];

	ProtoResultLog* rlog = ProtoResultLog::openForReading(rlogname);
	while (rlog->readResult(&voters, &choices, &error, &systemIndex, &mode, &dimensions, &happiness, &voterHappinessStd, &gini)) {
		count++;
		ResultHolder key(voters, choices, error, systemIndex, mode, dimensions);
		ResultHolder& value = results[key];
		value.addResult(happiness, voterHappinessStd, gini);
	}
	printf("read %d records in %zu configurations\n", count, results.size());
	count = 0;
	FILE* csv;
	{
		string csvname(rlogname);
		csvname += ".csv";
		csv = fopen(csvname.c_str(), "w");
		fprintf(csv, "System,Voters,Choices,Error,Mode,Dimensions,Happiness,Voter Happiness Std,Gini,System Std\n");
	}
	for (ResultsMap::iterator ri = results.begin(); ri != results.end(); ri++) {
		const ResultHolder& key = (*ri).first;
		//printf("v%d c%d e%f si%d m%d d%d\n", key.voters, key.choices, key.error, key.system_index, key.mode, key.dimensions);
		vsteps.insert(key.voters);
		csteps.insert(key.choices);
		esteps.insert(key.error);
		sisteps.insert(key.system_index);
		modesteps.insert(key.mode);
		dimsteps.insert(key.dimensions);
		//ResultHolder& rh = (*ri).second;
		ResultList* results = (*ri).second.results;
		//rh = (*ri).second;
		results->process();
		if (csv != NULL) {
			fprintf(csv, "\"%s\",%d,%d,%f,\"%s\",%d,%f,%f,%f,%f\n",
				rlog->name(key.system_index), key.voters, key.choices,
				key.error, VoterSim::modeName(key.mode), key.dimensions,
				results->mean_avg, results->voter_std_avg, results->gini_avg, results->mean_std);
		}
		count++;
	}
	printf("iterated %d configurations\n", count);
	printf("%zu vsteps:", vsteps.size());
	printIterable(vsteps.begin(), vsteps.end(), " %d");
	printf("\n%zu csteps:", csteps.size());
	printIterable(csteps.begin(), csteps.end(), " %d");
	printf("\n%zu esteps:", esteps.size());
	printIterable(esteps.begin(), esteps.end(), " %.2f");
	printf("\n%zu sisteps:", sisteps.size());
	printIterable(sisteps.begin(), sisteps.end(), " %d");
	printf("\n%zu modesteps:", modesteps.size());
	printIterable(modesteps.begin(), modesteps.end(), " %d");
	printf("\n%zu dimsteps:", dimsteps.size());
	printIterable(dimsteps.begin(), dimsteps.end(), " %d");
	printf("\n");
}

