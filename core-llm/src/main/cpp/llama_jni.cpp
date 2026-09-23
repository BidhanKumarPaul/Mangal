// JNI bridge between Kotlin (LlamaEngine.kt) and llama.cpp's public C API.
//
// HONESTY NOTE: I wrote this against llama.cpp's public llama.h API as I
// understand its shape, but that header's exact function signatures do
// change between releases and I have no way to compile this in my sandbox
// (no NDK, no network to fetch the submodule). Before trusting this file:
//   1. Build once and read every compiler error against your vendored
//      include/llama.h — rename/adjust calls to match.
//   2. Send me the actual error output and I'll fix it against the real
//      signatures rather than guessing again.

#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include "llama.h"

#define LOG_TAG "MangalLlama"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {
    bool g_backend_initialized = false;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_bkpit_mangal_llm_LlamaEngine_nativeLoadModel(
        JNIEnv *env, jobject /* this */, jstring modelPath, jint contextLength, jint nThreads) {

    if (!g_backend_initialized) {
        llama_backend_init();
        g_backend_initialized = true;
    }

    const char *path = env->GetStringUTFChars(modelPath, nullptr);

    llama_model_params model_params = llama_model_default_params();
    // model_params.n_gpu_layers = 0; // CPU-only for the free/portable path.

    llama_model *model = llama_model_load_from_file(path, model_params);
    env->ReleaseStringUTFChars(modelPath, path);

    if (model == nullptr) {
        LOGE("Failed to load model");
        return 0;
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = static_cast<uint32_t>(contextLength);
    ctx_params.n_threads = nThreads;
    ctx_params.n_threads_batch = nThreads;

    llama_context *ctx = llama_new_context_with_model(model, ctx_params);
    if (ctx == nullptr) {
        LOGE("Failed to create context");
        llama_model_free(model);
        return 0;
    }

    // Pack both pointers into the jlong handle Kotlin holds onto.
    auto *handle = new std::pair<llama_model *, llama_context *>(model, ctx);
    return reinterpret_cast<jlong>(handle);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_bkpit_mangal_llm_LlamaEngine_nativeComplete(
        JNIEnv *env, jobject /* this */, jlong handle, jstring prompt, jint maxTokens) {

    auto *pair = reinterpret_cast<std::pair<llama_model *, llama_context *> *>(handle);
    if (pair == nullptr) {
        return env->NewStringUTF("[error: model not loaded]");
    }
    llama_model *model = pair->first;
    llama_context *ctx = pair->second;

    const char *promptChars = env->GetStringUTFChars(prompt, nullptr);
    std::string promptStr(promptChars);
    env->ReleaseStringUTFChars(prompt, promptChars);

    const llama_vocab *vocab = llama_model_get_vocab(model);

    // Tokenize.
    std::vector<llama_token> tokens(promptStr.size() + 32);
    int nTokens = llama_tokenize(
            vocab, promptStr.c_str(), (int32_t) promptStr.size(),
            tokens.data(), (int32_t) tokens.size(), true, true);
    if (nTokens < 0) {
        tokens.resize(-nTokens);
        nTokens = llama_tokenize(
                vocab, promptStr.c_str(), (int32_t) promptStr.size(),
                tokens.data(), (int32_t) tokens.size(), true, true);
    }
    tokens.resize(nTokens);

    llama_batch batch = llama_batch_get_one(tokens.data(), (int32_t) tokens.size());
    if (llama_decode(ctx, batch) != 0) {
        return env->NewStringUTF("[error: initial decode failed]");
    }

    // Greedy sampling loop — good enough to prove the pipeline end to end.
    // Swap in a proper sampler chain (temperature/top-p/repeat-penalty) once
    // this compiles and you're iterating on output quality.
    llama_sampler *sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(sampler, llama_sampler_init_greedy());

    std::string result;
    for (int i = 0; i < maxTokens; i++) {
        llama_token newToken = llama_sampler_sample(sampler, ctx, -1);
        if (llama_vocab_is_eog(vocab, newToken)) break;

        char buf[256];
        int n = llama_token_to_piece(vocab, newToken, buf, sizeof(buf), 0, true);
        if (n > 0) result.append(buf, n);

        llama_batch nextBatch = llama_batch_get_one(&newToken, 1);
        if (llama_decode(ctx, nextBatch) != 0) break;
    }

    llama_sampler_free(sampler);
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_bkpit_mangal_llm_LlamaEngine_nativeUnload(JNIEnv * /*env*/, jobject /*this*/, jlong handle) {
    auto *pair = reinterpret_cast<std::pair<llama_model *, llama_context *> *>(handle);
    if (pair == nullptr) return;
    if (pair->second) llama_free(pair->second);
    if (pair->first) llama_model_free(pair->first);
    delete pair;
}
