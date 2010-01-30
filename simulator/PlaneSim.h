#ifndef PLANE_SIM_H
#define PLANE_SIM_H

#include "Voter.h"
#include "VotingSystem.h"

#include <math.h>
#include <pthread.h>
#include <stdint.h>

class XYSource;
class DoubleRandom;
class GaussianRandom;

class pos {
public:
	double x, y;
};

class ResultAccumulation {
public:
	ResultAccumulation(int x, int y, int z)
		: accum(NULL), px(x), py(y), planes(z) {
		accum = new int[px * py * planes];
	}
	~ResultAccumulation() {
		delete [] accum;
	}
	inline int getAccum( int x, int y, int c ) const {
		return accum[c + x*planes + y*planes*px];
	}
	inline void incAccum( int x, int y, int c ) {
		accum[c + x*planes + y*planes*px]++;
	}
	inline void setAccum( int x, int y, int c, int v ) {
		accum[c + x*planes + y*planes*px] = v;
	}

  void clear();

protected:
	int* accum;
	int px;
	int py;
	int planes;
};

class PlaneSim {
public:
	class candidatearg;

	pos* candidates;
	VoterArray they;
	
	double minx, maxx, miny, maxy;
	int px, py;
	double voterSigma;
	int electionsPerPixel;

	bool manhattanDistance;
	bool linearFalloff;

	VotingSystem** systems;
	int systemsLength;
	// there must be the same number of result accumulations as systems
	ResultAccumulation* accum;

	// indicates that certain pointers aren't owned and should not be freed
	bool isSlave;

	candidatearg* candroot;
	int candcount;

	uint8_t* pix;

	int seats;
	bool doCombinatoricExplode;
	VoterArray exploded;
	int* combos;

	DoubleRandom* rootRandom;
	GaussianRandom* gRandom;

	PlaneSim() : candidates( NULL ), minx( -2.0 ), maxx( 2.0 ), miny( -2.0 ), maxy( 2.0 ),
		px( 500 ), py( 500 ), voterSigma( 0.5 ), electionsPerPixel( 10 ),
		manhattanDistance( false ), linearFalloff( false ), accum( NULL ), isSlave( false ),
		candroot( NULL ), candcount( 0 ), pix( NULL ),
		seats(1), doCombinatoricExplode(false), combos(NULL),
		rootRandom(NULL), gRandom(NULL)
	{}
	~PlaneSim() {
		if (combos) {delete [] combos;}
	}
	
	void build( int numv );
	// build this to be a slave to it.
	void coBuild( const PlaneSim& it );
	
	void randomizeVoters( double centerx, double centery, double sigma );
	
	void run( VotingSystem* system );
	void runXYSource(VotingSystem* system, XYSource* source);
	void runPixel(VotingSystem* system, int x, int y, double dx, double dy, int* winners);

	size_t configStr( char* dest, size_t len );

	inline double xIndexToCoord( int x ) {
		double dx;
		dx = (maxx - minx)/(px - 1);
		return minx + dx*x;
	}
	inline double yIndexToCoord( int y ) {
		double dy;
		dy = (maxy - miny)/(py - 1);
		return miny + dy*y;
	}
	inline int getAccum( int x, int y, int c ) const {
		return accum->getAccum(x, y, c);
	}
	inline void incAccum( int x, int y, int c ) {
		accum->incAccum(x, y, c);
	}
	inline void setAccum( int x, int y, int c, int v ) {
		accum->setAccum(x, y, c, v);
	}
	
	inline int xCoordToIndex( double x ) {
		double dx;
		dx = (maxx - minx)/(px - 1);
		return (int)floor( (x - minx) / dx );
	}
	inline int yCoordToIndex( double y ) {
		double dy;
		dy = (maxy - miny)/(py - 1);
		return (int)floor( (y - miny) / dy );
	}

	void addCandidateArg( const char* arg );
	void addCandidateArg( double x, double y );
	
	inline uint8_t* getpxp( int x, int y ) {
		return pix + (x*3 + y*3*px);
	}
	inline void setpx( int x, int y, const uint8_t* color ) {
		int index = px*3*y + 3*x;
		uint8_t* p = pix + index;
		*p = *color;
		p++;color++;
		*p = *color;
		p++;color++;
		*p = *color;
	}
	void gaussTest( const char* filename, int nvoters );
	void writePNG( const char* filename );
	void writePlanePNG( const char* filename, int choice );
	void writeSumPNG( const char* filename );

	void drawDiamond( int x, int y, const uint8_t* color );
	
	public:
	class candidatearg {
	public:
		double x,y;
		candidatearg* next;
		candidatearg( const char* arg, candidatearg* next );
		candidatearg( double xIn, double yIn, candidatearg* nextIn )
			: x(xIn), y(yIn), next(nextIn) {}
	};
};

class PlaneSimThread {
public:
	PlaneSim* sim;
	pthread_t thread;
	VotingSystem* vs;
	XYSource* source;
};

// for pthread_create, arg is PlaneSimThread*
extern "C" void* runPlaneSimThread(void* arg);

#endif /* PLANE_SIM_H */
