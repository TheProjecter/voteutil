#ifndef STV_H
#define STV_H

#include "VotingSystem.h"

class STV : public VotingSystem {
public:
    STV() : VotingSystem( "STV" ), seatsDefault(1) {};
    virtual void init( const char** envp );
    virtual void runElection( int* winnerR, const VoterArray& they ) const;
	virtual bool runMultiSeatElection( int* winnerArray, const VoterArray& they, int seats ) const;
    virtual ~STV();

private:
    int seatsDefault;
};

#endif
