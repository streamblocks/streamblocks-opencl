#include "sb_native_source.h"

using namespace std::io::source;

#ifdef _WIN32
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <winsock2.h>

char std::io::source::getLastUserChar() {
    HANDLE tui_handle = GetStdHandle(STD_INPUT_HANDLE);
    DWORD tui_evtc;
    char retVal = 0;
    INPUT_RECORD tui_inrec;
    DWORD tui_numread;
    BOOLEAN tui_havehappened = FALSE;

    GetNumberOfConsoleInputEvents(tui_handle,&tui_evtc);
    while (tui_evtc > 0) {
        ReadConsoleInput(tui_handle,&tui_inrec,1,&tui_numread);
        if (tui_inrec.EventType == KEY_EVENT) {
            if (tui_inrec.Event.KeyEvent.bKeyDown) {
                retVal = tui_inrec.Event.KeyEvent.uChar.AsciiChar;
                tui_havehappened = TRUE;
            }
        }
        GetNumberOfConsoleInputEvents(tui_handle,&tui_evtc);
    }

    return retVal;
}

#else

char std::io::source::getLastUserChar() {
    char val = 0;
    fd_set rfds;
    struct timeval tv;
    int retval;

    FD_ZERO(&rfds);
    FD_SET(0, &rfds);

    tv.tv_sec = 0;
    tv.tv_usec = 1;

    retval = select(1, &rfds, NULL, NULL, &tv);

    if (retval > 0) {
        val = getchar();
    }
    return val;
}

#endif

void std::io::source::source_init(std::string fileName) {
    input_file = fileName;
    if (input_file.empty()) {
        std::cerr << "No input file given!" << std::endl;
        exit(1);
    }

    file.open(input_file.c_str(), std::ios::binary);
    if (!file.is_open()) {
        std::cerr << "could not open file " << input_file << std::endl;
        exit(1);
    }

    loopsCount = nbLoops;
}

int std::io::source::source_sizeOfFile() {
    file.seekg(0L, std::ios::end);
    long size = file.tellg();
    file.seekg(0L, std::ios::beg);
    return size;
}

void std::io::source::source_rewind() {
    file.clear();
    file.seekg(0, std::ios::beg);
}

unsigned int std::io::source::source_readByte() {
    return file.get();
}

void std::io::source::source_readNBytes(unsigned char outTable[], unsigned int nbTokenToRead) {
    file.read((char *) outTable, nbTokenToRead);
}

unsigned int std::io::source::source_getNbLoop(void) {
    return nbLoops;
}

void std::io::source::source_decrementNbLoops() {
    --loopsCount;
}

bool std::io::source::source_isMaxLoopsReached() {
    return nbLoops != -1 && loopsCount <= 0;
}

/*Native functions added for dpd benchmark: start here*/
void std::io::source::source_init_dpd(std::string fileName_ii, std::string fileName_iq)
{
	count = 0;
	atexit(close_all);
	if (file_ii == NULL)
	{
		file_ii = fopen(fileName_ii.c_str(), "r");

		if (file_ii == NULL)
		{
			printf("Unable to open file %s\nExit\n", fileName_ii);
			exit(0);
		}
		else
			printf("Opened file %s\n", fileName_ii.c_str());
	}

	if (file_iq == NULL)
	{
		file_iq = fopen(fileName_iq.c_str(), "r");

		if (file_iq == NULL)
		{
			printf("Unable to open file %s\nExit\n", fileName_iq);
			exit(0);
		}
		else
			printf("Opened file %s\n", fileName_iq.c_str());
	}


}

float std::io::source::source_read(FILE *file)
{
	int ret_val;
	float sample = 0.0;

	if (file != NULL)
	{
		ret_val = fscanf(file, "%f\n", &sample);

		if (ret_val != 1)
		{
			//close_all();
			//exit(0);
		}
	}
	return sample;
	count++;
}

float std::io::source::source_read_i()
{
	return source_read(file_ii);
}

float std::io::source::source_read_q()
{
	return source_read(file_iq);
}

void std::io::source::close_all()
{
	printf("Read %i complex samples.\nClosing files.\n", count);
	close_file(&file_ii);
	close_file(&file_iq);
}

void std::io::source::close_file(FILE **file)
{
	if (file[0] != NULL)
	{
		fclose(file[0]);
		file[0] = NULL;
	}
}
/*Native functions added for dpd benchmark: end here*/

/*Native functions and variables added for ZigBee benchmark: start here*/
void std::io::source::source_init_ZB(std::string fileName_ZB_i)
{
	source_packets = 0;
	sink_packets = 0;
	if (file_zb_i == NULL)
	{
		file_zb_i = fopen(fileName_ZB_i.c_str(), "r");

		if (file_zb_i == NULL)
		{
			printf("Unable to open file %s\nExit\n", fileName_ZB_i);
			close_file(&file_zb_i);
			exit(0);
		}
		else
			printf("Opened file %s\n", fileName_ZB_i.c_str());
	}
}

unsigned char std::io::source::source_readByte_ZB()
{
    int ret_val;
    int sample;

    ret_val = fscanf(file_zb_i, "%i\n", &sample);

    if(ret_val != 1){
        printf("Packet payload ended unexpectedly\nExit\n");
        close_file(&file_zb_i);
        exit(0);
    }

    return (unsigned char) sample;
}
int std::io::source::source_sizeOfFile_ZB()
{
    int ret_val;
    int sample;

    ret_val = fscanf(file_zb_i, "%i\n", &sample);

    if(ret_val != 1){
        return 1;
    }
    source_packets++;
    return sample;
}

void std::io::source::throw_away(int value)
{
    if(file_zb_o == NULL){
        file_zb_o = fopen("tx_stream.out", "w");
        if (file_zb_o == NULL)
        {
            printf("Unable to open output file tx_stream.out\nExit\n");
            close_file(&file_zb_o);
            exit(0);
        }
    }

    fprintf(file_zb_o, "%i\n", value);
}

void std::io::source::print_cyclecount()
{
    sink_packets++;

    if(feof(file_zb_i) && (sink_packets == source_packets) ){
        close_file(&file_zb_o);
        exit(0);
    }
}
/*Native functions and variables added for ZigBee benchmark: end here*/
