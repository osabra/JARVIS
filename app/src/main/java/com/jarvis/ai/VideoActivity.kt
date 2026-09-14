package com.jarvis.ai

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

private const val DEFAULT_BACKEND = "https://nifty-vid.workers.dev"

class VideoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VideoScreen() }
    }
}

@Composable
private fun VideoScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var backend by remember { mutableStateOf(context.getSharedPreferences("jarvis_video", 0).getString("backend", DEFAULT_BACKEND) ?: DEFAULT_BACKEND) }
    var image by remember { mutableStateOf<Uri?>(null) }
    var prompt by remember { mutableStateOf("Cinematic live-action battle. The characters move continuously and naturally, run, dodge attacks and react to each other. Dynamic tracking camera, realistic physics, dust, sparks, dramatic lighting, detailed CGI, no static poses.") }
    var duration by remember { mutableIntStateOf(5) }
    var status by remember { mutableStateOf("LISTO · GRATIS") }
    var videoUrl by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { image = it }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("JARVIS VIDEO AI", style = MaterialTheme.typography.headlineMedium)
            Text("Wan 2.2 · Image → Video · gratuito", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = backend, onValueChange = { backend = it }, label = { Text("Backend gratuito") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Text(if (image == null) "🖼️ Seleccionar imagen" else "✓ Imagen seleccionada")
            }
            OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("Movimiento / escena") }, modifier = Modifier.fillMaxWidth(), minLines = 5)
            Text("Duración")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15).forEach { d ->
                    FilterChip(selected = duration == d, onClick = { duration = d }, label = { Text("${d}s") })
                }
            }
            Button(
                onClick = {
                    val base = backend.trim().removeSuffix("/")
                    context.getSharedPreferences("jarvis_video", 0).edit().putString("backend", base).apply()
                    status = "GENERANDO…"
                    videoUrl = null
                    scope.launch {
                        runCatching { generateVideo(context, base, image!!, prompt, duration) }
                            .onSuccess { videoUrl = it; status = "COMPLETADO" }
                            .onFailure { status = "ERROR: ${it.message ?: "fallo desconocido"}" }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = image != null && status != "GENERANDO…"
            ) { Text("🎬 GENERAR VÍDEO") }
            Text(status)
            videoUrl?.let { url ->
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    factory = { ctx -> VideoView(ctx).apply { layoutParams = ViewGroup.LayoutParams(-1, -1); setMediaController(MediaController(ctx)); setVideoURI(Uri.parse(url)); start() } },
                    update = { it.setVideoURI(Uri.parse(url)); it.start() }
                )
            }
        }
    }
}

private suspend fun generateVideo(context: Context, backend: String, image: Uri, prompt: String, duration: Int): String = withContext(Dispatchers.IO) {
    val boundary = "----JarvisBoundary${System.currentTimeMillis()}"
    val conn = (URL("$backend/generate").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        connectTimeout = 30000
        readTimeout = 10 * 60 * 1000
        setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        setRequestProperty("Accept", "text/event-stream")
    }
    DataOutputStream(conn.outputStream).use { out ->
        val bytes = context.contentResolver.openInputStream(image)?.use { it.readBytes() } ?: error("No se pudo leer la imagen")
        writeFile(out, boundary, "image", "image.jpg", "image/jpeg", bytes)
        val params = "{\"prompt\":${jsonQuote(prompt)},\"duration_seconds\":$duration,\"steps\":6}"
        writeField(out, boundary, "params", params)
        out.writeBytes("--$boundary--\r\n")
    }
    val code = conn.responseCode
    val body = (if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)).bufferedReader().readText()
    conn.disconnect()
    if (code !in 200..299) error("Servidor HTTP $code: ${body.takeLast(1200)}")
    extractVideoUrl(body)
}

private fun extractVideoUrl(body: String): String {
    val pattern = Pattern.compile("https?://[^\\\"\\s]+(?:\\.mp4|/file=|/gradio_api/file=)[^\\\"\\s]*")
    val matcher = pattern.matcher(body)
    var last: String? = null
    while (matcher.find()) last = matcher.group()
    return last?.replace("\\u0026", "&") ?: error("El servidor no devolvió una URL de vídeo.")
}

private fun jsonQuote(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""

private fun writeField(out: DataOutputStream, boundary: String, name: String, value: String) {
    out.writeBytes("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n")
}

private fun writeFile(out: DataOutputStream, boundary: String, field: String, filename: String, mime: String, bytes: ByteArray) {
    out.writeBytes("--$boundary\r\nContent-Disposition: form-data; name=\"$field\"; filename=\"$filename\"\r\nContent-Type: $mime\r\n\r\n")
    out.write(bytes)
    out.writeBytes("\r\n")
}
