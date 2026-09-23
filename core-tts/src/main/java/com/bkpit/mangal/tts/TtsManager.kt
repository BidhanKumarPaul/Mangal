package com.bkpit.mangal.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

/**
 * This module needs no native library at all — Android's TextToSpeech is a
 * system service, so this file is genuinely complete and testable as-is,
 * unlike core-llm/core-stt's JNI bridges.
 */
class TtsManager(context: Context) {

    private var isReady = false
    private val pendingUtterances = mutableMapOf<String, () -> Unit>()

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        isReady = status == TextToSpeech.SUCCESS
    }.also { engine ->
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                utteranceId?.let { pendingUtterances.remove(it)?.invoke() }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                utteranceId?.let { pendingUtterances.remove(it)?.invoke() }
            }
        })
    }

    fun setRate(rate: Float) {
        tts.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
    }

    fun setLanguage(locale: Locale): Boolean =
        tts.setLanguage(locale) >= TextToSpeech.LANG_AVAILABLE

    /** Suspends until this utterance finishes playing (or errors). */
    suspend fun speakAndWait(text: String) = suspendCancellableCoroutine<Unit> { cont ->
        if (!isReady) {
            cont.resume(Unit)
            return@suspendCancellableCoroutine
        }
        val utteranceId = "mangal_${System.nanoTime()}"
        pendingUtterances[utteranceId] = { if (cont.isActive) cont.resume(Unit) }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

        cont.invokeOnCancellation {
            tts.stop()
            pendingUtterances.remove(utteranceId)
        }
    }

    fun stop() = tts.stop()

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
