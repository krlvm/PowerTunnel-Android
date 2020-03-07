cmake_minimum_required(VERSION 3.4.1)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp)
set(EXECUTABLE_OUTPUT_PATH "${CMAKE_CURRENT_SOURCE_DIR}/src/main/assets/${ANDROID_ABI}")

add_library( # Sets the name of the library.
        tun2http

        # Sets the library as a shared library.
        SHARED

        src/main/cpp/dhcp.c
        src/main/cpp/dns.c
        src/main/cpp/icmp.c
        src/main/cpp/ip.c
        src/main/cpp/http.c
        src/main/cpp/tun2http.c
        src/main/cpp/session.c
        src/main/cpp/tcp.c
        src/main/cpp/tls.c
        src/main/cpp/udp.c
        src/main/cpp/util.c
        )

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

target_link_libraries( # Specifies the target library.
        tun2http
        ${log-lib}
        )