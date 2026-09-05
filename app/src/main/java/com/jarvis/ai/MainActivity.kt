package com.jarvis.ai

import android.Manifest
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
import java.util.Locale

private val Bg = Color(0xFF05070B)
private val Panel = Color(0xFF0B1018)
private val Accent = Color(0xFF55D6FF)
private val TextMain = Color(0xFFE8F7FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JarvisApp() }
    }
}

@Composable
fun JarvisApp() {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("Buenos días. Soy JARVIS. ¿En qué puedo ayudarte?") }
    var listening by remember { mutableStateOf(false) }

    val tts = remember {
        TextToSpeech(context) { }
    }
    DisposableEffect(Unit) {
        tts.language = Locale("es", "ES")
        onDispose { tts.shutdown() }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        listening = false
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!text.isNullOrBlank()) input = text
    }

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            listening = true
            voiceLauncher.launch(
                android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Te escucho…")
                }
            )
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Panel)) {
        Column(
            modifier = Modifier.fillMaxSize().background(Bg).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("JARVIS", color = Accent, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("AI ASSISTANT", color = Color.Gray, fontSize = 11.sp, letterSpacing = 3.sp)
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Settings, "Configuración", tint = Color.Gray)
                }
            }

            Spacer(Modifier.height(35.dp))

            Box(
                modifier = Modifier.size(190.dp).shadow(22.dp, CircleShape).background(Panel, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(145.dp).background(Color(0xFF0E1C27), CircleShape), contentAlignment = Alignment.Center) {
                    Text(if (listening) "●" else "J", color = Accent, fontSize = 58.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(25.dp))
            Text(if (listening) "ESCUCHANDO…" else "JARVIS ONLINE", color = Accent, fontSize = 13.sp, letterSpacing = 2.sp)

            Spacer(Modifier.height(25.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Panel
            ) {
                Text(answer, color = TextMain, fontSize = 17.sp, modifier = Modifier.padding(18.dp))
            }

            Spacer(Modifier.weight(1f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Habla con JARVIS…", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Color(0xFF25313C),
                        focusedTextColor = TextMain,
                        unfocusedTextColor = TextMain
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            answer = "He recibido: ${input.trim()}\n\nConecta tu backend de IA para que JARVIS genere respuestas reales."
                            tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
                            input = ""
                        }
                    },
                    containerColor = Accent
                ) { Icon(Icons.Default.Send, "Enviar", tint = Color.Black) }
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = { micPermission.launch(Manifest.permission.RECORD_AUDIO) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF101A23)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Mic, "Micrófono", tint = Accent)
                Spacer(Modifier.width(10.dp))
                Text(if (listening) "Escuchando…" else "HABLAR CON JARVIS", color = Accent, fontWeight = FontWeight.Bold)
            }
        }
    }
}
