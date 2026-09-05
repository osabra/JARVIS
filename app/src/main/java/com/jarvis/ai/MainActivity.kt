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
private val BG = Color(0xFF010308)
private val PANEL = Color(0xFF07101A)
private val CYAN = Color(0xFF55DFFF)
private val CYAN2 = Color(0xFF167A9A)
private val WHITE = Color(0xFFEAF9FF)

data class ChatMessage(val role: String, val text: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { JarvisApp() } }
}

@Composable
fun JarvisApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("Sistemas nominales. Estoy listo.") }
    var status by remember { mutableStateOf("ONLINE") }
    var wakeMode by remember { mutableStateOf(false) }
    var thinking by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var apiKey by remember { mutableStateOf(context.getSharedPreferences(PREFS, 0).getString(API_KEY, "") ?: "") }
    val history = remember { mutableStateListOf<ChatMessage>() }
    val tts = remember { TextToSpeech(context) { it2 -> if (it2 == TextToSpeech.SUCCESS) tts.language = Locale("es", "ES") } }

    fun speak(text: String) { tts.speak(text.take(2500), TextToSpeech.QUEUE_FLUSH, null, "jarvis") }
    fun ask(prompt: String) {
        if (prompt.isBlank() || thinking) return
        val local = JarvisActions.execute(context, prompt)
        if (local != null) { input = ""; answer = local; status = "READY"; speak(local); return }
        if (apiKey.isBlank()) { answer = "Necesito configurar la clave de Gemini en Ajustes."; status = "CONFIG"; speak(answer); return }
        val p = prompt.trim(); input = ""; history.add(ChatMessage("user", p)); thinking = true; status = "THINKING"; answer = "Procesando…"
        scope.launch {
            val result = withContext(Dispatchers.IO) { callGemini(apiKey, history.toList()) }
            thinking = false
            result.onSuccess { text -> history.add(ChatMessage("model", text)); answer = text; status = "READY"; speak(text) }
                .onFailure { history.removeLastOrNull(); answer = "No he podido conectar con la IA: ${it.message ?: "error"}"; status = "ERROR"; speak(answer) }
        }
    }

    val requestMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { startWakeService(context); wakeMode = true; status = "LISTENING" }
        else { answer = "Necesito permiso de micrófono para escuchar «Jarvis»."; status = "MIC DENIED" }
    }
    val requestNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { requestMic.launch(Manifest.permission.RECORD_AUDIO) }

    val receiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.getStringExtra("type")) {
                    "READY", "WAITING" -> if (wakeMode) status = "LISTENING"
                    "LISTENING" -> status = "HEARING"
                    "WAKE" -> { status = "THINKING"; val command = intent.getStringExtra("command").orEmpty(); if (command.isBlank()) { answer = "Te escucho."; speak("Te escucho."); status = "READY" } else ask(command) }
                }
            }
        }
    }
    DisposableEffect(Unit) { val f = IntentFilter("com.jarvis.ai.WAKE_EVENT"); ContextCompat.registerReceiver(context, receiver, f, ContextCompat.RECEIVER_NOT_EXPORTED); onDispose { runCatching { context.unregisterReceiver(receiver) }; tts.shutdown() } }

    val rotation = rememberInfiniteTransition(label = "rotation")
    val angle by rotation.animateFloat(0f, 360f, infiniteRepeatable(tween(11000, easing = LinearEasing)), label = "angle")
    val pulse by rotation.animateFloat(1f, 1.07f, infiniteRepeatable(tween(if (status == "HEARING" || status == "THINKING") 500 else 1500), RepeatMode.Reverse), label = "pulse")

    MaterialTheme(colorScheme = darkColorScheme(background = BG, surface = PANEL, primary = CYAN, onPrimary = Color.Black)) {
        Column(Modifier.fillMaxSize().background(BG).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("JARVIS", color = WHITE, fontSize = 29.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp); Text("ARTIFICIAL INTELLIGENCE // CORE 02", color = CYAN2, fontSize = 8.sp, letterSpacing = 2.sp) }
                IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, null, tint = CYAN) }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { TechLabel("● $status"); TechLabel(if (wakeMode) "VOICE LINK ACTIVE" else "VOICE LINK OFF") }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.size(285.dp).scale(pulse), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().rotate(angle)) { val c = center; val r = size.minDimension * .45f; drawArc(CYAN, 8f, 72f, false, Offset(c.x-r,c.y-r), androidx.compose.ui.geometry.Size(r*2,r*2), style=androidx.compose.ui.graphics.drawscope.Stroke(2f)); drawArc(CYAN2, 190f, 55f, false, Offset(c.x-r,c.y-r), androidx.compose.ui.geometry.Size(r*2,r*2), style=androidx.compose.ui.graphics.drawscope.Stroke(3f)) }
                Canvas(Modifier.size(230.dp)) { val r = size.minDimension*.43f; drawCircle(CYAN2.copy(alpha=.12f), r); drawCircle(CYAN, r, style=androidx.compose.ui.graphics.drawscope.Stroke(2f)); drawCircle(CYAN2, r*.76f, style=androidx.compose.ui.graphics.drawscope.Stroke(1f)); drawCircle(CYAN.copy(alpha=.18f), r*.52f); for (i in 0..11) { val a = Math.toRadians((i*30).toDouble()); val p1=Offset(center.x+(r*.82f*Math.cos(a)).toFloat(),center.y+(r*.82f*Math.sin(a)).toFloat()); val p2=Offset(center.x+(r*.94f*Math.cos(a)).toFloat(),center.y+(r*.94f*Math.sin(a)).toFloat()); drawLine(CYAN,p1,p2,2f,StrokeCap.Round) } }
                Box(Modifier.size(145.dp).shadow(28.dp, CircleShape).background(Color(0xFF04121B), CircleShape).border(2.dp, CYAN, CircleShape), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(if (status=="HEARING") "◉" else if (status=="THINKING") "…" else "J", color=CYAN, fontSize=58.sp, fontWeight=FontWeight.Bold); Text(status, color=CYAN, fontSize=9.sp, letterSpacing=2.sp) } }
            }
            Text(if (wakeMode) "DI «JARVIS» PARA DESPERTARLO" else "VOICE LINK STANDBY", color = CYAN2, fontSize = 9.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(13.dp))
            Surface(Modifier.fillMaxWidth().weight(1f, false), shape=RoundedCornerShape(24.dp), color=PANEL, modifier=Modifier.border(1.dp,Color(0xFF12313D),RoundedCornerShape(24.dp))) {
                Column(Modifier.padding(18.dp)) { Text("JARVIS // RESPONSE", color=CYAN2, fontSize=9.sp, letterSpacing=2.sp); Spacer(Modifier.height(9.dp)); Text(answer, color=WHITE, fontSize=15.sp, lineHeight=22.sp); Spacer(Modifier.height(10.dp)); Text("LOCAL SYSTEMS  •  GEMINI CORE  •  VOICE", color=Color(0xFF496875), fontSize=8.sp, letterSpacing=1.sp) }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment=Alignment.CenterVertically) {
                OutlinedTextField(value=input,onValueChange={input=it},modifier=Modifier.weight(1f),placeholder={Text("Comando…",color=Color(0xFF52707C))},singleLine=true,shape=RoundedCornerShape(18.dp),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=CYAN,unfocusedBorderColor=Color(0xFF17303C),focusedTextColor=WHITE,unfocusedTextColor=WHITE,cursorColor=CYAN))
                Spacer(Modifier.width(7.dp)); FloatingActionButton(onClick={ask(input)},containerColor=CYAN,contentColor=Color.Black){Icon(Icons.Default.Send,null)}
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick={requestMic.launch(Manifest.permission.RECORD_AUDIO)},modifier=Modifier.weight(1f).height(52.dp),shape=RoundedCornerShape(17.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF0A1B25))){Icon(Icons.Default.Mic,null,tint=CYAN);Spacer(Modifier.width(6.dp));Text("HABLAR",color=CYAN,fontWeight=FontWeight.Bold)}
                Button(onClick={ if(wakeMode){context.stopService(Intent(context,JarvisWakeService::class.java));wakeMode=false;status="ONLINE"} else { if(android.os.Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS) else requestMic.launch(Manifest.permission.RECORD_AUDIO)} },modifier=Modifier.weight(1f).height(52.dp),shape=RoundedCornerShape(17.dp),colors=ButtonDefaults.buttonColors(containerColor=if(wakeMode)CYAN else Color(0xFF0A1B25))){Text(if(wakeMode)"DESACTIVAR":"DECIR JARVIS",color=if(wakeMode)Color.Black else CYAN,fontWeight=FontWeight.Bold)}
            }
        }
        if(showSettings) SettingsDialog(apiKey,{showSettings=false},{k->apiKey=k.trim();context.getSharedPreferences(PREFS,0).edit().putString(API_KEY,apiKey).apply();showSettings=false})
    }
}

