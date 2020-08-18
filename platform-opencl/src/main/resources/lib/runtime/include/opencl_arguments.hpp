#pragma once
#include <vector>
#include <CL\cl.hpp>

#define OPENCL_VERSION_1_2  1.2f
#define OPENCL_VERSION_2_0  2.0f

struct opencl_arguments {
	opencl_arguments();
	~opencl_arguments();

	cl_context       context;
	cl_device_id     device;
	cl_command_queue commandQueue;
	cl_program       program;
	std::vector<cl_kernel>        kernels;
	float            platformVersion;
	float            deviceVersion;
	float            compilerVersion;

	size_t		   globalWorkSize[1];
	size_t          localWorkSize[1];
	cl_int          work_Dim;
};
