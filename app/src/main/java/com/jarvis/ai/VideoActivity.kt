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
import java.util.UUID
import java.util.regex.Pattern

// No Cloudflare Worker is required. Android can call the public Gradio Space directly.
private const val DEFAULT_BACKEND = "https://cbensimon-wan2-2-fp8da-aoti-preview2.hf.space"
private const val FN_NAME = "generate_video"

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
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("JARVIS VIDEO AI", style = MaterialTheme.typography.headlineMedium)
            Text("Wan 2.2 · Image → Video · gratuito", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = backend, onValueChange = { backend = it }, label = { Text("Servidor gratuito") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Text(if (image == null) "🖼️ Seleccionar imagen" else "✓ Imagen seleccionada")
            }
            OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("Movimiento / escena") }, modifier = Modifier.fillMaxWidth(), minLines = 5)
            Text("Duración (Wan gratuito: aproximadamente 5 s por generación)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15).forEach { d -> FilterChip(selected = duration == d, onClick = { duration = d }, label = { Text("${d}s") }) }
            }
            Button(
                onClick = {
                    val base = backend.trim().removeSuffix("/")
                    context.getSharedPreferences("jarvis_video", 0).edit().putString("backend", base).apply()
                    status = "GENERANDO… (puede tardar 1–2 min si la GPU gratuita está fría)"
                    videoUrl = null
                    scope.launch {
                        runCatching { generateVideo(context, base, image!!, prompt, duration) }
                            .onSuccess { videoUrl = it; status = "COMPLETADO" }
                            .onFailure { status = "ERROR: ${it.message ?: "fallo desconocido"}" }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = image != null && !status.startsWith("GENERANDO")
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
    // The public free Space currently exposes the Gradio API used by NiftyVid.
    // Native Android is not subject to browser CORS, so no Cloudflare proxy is needed.
    val imagePath = uploadImage(context, backend, image)
    val eventId = submitJob(backend, imagePath, image.lastPathSegment ?: "input.jpg", prompt, duration)
    readResultStream(backend, eventId)
}

private fun uploadImage(context: Context, backend: String, image: Uri): String {
    val boundary = "----JarvisUpload${UUID.randomUUID()}"
    val conn = (URL("$backend/gradio_api/upload").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"; doOutput = true; connectTimeout = 30000; readTimeout = 120000
        setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
    }
    DataOutputStream(conn.outputStream).use { out ->
        val bytes = context.contentResolver.openInputStream(image)?.use { it.readBytes() } ?: error("No se pudo leer la imagen")
        writeFile(out, boundary, "files", "input.jpg", "image/jpeg", bytes)
        out.writeBytes("--$boundary--\r\n")
    }
    val code = conn.responseCode
    val body = (if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)).bufferedReader().readText()
    conn.disconnect()
    if (code !in 200..299) error("Subida HTTP $code: ${body.takeLast(800)}")
    return Regex("\\\"([^\\\"]+)\\\"").find(body)?.groupValues?.get(1) ?: error("La subida no devolvió la ruta de imagen.")
}

private fun submitJob(backend: String, imagePath: String, originalName: String, prompt: String, duration: Int): String {
    val inputImage = "{\"path\":${jsonQuote(imagePath)},\"url\":${jsonQuote(\"$backend/gradio_api/file=$imagePath\")},\"orig_name\":${jsonQuote(originalName)},\"size\":null,\"mime_type\":\"image/jpeg\",\"meta\":{\"_type\":\"gradio.FileData\"}}"
    val seconds = if (duration <= 5) 5.0 else 5.0
    val negative = "色调艳丽, 过曝, 静态, 细节模糊不清, 字幕, 风格, 作品, 画作, 画面, 静止, 整体发灰, 最差质量, 低质量, JPEG压缩残留, 丑陋的, 残缺的, 多余的手指, 画得不好的手部, 画得不好的脸部, 畸形的, 静止不动的画面, 杂乱的背景, 三条腿, 背景人很多, 倒着走"
    val data = "[{image},null,{prompt},6,{negative},$seconds,1,1,42,true,6,\"UniPCMultistep\",3.0,16,false,true]"
        .replace("{image}", inputImage)
        .replace("{prompt}", jsonQuote(prompt))
        .replace("{negative}", jsonQuote(negative))
    val body = "{\"data\":$data}"
    val conn = (URL("$backend/gradio_api/call/$FN_NAME").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"; doOutput = true; connectTimeout = 30000; readTimeout = 60000
        setRequestProperty("Content-Type", "application/json")
    }
    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
    val code = conn.responseCode
    val response = (if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)).bufferedReader().readText()
    conn.disconnect()
    if (code !in 200..299) error("Cola HTTP $code: ${response.takeLast(1000)}")
    return Regex("\\\"event_id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").find(response)?.groupValues?.get(1)
        ?: error("El servidor no devolvió event_id.")
}

private fun readResultStream(backend: String, eventId: String): String {
    val conn = (URL("$backend/gradio_api/call/$FN_NAME/$eventId").openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"; connectTimeout = 30000; readTimeout = 12 * 60 * 1000
        setRequestProperty("Accept", "text/event-stream")
    }
    val code = conn.responseCode
    if (code !in 200..299) {
        val error = (conn.errorStream ?: conn.inputStream).bufferedReader().readText()
        conn.disconnect(); error("Resultado HTTP $code: ${error.takeLast(1000)}")
    }
    var event = ""
    var data = ""
    conn.inputStream.bufferedReader().useLines { lines ->
        for (line in lines) {
            when {
                line.startsWith("event:") -> event = line.substringAfter(':').trim()
                line.startsWith("data:") -> data += line.substringAfter(':').trim()
                line.isBlank() -> {
                    if (event == "complete" || event == "error") {
                        if (event == "error") error("Wan devolvió un error: $data")
                        val url = extractVideoUrl(data, backend)
                        if (url != null) return@useLines
                    }
                    event = ""; data = ""
                }
            }
            val direct = extractVideoUrl(data, backend)
            if (event == "complete" && direct != null) { data = direct; break }
        }
    }
    conn.disconnect()
    val url = extractVideoUrl(data, backend) ?: error("Wan terminó sin devolver un vídeo.")
    return url
}

private fun extractVideoUrl(text: String, backend: String): String? {
    val absolute = Regex("https?://[^\\\"\\s]+(?:\\.mp4|/gradio_api/file=)[^\\\"\\s]*").find(text)?.value
    if (absolute != null) return absolute.replace("\\u0026", "&")
    val path = Regex("/tmp/gradio/[^\\\"\\s]+\\.(?:mp4|webm)").find(text)?.value
    return path?.let { "$backend/gradio_api/file=$it" }
}

private fun jsonQuote(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""

private fun writeFile(out: DataOutputStream, boundary: String, field: String, filename: String, mime: String, bytes: ByteArray) {
    out.writeBytes("--$boundary\r\nContent-Disposition: form-data; name=\"$field\"; filename=\"$filename\"\r\nContent-Type: $mime\r\n\r\n")
    out.write(bytes); out.writeBytes("\r\n")
}
