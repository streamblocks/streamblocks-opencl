#include "opencl_arguments.hpp"
#include "Translation.hpp"
#include <iostream>

opencl_arguments::opencl_arguments() :
context(NULL),
device(NULL),
commandQueue(NULL),
program(NULL),
platformVersion(OPENCL_VERSION_1_2),
deviceVersion(OPENCL_VERSION_1_2),
compilerVersion(OPENCL_VERSION_1_2)
{ }

opencl_arguments::~opencl_arguments() {
	cl_int errorCode = CL_SUCCESS;
	for(int i = 0; i < kernels.size(); ++i) {
		if (kernels[i]) {
			errorCode = clReleaseKernel(kernels[i]);
			if (errorCode != CL_SUCCESS) {
				std::cout << "Error: clReleaseKernel returned " << TranslateErrorCode(errorCode) << std::endl;
			}
		}
	}
	if (program) {
		errorCode = clReleaseProgram(program);
		if (errorCode != CL_SUCCESS) {
			std::cout << "Error: clReleaseProgram returned " << TranslateErrorCode(errorCode) << std::endl;
		}
	}
	if (commandQueue) {
		errorCode = clReleaseCommandQueue(commandQueue);
		if (errorCode != CL_SUCCESS) {
			std::cout << "Error: clReleaseCommandQueue returned " << TranslateErrorCode(errorCode) << std::endl;
		}
	}
	if (device) {
		errorCode = clReleaseDevice(device);
		if (errorCode != CL_SUCCESS) {
			std::cout << "Error: clReleaseDevice returned " << TranslateErrorCode(errorCode) << std::endl;
		}
	}
	if (context) {
		errorCode = clReleaseContext(context);
		if (errorCode != CL_SUCCESS) {
			std::cout << "Error: clReleaseContext returned " << TranslateErrorCode(errorCode) << std::endl;
		}
	}
}