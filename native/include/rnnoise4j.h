#ifndef RNNOISE4J_RNNOISE4J_H
#define RNNOISE4J_RNNOISE4J_H

#include <rnnoise.h>

#include "macros.h"

typedef struct Denoiser {
    DenoiseState *state;
    RNNModel *model;
    uint8_t *model_buffer;
} Denoiser;

/**
 * Gets the denoiser from the denoiser java object.
 *
 * @param denoiser_pointer the pointer to the denoiser
 * @param error the error code
 * @return the decoder or NULL - If the denoiser could not be retrieved, this will throw a runtime exception in Java
 */
Denoiser *get_denoiser(int64_t denoiser_pointer, int *error);

EXPORT int64_t rnnoise4j_create_denoiser(uint8_t *model_buffer, int32_t model_length, int *error);

EXPORT int32_t rnnoise4j_get_frame_size();

EXPORT int rnnoise4j_denoise(int64_t denoiser_pointer, const int16_t *pcm_input, int32_t input_length, int16_t *pcm_output);

EXPORT float rnnoise4j_denoise_in_place(bool denoise, int64_t denoiser_pointer, int16_t *input, int32_t input_length,
                                        int *error);

EXPORT void rnnoise4j_destroy_denoiser_buffer(int64_t denoiser_pointer, bool free_model_buffer);

EXPORT void rnnoise4j_destroy_denoiser(int64_t denoiser_pointer);

#endif
