#pragma once

#include <SDL.h>
#include <stdint.h>

namespace std {
    namespace video {
        namespace display {

            static unsigned int startTime;
            static unsigned int mappingTime;
            static unsigned int relativeStartTime;
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