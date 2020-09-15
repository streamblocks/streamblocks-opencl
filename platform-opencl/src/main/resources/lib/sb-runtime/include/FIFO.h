#pragma once

#include <string>
#include <algorithm>
#include <iostream>

template<int N>
struct FIFO_State {
    int read_index{0};
    int write_index{0};
    int max_size{N};
    bool full{false};
};


template<typename T, int N>
class FIFO {
    FIFO_State<N> state;
    T *data_ptr = new T[N];

public:
    FIFO();

    int get_size() const;

    T get_element();

    void put_element(T element);

    int get_free_space() const;

    void get_elements(T *elements, int elements_count);

    void put_elements(T *elements, int element_count);

    T element_preview(int offset = 0);

    void elements_preview(T *elements, int elements_count);

    void consume(int elements_count);

    T *get_data_ptr() const;

    int get_write_index() const;

    int get_read_index() const;

    void notify_read_done(int read_elements);

    void notify_write_done(int written_elements);
};


template<typename T, int N>
FIFO<T, N>::FIFO() {}

template<typename T, int N>
int FIFO<T, N>::get_size() const {
    if (state.full) return N;
    int s = (N + (state.write_index - state.read_index) % N) % N;
    return s;
};

template<typename T, int N>
T FIFO<T, N>::get_element() {
    T element = data_ptr[state.read_index++];
    if (state.read_index == N)state.read_index = 0;
    if (state.full && state.read_index != state.write_index)state.full = false;
    return element;
}

template<typename T, int N>
void FIFO<T, N>::put_element(T element) {
    data_ptr[state.write_index++] = element;
    if (state.write_index == N)state.write_index = 0;
    if (state.read_index == state.write_index) state.full = true;
}

template<typename T, int N>
int FIFO<T, N>::get_free_space() const {
    return N - get_size();
}

template<typename T, int N>
void FIFO<T, N>::get_elements(T *elements, int elements_count) {
    for (int i = 0; i < elements_count; i++) {
        elements[i] = get_element();
    }
}

template<typename T, int N>
void FIFO<T, N>::consume(int elements_count) {
    for (int i = 0; i < elements_count; i++) {
        get_element();
    }
}

template<typename T, int N>
void FIFO<T, N>::put_elements(T *elements, int elements_count) {
    for (int i = 0; i < elements_count; i++) {
        put_element(elements[i]);
    }
}

template<typename T, int N>
T FIFO<T, N>::element_preview(int offset) {
    int index = (state.read_index + offset) % N;
    return data_ptr[index];
}

template<typename T, int N>
void FIFO<T, N>::elements_preview(T *elements, int elements_count) {
    for (int i = 0; i < elements_count; i++) {
        elements[i] = element_preview(i);
    }
}

template<typename T, int N>
T *FIFO<T, N>::get_data_ptr() const {
    return data_ptr;
}

template<typename T, int N>
int FIFO<T, N>::get_write_index() const {
    return state.write_index;
}

template<typename T, int N>
int FIFO<T, N>::get_read_index() const {
    return state.read_index;
}

template<typename T, int N>
void FIFO<T, N>::notify_read_done(int read_elements) {
    state.read_index = (state.read_index + read_elements) % N;
    if (state.full && state.read_index != state.write_index)state.full = false;
}

template<typename T, int N>
void FIFO<T, N>::notify_write_done(int written_elements) {
    state.write_index = (state.write_index + written_elements) % N;
    if (state.write_index == state.read_index) {
        state.full = true;
    }
}
