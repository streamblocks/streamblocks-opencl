#pragma once

#ifdef DISPLAY_ENABLE
#include <SDL.h>
#endif

#include <stdint.h>

namespace std {
    namespace video {
        namespace display {
#ifdef DISPLAY_ENABLE
            static SDL_Window *pWindow1;
            static SDL_Renderer *pRenderer1;
            static SDL_Texture *bmpTex1;
            static uint8_t *pixels1;
            static int pitch1, size1;

            static int init = 0;
#endif
            char displayYUV_getFlags();

            void displayYUV_setSize(int width, int height);

            void displayYUV_displayPicture(unsigned char *pictureBufferY,
                                           unsigned char *pictureBufferU, unsigned char *pictureBufferV,
                                           unsigned int pictureWidth, unsigned int pictureHeight);

            void displayYUV_init();

            void display_close();

            /*Native functions and variables added for dpd benchmark: start here*/
            static FILE *file_oi = NULL;
            static FILE *file_oq = NULL;
            static int count;

            void close_all();
            void close_file(FILE ** file);
            void sink_init(std::string fileName_oi, std::string fileName_oq);
            void file_write(FILE * file, float value);
            void sink_consume_i(float value);
            void sink_consume_q(float value);
            /*Native functions and variables added for dpd benchmark: end here*/
        }
    }
}