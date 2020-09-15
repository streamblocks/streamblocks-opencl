#pragma once

#include <vector>
#include <algorithm>
#include <initializer_list>
#include <functional>
#include "FIFO.h"
#include "cl.h"
#include "utils.h"

using namespace std;

template<typename T, int N>
class Port {
private:
    std::vector<std::reference_wrapper<FIFO<T, N>>> connected_FIFOs;
    cl_mem read_buffer;
    cl_mem write_buffer;
    int write_offset{0};

public:
    Port(std::initializer_list<std::reference_wrapper<FIFO<T, N>>> init_list);

    int get_size() const;

    T get_element();

    void put_element(T element);

    int get_free_space() const;

    void get_elements(T *elements, int elements_count);

    void put_elements(T *elements, int element_count);

    T element_preview(int offset = 0);

    void elements_preview(T *elements, int elements_count);

    void consume(int element_count);

    cl_mem *get_read_buffer(const int number_elements, opencl_arguments &ocl);

    cl_mem *get_write_buffer(const int number_elements, opencl_arguments &ocl);

    void opencl_read_done();

    void opencl_write_done(opencl_arguments &ocl);
};

template<typename T, int N>
Port<T, N>::Port(std::initializer_list<std::reference_wrapper<FIFO<T, N>>> init_list) : connected_FIFOs(init_list) {
}

template<typename T, int N>
int Port<T, N>::get_size() const {
    return connected_FIFOs.front().get().get_size();
};

template<typename T, int N>
T Port<T, N>::get_element() {
    return connected_FIFOs.front().get().get_element();
}

template<typename T, int N>
void Port<T, N>::put_element(T element) {
    for (auto it = connected_FIFOs.begin(); it != connected_FIFOs.end(); ++it) {
        it->get().put_element(element);
    }
}

template<typename T, int N>
int Port<T, N>::get_free_space() const {
    int size{500000000}; //dummy number
    for (auto it = connected_FIFOs.begin(); it != connected_FIFOs.end(); ++it) {
        size = size < it->get().get_free_space() ? size : it->get().get_free_space();
    }
    return size;
}

template<typename T, int N>
void Port<T, N>::get_elements(T *elements, int elements_count) {
    connected_FIFOs.front().get().get_elements(elements, elements_count);
}

template<typename T, int N>
void Port<T, N>::put_elements(T *elements, int elements_count) {
    for (auto it = connected_FIFOs.begin(); it != connected_FIFOs.end(); ++it) {
        it->get().put_elements(elements, elements_count);
    }
}

template<typename T, int N>
T Port<T, N>::element_preview(int offset) {
    return connected_FIFOs.front().get().element_preview(offset);
}

template<typename T, int N>
void Port<T, N>::elements_preview(T *elements, int elements_count) {
    connected_FIFOs.front().get().elements_preview(elements, elements_count);
}

template<typename T, int N>
void Port<T, N>::consume(int elements_count ) {
    return connected_FIFOs.front().get().consume(elements_count);
}

