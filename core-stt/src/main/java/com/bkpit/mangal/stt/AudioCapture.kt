package com.bkpit.mangal.stt

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

/**
 * Captures mono 16kHz PCM from the mic — exactly the format whisper.cpp
 * expects, so no resampling step is needed downstream. This part needs no
 * native library at all and is real/complete, unlike the JNI bridges.
 *
 * Caller must have RECORD_AUDIO granted before calling [captureChunks] —
 * PermissionManager.MangalPermission.RECORD_AUDIO in core-tools.
 */
object AudioCapture {
    private const val SAMPLE_RATE = 16_000

    @SuppressLint("MissingPermission") // caller-checked via PermissionManager
    fun captureChunks(chunkDurationMs: Int = 3000): Flow<FloatArray> = callbackFlow {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        check(minBufferSize > 0) { "AudioRecord.getMinBufferSize failed on this device" }

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize * 2
        )

        val samplesPerChunk = SAMPLE_RATE * chunkDurationMs / 1000
        val shortBuffer = ShortArray(samplesPerChunk)

        audioRecord.startRecording()

        // Simple blocking-read loop on a background thread via callbackFlow;
        // fine for push-to-talk (Phase 3). For always-on wake-word listening
        // (Phase 5) this should move to a foreground Service so Android
        // doesn't kill the mic session when the app backgrounds.
        val thread = Thread {
            try {
                while (!isClosedForSend) {
                    var offset = 0
                    while (offset < shortBuffer.size) {
                        val read = audioRecord.read(shortBuffer, offset, shortBuffer.size - offset)
                        if (read <= 0) break
                        offset += read
                    }
                    val floatChunk = FloatArray(shortBuffer.size) { i ->
                        shortBuffer[i] / 32768.0f
                    }
                    trySend(floatChunk)
                }
            } catch (t: Throwable) {
                close(t)
            }
        }
        thread.start()

        awaitClose {
            audioRecord.stop()
            audioRecord.release()
            thread.interrupt()
        }
    }.flowOn(Dispatchers.IO)
}
