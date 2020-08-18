#include "utils.hpp"
#include <stdio.h>
#include <iostream>

cl_int ReadSourceFromFile(const char* fileName, char** source, size_t* sourceSize) {
	cl_int errorCode = CL_SUCCESS;
	FILE* fp = NULL;
	fopen_s(&fp, fileName, "rb");
	if (fp == NULL) {
		std::cout << "Error: Couldn't find program source file " << fileName << std::endl;
		errorCode = CL_INVALID_VALUE;
	}
	else {
		fseek(fp, 0, SEEK_END);
		*sourceSize = ftell(fp);
		fseek(fp, 0, SEEK_SET);
		*source = new char[*sourceSize];
		if (*source == NULL) {
			std::cout << "Error: Couldn't allocate " << *sourceSize << " bytes for program source from file " << fileName << std::endl;
			errorCode = CL_OUT_OF_HOST_MEMORY;
		}
		else {
			fread(*source, 1, *sourceSize, fp);
		}
	}
	return errorCode;
}

cl_platform_id FindOpenCLPlatform(cl_device_type deviceType,std::string platformName) {
	cl_int errorCode = CL_SUCCESS;
	cl_uint numPlatforms = 0;
	errorCode = clGetPlatformIDs(0, NULL, &numPlatforms);
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: clGetplatform_ids() to get num platforms returned " << TranslateErrorCode(errorCode) << std::endl;
		return NULL;
	}
	if (numPlatforms == 0) {
		std::cout << "Error: No platforms found!" << std::endl;
		return NULL;
	}
	std::vector<cl_platform_id> platforms(numPlatforms);
	errorCode = clGetPlatformIDs(numPlatforms, &platforms[0], NULL);
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: clGetplatform_ids() to get platforms returned " << TranslateErrorCode(errorCode) << std::endl;
		return NULL;
	}

	for (cl_uint i = 0; i < numPlatforms; ++i) {
		bool platformFound = false;
		cl_uint numDevices = 0;
		size_t stringLength = 0;
		errorCode = clGetPlatformInfo(platforms[i], CL_PLATFORM_NAME, 0, NULL, &stringLength);
		if (errorCode != CL_SUCCESS) {
			std::cout << "Error: clGetPlatformInfo() to get CL_PLATFORM_NAME length returned " << TranslateErrorCode(errorCode) << std::endl;
			platformFound = false;
			errorCode = CL_SUCCESS;
		}
		else {
			std::vector<char> foundPlatformName(stringLength);
			errorCode = clGetPlatformInfo(platforms[i], CL_PLATFORM_NAME, stringLength, &foundPlatformName[0], NULL);
			if (errorCode != CL_SUCCESS) {
				std::cout << "Error: clGetplatform_ids() to get CL_PLATFORM_NAME returned " << TranslateErrorCode(errorCode) << std::endl;
				platformFound = false;
				errorCode = CL_SUCCESS;
			}
			else {
				if (strstr(&foundPlatformName[0], platformName.c_str()) != 0) {
					platformFound = true;
					std::cout << "Required Platform found" << std::endl;
				}
			}
		}

		if (platformFound) {
			errorCode = clGetDeviceIDs(platforms[i], deviceType, 0, NULL, &numDevices);
			if (errorCode != CL_SUCCESS) {
				std::cout << "clGetDeviceIDs() returned " << TranslateErrorCode(errorCode) << std::endl;
			}

			if (numDevices != 0) {
				return platforms[i];
			}
		}
	}
	return NULL;
}

