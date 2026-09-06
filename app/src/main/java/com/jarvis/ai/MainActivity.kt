package com.jarvis.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HologramOnlyScreen() }
    }
}

@Composable
private fun HologramOnlyScreen() {
    val animation = rememberInfiniteTransition(label = "hologram")
    val angle by animation.animateFloat(0f, 360f, infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "angle")
    val pulse by animation.animateFloat(0.82f, 1.18f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "pulse")
    val scan by animation.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "scan")

    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(330.dp)) {
            val c = center
            val r = size.minDimension * 0.27f
            val orbit = r * 1.75f
            val a = angle * 0.017453292f

            drawCircle(Brush.radialGradient(listOf(Color(0x8855E9FF), Color(0x22004C66), Color.Transparent)), orbit * 1.35f, c)

            for (i in 0..3) {
                val p = a + i * 1.5708f
                val sy = 0.18f + 0.72f * abs(sin(p))
                val w = orbit * (0.9f + i * 0.08f)
                val h = w * sy
                drawOval(Color(0xCC55E9FF), Offset(c.x - w, c.y - h), Size(w * 2f, h * 2f), Stroke(if (i == 1) 2.5f else 1.2f))
            }

            for (lat in -6..6) {
                val y = lat / 6f
                val x = r * sqrt(maxOf(0f, 1f - y * y))
                drawOval(Color(0x7755E9FF), Offset(c.x - x, c.y + y * r - r * .025f), Size(x * 2f, r * .05f), Stroke(1f))
            }
            for (lon in 0 until 18) {
                val p = a + lon * 0.349066f
                val x = r * abs(cos(p))
                drawOval(Color(0x6655E9FF), Offset(c.x - x, c.y - r), Size(x * 2f, r * 2f), Stroke(1f))
            }

            for (i in 0 until 54) {
                val p = a * 1.6f + i * 0.11635f
                val d = orbit * (0.58f + (i % 8) * 0.06f)
                drawCircle(Color(0xCC55E9FF), if (i % 9 == 0) 3f else 1.2f, Offset(c.x + cos(p) * d, c.y + sin(p) * d * .58f))
            }

            for (i in 0 until 20) {
                val p = a * 2.4f + i * 0.314f
                val inner = r * .35f
                val outer = r * (.72f + .18f * sin(scan * 6.283f + i))
                drawLine(Color(0xAA55E9FF), Offset(c.x + cos(p) * inner, c.y + sin(p) * inner), Offset(c.x + cos(p) * outer, c.y + sin(p) * outer), if (i % 4 == 0) 2f else 1f)
            }

            drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFF55E9FF), Color(0x661689AD), Color.Transparent)), r * .55f * pulse, c)
            val scanY = c.y - r + scan * r * 2f
            drawLine(Color(0x9955E9FF), Offset(c.x - r, scanY), Offset(c.x + r, scanY), 1.2f)
            drawOval(Color(0xAA55E9FF), Offset(c.x - orbit * .72f, c.y + r * 1.05f), Size(orbit * 1.44f, r * .24f), Stroke(1.5f))
            drawOval(Color(0x5555E9FF), Offset(c.x - orbit * .48f, c.y + r * 1.16f), Size(orbit * .96f, r * .12f), Stroke(1f))
        }
        Text("J", color = Color(0xFF55E9FF), fontSize = 54.sp)
    }
}
