#pragma once

#include <iostream>
#include <fstream>
#include <string>
#include <sys/select.h>
#include <vector>
#include <stdlib.h>

namespace std {
    namespace io {
        namespace source {
            static std::ifstream file;

            static int loopsCount;
            static std::string input_file;

            static int nbLoops = 1;

            char getLastUserChar();

            void source_init(std::string fileName);

            int source_sizeOfFile();

            void source_rewind();

            unsigned int source_readByte();

            void source_readNBytes(unsigned char outTable[], unsigned int nbTokenToRead);

            unsigned int source_getNbLoop(void);

            void source_decrementNbLoops();

            bool source_isMaxLoopsReached();
        }
    }
}

