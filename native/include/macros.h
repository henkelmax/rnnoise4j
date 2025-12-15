#ifndef RNNOISE4J_RNNOISE_H
#define RNNOISE4J_RNNOISE_H

#if defined(_WIN32)
  #define EXPORT __declspec(dllexport)
#elif defined(__GNUC__) || defined(__clang__)
  #define EXPORT __attribute__((visibility("default")))
#else
  #define EXPORT
#endif

#endif
