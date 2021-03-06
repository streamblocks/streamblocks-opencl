#pragma once

#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <stdlib.h>
#include <stdio.h>

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

            /*Native functions and variables added for dpd benchmark: start here*/
            static FILE *file_ii = NULL;
            static FILE *file_iq = NULL;
            static int count;

            void source_init_dpd(std::string fileName_ii, std::string fileName_iq);
            float source_read(FILE * file);
            float source_read_i();
            float source_read_q();
            void close_all();
            void close_file(FILE ** file);
            /*Native functions and variables added for dpd benchmark: end here*/

            /*Native functions and variables added for ZigBee benchmark: start here*/
            static FILE *file_zb_i = NULL;
            static FILE *file_zb_o = NULL;

            static int source_packets;
            static int sink_packets;

            void source_init_ZB(std::string fileName_ZB_i);
            unsigned char source_readByte_ZB();
            int source_sizeOfFile_ZB();
            void throw_away(int value);
            void print_cyclecount();
            /*Native functions and variables added for ZigBee benchmark: end here*/
        }
    }
}

