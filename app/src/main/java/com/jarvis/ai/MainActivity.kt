package com.jarvis.ai

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
private val BG = Color(0xFF010409)
private val PANEL = Color(0xFF06131D)
private val PANEL2 = Color(0xFF091D29)
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
    var answer by remember { mutableStateOf("Sistemas nominales. Núcleo holográfico activo.") }
    var status by remember { mutableStateOf("ONLINE") }
    var thinking by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var apiKey by remember { mutableStateOf(context.getSharedPreferences(PREFS, 0).getString(API_KEY, "") ?: "") }
    var wakeMode by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<ChatMessage>() }
    val tts = remember { TextToSpeech(context) {} }

    DisposableEffect(Unit) {
        onDispose { tts.shutdown() }
    }

    fun speak(text: String) {
        tts.language = Locale("es", "ES")
        tts.speak(text.take(2500), TextToSpeech.QUEUE_FLUSH, null, "jarvis")
    }

    fun ask(prompt: String) {
        val p = prompt.trim()
        if (p.isBlank() || thinking) return
        input = ""
        if (p.equals("quién eres", true) || p.equals("quien eres", true)) {
            answer = "Soy JARVIS. Mi núcleo holográfico 3D está activo y listo."
            status = "ONLINE"
            speak(answer)
            return
        }
        if (apiKey.isBlank()) {
            answer = "Configura tu clave de Gemini en Ajustes para activar la IA."
            status = "CONFIG"
            return
        }
        history.add(ChatMessage("user", p))
        thinking = true
        status = "THINKING"
        answer = "Procesando…"
        scope.launch {
            val result = withContext(Dispatchers.IO) { callGemini(apiKey, history.toList()) }
            thinking = false
            result.onSuccess {
                history.add(ChatMessage("model", it))
                answer = it
                status = "ONLINE"
                speak(it)
            }.onFailure {
                if (history.isNotEmpty()) history.removeAt(history.lastIndex)
                answer = "Error de conexión con la IA: ${it.message ?: "error desconocido"}"
                status = "ERROR"
            }
        }
    }

    val requestMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        wakeMode = granted
        status = if (granted) "LISTENING" else "ONLINE"
        if (!granted) answer = "Necesito permiso de micrófono para activar la voz."
    }

    val animation = rememberInfiniteTransition(label = "jarvis_core")
    val angle by animation.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "angle")
    val reverse by animation.animateFloat(360f, 0f, infiniteRepeatable(tween(13000, easing = LinearEasing)), label = "reverse")
    val pulse by animation.animateFloat(0.97f, 1.04f, infiniteRepeatable(tween(if (thinking) 380 else 1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val wave by animation.animateFloat(0f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "wave")

    MaterialTheme(colorScheme = darkColorScheme(background = BG, surface = PANEL, primary = CYAN, onPrimary = Color.Black)) {
        Column(
            Modifier.fillMaxSize().background(BG).padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("JARVIS", color = WHITE, fontSize = 29.sp, fontWeight = FontWeight.Black, letterSpacing = 7.sp)
                    Text("HOLOGRAPHIC INTELLIGENCE CORE  //  3D", color = CYAN2, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                }
                Surface(shape = CircleShape, color = PANEL2, border = androidx.compose.foundation.BorderStroke(1.dp, CYAN2)) {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = CYAN)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("●  $status", color = CYAN, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text(if (wakeMode) "VOICE LINK / ACTIVE" else "VOICE LINK / STANDBY", color = MUTED, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            }

            Spacer(Modifier.height(4.dp))
            Box(Modifier.size(292.dp), contentAlignment = Alignment.Center) {
                HolographicCore3D(
                    status = status,
                    pulse = pulse,
                    angle = angle,
                    reverseAngle = reverse,
                    wave = wave
                )
            }
            Text("3D HOLOGRAPHIC CORE  //  ANIMATED", color = CYAN2, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            Spacer(Modifier.height(7.dp))

            Surface(
                Modifier.fillMaxWidth().weight(1f).border(1.dp, Color(0xFF173B49), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = PANEL
            ) {
                Column(Modifier.fillMaxSize().padding(13.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("NEURAL FEED", color = CYAN, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Spacer(Modifier.weight(1f))
                        Text("SECURE LINK", color = MUTED, fontSize = 6.sp, letterSpacing = 1.sp)
                    }
                    Spacer(Modifier.height(9.dp))
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = PANEL2) {
                        Column(Modifier.padding(12.dp)) {
                            Text("JARVIS CORE", color = CYAN2, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            Spacer(Modifier.height(5.dp))
                            Text(answer, color = WHITE, fontSize = 13.sp, lineHeight = 19.sp)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        repeat(20) { i ->
                            Box(Modifier.weight(1f).height((3 + ((i * 7) % 7)).dp).background(CYAN2.copy(alpha = if (thinking && i % 2 == 0) .65f else .22f)))
                        }
                    }
                }
            }

            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Habla con JARVIS…", color = MUTED) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CYAN, unfocusedBorderColor = CYAN2, focusedTextColor = WHITE, unfocusedTextColor = WHITE)
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = { ask(input) }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = CYAN, contentColor = Color.Black)) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar")
                }
            }
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { if (wakeMode) { wakeMode = false; status = "ONLINE" } else requestMic.launch(Manifest.permission.RECORD_AUDIO) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PANEL2, contentColor = CYAN)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text(if (wakeMode) "PARAR" else "HABLAR", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { speak(answer) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = CYAN, contentColor = Color.Black)
                ) { Text("DECIR JARVIS", fontWeight = FontWeight.Bold) }
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Ajustes JARVIS") },
            text = {
                Column {
                    Text("Clave de Gemini", fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, singleLine = true, placeholder = { Text("Pega tu clave aquí") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(API_KEY, apiKey.trim()).apply()
                    showSettings = false
                    status = "ONLINE"
                }) { Text("GUARDAR") }
            },
            dismissButton = { TextButton(onClick = { showSettings = false }) { Text("CANCELAR") } }
        )
    }
}

private fun callGemini(apiKey: String, history: List<ChatMessage>): Result<String> = runCatching {
    val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey")
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 20000
        readTimeout = 60000
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
    }
    val contents = JSONArray()
    history.takeLast(12).forEach { msg ->
        contents.put(JSONObject().apply {
            put("role", if (msg.role == "model") "model" else "user")
            put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
        })
    }
    val body = JSONObject().put("contents", contents)
    connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
    val code = connection.responseCode
    val stream = if (code in 200..299) connection.inputStream else connection.errorStream
    val response = stream.bufferedReader().use { it.readText() }
    if (code !in 200..299) error("HTTP $code: $response")
    val root = JSONObject(response)
    root.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
}