cl_int GetPlatformAndDeviceVersion(cl_platform_id platformID, opencl_arguments *ocl) {
	cl_int errorCode = CL_SUCCESS;
	size_t stringLength = 0;
	errorCode = clGetPlatformInfo(platformID, CL_PLATFORM_VERSION, 0, NULL, &stringLength);
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: clGetPlatformInfo() to get CL_PLATFORM_VERSION length returned " << TranslateErrorCode(errorCode) << std::endl;
		return errorCode;
	}

	std::vector<char> platformVersion(stringLength);
	errorCode = clGetPlatformInfo(platformID, CL_PLATFORM_VERSION, stringLength, &platformVersion[0], NULL);
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: clGetplatform_ids() to get CL_PLATFORM_VERSION returned " << TranslateErrorCode(errorCode) << std::endl;
		return errorCode;
	}

	if (strstr(&platformVersion[0], "OpenCL 2.0") != NULL) {
		ocl->platformVersion = OPENCL_VERSION_2_0;
	}

	errorCode = clGetDeviceInfo(ocl->device, CL_DEVICE_VERSION, 0, NULL, &stringLength);
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: clGetDeviceInfo() to get CL_DEVICE_VERSION length returned " << TranslateErrorCode(errorCode) << std::endl;
		return errorCode;
	}

	std::vector<char> deviceVersion(stringLength);
	errorCode = clGetDeviceInfo(ocl->device, CL_DEVICE_VERSION, stringLength, &deviceVersion[0], NULL);
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: clGetDeviceInfo() to get CL_DEVICE_VERSION returned " << TranslateErrorCode(errorCode) << std::endl;
		return errorCode;
	}

	if (strstr(&deviceVersion[0], "OpenCL 2.0") != NULL) {
		ocl->deviceVersion = OPENCL_VERSION_2_0;
	}

	errorCode = clGetDeviceInfo(ocl->device, CL_DEVICE_OPENCL_C_VERSION, 0, NULL, &stringLength);
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: clGetDeviceInfo() to get CL_DEVICE_OPENCL_C_VERSION length returned " << TranslateErrorCode(errorCode) << std::endl;
		return errorCode;
	}

	std::vector<char> compilerVersion(stringLength);
	errorCode = clGetDeviceInfo(ocl->device, CL_DEVICE_OPENCL_C_VERSION, stringLength, &compilerVersion[0], NULL);
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: clGetDeviceInfo() to get CL_DEVICE_OPENCL_C_VERSION returned " << TranslateErrorCode(errorCode) << std::endl;
		return errorCode;
	}
	else if (strstr(&compilerVersion[0], "OpenCL C 2.0") != NULL) {
		ocl->compilerVersion = OPENCL_VERSION_2_0;
	}
	return errorCode;
}

cl_int SetupOpenCL(opencl_arguments *ocl, cl_device_type deviceType, std::string platformName) {
	cl_int errorCode = CL_SUCCESS;
	cl_platform_id platformID = FindOpenCLPlatform( deviceType,platformName);
	if (platformID == NULL) {
		std::cout << "Error: Failed to find OpenCL platform." << std::endl;
		return CL_INVALID_VALUE;
	}

	cl_context_properties contextProperties[] = { CL_CONTEXT_PLATFORM, (cl_context_properties)platformID, 0 };
	ocl->context = clCreateContextFromType(contextProperties, deviceType, NULL, NULL, &errorCode);
	if ((errorCode != CL_SUCCESS) || (NULL == ocl->context)) {
		std::cout << "Couldn't create a context, clCreateContextFromType() returned " << TranslateErrorCode(errorCode) << std::endl;
		return errorCode;
	}

	errorCode = clGetContextInfo(ocl->context, CL_CONTEXT_DEVICES, sizeof(cl_device_id), &ocl->device, NULL);
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: clGetContextInfo() to get list of devices returned " << TranslateErrorCode(errorCode) << std::endl;
		return errorCode;
	}

	GetPlatformAndDeviceVersion(platformID, ocl);

#ifdef CL_VERSION_2_0
	if (OPENCL_VERSION_2_0 == ocl->deviceVersion) {
		const cl_command_queue_properties properties[] = { CL_QUEUE_PROPERTIES, CL_QUEUE_PROFILING_ENABLE, 0 };
		ocl->commandQueue = clCreateCommandQueueWithProperties(ocl->context, ocl->device, properties, &errorCode);
	}
	else {
		cl_command_queue_properties properties = CL_QUEUE_PROFILING_ENABLE;
		ocl->commandQueue = clCreateCommandQueue(ocl->context, ocl->device, properties, &errorCode);
	}
