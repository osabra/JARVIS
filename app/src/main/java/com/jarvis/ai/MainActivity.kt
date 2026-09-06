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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private const val PREFS = "jarvis_settings"
private const val API_KEY = "gemini_api_key"
private const val MODEL = "gemini-3.8-flash"
private val BG = Color(0xFF02050A)
private val PANEL = Color(0xFF07121C)
private val PANEL2 = Color(0xFF0A1B27)
private val CYAN = Color(0xFF52DEFF)
private val CYAN2 = Color(0xFF1B7897)
private val WHITE = Color(0xFFE9F8FC)
private val MUTED = Color(0xFF63808C)

data class ChatMessage(val role: String, val text: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JarvisApp() }
    }
}

@Composable
fun JarvisApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("Sistemas nominales. Estoy listo para ayudarte.") }
    var lastPrompt by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("READY") }
    var wakeMode by remember { mutableStateOf(false) }
    var thinking by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var apiKey by remember { mutableStateOf(context.getSharedPreferences(PREFS, 0).getString(API_KEY, "") ?: "") }
    val history = remember { mutableStateListOf<ChatMessage>() }
    val tts = remember { TextToSpeech(context) {} }

    DisposableEffect(tts) {
        tts.language = Locale("es", "ES")
        onDispose { tts.shutdown() }
    }

    fun speak(text: String) {
        tts.speak(text.take(2500), TextToSpeech.QUEUE_FLUSH, null, "jarvis")
    }

    fun ask(prompt: String) {
        if (prompt.isBlank() || thinking) return
        val local = JarvisActions.execute(context, prompt)
        if (local != null) {
            input = ""
            lastPrompt = prompt.trim()
            answer = local
            status = "READY"
            speak(local)
            return
        }
        if (apiKey.isBlank()) {
            answer = "Necesito configurar la clave de Gemini en Ajustes."
            status = "CONFIG"
            speak(answer)
            return
        }
        val p = prompt.trim()
        input = ""
        lastPrompt = p
        history.add(ChatMessage("user", p))
        thinking = true
        status = "THINKING"
        answer = "Analizando…"
        scope.launch {
            val result = withContext(Dispatchers.IO) { callGemini(apiKey, history.toList()) }
            thinking = false
            result.onSuccess { text ->
                history.add(ChatMessage("model", text))
                answer = text
                status = "READY"
                speak(text)
            }.onFailure {
                if (history.isNotEmpty()) history.removeAt(history.lastIndex)
                answer = "No he podido conectar con la IA: ${it.message ?: "error"}"
                status = "ERROR"
                speak(answer)
            }
        }
    }

    val requestMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startWakeService(context)
            wakeMode = true
            status = "LISTENING"
        } else {
            answer = "Necesito permiso de micrófono para escuchar «Jarvis»."
            status = "MIC DENIED"
        }
    }
    val requestNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        requestMic.launch(Manifest.permission.RECORD_AUDIO)
    }

    val receiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.getStringExtra("type")) {
                    "READY", "WAITING" -> if (wakeMode) status = "LISTENING"
                    "LISTENING" -> status = "HEARING"
                    "WAKE" -> {
                        status = "THINKING"
                        val command = intent.getStringExtra("command").orEmpty()
                        if (command.isBlank()) {
                            answer = "Te escucho."
                            speak("Te escucho.")
                            status = "READY"
                        } else {
                            ask(command)
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        val filter = IntentFilter("com.jarvis.ai.WAKE_EVENT")
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    val rotation = rememberInfiniteTransition(label = "rotation")
    val angle by rotation.animateFloat(0f, 360f, infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "angle")
    val pulse by rotation.animateFloat(
        1f, 1.055f,
        infiniteRepeatable(tween(if (status == "HEARING" || status == "THINKING") 500 else 1800), RepeatMode.Reverse),
        label = "pulse"
    )

    MaterialTheme(colorScheme = darkColorScheme(background = BG, surface = PANEL, primary = CYAN, onPrimary = Color.Black)) {
        Column(
            modifier = Modifier.fillMaxSize().background(BG).padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("JARVIS", color = WHITE, fontSize = 31.sp, fontWeight = FontWeight.Black, letterSpacing = 7.sp)
                    Text("PERSONAL AI SYSTEM", color = CYAN2, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.4.sp)
                }
                Surface(shape = CircleShape, color = Color(0xFF081822), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF194050))) {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = CYAN)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                StatusChip("●  $status")
                Text(if (wakeMode) "VOICE • ACTIVE" else "VOICE • STANDBY", color = MUTED, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            }

            Spacer(Modifier.height(4.dp))
            Box(Modifier.size(265.dp).scale(pulse), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().rotate(angle)) {
                    val r = size.minDimension * .44f
                    val topLeft = Offset(center.x - r, center.y - r)
                    val arcSize = Size(r * 2, r * 2)
                    drawArc(CYAN, 15f, 82f, false, topLeft, arcSize, style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                    drawArc(CYAN2, 155f, 68f, false, topLeft, arcSize, style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
                    drawArc(CYAN, 285f, 38f, false, topLeft, arcSize, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
                }
                Canvas(Modifier.size(225.dp)) {
                    val r = size.minDimension * .43f
                    drawCircle(CYAN2.copy(alpha = .10f), r)
                    drawCircle(CYAN.copy(alpha = .9f), r, style = androidx.compose.ui.graphics.drawscope.Stroke(2.2f))
                    drawCircle(CYAN2, r * .78f, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
                    for (i in 0..23) {
                        val a = Math.toRadians((i * 15).toDouble())
                        val inner = if (i % 2 == 0) .86f else .91f
                        val p1 = Offset(center.x + (r * inner * Math.cos(a)).toFloat(), center.y + (r * inner * Math.sin(a)).toFloat())
                        val p2 = Offset(center.x + (r * .97f * Math.cos(a)).toFloat(), center.y + (r * .97f * Math.sin(a)).toFloat())
                        drawLine(CYAN.copy(alpha = if (i % 2 == 0) .85f else .35f), p1, p2, if (i % 2 == 0) 2f else 1f, StrokeCap.Round)
                    }
                }
                Box(
                    Modifier.size(142.dp).shadow(30.dp, CircleShape).background(Color(0xFF06131D), CircleShape).border(2.dp, CYAN, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (status == "HEARING") "◉" else if (status == "THINKING") "…" else "J", color = CYAN, fontSize = 56.sp, fontWeight = FontWeight.Bold)
                        Text(if (status == "HEARING") "HEARING" else status, color = CYAN, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
                    }
                }
            }

            Text(if (wakeMode) "SAY  «JARVIS»  TO ACTIVATE" else "VOICE SYSTEM READY", color = CYAN2, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f).border(1.dp, Color(0xFF17333F), RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                color = PANEL
            ) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("JARVIS", color = CYAN, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.weight(1f).height(1.dp).background(Color(0xFF17333F)))
                        Text("SECURE SESSION", color = MUTED, fontSize = 7.sp, letterSpacing = 1.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (lastPrompt.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp), color = Color(0xFF0C2633), modifier = Modifier.align(Alignment.End)) {
                            Text(lastPrompt, color = WHITE, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                        }
                        Spacer(Modifier.height(9.dp))
                    }
                    Surface(shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp), color = PANEL2, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text("JARVIS", color = CYAN2, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            Spacer(Modifier.height(5.dp))
                            Text(answer, color = WHITE, fontSize = 14.sp, lineHeight = 21.sp)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text("AI CORE  •  VOICE  •  LOCAL ACTIONS", color = Color(0xFF385663), fontSize = 7.sp, letterSpacing = 1.2.sp)
                }
            }

            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f).height(58.dp),
                    placeholder = { Text("Escribe un comando…", color = Color(0xFF56717B), fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CYAN,
                        unfocusedBorderColor = Color(0xFF1A3541),
                        focusedTextColor = WHITE,
                        unfocusedTextColor = WHITE,
                        cursorColor = CYAN
                    )
                )
                Spacer(Modifier.width(7.dp))
                FloatingActionButton(onClick = { ask(input) }, modifier = Modifier.size(56.dp), containerColor = CYAN, contentColor = Color.Black) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar")
                }
            }
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Button(
                    onClick = { requestMic.launch(Manifest.permission.RECORD_AUDIO) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A1C26))
                ) {
                    Icon(Icons.Default.Mic, null, tint = CYAN)
                    Spacer(Modifier.width(5.dp))
                    Text("HABLAR", color = CYAN, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        if (wakeMode) {
                            context.stopService(Intent(context, JarvisWakeService::class.java))
                            wakeMode = false
                            status = "READY"
                        } else {
                            if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                requestMic.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (wakeMode) CYAN else Color(0xFF0A1C26))
                ) {
                    Text(if (wakeMode) "DETENER VOZ" else "ACTIVAR JARVIS", color = if (wakeMode) Color.Black else CYAN, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showSettings) {
            SettingsDialog(apiKey, { showSettings = false }) { key ->
                apiKey = key.trim()
                context.getSharedPreferences(PREFS, 0).edit().putString(API_KEY, apiKey).apply()
                showSettings = false
            }
        }
    }
}

