#pragma once

#include "Translation.h"
#include "opencl_arguments.h"

#include "cmdL_arguments.h"

extern cmd_line_options *opt;

//Parts of the Source Code from intel template
cl_int SetupOpenCL(opencl_arguments *ocl, cl_device_type deviceType, std::string platformName);

cl_int SetupOpenCLAuto(opencl_arguments *ocl, cmd_line_options *opt);

//Parts of the Source Code from intel template
cl_int CreateAndBuildProgram(opencl_arguments *ocl, std::string fileName);

cl_int ExecuteKernel(opencl_arguments *ocl, int index, cl_event *event = NULL);

bool printClDevices();

bool printDeviceInformation(cl_device_id id);

cl_int SetupOpenCLByDeviceID(opencl_arguments *ocl, cl_device_id id);

cl_device_id findDeviceID(int id);

void displayCLInfobyParam();