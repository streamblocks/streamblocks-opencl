#ifndef __PRELUDE__
#define __PRELUDE__

#include <iostream>
#include "native_source.h"


namespace prelude {
    inline void print(std::string s) {
        std::cout << s;
    }

    inline void println(std::string s) {
        std::cout << s << std::endl;
    }
}

#endif // __PRELUDE__