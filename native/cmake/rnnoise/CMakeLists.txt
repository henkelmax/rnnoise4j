cmake_minimum_required(VERSION 3.15)
project(rnnoise LANGUAGES C)

set(CMAKE_C_STANDARD 99)
set(CMAKE_C_STANDARD_REQUIRED ON)

file(GLOB RNNOISE_SRC CONFIGURE_DEPENDS
        "${CMAKE_CURRENT_SOURCE_DIR}/src/*.c"
)

add_library(rnnoise-static STATIC ${RNNOISE_SRC})
set_target_properties(rnnoise-static PROPERTIES
        OUTPUT_NAME rnnoise
        POSITION_INDEPENDENT_CODE ON
)

target_include_directories(rnnoise-static PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/include
        ${CMAKE_CURRENT_SOURCE_DIR}/src
)
