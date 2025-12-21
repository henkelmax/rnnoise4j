#include <jni.h>
#include <stdbool.h>
#include <stdlib.h>
#include <stdint.h>
#include <rnnoise4j.h>
#include <rnnoise.h>

#include "java_exceptions.h"

JNIEXPORT jlong JNICALL Java_de_maxhenkel_rnnoise4j_Denoiser_createDenoiser0(
    JNIEnv *env,
    jclass clazz,
    const jbyteArray model
) {
    if (model == NULL) {
        throw_illegal_argument_exception(env, "Model is null");
        return 0LL;
    }
    const jsize model_length = (*env)->GetArrayLength(env, model);
    if (model_length <= 0) {
        throw_illegal_argument_exception(env, "Model is empty");
        return 0LL;
    }
    uint8_t *model_buffer = malloc(model_length);
    (*env)->GetByteArrayRegion(env, model, 0, model_length, (jbyte *) model_buffer);
    if ((*env)->ExceptionCheck(env)) {
        free(model_buffer);
        return 0LL;
    }
    int error = RNNOISE_NO_ERROR;
    const int64_t pointer = rnnoise4j_create_denoiser(model_buffer, model_length, &error);
    if (error != RNNOISE_NO_ERROR) {
        throw_error(error, env);
        free(model_buffer);
    }
    return pointer;
}

JNIEXPORT jint JNICALL Java_de_maxhenkel_rnnoise4j_Denoiser_getFrameSize0(
    JNIEnv *env,
    jclass clazz
) {
    return rnnoise4j_get_frame_size();
}

JNIEXPORT jshortArray JNICALL Java_de_maxhenkel_rnnoise4j_Denoiser_denoise0(
    JNIEnv *env,
    jobject obj,
    const jlong denoiser_pointer,
    const jshortArray input
) {
    if (input == NULL) {
        throw_illegal_argument_exception(env, "Input array is null");
        return 0;
    }
    const jsize input_length = (*env)->GetArrayLength(env, input);
    jshort *pcm_input = (*env)->GetShortArrayElements(env, input, NULL);
    jshort *output_buffer = calloc(input_length, sizeof(jshort));
    const int error = rnnoise4j_denoise(denoiser_pointer, pcm_input, input_length, output_buffer);
    if (error != RNNOISE_NO_ERROR) {
        throw_error(error, env);
        return 0;
    }

    const jshortArray pcm_output_java = (*env)->NewShortArray(env, input_length);
    (*env)->SetShortArrayRegion(env, pcm_output_java, 0, input_length, output_buffer);

    free(output_buffer);
    (*env)->ReleaseShortArrayElements(env, input, pcm_input, JNI_ABORT);

    return pcm_output_java;
}

jfloat denoiseInPlace(
    JNIEnv *env,
    const bool denoise,
    const jlong denoiser_pointer,
    const jshortArray input,
    int *error
) {
    const Denoiser *denoiser = get_denoiser(denoiser_pointer, error);
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

    const int frame_size = rnnoise_get_frame_size();
    if (input_length % frame_size != 0) {
        throw_illegal_argument_exception(env, "Input array is not a multiple of the frame size");
        return 0;
    }
    jshort *input_pcm_buffer = calloc(input_length, sizeof(jshort));
    (*env)->GetShortArrayRegion(env, input, 0, input_length, input_pcm_buffer);
    const float speech_probability = rnnoise4j_denoise_in_place(denoise, denoiser_pointer, input_pcm_buffer,
                                                                input_length, error);
    if (denoise) {
        (*env)->SetShortArrayRegion(env, input, 0, input_length, input_pcm_buffer);
    }
    free(input_pcm_buffer);
    return speech_probability;
}

JNIEXPORT jfloat JNICALL Java_de_maxhenkel_rnnoise4j_Denoiser_denoiseInPlace0(
    JNIEnv *env,
    jobject obj,
    const jlong denoiser_pointer,
    const jshortArray input
) {
    int error = RNNOISE_NO_ERROR;
    const float result = denoiseInPlace(env, true, denoiser_pointer, input, &error);
    if (error != RNNOISE_NO_ERROR) {
        throw_error(error, env);
        return 0;
    }
    return result;
}

JNIEXPORT jfloat JNICALL Java_de_maxhenkel_rnnoise4j_Denoiser_getSpeechProbability0(
    JNIEnv *env,
    jobject obj,
    const jlong denoiser_pointer,
    const jshortArray input
) {
    int error = RNNOISE_NO_ERROR;
    const float result = denoiseInPlace(env, false, denoiser_pointer, input, &error);
    if (error != RNNOISE_NO_ERROR) {
        throw_error(error, env);
        return 0;
    }
    return result;
}

JNIEXPORT void JNICALL Java_de_maxhenkel_rnnoise4j_Denoiser_destroyDenoiser0(
    JNIEnv *env,
    jobject obj,
    const jlong denoiser_pointer
) {
    rnnoise4j_destroy_denoiser_buffer(denoiser_pointer, true);
}
