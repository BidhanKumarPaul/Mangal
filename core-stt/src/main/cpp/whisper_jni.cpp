// JNI bridge between Kotlin (WhisperEngine.kt) and whisper.cpp's public C API.
// Same honesty note as core-llm/llama_jni.cpp: written against whisper.cpp's
// public whisper.h shape from memory, not compiled here. Verify against your
// vendored header and send me real compiler errors to fix against ground
// truth rather than have me guess twice.

#include <jni.h>
#include <android/log.h>
#include <vector>
#include <string>
#include "whisper.h"

#define LOG_TAG "MangalWhisper"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jlong JNICALL
Java_com_bkpit_mangal_stt_WhisperEngine_nativeLoadModel(
        JNIEnv *env, jobject /* this */, jstring modelPath) {

    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    struct whisper_context_params cparams = whisper_context_default_params();
    struct whisper_context *ctx = whisper_init_from_file_with_params(path, cparams);
    env->ReleaseStringUTFChars(modelPath, path);

    if (ctx == nullptr) {
        LOGE("Failed to load whisper model");
        return 0;
    }
    return reinterpret_cast<jlong>(ctx);
}

// pcmF32: mono, 16kHz, normalized [-1, 1] float samples — AudioCapture.kt is
// responsible for getting AudioRecord's 16-bit PCM into exactly this shape.
extern "C" JNIEXPORT jstring JNICALL
Java_com_bkpit_mangal_stt_WhisperEngine_nativeTranscribe(
        JNIEnv *env, jobject /* this */, jlong handle, jfloatArray pcmF32, jint nThreads) {

    auto *ctx = reinterpret_cast<whisper_context *>(handle);
    if (ctx == nullptr) return env->NewStringUTF("[error: model not loaded]");

    jsize len = env->GetArrayLength(pcmF32);
    std::vector<float> samples(len);
    env->GetFloatArrayRegion(pcmF32, 0, len, samples.data());

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.n_threads = nThreads;
    params.language = "auto";
    params.translate = false;
    params.print_progress = false;
    params.print_special = false;
    params.no_context = true;

    if (whisper_full(ctx, params, samples.data(), (int) samples.size()) != 0) {
        return env->NewStringUTF("[error: whisper_full failed]");
    }

    std::string result;
    int nSegments = whisper_full_n_segments(ctx);
    for (int i = 0; i < nSegments; i++) {
        result += whisper_full_get_segment_text(ctx, i);
    }
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_bkpit_mangal_stt_WhisperEngine_nativeUnload(JNIEnv * /*env*/, jobject /*this*/, jlong handle) {
    auto *ctx = reinterpret_cast<whisper_context *>(handle);
    if (ctx != nullptr) whisper_free(ctx);
}
