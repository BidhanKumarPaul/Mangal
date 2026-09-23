package com.bkpit.mangal.stt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class WhisperEngine {

    @Volatile
    private var handle: Long = 0L

    val isLoaded: Boolean get() = handle != 0L

    suspend fun load(modelFile: File) = withContext(Dispatchers.Default) {
        check(modelFile.exists()) { "Whisper model not found: ${modelFile.path}" }
        handle = nativeLoadModel(modelFile.absolutePath)
        check(handle != 0L) { "Native whisper load failed" }
    }

    /** [pcmF32] must be mono, 16kHz, floats in [-1, 1] — see AudioCapture. */
    suspend fun transcribe(pcmF32: FloatArray, threads: Int = 4): String =
        withContext(Dispatchers.Default) {
            check(isLoaded) { "Whisper model not loaded" }
            nativeTranscribe(handle, pcmF32, threads)
        }

    suspend fun unload() = withContext(Dispatchers.Default) {
        if (handle != 0L) {
            nativeUnload(handle)
            handle = 0L
        }
    }

    private external fun nativeLoadModel(modelPath: String): Long
    private external fun nativeTranscribe(handle: Long, pcmF32: FloatArray, nThreads: Int): String
    private external fun nativeUnload(handle: Long)

    companion object {
        init { System.loadLibrary("mangal_whisper") }
    }
}
