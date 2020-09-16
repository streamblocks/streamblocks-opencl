#include <iostream>
#include "sb_native_display.h"

using namespace std::video::display;

char std::video::display::displayYUV_getFlags() {
    return 1;
}

void std::video::display::displayYUV_setSize(int width, int height) {
}

void std::video::display::displayYUV_displayPicture(unsigned char *pictureBufferY,
                                                    unsigned char *pictureBufferU, unsigned char *pictureBufferV,
                                                    unsigned int pictureWidth, unsigned int pictureHeight) {
}

void std::video::display::displayYUV_init() {

}

void std::video::display::display_close() {
}
