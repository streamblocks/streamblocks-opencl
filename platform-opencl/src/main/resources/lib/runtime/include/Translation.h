#pragma once

#include <string>
#include <CL/cl.hpp>

std::string TranslateErrorCode(cl_int errorCode);

std::string TranslateDeviceType(cl_device_type deviceType);

std::string TranslateMemFlag(cl_mem_flags flag);