package com.jarvis.ai

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
private val Bg = Color(0xFF02050A)
private val Panel = Color(0xFF07111A)
private val Panel2 = Color(0xFF0B1822)
private val Accent = Color(0xFF55D6FF)
private val AccentSoft = Color(0xFF123B4A)
private val TextMain = Color(0xFFE8F7FF)

data class ChatMessage(val role: String, val text: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { JarvisApp() } }
}

@Composable
fun JarvisApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val handler = remember { Handler(Looper.getMainLooper()) }
    var input by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("Buenos días. Soy JARVIS. ¿En qué puedo ayudarte?") }
    var listening by remember { mutableStateOf(false) }
    var wakeMode by remember { mutableStateOf(false) }
    var thinking by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var apiKey by remember { mutableStateOf(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(API_KEY, "") ?: "") }
    val history = remember { mutableStateListOf<ChatMessage>() }
    val tts = remember { TextToSpeech(context) {} }
    val recognizer = remember { if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null }

    DisposableEffect(Unit) { tts.language = Locale("es", "ES"); onDispose { tts.shutdown() } }
    DisposableEffect(Unit) { onDispose { recognizer?.destroy(); handler.removeCallbacksAndMessages(null) } }
    fun speak(text: String) { tts.speak(text.take(2500), TextToSpeech.QUEUE_FLUSH, null, "jarvis") }
    fun startRecognition() { recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES"); putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false); putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3) }) }
    fun askJarvis(prompt: String) {
        if (prompt.isBlank() || thinking) return
        val local = JarvisActions.execute(context, prompt)
        if (local != null) { input = ""; answer = local; speak(local); return }
        if (apiKey.isBlank()) { answer = "Necesito una clave de Gemini. Pulsa ⚙ para configurarla."; speak(answer); return }
        val userText = prompt.trim(); input = ""; history.add(ChatMessage("user", userText)); thinking = true; answer = "Estoy procesando tu solicitud…"
        scope.launch {
            val result = withContext(Dispatchers.IO) { callGemini(apiKey, history.toList()) }
            thinking = false
            result.onSuccess { text -> history.add(ChatMessage("model", text)); answer = text; speak(text) }
                .onFailure { error -> history.removeLastOrNull(); answer = "No he podido conectar con la IA: ${error.message ?: "error desconocido"}"; speak(answer) }
        }
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) { listening = true; startRecognition() } else answer = "Necesito permiso para usar el micrófono." }
    val wakePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) wakeMode = true else answer = "Necesito permiso de micrófono para activar JARVIS." }
    LaunchedEffect(wakeMode) {
        if (!wakeMode || recognizer == null) { recognizer?.cancel(); listening = false; return@LaunchedEffect }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true }; override fun onBeginningOfSpeech() { listening = true }; override fun onRmsChanged(rmsdB: Float) = Unit; override fun onBufferReceived(buffer: ByteArray?) = Unit; override fun onEndOfSpeech() { listening = false }; override fun onPartialResults(partialResults: Bundle?) = Unit; override fun onEvent(eventType: Int, params: Bundle?) = Unit
            override fun onError(error: Int) { listening = false; if (wakeMode) handler.postDelayed({ if (wakeMode) startRecognition() }, 700) }
            override fun onResults(results: Bundle?) { listening = false; val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty(); val lower = heard.lowercase(Locale.getDefault()); if (lower.contains("jarvis")) { val start = lower.indexOf("jarvis") + 6; val command = heard.substring(start).trim().trim(',', '.', ':', ';'); if (command.isBlank()) { answer = "Te escucho."; speak("Te escucho.") } else askJarvis(command) }; if (wakeMode) handler.postDelayed({ if (wakeMode) startRecognition() }, 500) }
        }); answer = "Modo JARVIS activo. Di: «Jarvis…»"; startRecognition()
    }

    MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Panel, primary = Accent, onPrimary = Color.Black)) {
        Column(Modifier.fillMaxSize().background(Bg).padding(horizontal = 20.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("JARVIS", color = Accent, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp); Text("PERSONAL AI SYSTEM", color = Color(0xFF63808E), fontSize = 10.sp, letterSpacing = 3.sp) }
                Surface(shape = CircleShape, color = Panel2, modifier = Modifier.border(1.dp, AccentSoft, CircleShape)) { IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, "Configuración", tint = Accent) } }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatusChip(if (wakeMode) "● LISTO PARA ESCUCHAR" else "● SISTEMA ONLINE"); StatusChip(if (thinking) "PROCESANDO" else "GEMINI CORE") }
            Spacer(Modifier.height(18.dp))
            val transition = rememberInfiniteTransition(label = "core")
            val pulse by transition.animateFloat(1f, 1.10f, infiniteRepeatable(tween(if (thinking || listening) 650 else 1400), RepeatMode.Reverse), label = "pulse")
            Box(Modifier.size(236.dp).scale(if (thinking || listening || wakeMode) pulse else 1f).shadow(30.dp, CircleShape).border(2.dp, AccentSoft, CircleShape).background(Panel, CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(192.dp).border(1.dp, Accent, CircleShape).background(Color(0xFF061923), CircleShape), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(150.dp).border(2.dp, AccentSoft, CircleShape).background(Color(0xFF07141D), CircleShape), contentAlignment = Alignment.Center) { Text(if (listening) "◉" else if (thinking) "…" else "J", color = Accent, fontSize = 62.sp, fontWeight = FontWeight.Bold) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(when { listening -> "ESCUCHANDO"; thinking -> "PROCESANDO"; wakeMode -> "JARVIS EN ESPERA"; else -> "JARVIS ONLINE" }, color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Spacer(Modifier.height(14.dp))
            Surface(Modifier.fillMaxWidth().weight(1f, false), shape = RoundedCornerShape(22.dp), color = Panel, tonalElevation = 4.dp) {
                Column(Modifier.padding(18.dp)) {
                    Text("JARVIS", color = Color(0xFF6D8A98), fontSize = 10.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(7.dp)); Text(answer, color = TextMain, fontSize = 16.sp, lineHeight = 23.sp)
                    Spacer(Modifier.height(12.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Suggestion("Buscar", Icons.Default.Search) { input = "busca en internet " }; Suggestion("Alexa", Icons.Default.Settings) { askJarvis("abre Alexa") } }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Escribe a JARVIS…", color = Color(0xFF58707D)) }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = Color(0xFF1A2B35), focusedTextColor = TextMain, unfocusedTextColor = TextMain, cursorColor = Accent), shape = RoundedCornerShape(20.dp), singleLine = true)
                Spacer(Modifier.width(8.dp)); FloatingActionButton(onClick = { askJarvis(input) }, containerColor = Accent, contentColor = Color.Black) { Icon(Icons.Default.Send, "Enviar") }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { micPermission.launch(Manifest.permission.RECORD_AUDIO) }, modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Panel2), shape = RoundedCornerShape(18.dp)) { Icon(Icons.Default.Mic, null, tint = Accent); Spacer(Modifier.width(7.dp)); Text("HABLAR", color = Accent, fontWeight = FontWeight.Bold) }
                Button(onClick = { if (wakeMode) wakeMode = false else wakePermission.launch(Manifest.permission.RECORD_AUDIO) }, modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = if (wakeMode) Accent else Panel2), shape = RoundedCornerShape(18.dp)) { Text(if (wakeMode) "DESACTIVAR" else "DECIR JARVIS", color = if (wakeMode) Color.Black else Accent, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(5.dp)); Text("Di «Jarvis» para activarlo y después tu comando", color = Color(0xFF58707D), fontSize = 10.sp)
        }
        if (showSettings) SettingsDialog(apiKey, wakeMode, { showSettings = false }, { newKey -> apiKey = newKey.trim(); context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(API_KEY, apiKey).apply(); showSettings = false })
    }
}

@Composable private fun StatusChip(text: String) { Surface(shape = RoundedCornerShape(50), color = Panel2, modifier = Modifier.border(1.dp, Color(0xFF17303C), RoundedCornerShape(50))) { Text(text, color = Accent, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) } }
@Composable private fun Suggestion(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) { AssistChip(onClick = onClick, label = { Text(text, fontSize = 11.sp) }, leadingIcon = { Icon(icon, null, Modifier.size(16.dp)) }) }

@Composable private fun SettingsDialog(initialKey: String, wakeMode: Boolean, onDismiss: () -> Unit, onSave: (String) -> Unit) { var key by remember { mutableStateOf(initialKey) }; Dialog(onDismissRequest = onDismiss) { Surface(shape = RoundedCornerShape(22.dp), color = Panel) { Column(Modifier.padding(22.dp)) { Text("Configuración de JARVIS", color = Accent, fontSize = 22.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text("Gemini es el cerebro de JARVIS. La clave se guarda solo en este dispositivo.", color = TextMain, fontSize = 14.sp); Spacer(Modifier.height(12.dp)); OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("Gemini API key") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = Color(0xFF25313C), focusedTextColor = TextMain, unfocusedTextColor = TextMain, focusedLabelColor = Accent, unfocusedLabelColor = Color.Gray)); Spacer(Modifier.height(10.dp)); Text(if (wakeMode) "El modo de escucha está activo." else "El modo «Decir JARVIS» se activa desde la pantalla principal.", color = Color.Gray, fontSize = 12.sp); Spacer(Modifier.height(16.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onDismiss) { Text("Cancelar") }; Button(onClick = { onSave(key) }) { Text("Guardar") } } } } } }

