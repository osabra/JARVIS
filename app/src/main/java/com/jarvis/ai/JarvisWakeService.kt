package com.jarvis.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * JARVIS voice trigger service.
 *
 * This is a fresh implementation for the JARVIS project; it does not rely on
 * any private/internal assistant code. Android's SpeechRecognizer is used as
 * the microphone front-end while the app is running as a foreground service.
 */
class JarvisWakeService : Service() {
    private val handler by lazy { Handler(mainLooper) }
    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var stopping = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        beginListening(250L)
    }

    private fun beginListening(delayMs: Long = 0L) {
        if (stopping || !SpeechRecognizer.isRecognitionAvailable(this)) {
            broadcast("ERROR", "")
            return
        }
        handler.postDelayed({
            if (stopping) return@postDelayed
            destroyRecognizer()
            recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(listener)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("es", "ES"))
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-ES")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 8)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                }
                listening = true
                broadcast("READY", "")
                runCatching { startListening(intent) }
                    .onFailure { listening = false; scheduleRestart() }
            }
        }, delayMs)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            broadcast("READY", "")
        }

        override fun onBeginningOfSpeech() {
            broadcast("LISTENING", "")
        }

        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
        override fun onPartialResults(partialResults: Bundle?) = Unit

        override fun onEndOfSpeech() {
            listening = false
            broadcast("WAITING", "")
        }

        override fun onError(error: Int) {
            listening = false
            if (!stopping) scheduleRestart()
        }

        override fun onResults(results: Bundle?) {
            listening = false
            val heard = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()

            val command = extractJarvisCommand(heard)
            if (command != null) {
                broadcast("WAKE", command)
            }
            if (!stopping) scheduleRestart()
        }
    }

    private fun extractJarvisCommand(text: String): String? {
        val normalized = text
            .lowercase(Locale.getDefault())
            .replace(Regex("\\s+"), " ")
            .trim()

        val wakeWords = listOf(
            "jarvis",
            "jarvises",
            "jarbi",
            "jarbis",
            "harvis",
            "jervis",
            "ok jarvis",
            "oye jarvis"
        )

        val matched = wakeWords
            .sortedByDescending { it.length }
            .firstOrNull { normalized.contains(it) }
            ?: return null

        val index = normalized.indexOf(matched)
        if (index < 0) return ""

        return normalized
            .substring(index + matched.length)
            .trim()
            .trim(',', '.', ':', ';', '-', '¿', '?')
    }

    private fun scheduleRestart() {
        handler.removeCallbacksAndMessages(null)
        if (!stopping) beginListening(RESTART_DELAY_MS)
    }

    private fun destroyRecognizer() {
        recognizer?.runCatching { stopListening() }
        recognizer?.destroy()
        recognizer = null
        listening = false
    }

    private fun broadcast(type: String, command: String) {
        sendBroadcast(
            Intent(ACTION_WAKE_EVENT)
                .setPackage(packageName)
                .putExtra("type", type)
                .putExtra("command", command)
        )
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "JARVIS Voice",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Servicio de voz de JARVIS"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification = Notification.Builder(this, CHANNEL_ID)
        .setContentTitle("JARVIS activo")
        .setContentText("Di «Jarvis» para activarlo")
        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
        .setOngoing(true)
        .setCategory(Notification.CATEGORY_SERVICE)
        .build()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopping = false
        if (!listening) beginListening(100L)
        return START_STICKY
    }

    override fun onDestroy() {
        stopping = true
        handler.removeCallbacksAndMessages(null)
        destroyRecognizer()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "jarvis_voice"
        private const val NOTIFICATION_ID = 71
        private const val RESTART_DELAY_MS = 500L
        const val ACTION_WAKE_EVENT = "com.jarvis.ai.WAKE_EVENT"
    }
}
