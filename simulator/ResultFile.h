#ifndef RESULT_FILE_H
#define RESULT_FILE_H

#include <sys/types.h>

// struct on disk
struct SystemResult {
    double meanHappiness; // average
    double reliabilityStd; // stddev of averages
    double consensusStdAvg; // avegage of stddevs
    double giniWelfare; // gini welfare function
};
typedef struct SystemResult SystemResult; 

// struct on disk
/*!
 @struct Result
*/
struct Result {
    u_int32_t trials;

    /*! @var systems size is numSystems * numStrategies
     * accessed as systems[strategy * numSystems + system] */
    SystemResult systems[1];
};
typedef struct Result Result;

#ifndef INLINE
#ifdef __cplusplus
#define INLINE inline
#else
#define INLINE __inline
#endif
#endif
INLINE size_t sizeofRusult( unsigned int nsys ) {
    return (sizeof(struct SystemResult) * nsys + sizeof(u_int32_t) * 2);
}
INLINE size_t sizeofRusult( unsigned int nsys, unsigned int nstrat ) {
    return (sizeof(struct SystemResult) * (nsys * (nstrat + 1)) + sizeof(u_int32_t) * 2);
}

typedef enum printStyle {
	noPrint = 0,
	basic = 1,
	smallBasic = 2,
	smallerBasic = 3,
	html = 4,
	htmlWithStrategy = 5,
	penultimatePrintStyle
} printStyle;

Result* newResult( int nsys );
// a = weighted average of a and b
void mergeResult( Result* a, Result* b, int nsys );
void printSystemResult( void* file, const struct SystemResult* it, const char* name, printStyle style = basic );
void printResult( void* file, const Result* it, char** names, int nsys, printStyle style = basic );
void printResultHeader( void* file, u_int32_t numc, u_int32_t numv, u_int32_t trials, float error, int nsys, printStyle style = basic );
void printResultFooter( void* file, u_int32_t numc, u_int32_t numv, u_int32_t trials, float error, int nsys, printStyle style = basic );

struct NameBlock {
    char* block;
    int blockLen;
    char** names;
    int nnames;
};
typedef struct NameBlock NameBlock;

class VotingSystem;

void votingSystemArrayToNameBlock( NameBlock* ret, VotingSystem** systems, int nsys );
void parseNameBlock( NameBlock* it );
void makeBlock( NameBlock* names );

#endif

