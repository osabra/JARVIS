package com.jarvis.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class JarvisWakeService : Service() {
    private var recognizer: SpeechRecognizer? = null
    private var restarting = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(71, notification())
        startListening()
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { sr ->
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p: Bundle?) { broadcast("READY", "") }
                override fun onBeginningOfSpeech() { broadcast("LISTENING", "") }
                override fun onRmsChanged(v: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() { broadcast("WAITING", "") }
                override fun onPartialResults(b: Bundle?) {}
                override fun onEvent(t: Int, p: Bundle?) {}
                override fun onError(e: Int) { restart() }
                override fun onResults(b: Bundle?) {
                    val heard = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    val lower = heard.lowercase(Locale.getDefault())
                    val wake = listOf("jarvis", "jarvises", "harvis", "jarbi", "jarbis").firstOrNull { lower.contains(it) }
                    if (wake != null) {
                        val command = heard.substringAfter(wake, "", ignoreCase = true).trim().trim(',', '.', ':', ';')
                        broadcast("WAKE", command)
                    }
                    restart()
                }
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            }
            runCatching { sr.startListening(intent) }
        }
    }

    private fun restart() {
        if (restarting) return
        restarting = true
        android.os.Handler(mainLooper).postDelayed({ restarting = false; startListening() }, 650)
    }

    private fun broadcast(type: String, command: String) {
        sendBroadcast(Intent("com.jarvis.ai.WAKE_EVENT").setPackage(packageName).putExtra("type", type).putExtra("command", command))
    }

    private fun createChannel() {
        val channel = NotificationChannel("jarvis_wake", "JARVIS", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun notification(): Notification = Notification.Builder(this, "jarvis_wake")
        .setContentTitle("JARVIS activo")
        .setContentText("Escuchando la palabra de activación")
        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
        .setOngoing(true)
        .build()

    override fun onDestroy() { recognizer?.destroy(); recognizer = null; super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
