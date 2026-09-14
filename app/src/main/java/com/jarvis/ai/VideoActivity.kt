package com.jarvis.ai

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

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
    var backend by remember { mutableStateOf(context.getSharedPreferences("jarvis_openai", 0).getString("video_backend", "") ?: "") }
    var image by remember { mutableStateOf<Uri?>(null) }
    var prompt by remember { mutableStateOf("Cinematic live-action battle. The characters move continuously and naturally, run, dodge attacks and react to each other. Dynamic tracking camera, realistic physics, dust, sparks, dramatic lighting, detailed CGI, no static poses.") }
    var duration by remember { mutableStateOf(5) }
    var status by remember { mutableStateOf("LISTO") }
    var videoUrl by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { image = it }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("JARVIS VIDEO AI", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(backend, { backend = it }, label = { Text("URL del backend") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button({ picker.launch("image/*") }, Modifier.fillMaxWidth()) { Text(if (image == null) "Seleccionar imagen" else "✓ Imagen seleccionada") }
            OutlinedTextField(prompt, { prompt = it }, label = { Text("Movimiento / escena") }, modifier = Modifier.fillMaxWidth(), minLines = 5)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(duration == 5, { duration = 5 }, label = { Text("5 s") })
                FilterChip(duration == 10, { duration = 10 }, label = { Text("10 s") })
            }
            Button(
                enabled = image != null && backend.isNotBlank() && status != "GENERANDO",
                onClick = {
                    context.getSharedPreferences("jarvis_openai", 0).edit().putString("video_backend", backend.trim().removeSuffix("/")).apply()
                    status = "GENERANDO"
                    videoUrl = null
                    scope.launch {
                        runCatching { generateVideo(context, backend.trim().removeSuffix("/"), image!!, prompt, duration) }
                            .onSuccess { videoUrl = it; status = "COMPLETADO" }
                            .onFailure { status = "ERROR: ${it.message ?: "fallo desconocido"}" }
                    }
                }, Modifier.fillMaxWidth()
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
    val conn = (URL("$backend/v1/image-to-video").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        connectTimeout = 30000
        readTimeout = 120000
        setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
    }
    DataOutputStream(conn.outputStream).use { out ->
        writeField(out, boundary, "prompt", prompt)
        writeField(out, boundary, "duration", duration.toString())
        out.writeBytes("--$boundary\r\nContent-Disposition: form-data; name=\"image\"; filename=\"image.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n")
        context.contentResolver.openInputStream(image)?.use { it.copyTo(out) } ?: error("No se pudo leer la imagen")
        out.writeBytes("\r\n--$boundary--\r\n")
    }
    val code = conn.responseCode
    val body = (if (code in 200..299) conn.inputStream else conn.errorStream).bufferedReader().readText()
    if (code !in 200..299) error(JSONObject(body).optString("error", "Servidor HTTP $code"))
    val taskId = JSONObject(body).getString("taskId")
    conn.disconnect()

    repeat(120) {
        delay(3000)
        val poll = (URL("$backend/v1/tasks/$taskId").openConnection() as HttpURLConnection).apply { requestMethod = "GET" }
        val pollCode = poll.responseCode
        val text = (if (pollCode in 200..299) poll.inputStream else poll.errorStream).bufferedReader().readText()
        poll.disconnect()
        if (pollCode !in 200..299) error("No se pudo consultar la tarea")
        val json = JSONObject(text)
        val status = json.optString("status")
        if (status.equals("SUCCEEDED", true)) {
            val output = json.optJSONArray("output") ?: error("Runway no devolvió vídeo")
            return@withContext output.getString(0)
        }
        if (status.equals("FAILED", true) || status.equals("CANCELLED", true)) error("Runway terminó con estado $status")
    }
    error("Tiempo de espera agotado")
}

private fun writeField(out: DataOutputStream, boundary: String, name: String, value: String) {
    out.writeBytes("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n")
}