#else
	cl_command_queue_properties properties = CL_QUEUE_PROFILING_ENABLE;
	ocl->commandQueue = clCreateCommandQueue(ocl->context, ocl->device, properties, &errorCode);
#endif
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: clCreateCommandQueue() returned " << TranslateErrorCode(errorCode) << std::endl;
	}
	return errorCode;
}

cl_int CreateAndBuildProgram(opencl_arguments *ocl, std::string fileName) {
	cl_int errorCode = CL_SUCCESS;
	char* source = NULL;
	size_t source_size = 0;
	errorCode = ReadSourceFromFile(fileName.c_str(), &source, &source_size);
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: ReadSourceFromFile returned " << TranslateErrorCode(errorCode) << std::endl;
		delete[] source;
		return errorCode;
	}

	ocl->program = clCreateProgramWithSource(ocl->context, 1, (const char**)&source, &source_size, &errorCode);
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: clCreateProgramWithSource returned " << TranslateErrorCode(errorCode) << std::endl;
		delete[] source;
		return errorCode;
	}

	errorCode = clBuildProgram(ocl->program, 1, &ocl->device, "", NULL, NULL);
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: clBuildProgram() for source program returned " << TranslateErrorCode(errorCode) << std::endl;

	}

	return errorCode;
}

cl_int ExecuteKernel(opencl_arguments *ocl, int index,cl_event *event) {
	cl_int errorCode = CL_SUCCESS;
	if (ocl->localWorkSize[0] == NULL) {
		errorCode = clEnqueueNDRangeKernel(ocl->commandQueue, ocl->kernels[index], ocl->work_Dim, NULL, ocl->globalWorkSize, NULL, 0, NULL, event);
	}
	else {
		errorCode = clEnqueueNDRangeKernel(ocl->commandQueue, ocl->kernels[index], ocl->work_Dim, NULL, ocl->globalWorkSize, ocl->localWorkSize, 0, NULL, event);
	}

	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: Failed to run the kernel, clEnqueueNDRangeKernel call returned " << TranslateErrorCode(errorCode) << std::endl;
		//return errorCode;
	}

	/*errorCode = clFinish(ocl->commandQueue);
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: clFinish returned " << TranslateErrorCode(errorCode) << std::endl;
	}*/
	return errorCode;
}

