#ifndef __ACTOR_H__
#define __ACTOR_H__

#include <atomic>

class Actor {

public:
    virtual bool schedule() = 0;

    virtual ~Actor(){}

    std::atomic_flag running;

};

#endif //__ACTOR_H__