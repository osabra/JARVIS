package com.jarvis.ai

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.cos
import kotlin.math.sin

private val Cyan = Color(0xFF54E8FF)
private val Cyan2 = Color(0xFF159BC2)

@Composable
fun HolographicCore3D(status: String, pulse: Float, angle: Float, reverseAngle: Float, wave: Float) {
    val active = status == "HEARING" || status == "THINKING"
    Box(
        Modifier
            .size(290.dp)
            .graphicsLayer {
                rotationX = (9f * sin(Math.toRadians(angle.toDouble()))).toFloat()
                rotationY = (12f * cos(Math.toRadians(angle.toDouble() * .7))).toFloat()
                cameraDistance = 20f * density
                scaleX = pulse
                scaleY = pulse
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = center.x
            val cy = center.y
            val base = size.minDimension * .47f
            val intensity = if (active) 1f else .55f
            drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha = .30f * intensity), Color.Transparent)), base * 1.05f, Offset(cx, cy))
            for (i in 6 downTo 1) {
                val r = base * (.34f + i * .055f)
                drawOval(Brush.radialGradient(listOf(Cyan2.copy(alpha = .13f), Color.Transparent)), Offset(cx - r, cy - r * .68f + i * 2f), Size(r * 2f, r * 1.36f))
            }
            for (i in -2..2) {
                val ry = base * (.18f + kotlin.math.abs(i) * .12f)
                drawOval(Cyan.copy(alpha = .24f * intensity), Offset(cx - base * .78f, cy - ry), Size(base * 1.56f, ry * 2f), style = Stroke(if (i == 0) 2f else 1f))
            }
            drawOval(Cyan.copy(alpha = .72f * intensity), Offset(cx - base * .40f, cy - base * .84f), Size(base * .80f, base * 1.68f), style = Stroke(1.4f))
            drawOval(Cyan.copy(alpha = .9f * intensity), Offset(cx - base * .94f, cy - base * .25f), Size(base * 1.88f, base * .50f), style = Stroke(2.4f))
            drawOval(Cyan2.copy(alpha = .9f * intensity), Offset(cx - base * .72f, cy - base * .98f), Size(base * 1.44f, base * 1.96f), style = Stroke(2f))
            for (i in 0 until 20) {
                val a = Math.toRadians(i * 18.0 + angle * 1.8)
                val px = cx + (base * .94f * cos(a)).toFloat()
                val py = cy + (base * .25f * sin(a)).toFloat()
                drawCircle(Cyan.copy(alpha = .25f + .65f * ((i % 5) / 5f) * intensity), if (i % 5 == 0) 4.5f else 2f, Offset(px, py))
            }
            for (i in 0 until 12) {
                val a = Math.toRadians(i * 30.0 - reverseAngle * 1.4)
                val px = cx + (base * .72f * cos(a)).toFloat()
                val py = cy + (base * .98f * sin(a)).toFloat()
                drawCircle(Cyan2.copy(alpha = .65f * intensity), 2.2f, Offset(px, py))
            }
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = .98f), Cyan.copy(alpha = .9f), Cyan2.copy(alpha = .35f), Color.Transparent)), base * (.34f + wave * .04f), Offset(cx, cy))
            drawCircle(Cyan.copy(alpha = .35f * intensity), base * .50f, Offset(cx, cy), style = Stroke(1.2f))
        }
        Box(Modifier.size(112.dp).shadow(32.dp, CircleShape).background(Color(0xFF03131C).copy(alpha = .78f), CircleShape).border(1.5.dp, Cyan.copy(alpha = .85f), CircleShape), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (status == "HEARING") "◉" else if (status == "THINKING") "…" else "J", color = Cyan, fontSize = 46.sp, fontWeight = FontWeight.Bold)
                Text(if (status == "HEARING") "HEARING" else if (status == "THINKING") "PROCESSING" else status, color = Cyan, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