bool printClDevices() {
	cl_uint numPlatforms = 0;
	cl_int errorCode = CL_SUCCESS;
	int devSerialNum = 0;

	errorCode = clGetPlatformIDs(0, NULL, &numPlatforms);
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: clGetplatform_ids() to get num platforms returned " << TranslateErrorCode(errorCode) << std::endl;
		return false;
	}

	if (numPlatforms == 0) {
		std::cout << "Error: No platforms found!" << std::endl;
		return false;
	}

	std::vector<cl_platform_id> platforms(numPlatforms);
	errorCode = clGetPlatformIDs(numPlatforms, &platforms[0], NULL);
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: clGetplatform_ids() to get platforms returned " << TranslateErrorCode(errorCode) << std::endl;
		return false;
	}

	for (cl_uint i = 0; i < numPlatforms; ++i) {
		cl_uint numDevices = 0;
		size_t stringLength = 0;

		errorCode = clGetPlatformInfo(platforms[i], CL_PLATFORM_NAME, 0, NULL, &stringLength);
		if (errorCode != CL_SUCCESS) {
			std::cout << "Error: clGetPlatformInfo() to get CL_PLATFORM_NAME length returned " << TranslateErrorCode(errorCode) << std::endl;
			return false;
		}

		std::vector<char> platformName(stringLength);
		errorCode = clGetPlatformInfo(platforms[i], CL_PLATFORM_NAME, stringLength, &platformName[0], NULL);
		if (errorCode != CL_SUCCESS) {
			std::cout << "Error: clGetplatform_ids() to get CL_PLATFORM_NAME returned " << TranslateErrorCode(errorCode) << std::endl;
			return false;
		}

		errorCode = clGetDeviceIDs(platforms[i], CL_DEVICE_TYPE_ALL, 0, NULL, &numDevices);
		if (errorCode != CL_SUCCESS) {
			std::cout << "Error: clGetplatform_ids() to get num platforms returned " << TranslateErrorCode(errorCode) << std::endl;
			return false;
		}

		if (numDevices == 0) {
			std::cout << "Error: No devices found!" << std::endl;
			return false;
		}
		std::vector<cl_device_id> devices(numDevices);

		errorCode = clGetDeviceIDs(platforms[i], CL_DEVICE_TYPE_ALL, numDevices, &devices[0], NULL);
		if (errorCode != CL_SUCCESS) {
			std::cout << "Error: clGetDeviceIDs() to get platforms returned " << TranslateErrorCode(errorCode) << std::endl;
			return false;
		}

		for (int j = 0; j < numDevices; j++) {
			std::cout << "   Device Serial Number : " << ++devSerialNum;
			std::cout << "   DeviceID: " << devices[j] << ", ";
			size_t deviceStringLength = 0;

			errorCode = clGetDeviceInfo(devices[j], CL_DEVICE_NAME, 0, NULL, &deviceStringLength);
			if (errorCode != CL_SUCCESS) {
				std::cout << "Error: clGetDeviceInfo() to get device name length returned " << TranslateErrorCode(errorCode) << std::endl;
				return false;
			}
			std::vector<char> deviceName(deviceStringLength);
			errorCode = clGetDeviceInfo(devices[j], CL_DEVICE_NAME, deviceStringLength, &deviceName[0], NULL);
			if (errorCode != CL_SUCCESS) {
				std::cout << "Error: clGetDeviceInfo() to get device name returned " << TranslateErrorCode(errorCode) << std::endl;
				return false;
			}

			std::cout << "Device Name: " <<  &deviceName[0] << ", ";
			cl_device_type type;
			errorCode = clGetDeviceInfo(devices[j], CL_DEVICE_TYPE, sizeof(cl_device_type), &type, NULL);
			if (errorCode != CL_SUCCESS) {
				std::cout << "Error: clGetDeviceInfo() to get device type returned " << TranslateErrorCode(errorCode) << std::endl;
				return false;
			}
			printf("Device Type:%s, ", TranslateDeviceType(type));

			cl_uint max_compute_units;
			errorCode = clGetDeviceInfo(devices[j], CL_DEVICE_MAX_COMPUTE_UNITS, sizeof(cl_uint), &max_compute_units, NULL);
			if (errorCode != CL_SUCCESS) {
				std::cout << "Error: clGetDeviceInfo() to get device max compute units returned " << TranslateErrorCode(errorCode) << std::endl;
				return false;
			}
			std::cout << "Device Max Compute Units: " << max_compute_units << ", ";

			size_t max_work_group_size;
			errorCode = clGetDeviceInfo(devices[j], CL_DEVICE_MAX_WORK_GROUP_SIZE, sizeof(size_t), &max_work_group_size, NULL);
			if (errorCode != CL_SUCCESS) {
				std::cout << "Error: clGetDeviceInfo() to get device max work group size returned " << TranslateErrorCode(errorCode) << std::endl;
				return false;
			}
			std::cout << "Device Max Work Group Size:" <<  max_work_group_size << ", \n";

			cl_ulong max_global_mem;
			errorCode = clGetDeviceInfo(devices[j], CL_DEVICE_GLOBAL_MEM_SIZE, sizeof(cl_ulong), &max_global_mem, NULL);
			if (errorCode != CL_SUCCESS)  {
				std::cout << "Error: clGetDeviceInfo() to get device max global mem returned " << TranslateErrorCode(errorCode) << std::endl;
				return false;
			}
			std::cout << "	  Device Max Global Memory: " <<  max_global_mem << " Byte, ";

			cl_ulong max_global_mem_cache;
			errorCode = clGetDeviceInfo(devices[j], CL_DEVICE_GLOBAL_MEM_CACHE_SIZE, sizeof(cl_ulong), &max_global_mem_cache, NULL);
			if (errorCode != CL_SUCCESS) {
				std::cout << "Error: clGetDeviceInfo() to get device max global mem cache size returned " << TranslateErrorCode(errorCode) << std::endl;
				return false;
			}
			std::cout << "Device Max Global Memory Cache Size:" << max_global_mem_cache << " Byte, ";

			cl_uint max_global_mem_cacheline_size;
			errorCode = clGetDeviceInfo(devices[j], CL_DEVICE_GLOBAL_MEM_CACHELINE_SIZE, sizeof(cl_uint), &max_global_mem_cacheline_size, NULL);
			if (errorCode != CL_SUCCESS) {
				std::cout << "Error: clGetDeviceInfo() to get device max global mem cache size returned " << TranslateErrorCode(errorCode) << std::endl;
				return false;
			}
			std::cout << "Device Max Global Memory Cacheline Size:" << max_global_mem_cacheline_size << " Byte, ";

			cl_ulong max_local_mem;
			errorCode = clGetDeviceInfo(devices[j], CL_DEVICE_LOCAL_MEM_SIZE, sizeof(cl_ulong), &max_local_mem, NULL);
			if (errorCode != CL_SUCCESS) {
				std::cout << "Error: clGetDeviceInfo() to get device max local mem size returned " << TranslateErrorCode(errorCode) << std::endl;
				return false;
			}
			std::cout << "Device Max Local Memory Size:" << max_local_mem << " Byte, ";
			std::cout << std::endl << std::endl;
		}
	}
	return true;
}