private fun callGemini(apiKey: String, messages: List<ChatMessage>): Result<String> = runCatching {
    val contents = JSONArray(); messages.takeLast(20).forEach { m -> contents.put(JSONObject().put("role", if (m.role == "model") "model" else "user").put("parts", JSONArray().put(JSONObject().put("text", m.text)))) }
    val body = JSONObject().put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", "Eres JARVIS, un asistente personal en español. Responde de forma natural, útil y concisa.")))).put("contents", contents)
    var lastError = "Error desconocido"
    for (attempt in 0..3) {
        val connection = (URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent").openConnection() as HttpURLConnection).apply { requestMethod = "POST"; connectTimeout = 20_000; readTimeout = 60_000; doOutput = true; setRequestProperty("Content-Type", "application/json"); setRequestProperty("x-goog-api-key", apiKey) }
        try { connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }; val code = connection.responseCode; val stream = if (code in 200..299) connection.inputStream else connection.errorStream; val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty(); if (code in 200..299) { val text = JSONObject(response).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).optString("text"); if (text.isBlank()) throw IllegalStateException("Respuesta vacía"); return@runCatching text }; lastError = "HTTP $code"; val detail = runCatching { JSONObject(response).optJSONObject("error")?.optString("message") }.getOrNull(); if (!detail.isNullOrBlank()) lastError = "HTTP $code: $detail"; if (code !in listOf(429, 500, 502, 503, 504) || attempt == 3) break; Thread.sleep(1200L * (1L shl attempt)) } finally { connection.disconnect() }
    }
    throw IllegalStateException(lastError)
}
