cmake_minimum_required(VERSION 3.22.1)
project(honda_serial C)
add_library(honda_serial SHARED direct_serial.c)
target_compile_options(honda_serial PRIVATE -Wall -Wextra -Werror)
