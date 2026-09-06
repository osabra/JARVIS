package com.jarvis.ai

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private val Cyan = Color(0xFF55E9FF)
private val CyanDim = Color(0xFF1689AD)
private val CyanDeep = Color(0xFF063746)

@Composable
fun HolographicCore3D(status: String, pulse: Float, angle: Float, reverseAngle: Float, wave: Float) {
    val active = status == "HEARING" || status == "THINKING"
    val power = if (active) 1f else 0.72f
    Box(Modifier.size(290.dp).graphicsLayer {
        rotationX = (7f * sin(Math.toRadians(angle.toDouble() * 0.8))).toFloat()
        rotationY = (15f * sin(Math.toRadians(angle.toDouble() * 0.55))).toFloat()
        cameraDistance = 24f * density
        scaleX = pulse
        scaleY = pulse
    }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = size.minDimension * 0.31f
            val outer = r * 1.58f
            drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha = 0.42f * power), CyanDeep.copy(alpha = 0.18f), Color.Transparent)), outer * 1.05f, c)
            val orbitAngles = floatArrayOf(0f, 60f, -60f)
            for (i in orbitAngles.indices) {
                val tilt = Math.toRadians(orbitAngles[i].toDouble())
                val squash = 0.25f + 0.55f * abs(sin(tilt)).toFloat()
                val rx = outer * (0.92f + i * 0.08f)
                val ry = rx * squash
                drawOval(Cyan.copy(alpha = (0.58f + i * 0.12f) * power), androidx.compose.ui.geometry.Offset(c.x - rx, c.y - ry), androidx.compose.ui.geometry.Size(rx * 2f, ry * 2f), style = Stroke(if (i == 1) 2.5f else 1.5f))
            }
            drawCircle(Cyan.copy(alpha = 0.18f * power), r * 1.02f, c, style = Stroke(1f))
            for (lat in -4..4) {
                val y = lat / 4f
                val latY = r * y * 0.92f
                val latRx = r * sqrt(maxOf(0f, 1f - y * y))
                drawOval(Cyan.copy(alpha = (0.28f + 0.08f * (4 - abs(lat))) * power), androidx.compose.ui.geometry.Offset(c.x - latRx, c.y - latY), androidx.compose.ui.geometry.Size(latRx * 2f, maxOf(2f, r * 0.13f)), style = Stroke(1f))
            }
            for (lon in 0 until 10) {
                val phase = Math.toRadians(lon * 36.0 + angle * 1.2)
                val xRadius = r * abs(cos(phase)).toFloat()
                drawOval(Cyan.copy(alpha = 0.38f * power), androidx.compose.ui.geometry.Offset(c.x - xRadius, c.y - r), androidx.compose.ui.geometry.Size(xRadius * 2f, r * 2f), style = Stroke(if (lon % 2 == 0) 1.2f else 0.7f))
            }
            val pulseRadius = r * (0.34f + wave * 0.12f)
            drawCircle(Brush.radialGradient(listOf(Color.White, Cyan.copy(alpha = 0.95f), CyanDim.copy(alpha = 0.55f), Color.Transparent)), pulseRadius, c)
            for (i in 0 until 18) {
                val a = Math.toRadians(i * 20.0 + angle * 2.6)
                val inner = r * 0.48f
                val outerP = r * (0.9f + 0.12f * sin(Math.toRadians(wave * 360f + i * 27f)).toFloat())
                val p1 = androidx.compose.ui.geometry.Offset(c.x + inner * cos(a).toFloat(), c.y + inner * sin(a).toFloat())
                val p2 = androidx.compose.ui.geometry.Offset(c.x + outerP * cos(a).toFloat(), c.y + outerP * sin(a).toFloat())
                drawLine(Cyan.copy(alpha = 0.25f + 0.55f * power), p1, p2, if (i % 3 == 0) 2f else 1f)
            }
            for (i in 0 until 30) {
                val a = Math.toRadians(i * 31.0 + angle * 1.7)
                val depth = 0.55f + ((i * 17) % 45) / 100f
                val px = c.x + outer * depth * cos(a).toFloat()
                val py = c.y + outer * 0.62f * depth * sin(a).toFloat()
                drawCircle(Cyan.copy(alpha = 0.25f + 0.55f * power), if (i % 7 == 0) 3f else 1.4f, androidx.compose.ui.geometry.Offset(px, py))
            }
            val scanY = c.y + sin(Math.toRadians(wave * 360.0)).toFloat() * r * 0.85f
            drawLine(Cyan.copy(alpha = 0.22f * power), androidx.compose.ui.geometry.Offset(c.x - outer, scanY), androidx.compose.ui.geometry.Offset(c.x + outer, scanY), 1f)
            drawOval(Cyan.copy(alpha = 0.48f * power), androidx.compose.ui.geometry.Offset(c.x - outer * 0.72f, c.y + r * 0.82f), androidx.compose.ui.geometry.Size(outer * 1.44f, r * 0.22f), style = Stroke(1.2f))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("J", color = Cyan, fontSize = 48.sp, fontWeight = FontWeight.Black)
            Text(if (status == "HEARING") "HEARING" else if (status == "THINKING") "PROCESSING" else "ONLINE", color = Cyan, fontSize = 7.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
    }
}
