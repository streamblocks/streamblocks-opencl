#include <iostream>

#include "sb_native_framerate.h"

using namespace std::video::display;

void std::video::display::print_fps_avg() {
    clock_t endTime = clock();

    float decodingTime = (endTime - startTime) / CLOCKS_PER_SEC;
    float framerate = numPicturesDecoded / decodingTime;

    std::cout << numPicturesDecoded << " images in " << decodingTime << " seconds: " << framerate << " FPS" << std::endl;
}

void std::video::display::fpsPrintInit() {
    startTime = clock();
    numPicturesDecoded = 0;
    partialNumPicturesDecoded = 0;
    lastNumPic = 0;
    atexit(print_fps_avg);
    relativeStartTime = startTime;
}

void std::video::display::fpsPrintNewPicDecoded() {
    unsigned int endTime;
    numPicturesDecoded++;
    partialNumPicturesDecoded++;
    endTime = clock();

    float relativeTime = (endTime - relativeStartTime) / CLOCKS_PER_SEC;

    if (relativeTime >= 5) {
        float framerate = (numPicturesDecoded - lastNumPic) / relativeTime;
        std::cout << framerate << " images/sec" << std::endl;

        relativeStartTime = endTime;
        lastNumPic = numPicturesDecoded;
    }
}