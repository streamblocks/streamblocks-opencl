cmake_minimum_required(VERSION 3.10)

project(sb-lib)

find_package(Threads QUIET)
if(THREADS_FOUND)
    list(APPEND extra_libraries ${CMAKE_THREAD_LIBS_INIT})
    list(APPEND extra_definitions -DTHREADS_ENABLE)
else()
    message(STATUS "Cannot find Threads")
endif()

find_package(OpenCL QUIET)
if(OpenCL_FOUND)
    list(APPEND extra_libraries ${OpenCL_LIBRARIES})
    list(APPEND extra_includes ${OpenCL_INCLUDE_DIRS})
else()
    message(STATUS "Cannot find Threads")
endif()


include_directories(sb-runtime/include)
include_directories(sb-native/include)

add_subdirectory(sb-runtime)
add_subdirectory(sb-native)

set(extra_definitions ${extra_definitions} PARENT_SCOPE)
set(extra_includes ${extra_includes} PARENT_SCOPE)
set(extra_libraries ${extra_libraries} PARENT_SCOPE)