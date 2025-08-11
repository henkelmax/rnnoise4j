cmake_minimum_required(VERSION 3.12)

project(rnnoise4j LANGUAGES C)

set(JDK_8_VERSION "943a5ea328fd2fc8eed0aed4ec9b1957d41f8144") # jdk8u462-ga
set(JNI_HEADER_HASH "ed99792df48670072b78028faf704a8dcb6868fe140ccc7eced9b01dfa62fef4")
set(RNNOISE_VERSION "372f7b4b76cde4ca1ec4605353dd17898a99de38") # one commit after v0.2 to fix cross compilation
set(RNNOISE_DATA_FILENAME "0b50c45")
set(RNNOISE_DATA_HASH "4ac81c5c0884ec4bd5907026aaae16209b7b76cd9d7f71af582094a2f98f4b43")

if (WIN32)
    set(JNI_MD_URL "https://raw.githubusercontent.com/openjdk/jdk8u/${JDK_8_VERSION}/jdk/src/windows/javavm/export/jni_md.h")
elseif (APPLE)
    set(JNI_MD_URL "https://raw.githubusercontent.com/openjdk/jdk8u/${JDK_8_VERSION}/jdk/src/macosx/javavm/export/jni_md.h")
elseif (UNIX)
    set(JNI_MD_URL "https://raw.githubusercontent.com/openjdk/jdk8u/${JDK_8_VERSION}/jdk/src/solaris/javavm/export/jni_md.h")
else ()
    message(WARNING "Unknown OS, not compiling jni_md.h")
endif ()

file(DOWNLOAD
        "https://raw.githubusercontent.com/openjdk/jdk8u/${JDK_8_VERSION}/jdk/src/share/javavm/export/jni.h"
        ${CMAKE_BINARY_DIR}/jni_headers/jni.h
        EXPECTED_HASH SHA256=${JNI_HEADER_HASH}
)

if (DEFINED JNI_MD_URL)
    file(DOWNLOAD
            ${JNI_MD_URL}
            ${CMAKE_BINARY_DIR}/jni_headers/jni_md.h
    )
endif ()

set(RNNOISE_DATA_ARCHIVE "${CMAKE_BINARY_DIR}/rnnoise_data.tar.gz")
set(RNNOISE_DATA "${CMAKE_BINARY_DIR}/rnnoise_data")

file(DOWNLOAD
        "https://media.xiph.org/rnnoise/models/rnnoise_data-${RNNOISE_DATA_FILENAME}.tar.gz"
        ${RNNOISE_DATA_ARCHIVE}
        EXPECTED_HASH SHA256=${RNNOISE_DATA_HASH}
)
file(ARCHIVE_EXTRACT
        INPUT ${RNNOISE_DATA_ARCHIVE}
        DESTINATION ${RNNOISE_DATA}
)

file(READ "${RNNOISE_DATA}/src/rnnoise_data_little.c" MODEL_DATA_STRING)

string(REGEX REPLACE
        "=[ \t\r\n]*{[ \t\r\n\-0-9.,e]*};"
        "= {};"
        MODEL_DATA_STRING_CLEANED "${MODEL_DATA_STRING}"
)

file(WRITE "${RNNOISE_DATA}/src/rnnoise_data_little_dummy.c" "${MODEL_DATA_STRING_CLEANED}")

include(FetchContent)
FetchContent_Declare(
        rnnoise
        GIT_REPOSITORY https://github.com/xiph/rnnoise.git
        GIT_TAG ${RNNOISE_VERSION}
        PATCH_COMMAND ${CMAKE_COMMAND} -E copy ${CMAKE_CURRENT_LIST_DIR}/cmake/rnnoise/CMakeLists.txt ${CMAKE_CURRENT_BINARY_DIR}/_deps/rnnoise-src/CMakeLists.txt && ${CMAKE_COMMAND} -E copy ${RNNOISE_DATA}/src/rnnoise_data_little.h ${CMAKE_CURRENT_BINARY_DIR}/_deps/rnnoise-src/src/rnnoise_data.h && ${CMAKE_COMMAND} -E copy ${RNNOISE_DATA}/src/rnnoise_data_little_dummy.c ${CMAKE_CURRENT_BINARY_DIR}/_deps/rnnoise-src/src/rnnoise_data.c
)
FetchContent_MakeAvailable(rnnoise)

add_library(rnnoise4j SHARED
        src/rnnoise.c
        src/exceptions.c
)

target_include_directories(rnnoise4j PRIVATE
        ${CMAKE_SOURCE_DIR}/include
        ${JNI_INCLUDE_DIRS}
        ${CMAKE_BINARY_DIR}/jni_headers
        ${rnnoise_SOURCE_DIR}/include
        ${rnnoise_SOURCE_DIR}/src
)

target_link_libraries(rnnoise4j PRIVATE
        rnnoise-static
)

message(STATUS "C compiler executable: ${CMAKE_C_COMPILER}")
