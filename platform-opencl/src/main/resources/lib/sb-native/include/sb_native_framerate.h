#pragma once

#include <stdint.h>

namespace std {
    namespace video {
        namespace display {
#ifdef DISPLAY
            static unsigned int startTime;
            static unsigned int relativeStartTime;
#else
            static clock_t startTime;
            static clock_t relativeStartTime;
#endif
            static int lastNumPic;
            static int numPicturesDecoded;
            static int numAlreadyDecoded;
            static int partialNumPicturesDecoded;

            void print_fps_avg();

            void fpsPrintInit();

            void fpsPrintNewPicDecoded();
        }
    }
}