package com.jarvis.ai

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HolographicCore3D(
    status: String,
    pulse: Float,
    angle: Float,
    reverseAngle: Float,
    wave: Float
) {
    val active = status == "HEARING" || status == "THINKING"
    Box(
        Modifier.size(290.dp).scale(pulse).graphicsLayer {
            rotationX = 10f * sin(Math.toRadians(angle.toDouble())).toFloat()
            rotationY = 8f * cos(Math.toRadians(angle.toDouble() * 0.7)).toFloat()
            cameraDistance = 18f * density
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = center.x
            val cy = center.y
            val base = size.minDimension * .47f
            val glow = .18f + wave * .10f

            drawCircle(
                Brush.radialGradient(listOf(CYAN.copy(alpha = .28f), Color.Transparent)),
                base * 1.02f,
                Offset(cx, cy)
            )

            // Deep 3D volume: stacked translucent spheres/ellipses.
            for (i in 7 downTo 1) {
                val rr = base * (.47f + i * .055f)
                val y = cy + i * 3.2f
                drawOval(
                    Brush.radialGradient(listOf(Color(0xFF0A3445).copy(alpha = .12f), Color.Transparent)),
                    Offset(cx - rr, y - rr * .70f),
                    Size(rr * 2f, rr * 1.4f)
                )
            }

            // Perspective latitude rings.
            for (i in 0..4) {
                val ry = base * (.18f + i * .105f)
                drawOval(
                    CYAN.copy(alpha = if (active) .30f else .17f),
                    Offset(cx - base * .80f, cy - ry),
                    Size(base * 1.60f, ry * 2f),
                    style = Stroke(if (i == 2) 2.2f else 1f)
                )
            }

            // Rotating orbital planes create the 3D illusion.
            drawOval(
                CYAN.copy(alpha = .85f),
                Offset(cx - base * .92f, cy - base * .30f),
                Size(base * 1.84f, base * .60f),
                style = Stroke(2.2f)
            )
            drawOval(
                CYAN2.copy(alpha = .85f),
                Offset(cx - base * .72f, cy - base * .98f),
                Size(base * 1.44f, base * 1.96f),
                style = Stroke(2f)
            )

            // Moving energy particles on the orbits.
            for (i in 0 until 16) {
                val a = Math.toRadians((i * 22.5 + angle * 1.2).toDouble())
                val rx = base * .92f
                val ry = base * .30f
                val px = cx + (rx * cos(a)).toFloat()
                val py = cy + (ry * sin(a)).toFloat()
                val s = if (i % 4 == 0) 5f else 2.2f
                drawCircle(CYAN.copy(alpha = .25f + .7f * (i % 4) / 4f), s, Offset(px, py))
            }

            // Central energy sphere with a bright moving core.
            drawCircle(
                Brush.radialGradient(
                    listOf(
                        Color.White.copy(alpha = .92f),
                        CYAN.copy(alpha = .78f),
                        Color(0xFF0B5E78).copy(alpha = .55f),
                        Color.Transparent
                    )
                ),
                base * (.36f + wave * .035f),
                Offset(cx, cy)
            )
            drawCircle(CYAN.copy(alpha = glow), base * .52f, Offset(cx, cy), style = Stroke(1.2f))

            // Vertical hologram scan lines.
            for (i in -5..5) {
                val x = cx + i * base * .105f
                drawLine(CYAN.copy(alpha = .07f), Offset(x, cy - base * .34f), Offset(x, cy + base * .34f), 1f)
            }
        }

        Box(
            Modifier.size(118.dp)
                .shadow(38.dp, CircleShape)
                .background(Color(0xFF03131C).copy(alpha = .72f), CircleShape)
                .border(1.5.dp, CYAN.copy(alpha = .8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (status == "HEARING") "◉" else if (status == "THINKING") "…" else "J",
                    color = CYAN,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (status == "HEARING") "HEARING" else if (status == "THINKING") "PROCESSING" else status,
                    color = CYAN,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
