#ifndef EXCEPTIONS_H
#define EXCEPTIONS_H

#include <jni.h>

#define RNNOISE_NO_ERROR 0
#define RNNOISE_ERROR_COULD_NOT_LOAD_MODEL 1001
#define RNNOISE_ERROR_COULD_NOT_CREATE_DENOISER 1002
#define RNNOISE_ERROR_DENOISER_CLOSED 1003
#define RNNOISE_ERROR_INPUT_ARRAY_NULL 1004
#define RNNOISE_ERROR_INPUT_ARRAY_EMPTY 1005
#define RNNOISE_ERROR_INPUT_ARRAY_NOT_MULTIPLE_OF_FRAME_SIZE 1006

#define throw_error(errcode, env) \
    switch (errcode) { \
        case RNNOISE_ERROR_COULD_NOT_LOAD_MODEL: \
            throw_io_exception(env, "Could not load model"); \
            break; \
        case RNNOISE_ERROR_COULD_NOT_CREATE_DENOISER: \
            throw_io_exception(env, "Could not create denoiser"); \
            break; \
        case RNNOISE_ERROR_DENOISER_CLOSED: \
            throw_runtime_exception(env, "Denoiser is closed"); \
            break; \
        case RNNOISE_ERROR_INPUT_ARRAY_NULL: \
            throw_illegal_argument_exception(env, "Input array is null"); \
            break; \
        case RNNOISE_ERROR_INPUT_ARRAY_EMPTY: \
            throw_illegal_argument_exception(env, "Input array is empty"); \
            break; \
        case RNNOISE_ERROR_INPUT_ARRAY_NOT_MULTIPLE_OF_FRAME_SIZE: \
            throw_illegal_argument_exception(env, "Input array is not a multiple of the frame size"); \
            break; \
        default: \
            throw_runtime_exception(env, "Unknown error"); \
            break; \
    }


char *string_format(const char *fmt, ...);

void throw_runtime_exception(JNIEnv *env, const char *message);

void throw_illegal_state_exception(JNIEnv *env, const char *message);

void throw_io_exception(JNIEnv *env, const char *message);

void throw_illegal_argument_exception(JNIEnv *env, const char *message);

#endif
