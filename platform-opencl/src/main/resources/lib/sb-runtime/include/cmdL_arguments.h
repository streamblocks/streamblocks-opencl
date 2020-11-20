#pragma once

struct cmd_line_options {
	/* Input/Output specific options */
	char *input_file;
	char *input_directory;
	char *output_file;
	char *output_directory;

	/* OpenCL specific options */
	char *device_type;
	char *device_name;

};