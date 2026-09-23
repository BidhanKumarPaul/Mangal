package com.bkpit.mangal.wakeword

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Phase 5, and honestly the least finished part of this project. The
 * foreground-service plumbing below (notification, lifecycle, permission
 * declaration) is real and correct. What's NOT here is an actual wake-word
 * detection loop, because that requires picking and vetting one specific
 * library first:
 *   - openWakeWord: no official Android/Kotlin port as of my last check —
 *     you'd be running its TFLite models yourself via the tflite Android
 *     runtime, feeding it the same 16kHz mono audio AudioCapture.kt already
 *     produces in core-stt.
 *   - Porcupine (Picovoice): has an official Android SDK, but it needs a
 *     Picovoice AccessKey tied to an account, and free-tier usage is capped
 *     — worth double-checking that's acceptable against "100% offline,
 *     free-tier" before wiring it in.
 * Tell me which one you want and I'll write the actual detection loop
 * against that library's real API — I did not want to fabricate calls to
 * either SDK the way I'd be doing if I guessed here.
 */
class WakeWordService : Service() {

    companion object {
        private const val CHANNEL_ID = "mangal_wakeword"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        // TODO(Phase 5): start the chosen wake-word engine here, feeding it
        // AudioCapture.captureChunks(...) and calling onWakeWordDetected()
        // when it fires.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun onWakeWordDetected() {
        // Launch MainActivity into listening state, or broadcast to it if
        // already foreground. Left for you to wire once the detection loop
        // above is real.
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Mangal wake word listening", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mangal is listening for the wake word")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
}
