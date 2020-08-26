# OpenCL Runtime sources
set(runtime_sources
        src/opencl_arguments.cpp
        src/Translation.cpp
        src/utils.cpp
        )

# OpenCL Runtime includes
set(runtime_header
        include/Actor.h
        include/prelude.h
        include/Scheduling.h
        include/FIFO.h
        include/Port.h
        include/CLI11.h
        include/opencl_arguments.h
        include/Translation.h
        include/utils.h
        include/cl.h)

include_directories(${extra_includes})
add_definitions(${extra_definitions})

add_library(cl-runtime STATIC ${runtime_sources} ${runtime_header})

target_link_libraries(cl-runtime ${extra_libraries})

install(TARGETS cl-runtime
        ARCHIVE DESTINATION lib)

set(extra_definitions ${extra_definitions} PARENT_SCOPE)
set(extra_includes ${extra_includes} PARENT_SCOPE)
set(extra_libraries ${extra_libraries} PARENT_SCOPE)