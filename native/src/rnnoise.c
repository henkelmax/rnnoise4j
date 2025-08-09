#include <jni.h>
#include <math.h>
#include <stdbool.h>
#include <stdlib.h>
#include <stdint.h>
#include <rnnoise.h>

#include "exceptions.h"

/**
 * Gets the denoiser from the denoiser java object.
 *
 * @param env the JNI environment
 * @param denoiser_pointer the pointer to the denoiser
 * @return the decoder or NULL - If the denoiser could not be retrieved, this will throw a runtime exception in Java
 */
DenoiseState *get_denoiser(JNIEnv *env, const jlong denoiser_pointer) {
    if (denoiser_pointer == 0) {
        throw_runtime_exception(env, "Denoiser is closed");
        return NULL;
    }
    return (DenoiseState *) (uintptr_t) denoiser_pointer;
}


JNIEXPORT jlong JNICALL Java_de_maxhenkel_rnnoise4j_Denoiser_createDenoiser0(
    JNIEnv *env,
    jclass clazz
) {
    DenoiseState *rnnoise = rnnoise_create(NULL);
    return (jlong) (uintptr_t) rnnoise;
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
    DenoiseState *denoiser = get_denoiser(env, denoiser_pointer);
    if (denoiser == NULL) {
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

    jshort *pcm_input = (*env)->GetShortArrayElements(env, input, false);

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
        rnnoise_process_frame(denoiser, output_buffer, input_buffer);
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
    DenoiseState *denoiser = (DenoiseState *) (uintptr_t) denoiser_pointer;
    rnnoise_destroy(denoiser);
}
