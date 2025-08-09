#ifndef EXCEPTIONS_H
#define EXCEPTIONS_H

char *string_format(const char *fmt, ...);

void throw_runtime_exception(JNIEnv *env, const char *message);

void throw_illegal_state_exception(JNIEnv *env, const char *message);

void throw_io_exception(JNIEnv *env, const char *message);

void throw_illegal_argument_exception(JNIEnv *env, const char *message);

#endif
