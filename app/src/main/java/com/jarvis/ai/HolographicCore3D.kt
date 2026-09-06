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
import androidx.compose.ui.geometry.Size
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

private fun point(x: Float, y: Float): Offset = Offset(x, y)
private fun rectSize(width: Float, height: Float): Size = Size(width, height)

@Composable
fun HolographicCore3D(status: String, pulse: Float, angle: Float, reverseAngle: Float, wave: Float) {
    val active = status == "HEARING" || status == "LISTENING" || status == "THINKING"
    val power = if (active) 1f else 0.78f
    Box(Modifier.size(290.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = size.minDimension * 0.30f
            val outer = r * 1.70f
            val spin = Math.toRadians(angle.toDouble())
            val reverse = Math.toRadians(reverseAngle.toDouble())
            drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha = 0.30f * power), CyanDeep.copy(alpha = 0.18f), Color.Transparent)), outer * 1.12f, c)
            val rings = floatArrayOf(0f, 0.72f, -0.72f)
            rings.forEachIndexed { index, tilt ->
                val dynamic = (0.30 + 0.68 * abs(sin(spin + tilt))).toFloat()
                val rx = outer * (0.86f + index * 0.08f)
                val ry = rx * dynamic
                drawOval(Cyan.copy(alpha = (0.42f + index * 0.12f) * power), point(c.x - rx, c.y - ry), rectSize(rx * 2f, ry * 2f), Stroke(if (index == 1) 2.2f else 1.25f))
            }
            drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha = 0.22f), CyanDeep.copy(alpha = 0.30f), Color.Transparent)), r * 1.10f, c)
            for (lat in -4..4) {
                val y = lat / 4f
                val latY = r * y * 0.92f
                val latRx = r * sqrt(maxOf(0f, 1f - y * y))
                drawOval(Cyan.copy(alpha = (0.22f + 0.08f * (4 - abs(lat))) * power), point(c.x - latRx, c.y - latY), rectSize(latRx * 2f, maxOf(1.5f, r * 0.11f)), Stroke(0.9f))
            }
            for (lon in 0 until 12) {
                val phase = reverse + Math.toRadians(lon * 30.0)
                val xRadius = r * abs(cos(phase)).toFloat()
                drawOval(Cyan.copy(alpha = 0.32f * power), point(c.x - xRadius, c.y - r), rectSize(xRadius * 2f, r * 2f), Stroke(if (lon % 3 == 0) 1.2f else 0.7f))
            }
            val pulseRadius = r * (0.30f + wave * 0.18f) * pulse
            drawCircle(Brush.radialGradient(listOf(Color.White, Cyan.copy(alpha = 0.95f), CyanDim.copy(alpha = 0.50f), Color.Transparent)), pulseRadius, c)
            for (i in 0 until 18) {
                val a = spin * 2.6 + Math.toRadians(i * 20.0)
                val inner = r * 0.48f
                val outerP = r * (0.88f + 0.14f * sin(wave.toDouble() * Math.PI * 2.0 + i * 0.45).toFloat())
                drawLine(Cyan.copy(alpha = 0.25f + 0.55f * power), point(c.x + inner * cos(a).toFloat(), c.y + inner * sin(a).toFloat()), point(c.x + outerP * cos(a).toFloat(), c.y + outerP * sin(a).toFloat()), if (i % 3 == 0) 2f else 1f)
            }
            for (i in 0 until 36) {
                val a = spin * 1.7 + Math.toRadians(i * 31.0)
                val depth = 0.52f + ((i * 17) % 45) / 100f
                val px = c.x + outer * depth * cos(a).toFloat()
                val py = c.y + outer * 0.62f * depth * sin(a).toFloat()
                drawCircle(Cyan.copy(alpha = 0.22f + 0.52f * power), if (i % 8 == 0) 2.7f else 1.2f, point(px, py))
            }
            val scanY = c.y + sin(wave.toDouble() * Math.PI * 2.0).toFloat() * r * 0.86f
            drawLine(Cyan.copy(alpha = 0.30f * power), point(c.x - outer, scanY), point(c.x + outer, scanY), 1f)
            drawOval(Cyan.copy(alpha = 0.48f * power), point(c.x - outer * 0.72f, c.y + r * 0.84f), rectSize(outer * 1.44f, r * 0.22f), Stroke(1.2f))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("J", color = Cyan, fontSize = 48.sp, fontWeight = FontWeight.Black)
            Text(if (status == "HEARING" || status == "LISTENING") "LISTENING" else if (status == "THINKING") "PROCESSING" else "ONLINE", color = Cyan, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
    }
}
