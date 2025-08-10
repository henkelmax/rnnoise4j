#include <jni.h>
#include <math.h>
#include <stdbool.h>
#include <stdlib.h>
#include <stdint.h>
#include <rnnoise.h>

#include "exceptions.h"

typedef struct Denoiser {
    DenoiseState *state;
    RNNModel *model;
    uint8_t *model_buffer;
} Denoiser;

/**
 * Gets the denoiser from the denoiser java object.
 *
 * @param env the JNI environment
 * @param denoiser_pointer the pointer to the denoiser
 * @return the decoder or NULL - If the denoiser could not be retrieved, this will throw a runtime exception in Java
 */
Denoiser *get_denoiser(JNIEnv *env, const jlong denoiser_pointer) {
    if (denoiser_pointer == 0) {
        throw_runtime_exception(env, "Denoiser is closed");
        return NULL;
    }
    return (Denoiser *) (uintptr_t) denoiser_pointer;
}

JNIEXPORT jlong JNICALL Java_de_maxhenkel_rnnoise4j_Denoiser_createDenoiser0(
    JNIEnv *env,
    jclass clazz,
    const jbyteArray model
) {
    if (model == NULL) {
        throw_illegal_argument_exception(env, "Model is null");
        return 0;
    }
    const jsize model_length = (*env)->GetArrayLength(env, model);
    if (model_length <= 0) {
        throw_illegal_argument_exception(env, "Model is empty");
        return 0;
    }
    uint8_t *model_buffer = malloc(model_length);
    (*env)->GetByteArrayRegion(env, model, 0, model_length, (jbyte *) model_buffer);
    if ((*env)->ExceptionCheck(env)) {
        free(model_buffer);
        return 0;
    }

    RNNModel *rnn_model = rnnoise_model_from_buffer(model_buffer, model_length);
    if (rnn_model == NULL) {
        free(model_buffer);
        throw_io_exception(env, "Could not load model");
        return 0;
    }

    DenoiseState *rnnoise = rnnoise_create(rnn_model);
    if (rnnoise == NULL) {
        free(model_buffer);
        rnnoise_model_free(rnn_model);
        throw_io_exception(env, "Could not create denoiser");
        return 0;
    }
    Denoiser *denoiser = malloc(sizeof(Denoiser));
    denoiser->model = rnn_model;
    denoiser->state = rnnoise;
    denoiser->model_buffer = model_buffer;
    return (jlong) (uintptr_t) denoiser;
}

JNIEXPORT jint JNICALL Java_de_maxhenkel_rnnoise4j_Denoiser_getFrameSize0(
    JNIEnv *env,
    jclass clazz
) {
    return rnnoise_get_frame_size();
}

JNIEXPORT jshortArray JNICALL Java_de_maxhenkel_rnnoise4j_Denoiser_denoise0(
    JNIEnv *env,
    jobject obj,
    const jlong denoiser_pointer,
    const jshortArray input
) {
    const Denoiser *denoiser = get_denoiser(env, denoiser_pointer);
    if (denoiser == NULL) {
        return 0;
    }

    if (input == NULL) {
        throw_illegal_argument_exception(env, "Input array is null");
        return 0;
    }

    const jsize input_length = (*env)->GetArrayLength(env, input);

    if (input_length <= 0) {
        throw_illegal_argument_exception(env, "Input array is empty");
        return 0;
    }
    if (input_length % rnnoise_get_frame_size() != 0) {
        throw_illegal_argument_exception(env, "Input array is not a multiple of the frame size");
        return 0;
    }

    jshort *pcm_input = (*env)->GetShortArrayElements(env, input, NULL);

    const jshortArray pcm_output = (*env)->NewShortArray(env, input_length);

    const int frame_size = rnnoise_get_frame_size();

    float *input_buffer = calloc(frame_size, sizeof(float));
    float *output_buffer = calloc(frame_size, sizeof(float));
    jshort *output_pcm_buffer = calloc(frame_size, sizeof(jshort));

    const int frame_count = input_length / frame_size;
    for (int i = 0; i < frame_count; i++) {
        for (int frame_index = 0; frame_index < frame_size; frame_index++) {
            input_buffer[frame_index] = (float) pcm_input[i * frame_size + frame_index];
        }
        rnnoise_process_frame(denoiser->state, output_buffer, input_buffer);
        for (int frame_index = 0; frame_index < frame_size; frame_index++) {
            const float sample = output_buffer[frame_index];
            if (sample >= 32767.0f) {
                output_pcm_buffer[frame_index] = 32767;
                continue;
            }
            if (sample <= -32768.0f) {
                output_pcm_buffer[frame_index] = -32768;
                continue;
            }
            output_pcm_buffer[frame_index] = (jshort) lrintf(sample);
        }
        (*env)->SetShortArrayRegion(env, pcm_output, i * frame_size, frame_size, output_pcm_buffer);
    }

    free(input_buffer);
    free(output_buffer);
    free(output_pcm_buffer);
    (*env)->ReleaseShortArrayElements(env, input, pcm_input, JNI_ABORT);

    return pcm_output;
}

JNIEXPORT void JNICALL Java_de_maxhenkel_rnnoise4j_Denoiser_destroyDenoiser0(
    JNIEnv *env,
    jobject obj,
    const jlong denoiser_pointer
) {
    if (denoiser_pointer == 0) {
        return;
    }
    Denoiser *denoiser = (Denoiser *) (uintptr_t) denoiser_pointer;
    rnnoise_destroy(denoiser->state);
    // rnnoise_model_free(denoiser->model); somehow causes EXCEPTION_ACCESS_VIOLATION, so we manually free it
    free(denoiser->model);
    free(denoiser->model_buffer);
    denoiser->state = NULL;
    denoiser->model = NULL;
    denoiser->model_buffer = NULL;
    free(denoiser);
}