template<typename T, int N>
cl_mem *Port<T, N>::get_read_buffer(const int number_elements, opencl_arguments &ocl) {
    cl_int err = CL_SUCCESS;
    read_buffer = clCreateBuffer(ocl.context, CL_MEM_READ_WRITE /*| CL_MEM_ALLOC_HOST_PTR*/,
                                 sizeof(T) * number_elements, NULL, &err);
    if (CL_SUCCESS != err) {
        printf("Error: clCreateBuffer for inputBuffer returned %s\n", TranslateErrorCode(err));
        exit(err);
    }
    T *resultPtr = (T *) clEnqueueMapBuffer(ocl.commandQueue, read_buffer, true, CL_MAP_WRITE, 0,
                                            sizeof(T) * number_elements, 0, NULL, NULL, &err);
    if (CL_SUCCESS != err) {
        printf("Error:(get_read_buffer) clEnqueueMapBuffer returned %s\n", TranslateErrorCode(err));
        exit(err);
    }
    err = clFinish(ocl.commandQueue);
    if (CL_SUCCESS != err) {
        printf("Error: clFinish returned %s during the creation of the initial buffer\n", TranslateErrorCode(err));
        exit(err);
    }
    FIFO<T, N> &attached_read_fifo = connected_FIFOs.front().get();
    if (attached_read_fifo.get_read_index() + number_elements > N) {
        int read_before_cycle = N - attached_read_fifo.get_read_index();
        int read_after_cycle = number_elements - read_before_cycle;
        memcpy(resultPtr, attached_read_fifo.get_data_ptr() + attached_read_fifo.get_read_index(),
               sizeof(T) * read_before_cycle);
        memcpy(resultPtr + read_before_cycle, attached_read_fifo.get_data_ptr(), sizeof(T) * read_after_cycle);
    } else {
        memcpy(resultPtr, attached_read_fifo.get_data_ptr() + attached_read_fifo.get_read_index(),
               sizeof(T) * number_elements);
    }
    attached_read_fifo.notify_read_done(number_elements);
    err = clEnqueueUnmapMemObject(ocl.commandQueue, read_buffer, resultPtr, 0, NULL, NULL);
    if (CL_SUCCESS != err) {
        printf("Error: clEnqueueUnmapMemObject returned %s during the creation of the initial input buffer\n",
               TranslateErrorCode(err));
        exit(err);
    }
    return &read_buffer;
}

template<typename T, int N>
cl_mem *Port<T, N>::get_write_buffer(const int number_elements, opencl_arguments &ocl) {
    cl_int err = CL_SUCCESS;
    write_buffer = clCreateBuffer(ocl.context, CL_MEM_READ_WRITE /*| CL_MEM_ALLOC_HOST_PTR*/,
                                  sizeof(T) * number_elements, NULL, &err);
    if (CL_SUCCESS != err) {
        printf("Error: clCreateBuffer for outputBuffer returned %s\n", TranslateErrorCode(err));
        exit(err);
    }
    write_offset = number_elements;
    return &write_buffer;
}

template<typename T, int N>
void Port<T, N>::opencl_read_done() {
    if (read_buffer == NULL)
        return;
    cl_int err = CL_SUCCESS;
    err = clReleaseMemObject(read_buffer);
    if (CL_SUCCESS != err) {
        printf("Error: clReleaseMemObject returned %s\n", TranslateErrorCode(err));
        exit(err);
    }
    read_buffer = NULL;
}

template<typename T, int N>
void Port<T, N>::opencl_write_done(opencl_arguments &ocl) {
    if (write_buffer == NULL)
        return;
    cl_int err = CL_SUCCESS;
    T *resultPtr2 = (T *) clEnqueueMapBuffer(ocl.commandQueue, write_buffer, true, CL_MAP_READ, 0,
                                             sizeof(T) * write_offset, 0, NULL, NULL, &err);
    if (CL_SUCCESS != err) {
        printf("Error:(opencl_write_done) clEnqueueMapBuffer returned %s\n", TranslateErrorCode(err));
        exit(err);
    }
    err = clFinish(ocl.commandQueue);
    if (CL_SUCCESS != err) {
        printf("Error: clFinish returned %s during reading results\n", TranslateErrorCode(err));
        exit(err);
    }
    for (auto it = connected_FIFOs.begin(); it != connected_FIFOs.end(); ++it) {
        if (it->get().get_write_index() + write_offset > N) {
            int write_before_cycle = N - it->get().get_write_index();
            int write_after_cycle = write_offset - write_before_cycle;
            memcpy(it->get().get_data_ptr() + it->get().get_write_index(), resultPtr2, sizeof(T) * write_before_cycle);
            memcpy(it->get().get_data_ptr(), resultPtr2 + write_before_cycle, sizeof(T) * write_after_cycle);
        } else {
            memcpy(it->get().get_data_ptr() + it->get().get_write_index(), resultPtr2, sizeof(T) * write_offset);
        }
        it->get().notify_write_done(write_offset);
    }
    if (CL_SUCCESS != clReleaseMemObject(write_buffer)) {
        printf("Error: clReleaseMemObject returned %s\n", TranslateErrorCode(err));
        exit(err);
    }
    write_buffer = NULL;
}
