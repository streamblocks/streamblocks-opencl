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

/*Native functions and variables added for dpd benchmark: start here*/
void std::video::display::sink_init(std::string fileName_oi, std::string fileName_oq)
{
	count = 0;
	atexit(close_all);
	if (file_oi == NULL)
	{
		file_oi = fopen(fileName_oi.c_str(), "w");

		if (file_oi == NULL)
		{
			printf("Unable to open file %s\nExit\n", fileName_oi);
			exit(0);
		}
		else
			printf("Opened file %s\n", fileName_oi.c_str());
	}

	if (file_oq == NULL)
	{
		file_oq = fopen(fileName_oq.c_str(), "w");

		if (file_oq == NULL)
		{
			printf("Unable to open file %s\nExit\n", fileName_oq);
			exit(0);
		}
		else
			printf("Opened file %s\n", fileName_oq.c_str());
	}
}

void std::video::display::file_write(FILE *file, float value)
{
	fprintf(file, "%f\n", value);
}

void std::video::display::sink_consume_i(float value)
{
	file_write(file_oi, value);
	count++;
}

void std::video::display::sink_consume_q(float value)
{
	file_write(file_oq, value);
	count++;
}

void std::video::display::close_all()
{
	printf("Written %i complex samples.\nClosing files.\n", count);
	close_file(&file_oi);
	close_file(&file_oq);
}

void std::video::display::close_file(FILE **file)
{
	if (file[0] != NULL)
	{
		fclose(file[0]);
		file[0] = NULL;
	}
}
/*Native functions and variables added for dpd benchmark: end here*/