package com.jarvis.ai

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
private const val MODEL = "gemini-2.5-flash"
private val Bg = Color(0xFF05070B)
private val Panel = Color(0xFF0B1018)
private val Accent = Color(0xFF55D6FF)
private val TextMain = Color(0xFFE8F7FF)

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
    var answer by remember { mutableStateOf("Buenos días. Soy JARVIS. ¿En qué puedo ayudarte?") }
    var listening by remember { mutableStateOf(false) }
    var thinking by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var apiKey by remember { mutableStateOf(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(API_KEY, "") ?: "") }
    val history = remember { mutableStateListOf<ChatMessage>() }
    val tts = remember { TextToSpeech(context) {} }
    DisposableEffect(Unit) { tts.language = Locale("es", "ES"); onDispose { tts.shutdown() } }
    fun speak(text: String) { tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis") }
    fun askJarvis(prompt: String) {
        if (prompt.isBlank() || thinking) return
        if (apiKey.isBlank()) { answer = "Necesito una clave de Gemini. Pulsa ⚙️ para configurarla."; speak(answer); return }
        val userText = prompt.trim(); input = ""; history.add(ChatMessage("user", userText)); thinking = true; answer = "Estoy pensando…"
        scope.launch {
            val result = withContext(Dispatchers.IO) { callGemini(apiKey, history.toList()) }
            thinking = false
            result.onSuccess { text -> history.add(ChatMessage("model", text)); answer = text; speak(text) }
                .onFailure { error -> history.removeLastOrNull(); answer = "No he podido conectar con la IA: ${error.message ?: "error desconocido"}"; speak(answer) }
        }
    }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        listening = false
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { askJarvis(it) }
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { listening = true; voiceLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Te escucho…")
        }) }
    }
    MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Panel)) {
        Column(Modifier.fillMaxSize().background(Bg).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("JARVIS", color = Accent, fontSize = 30.sp, fontWeight = FontWeight.Bold); Text("AI ASSISTANT", color = Color.Gray, fontSize = 11.sp, letterSpacing = 3.sp) }
                IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, "Configuración", tint = Color.Gray) }
            }
            Spacer(Modifier.height(35.dp))
            Box(Modifier.size(190.dp).shadow(22.dp, CircleShape).background(Panel, CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(145.dp).background(Color(0xFF0E1C27), CircleShape), contentAlignment = Alignment.Center) {
                    Text(when { listening -> "●"; thinking -> "…"; else -> "J" }, color = Accent, fontSize = 58.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(25.dp)); Text(when { listening -> "ESCUCHANDO…"; thinking -> "PENSANDO…"; else -> "JARVIS ONLINE" }, color = Accent, fontSize = 13.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(25.dp))
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Panel) { Text(answer, color = TextMain, fontSize = 17.sp, modifier = Modifier.padding(18.dp)) }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Habla con JARVIS…", color = Color.Gray) }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = Color(0xFF25313C), focusedTextColor = TextMain, unfocusedTextColor = TextMain), shape = RoundedCornerShape(18.dp), singleLine = true)
                Spacer(Modifier.width(8.dp)); FloatingActionButton(onClick = { askJarvis(input) }, containerColor = Accent) { Icon(Icons.Default.Send, "Enviar", tint = Color.Black) }
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = { micPermission.launch(Manifest.permission.RECORD_AUDIO) }, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF101A23)), shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Default.Mic, "Micrófono", tint = Accent); Spacer(Modifier.width(10.dp)); Text(if (listening) "Escuchando…" else "HABLAR CON JARVIS", color = Accent, fontWeight = FontWeight.Bold)
            }
        }
        if (showSettings) SettingsDialog(apiKey, { showSettings = false }) { newKey -> apiKey = newKey.trim(); context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(API_KEY, apiKey).apply(); showSettings = false }
    }
}

@Composable
private fun SettingsDialog(initialKey: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var key by remember { mutableStateOf(initialKey) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(22.dp), color = Panel) {
            Column(Modifier.padding(22.dp)) {
                Text("Configuración de JARVIS", color = Accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp)); Text("Introduce tu clave de Google AI Studio. Se guarda solo en este dispositivo. No la incluyas en GitHub.", color = TextMain, fontSize = 14.sp)
                Spacer(Modifier.height(14.dp)); OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("Gemini API key") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = Color(0xFF25313C), focusedTextColor = TextMain, unfocusedTextColor = TextMain, focusedLabelColor = Accent, unfocusedLabelColor = Color.Gray))
                Spacer(Modifier.height(16.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onDismiss) { Text("Cancelar") }; Button(onClick = { onSave(key) }) { Text("Guardar") } }
            }
        }
    }
}

private fun callGemini(apiKey: String, messages: List<ChatMessage>): Result<String> = runCatching {
    val connection = (URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"; connectTimeout = 20_000; readTimeout = 60_000; doOutput = true
        setRequestProperty("Content-Type", "application/json"); setRequestProperty("x-goog-api-key", apiKey)
    }
    val contents = JSONArray()
    messages.takeLast(20).forEach { m -> contents.put(JSONObject().put("role", if (m.role == "model") "model" else "user").put("parts", JSONArray().put(JSONObject().put("text", m.text)))) }
    val body = JSONObject().put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", "Tu nombre es JARVIS. Eres un asistente personal en español, claro, útil y natural. Responde de forma concisa salvo que el usuario pida más detalle.")))).put("contents", contents)
    connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
    val code = connection.responseCode; val stream = if (code in 200..299) connection.inputStream else connection.errorStream
    val response = stream.bufferedReader().use { it.readText() }; connection.disconnect()
    if (code !in 200..299) throw IllegalStateException("HTTP $code")
    val text = JSONObject(response).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).optString("text")
    if (text.isBlank()) throw IllegalStateException("Respuesta vacía"); text
}
