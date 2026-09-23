package com.bkpit.mangal.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Thin Kotlin wrapper around the native llama.cpp JNI bridge (llama_jni.cpp).
 * All calls are suspend + Dispatchers.Default because inference is CPU-bound
 * and must never block the main/UI thread.
 *
 * State is a single mutable handle rather than a class instance per model,
 * matching the "one model loaded at a time" assumption baked into Phase 5's
 * battery/thermal throttling (ModelLifecycleObserver unloads this on
 * background, reloads on next request).
 */
class LlamaEngine {

    @Volatile
    private var handle: Long = 0L

    val isLoaded: Boolean get() = handle != 0L

    suspend fun load(modelFile: File, contextLength: Int = 2048, threads: Int = 4) =
        withContext(Dispatchers.Default) {
            check(modelFile.exists()) { "Model file does not exist: ${modelFile.path}" }
            unload() // guard against double-load leaking a context
            handle = nativeLoadModel(modelFile.absolutePath, contextLength, threads)
            check(handle != 0L) { "Native model load failed for ${modelFile.name}" }
        }

    suspend fun complete(prompt: String, maxTokens: Int = 256): String =
        withContext(Dispatchers.Default) {
            check(isLoaded) { "Model not loaded" }
            nativeComplete(handle, prompt, maxTokens)
        }

    suspend fun unload() = withContext(Dispatchers.Default) {
        if (handle != 0L) {
            nativeUnload(handle)
            handle = 0L
        }
    }

    private external fun nativeLoadModel(modelPath: String, contextLength: Int, nThreads: Int): Long
    private external fun nativeComplete(handle: Long, prompt: String, maxTokens: Int): String
    private external fun nativeUnload(handle: Long)

    companion object {
        init {
            // Matches add_library(mangal_llama ...) in CMakeLists.txt.
            System.loadLibrary("mangal_llama")
        }
    }
}
