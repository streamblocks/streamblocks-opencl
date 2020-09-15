#ifndef __SCHEDULING_H__
#define __SCHEDULING_H__

#include <vector>
#include "Actor.h"

namespace scheduling {

    inline void RR(std::vector<std::shared_ptr<Actor>> actors) {
        bool progress;
        do {
            progress = false;
            for (const std::shared_ptr<Actor>& a : actors) {
                progress |= a->schedule();
            }
        } while (progress);
    }
}

#endif // __SCHEDULING_H__