bool printDeviceInformation(cl_device_id id) {
	printf("DeviceID:%d, ", id);
	cl_int errorCode = CL_SUCCESS;
	size_t deviceStringLength = 0;

	errorCode = clGetDeviceInfo(id, CL_DEVICE_NAME, 0, NULL, &deviceStringLength);
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: clGetDeviceInfo() to get device name length returned " << TranslateErrorCode(errorCode) << std::endl;
		return false;
	}
	std::vector<char> deviceName(deviceStringLength);
	errorCode = clGetDeviceInfo(id, CL_DEVICE_NAME, deviceStringLength, &deviceName[0], NULL);
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: clGetDeviceInfo() to get device name returned " << TranslateErrorCode(errorCode) << std::endl;
		return false;
	}
	std::cout << "Device Name:" << &deviceName[0] << ", ";

	cl_device_type type;
	errorCode = clGetDeviceInfo(id, CL_DEVICE_TYPE, sizeof(cl_device_type), &type, NULL);
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: clGetDeviceInfo() to get device type returned " << TranslateErrorCode(errorCode) << std::endl;
		return false;
	}
	std::cout << "Device Type:" << TranslateDeviceType(type) << ", ";

	cl_uint max_compute_units;
	errorCode = clGetDeviceInfo(id, CL_DEVICE_MAX_COMPUTE_UNITS, sizeof(cl_uint), &max_compute_units, NULL);
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: clGetDeviceInfo() to get device max compute units returned " << TranslateErrorCode(errorCode) << std::endl;
		return false;
	}
	std::cout << "Device Max Compute Units:" << max_compute_units << ", ";

	size_t max_work_group_size;
	errorCode = clGetDeviceInfo(id, CL_DEVICE_MAX_WORK_GROUP_SIZE, sizeof(size_t), &max_work_group_size, NULL);
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: clGetDeviceInfo() to get device max work group size returned " << TranslateErrorCode(errorCode) << std::endl;
		return false;
	}
	std::cout << "Device Max Work Group Size:" << max_work_group_size << ",\n";
	std::cout << std::endl << std::endl;
	return true;
}