private fun startWakeService(context: Context) {
    val intent = Intent(context, JarvisWakeService::class.java)
    ContextCompat.startForegroundService(context, intent)
}

@Composable
private fun StatusChip(text: String) {
    Surface(shape = RoundedCornerShape(50), color = Color(0xFF071A24), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF164353))) {
        Text(text, color = CYAN, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

@Composable
private fun SettingsDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var key by remember { mutableStateOf(initial) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(22.dp), color = PANEL) {
            Column(Modifier.padding(22.dp)) {
                Text("JARVIS CONFIG", color = CYAN, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text("La clave de Gemini se guarda localmente en este dispositivo.", color = WHITE, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("Gemini API key") }, singleLine = true)
                Spacer(Modifier.height(15.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Button(onClick = { onSave(key) }) { Text("Guardar") }
                }
            }
        }
    }
}

private fun callGemini(apiKey: String, messages: List<ChatMessage>): Result<String> = runCatching {
    val contents = JSONArray()
    messages.takeLast(20).forEach { message ->
        contents.put(JSONObject().put("role", if (message.role == "model") "model" else "user").put("parts", JSONArray().put(JSONObject().put("text", message.text))))
    }
    val body = JSONObject()
        .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", "Eres JARVIS, un asistente personal en español. Responde natural, útil y conciso."))))
        .put("contents", contents)
    var last = "Error desconocido"
    for (attempt in 0..3) {
        val connection = (URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-goog-api-key", apiKey)
        }
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code in 200..299) {
                val text = JSONObject(response).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).optString("text")
                if (text.isBlank()) throw IllegalStateException("Respuesta vacía")
                return@runCatching text
            }
            last = "HTTP $code"
            val detail = runCatching { JSONObject(response).optJSONObject("error")?.optString("message") }.getOrNull()
            if (!detail.isNullOrBlank()) last = "HTTP $code: $detail"
            if (code !in listOf(429, 500, 502, 503, 504) || attempt == 3) break
            Thread.sleep(1200L * (1L shl attempt))
        } finally {
            connection.disconnect()
        }
    }
    throw IllegalStateException(last)
}