private fun startWakeService(context: Context) { val i=Intent(context,JarvisWakeService::class.java); ContextCompat.startForegroundService(context,i) }

@Composable private fun TechLabel(text:String){Text(text,color=CYAN,fontSize=8.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)}
@Composable private fun SettingsDialog(initial:String,onDismiss:()->Unit,onSave:(String)->Unit){var key by remember{mutableStateOf(initial)};Dialog(onDismissRequest=onDismiss){Surface(shape=RoundedCornerShape(22.dp),color=PANEL){Column(Modifier.padding(22.dp)){Text("JARVIS CONFIG",color=CYAN,fontSize=21.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(10.dp));Text("La clave de Gemini se guarda localmente en este dispositivo.",color=WHITE,fontSize=13.sp);Spacer(Modifier.height(12.dp));OutlinedTextField(value=key,onValueChange={key=it},label={Text("Gemini API key")},singleLine=true);Spacer(Modifier.height(15.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){TextButton(onClick=onDismiss){Text("Cancelar")};Button(onClick={onSave(key)}){Text("Guardar")}}}}}}

private fun callGemini(apiKey:String,messages:List<ChatMessage>):Result<String> = runCatching {
    val contents=JSONArray();messages.takeLast(20).forEach{m->contents.put(JSONObject().put("role",if(m.role=="model")"model"else"user").put("parts",JSONArray().put(JSONObject().put("text",m.text))))}
    val body=JSONObject().put("systemInstruction",JSONObject().put("parts",JSONArray().put(JSONObject().put("text","Eres JARVIS, un asistente personal en español. Responde natural, útil y conciso.")))).put("contents",contents)
    var last="Error desconocido"
    for(attempt in 0..3){val c=(URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent").openConnection()as HttpURLConnection).apply{requestMethod="POST";connectTimeout=20000;readTimeout=60000;doOutput=true;setRequestProperty("Content-Type","application/json");setRequestProperty("x-goog-api-key",apiKey)};try{c.outputStream.use{it.write(body.toString().toByteArray())};val code=c.responseCode;val stream=if(code in 200..299)c.inputStream else c.errorStream;val res=stream?.bufferedReader()?.use{it.readText()}.orEmpty();if(code in 200..299){val t=JSONObject(res).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).optString("text");if(t.isBlank())throw IllegalStateException("Respuesta vacía");return@runCatching t};last="HTTP $code";val d=runCatching{JSONObject(res).optJSONObject("error")?.optString("message")}.getOrNull();if(!d.isNullOrBlank())last="HTTP $code: $d";if(code !in listOf(429,500,502,503,504)||attempt==3)break;Thread.sleep(1200L*(1L shl attempt))}finally{c.disconnect()}}
    throw IllegalStateException(last)
}
