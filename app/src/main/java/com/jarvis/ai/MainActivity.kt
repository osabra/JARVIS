package com.jarvis.ai

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JarvisVideoScreen() }
    }
}

@Composable
private fun JarvisVideoScreen() {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var prompt by remember {
        mutableStateOf(
            "Cinematic live-action battle. The characters run, dodge and attack naturally. " +
                "Dynamic camera, realistic physics, dust, sparks, detailed lighting and continuous movement."
        )
    }
    var duration by remember { mutableStateOf("10") }
    var endpoint by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Listo para generar") }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        imageUri = uri
        if (uri != null) status = "Imagen seleccionada"
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("JARVIS", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("Video AI · Imagen → Vídeo", style = MaterialTheme.typography.titleMedium)
                Text("Crea escenas con movimiento real conectando un proveedor de vídeo IA.")

                OutlinedButton(
                    onClick = { picker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (imageUri == null) "🖼️ Seleccionar imagen" else "✓ Imagen seleccionada")
                }

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Describe la escena y el movimiento") },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Duración", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("5", "10", "15").forEach { value ->
                        FilterChip(
                            selected = duration == value,
                            onClick = { duration = value },
                            label = { Text("$value s") }
                        )
                    }
                }

                OutlinedTextField(
                    value = endpoint,
                    onValueChange = { endpoint = it },
                    label = { Text("Endpoint Image-to-Video") },
                    placeholder = { Text("https://tu-servidor/generate") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        status = when {
                            imageUri == null -> "Selecciona una imagen."
                            endpoint.isBlank() -> "Configura el endpoint del proveedor."
                            apiKey.isBlank() -> "Introduce la API key."
                            else -> "Solicitud preparada para $duration s. Falta implementar el adaptador específico del proveedor."
                        }
                    },
                    enabled = imageUri != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🎬 GENERAR VÍDEO")
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Estado", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(status)
                    }
                }

                Text(
                    "Seguridad: no incluyas claves privadas en GitHub. La versión de producción debe usar un backend propio para ocultar las credenciales del proveedor.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
