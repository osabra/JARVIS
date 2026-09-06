package com.jarvis.ai

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.util.Locale

private const val PREFS = "jarvis_openai"
private const val BACKEND = "backend_url"
private val BG = Color(0xFF010308)
private val PANEL = Color(0xFF07111A)
private val CYAN = Color(0xFF59E0FF)
private val CYAN_DARK = Color(0xFF155D72)
private val WHITE = Color(0xFFE9FAFF)

data class JarvisLine(val role: String, val text: String)

class JarvisOpenAIActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JarvisOpenAIApp() }
    }
}

@Composable
private fun JarvisOpenAIApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var backend by remember { mutableStateOf(context.getSharedPreferences(PREFS, 0).getString(BACKEND, "") ?: "") }
    var input by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("ONLINE") }
    var wakeEnabled by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    var previousId by remember { mutableStateOf<String?>(null) }
    val lines = remember { mutableStateListOf<JarvisLine>() }
    val tts = remember { TextToSpeech(context) {} }
    val client = remember(backend) { OpenAIClient(backend) }

    DisposableEffect(Unit) {
        tts.language = Locale("es", "ES")
        onDispose { tts.shutdown() }
    }

    fun speak(text: String) { tts.speak(text.take(3000), TextToSpeech.QUEUE_FLUSH, null, "jarvis") }
    fun ask(text: String) {
        val prompt = text.trim()
        if (prompt.isBlank() || state == "THINKING") return
        if (backend.isBlank()) {
            settings = true
            state = "CONFIG"
            return
        }
        val local = JarvisActions.execute(context, prompt)
        if (local != null) {
            lines.add(JarvisLine("user", prompt)); lines.add(JarvisLine("assistant", local)); speak(local); state = "READY"; input = ""; return
        }
        lines.add(JarvisLine("user", prompt)); input = ""; state = "THINKING"
        scope.launch {
            client.chat(lines.filter { it.role == "user" || it.role == "assistant" }.map { it.role to it.text }, previousId)
                .onSuccess { reply -> previousId = reply.id; lines.add(JarvisLine("assistant", reply.text)); speak(reply.text); state = "READY" }
                .onFailure { error -> lines.add(JarvisLine("assistant", "No puedo conectar con mi cerebro JARVIS: ${error.message ?: "error"}")); state = "ERROR" }
        }
    }

    val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { ContextCompat.startForegroundService(context, Intent(context, JarvisWakeService::class.java)); wakeEnabled = true; state = "LISTENING" }
    }
    val receiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.getStringExtra("type")) {
                    "READY", "WAITING", "LISTENING" -> if (wakeEnabled) state = "LISTENING"
                    "WAKE" -> { state = "THINKING"; val command = intent.getStringExtra("command").orEmpty(); if (command.isBlank()) { speak("Te escucho."); state = "READY" } else ask(command) }
                }
            }
        }
    }
    DisposableEffect(Unit) {
        ContextCompat.registerReceiver(context, receiver, IntentFilter("com.jarvis.ai.WAKE_EVENT"), ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    val anim = rememberInfiniteTransition(label = "jarvis")
    val angle by anim.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "angle")
    val pulse by anim.animateFloat(1f, 1.08f, infiniteRepeatable(tween(if (state == "THINKING" || state == "LISTENING") 450 else 1300), RepeatMode.Reverse), label = "pulse")

    MaterialTheme(colorScheme = darkColorScheme(background = BG, surface = PANEL, primary = CYAN)) {
        Column(Modifier.fillMaxSize().background(BG).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("JARVIS", color = WHITE, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp); Text("OPENAI INTELLIGENCE CORE", color = CYAN_DARK, fontSize = 8.sp, letterSpacing = 2.sp) }
                IconButton(onClick = { settings = true }) { Icon(Icons.Default.Settings, null, tint = CYAN) }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("● $state", color = CYAN, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text(if (wakeEnabled) "VOICE ACTIVE" else "VOICE STANDBY", color = CYAN_DARK, fontSize = 9.sp) }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(235.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(220.dp).scale(pulse).rotate(angle)) {
                    drawArc(CYAN, 15f, 80f, false, androidx.compose.ui.geometry.Offset(5f, 5f), androidx.compose.ui.geometry.Size(size.width - 10f, size.height - 10f), style = Stroke(2f))
                    drawArc(CYAN_DARK, 190f, 60f, false, androidx.compose.ui.geometry.Offset(15f, 15f), androidx.compose.ui.geometry.Size(size.width - 30f, size.height - 30f), style = Stroke(3f))
                    for (i in 0..11) {
                        val a = Math.toRadians((i * 30).toDouble()); val x1 = center.x + (size.minDimension * .40f * Math.cos(a)).toFloat(); val y1 = center.y + (size.minDimension * .40f * Math.sin(a)).toFloat(); val x2 = center.x + (size.minDimension * .47f * Math.cos(a)).toFloat(); val y2 = center.y + (size.minDimension * .47f * Math.sin(a)).toFloat(); drawLine(CYAN, androidx.compose.ui.geometry.Offset(x1,y1), androidx.compose.ui.geometry.Offset(x2,y2), 2f, StrokeCap.Round)
                    }
                }
                Surface(Modifier.size(125.dp), shape = CircleShape, color = Color(0xFF04141E), border = androidx.compose.foundation.BorderStroke(2.dp, CYAN)) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(if (state == "THINKING") "…" else "J", color = CYAN, fontSize = 56.sp, fontWeight = FontWeight.Bold); Text(state, color = CYAN, fontSize = 8.sp, letterSpacing = 2.sp) }
                }
            }
            Surface(Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(22.dp), color = PANEL, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF12333F))) {
                if (lines.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Hola. Soy JARVIS. Habla conmigo.", color = CYAN_DARK, fontSize = 14.sp) }
                else LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(lines) { line -> Surface(shape = RoundedCornerShape(16.dp), color = if (line.role == "user") Color(0xFF0C2631) else Color(0xFF0A171F)) { Text(line.text, color = WHITE, fontSize = 14.sp, modifier = Modifier.padding(12.dp)) } } }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("Habla con JARVIS…") }, singleLine = true, shape = RoundedCornerShape(18.dp))
                Spacer(Modifier.width(7.dp)); FloatingActionButton(onClick = { ask(input) }, containerColor = CYAN) { Icon(Icons.Default.Send, null) }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { mic.launch(Manifest.permission.RECORD_AUDIO) }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A1B24))) { Icon(Icons.Default.Mic, null, tint = CYAN); Spacer(Modifier.width(5.dp)); Text("HABLAR", color = CYAN) }
                Button(onClick = { if (wakeEnabled) { context.stopService(Intent(context, JarvisWakeService::class.java)); wakeEnabled = false; state = "ONLINE" } else mic.launch(Manifest.permission.RECORD_AUDIO) }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (wakeEnabled) CYAN else Color(0xFF0A1B24))) { Text(if (wakeEnabled) "PARAR" else "DECIR JARVIS", color = if (wakeEnabled) Color.Black else CYAN) }
            }
        }
        if (settings) JarvisSettingsDialog(backend, { settings = false }) { value -> backend = value.trim().removeSuffix("/"); context.getSharedPreferences(PREFS, 0).edit().putString(BACKEND, backend).apply(); settings = false }
    }
}

@Composable
private fun JarvisSettingsDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(22.dp), color = PANEL) {
            Column(Modifier.padding(20.dp)) {
                Text("JARVIS CORE", color = CYAN, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp)); Text("URL del servidor seguro de JARVIS. La clave de OpenAI nunca se guarda en la APK.", color = WHITE, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp)); OutlinedTextField(value, { value = it }, label = { Text("Backend URL") }, singleLine = true)
                Spacer(Modifier.height(14.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onDismiss) { Text("Cancelar") }; Button(onClick = { onSave(value) }) { Text("Guardar") } }
            }
        }
    }
}
