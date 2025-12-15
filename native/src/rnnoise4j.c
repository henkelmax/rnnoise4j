#include <math.h>
#include <stdbool.h>
#include <stdlib.h>
#include <stdint.h>
#include <rnnoise.h>
#include <rnnoise4j.h>

#include "java_exceptions.h"

Denoiser *get_denoiser(const int64_t denoiser_pointer, int *error) {
    if (denoiser_pointer == 0) {
        *error = RNNOISE_ERROR_DENOISER_CLOSED;
        return NULL;
    }
    return (Denoiser *) (uintptr_t) denoiser_pointer;
}

int64_t rnnoise4j_create_denoiser(uint8_t *model_buffer, const int32_t model_length, int *error) {
    RNNModel *rnn_model = rnnoise_model_from_buffer(model_buffer, model_length);
    if (rnn_model == NULL) {
        *error = RNNOISE_ERROR_COULD_NOT_LOAD_MODEL;
        return 0LL;
    }

    DenoiseState *rnnoise = rnnoise_create(rnn_model);
    if (rnnoise == NULL) {
        rnnoise_model_free(rnn_model);
        *error = RNNOISE_ERROR_COULD_NOT_CREATE_DENOISER;
        return 0LL;
    }
    Denoiser *denoiser = malloc(sizeof(Denoiser));
    denoiser->model = rnn_model;
    denoiser->state = rnnoise;
    denoiser->model_buffer = model_buffer;
    return (int64_t) (uintptr_t) denoiser;
}

int32_t rnnoise4j_get_frame_size() {
    return rnnoise_get_frame_size();
}

int16_t *rnnoise4j_denoise(const int64_t denoiser_pointer, const int16_t *pcm_input,
                           const int32_t input_length, int *error) {
    const Denoiser *denoiser = get_denoiser(denoiser_pointer, error);
    if (denoiser == NULL) {
        return 0;
    }
    if (pcm_input == NULL) {
        *error = RNNOISE_ERROR_INPUT_ARRAY_NULL;
        return 0;
    }
    if (input_length <= 0) {
        *error = RNNOISE_ERROR_INPUT_ARRAY_EMPTY;
        return 0;
    }
    if (input_length % rnnoise_get_frame_size() != 0) {
        *error = RNNOISE_ERROR_INPUT_ARRAY_NOT_MULTIPLE_OF_FRAME_SIZE;
        return 0;
    }
    const int frame_size = rnnoise_get_frame_size();

    float *input_buffer = calloc(frame_size, sizeof(float));
    float *output_buffer = calloc(frame_size, sizeof(float));
    int16_t *output_pcm_buffer = calloc(frame_size, sizeof(int16_t));

    const int frame_count = input_length / frame_size;
    for (int i = 0; i < frame_count; i++) {
        for (int frame_index = 0; frame_index < frame_size; frame_index++) {
            input_buffer[frame_index] = (float) pcm_input[i * frame_size + frame_index];
        }
        rnnoise_process_frame(denoiser->state, output_buffer, input_buffer);
        for (int frame_index = 0; frame_index < frame_size; frame_index++) {
            const float sample = output_buffer[frame_index];
            if (sample >= 32767.0F) {
                output_pcm_buffer[frame_index] = 32767;
                continue;
            }
            if (sample <= -32768.0F) {
                output_pcm_buffer[frame_index] = -32768;
                continue;
            }
            output_pcm_buffer[frame_index] = (int16_t) lrintf(sample);
        }
    }

    free(input_buffer);
    free(output_buffer);
    return output_pcm_buffer;
}

float rnnoise4j_denoise_in_place(
    const bool denoise,
    const int64_t denoiser_pointer,
    int16_t *input,
    const int32_t input_length,
    int *error
) {
    const Denoiser *denoiser = get_denoiser(denoiser_pointer, error);
    if (denoiser == NULL) {
        return 0.0F;
    }

    if (input == NULL) {
        *error = RNNOISE_ERROR_INPUT_ARRAY_NULL;
        return 0.0F;
    }
    if (input_length <= 0) {
        *error = RNNOISE_ERROR_INPUT_ARRAY_EMPTY;
        return 0.0F;
    }

    const int frame_size = rnnoise_get_frame_size();
    if (input_length % frame_size != 0) {
        *error = RNNOISE_ERROR_INPUT_ARRAY_NOT_MULTIPLE_OF_FRAME_SIZE;
        return 0.0F;
    }

    float *input_buffer = calloc(frame_size, sizeof(float));
    float *output_buffer = calloc(frame_size, sizeof(float));

    const int frame_count = input_length / frame_size;
    float total_speech_probability = 0.0F;
    for (int i = 0; i < frame_count; i++) {
        for (int frame_index = 0; frame_index < frame_size; frame_index++) {
            input_buffer[frame_index] = (float) input[i * frame_size + frame_index];
        }
        const float speech_probability = rnnoise_process_frame(denoiser->state, output_buffer, input_buffer);
        if (denoise) {
            for (int frame_index = 0; frame_index < frame_size; frame_index++) {
                const float sample = output_buffer[frame_index];
                if (sample >= 32767.0F) {
                    input[i * frame_size + frame_index] = 32767;
                    continue;
                }
                if (sample <= -32768.0F) {
                    input[i * frame_size + frame_index] = -32768;
                    continue;
                }
                input[i * frame_size + frame_index] = (int16_t) lrintf(sample);
            }
        }
        if (speech_probability > total_speech_probability) {
            total_speech_probability = speech_probability;
        }
    }

    free(input_buffer);
    free(output_buffer);

    return total_speech_probability;
}

void rnnoise4j_destroy_denoiser_buffer(
    const int64_t denoiser_pointer,
    const bool free_model_buffer
) {
    if (denoiser_pointer == 0) {
        return;
    }
    Denoiser *denoiser = (Denoiser *) (uintptr_t) denoiser_pointer;
    rnnoise_destroy(denoiser->state);
    // rnnoise_model_free(denoiser->model); somehow causes EXCEPTION_ACCESS_VIOLATION, so we manually free it
    free(denoiser->model);
    if (free_model_buffer) {
        free(denoiser->model_buffer);
    }
    denoiser->state = NULL;
    denoiser->model = NULL;
    if (free_model_buffer) {
        denoiser->model_buffer = NULL;
    }
    free(denoiser);
}

void rnnoise4j_destroy_denoiser(
    const int64_t denoiser_pointer
) {
    // Don't free the model buffer as we keep it in Java memory
    rnnoise4j_destroy_denoiser_buffer(denoiser_pointer, false);
}

void rnnoise4j_free(void *object) {
    free(object);
}
