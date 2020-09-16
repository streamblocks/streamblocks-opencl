#pragma once

#include <SDL.h>
#include <stdint.h>

namespace std {
    namespace video {
        namespace display {
            static SDL_Window *pWindow1;
            static SDL_Renderer *pRenderer1;
            static SDL_Texture *bmpTex1;
            static uint8_t *pixels1;
            static int pitch1, size1;

            static int init = 0;

            char displayYUV_getFlags();

            void displayYUV_setSize(int width, int height);

            void displayYUV_displayPicture(unsigned char *pictureBufferY,
                                           unsigned char *pictureBufferU, unsigned char *pictureBufferV,
                                           unsigned int pictureWidth, unsigned int pictureHeight);

            void displayYUV_init();

            void display_close();
        }
    }
}