cl_int SetupOpenCLByDeviceID(opencl_arguments *ocl, cl_device_id id) {
	cl_int errorCode = CL_SUCCESS;
	cl_device_id *deviceid = (cl_device_id*)malloc(sizeof(cl_device_id));
	*deviceid = id;
	cl_platform_id platformid;
	errorCode = clGetDeviceInfo(id, CL_DEVICE_PLATFORM, sizeof(cl_platform_id), &platformid, NULL);
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: clGetDeviceInfo() to get platform id returned " << TranslateErrorCode(errorCode) << std::endl;
		return errorCode;
	}

	cl_context_properties contextProperties[] = { CL_CONTEXT_PLATFORM, (cl_context_properties)platformid, 0 };
	ocl->context = clCreateContext(contextProperties, 1, deviceid, NULL, NULL, &errorCode);
	if ((errorCode != CL_SUCCESS) || (NULL == ocl->context)) {
		std::cout << "Couldn't create a context, clCreateContext() returned " << TranslateErrorCode(errorCode) << std::endl;
		return errorCode;
	}

	errorCode = clGetContextInfo(ocl->context, CL_CONTEXT_DEVICES, sizeof(cl_device_id), &ocl->device, NULL);
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: clGetContextInfo() to get list of devices returned " << TranslateErrorCode(errorCode) << std::endl;
		return errorCode;
	}

	GetPlatformAndDeviceVersion(platformid, ocl);

#ifdef CL_VERSION_2_0
	if (OPENCL_VERSION_2_0 == ocl->deviceVersion) {
		const cl_command_queue_properties properties[] = { CL_QUEUE_PROPERTIES, CL_QUEUE_PROFILING_ENABLE, 0 };
		ocl->commandQueue = clCreateCommandQueueWithProperties(ocl->context, ocl->device, properties, &errorCode);
	}
	else {
		cl_command_queue_properties properties = CL_QUEUE_PROFILING_ENABLE;
		ocl->commandQueue = clCreateCommandQueue(ocl->context, ocl->device, properties, &errorCode);
	}
#else
	cl_command_queue_properties properties = CL_QUEUE_PROFILING_ENABLE;
	ocl->commandQueue = clCreateCommandQueue(ocl->context, ocl->device, properties, &errorCode);
#endif
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: clCreateCommandQueue() returned " << TranslateErrorCode(errorCode) << std::endl;
	}
	return errorCode;
}

cl_device_id findDeviceID(int id) {
	cl_uint numPlatforms = 0;
	cl_int errorCode = CL_SUCCESS;

	errorCode = clGetPlatformIDs(0, NULL, &numPlatforms);
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: clGetplatform_ids() to get the number of platforms returned " << TranslateErrorCode(errorCode) << std::endl;
		return NULL;
	}

	if (numPlatforms == 0) {
		std::cout << "Error: No platforms found!" << std::endl;
		return NULL;
	}

	std::vector<cl_platform_id> platforms(numPlatforms);
	errorCode = clGetPlatformIDs(numPlatforms, &platforms[0], NULL);
	if (errorCode != CL_SUCCESS) {
		std::cout << "Error: clGetplatform_ids() to get platforms returned " << TranslateErrorCode(errorCode) << std::endl;
		return NULL;
	}

	for (int i = 0; i < numPlatforms; i++) {
		cl_uint numDevices = 0;

		errorCode = clGetDeviceIDs(platforms[i], CL_DEVICE_TYPE_ALL, 0, NULL, &numDevices);
		if (errorCode != CL_SUCCESS) {
			std::cout << "Error: clGetDeviceIDs() to get the number of devices returned " << TranslateErrorCode(errorCode) << std::endl;
			return NULL;
		}

		if (numDevices == 0) {
			std::cout << "Error: No devices found!" << std::endl;
			return NULL;
		}
		std::vector<cl_device_id> devices(numDevices);

		errorCode = clGetDeviceIDs(platforms[i], CL_DEVICE_TYPE_ALL, numDevices, &devices[0], NULL);
		if (errorCode != CL_SUCCESS ) {
			std::cout << "Error: clGetDeviceIDs() to get platforms returned " << TranslateErrorCode(errorCode) << std::endl;
			return NULL;
		}
		for (int j = 0; j < numDevices; ++j) {
			if (!--id) return devices[j];
		}
	}
	return NULL;
}