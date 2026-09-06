package com.jarvis.ai

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private val Cyan = Color(0xFF55E9FF)
private val CyanDim = Color(0xFF1689AD)
private val CyanDeep = Color(0xFF063746)

@Composable
fun HolographicCore3D(status: String, pulse: Float, angle: Float, reverseAngle: Float, wave: Float) {
    val active = status == "HEARING" || status == "LISTENING" || status == "THINKING"
    val power = if (active) 1f else 0.78f
    Box(Modifier.size(290.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = size.minDimension * 0.30f
            val outer = r * 1.70f
            val spin = angle * 0.017453292f
            val reverse = reverseAngle * 0.017453292f
            drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha = 0.30f * power), CyanDeep.copy(alpha = 0.18f), Color.Transparent)), outer * 1.12f, c)
            val rings = floatArrayOf(0f, 0.72f, -0.72f)
            rings.forEachIndexed { index, tilt ->
                val dynamic = (0.30f + 0.68f * abs(sin(spin + tilt).toFloat()))
                val rx = outer * (0.86f + index * 0.08f)
                val ry = rx * dynamic
                drawOval(Cyan.copy(alpha = (0.42f + index * 0.12f) * power), topLeft = Offset(c.x - rx, c.y - ry), size = androidx.compose.ui.geometry.Size(rx * 2f, ry * 2f), style = Stroke(if (index == 1) 2.2f else 1.25f))
            }
            drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha = 0.22f), CyanDeep.copy(alpha = 0.30f), Color.Transparent)), r * 1.10f, c)
            for (lat in -4..4) {
                val y = lat / 4f
                val latY = r * y * 0.92f
                val latRx = r * sqrt(maxOf(0f, 1f - y * y))
                drawOval(Cyan.copy(alpha = (0.22f + 0.08f * (4 - abs(lat))) * power), topLeft = Offset(c.x - latRx, c.y - latY), size = androidx.compose.ui.geometry.Size(latRx * 2f, maxOf(1.5f, r * 0.11f)), style = Stroke(0.9f))
            }
            for (lon in 0 until 12) {
                val phase = reverse + lon * 0.5235988f
                val xRadius = r * abs(cos(phase).toFloat())
                drawOval(Cyan.copy(alpha = 0.32f * power), topLeft = Offset(c.x - xRadius, c.y - r), size = androidx.compose.ui.geometry.Size(xRadius * 2f, r * 2f), style = Stroke(if (lon % 3 == 0) 1.2f else 0.7f))
            }
            val pulseRadius = r * (0.30f + wave * 0.18f) * pulse
            drawCircle(Brush.radialGradient(listOf(Color.White, Cyan.copy(alpha = 0.95f), CyanDim.copy(alpha = 0.50f), Color.Transparent)), pulseRadius, c)
            for (i in 0 until 18) {
                val a = spin * 2.6f + i * 0.34906584f
                val inner = r * 0.48f
                val outerP = r * (0.88f + 0.14f * sin(wave * 6.2831855f + i * 0.45f).toFloat())
                drawLine(Cyan.copy(alpha = 0.25f + 0.55f * power), Offset(c.x + inner * cos(a).toFloat(), c.y + inner * sin(a).toFloat()), Offset(c.x + outerP * cos(a).toFloat(), c.y + outerP * sin(a).toFloat()), if (i % 3 == 0) 2f else 1f)
            }
            for (i in 0 until 36) {
                val a = spin * 1.7f + i * 0.541878f
                val depth = 0.52f + ((i * 17) % 45) / 100f
                drawCircle(Cyan.copy(alpha = 0.22f + 0.52f * power), if (i % 8 == 0) 2.7f else 1.2f, Offset(c.x + outer * depth * cos(a).toFloat(), c.y + outer * 0.62f * depth * sin(a).toFloat()))
            }
            val scanY = c.y + sin(wave * 6.2831855f).toFloat() * r * 0.86f
            drawLine(Cyan.copy(alpha = 0.30f * power), Offset(c.x - outer, scanY), Offset(c.x + outer, scanY), 1f)
            drawOval(Cyan.copy(alpha = 0.48f * power), topLeft = Offset(c.x - outer * 0.72f, c.y + r * 0.84f), size = androidx.compose.ui.geometry.Size(outer * 1.44f, r * 0.22f), style = Stroke(1.2f))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("J", color = Cyan, fontSize = 48.sp, fontWeight = FontWeight.Black)
            Text(if (status == "HEARING" || status == "LISTENING") "LISTENING" else if (status == "THINKING") "PROCESSING" else "ONLINE", color = Cyan, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
    }
}
