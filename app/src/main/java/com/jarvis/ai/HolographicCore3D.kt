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

private val C = Color(0xFF5DEBFF)
private val C2 = Color(0xFF1598C0)
private val Deep = Color(0xFF031821)

@Composable
fun HolographicCore3D(status: String, pulse: Float, angle: Float, reverseAngle: Float, wave: Float) {
    val active = status != "ONLINE"
    val glow = if (active) 1f else .82f
    Box(Modifier.size(290.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = center.x
            val cy = center.y
            val radius = size.minDimension * .285f
            val orbit = radius * 1.65f
            val a = angle * .017453292f
            val b = reverseAngle * .017453292f

            drawCircle(Brush.radialGradient(listOf(C.copy(alpha=.34f*glow), C2.copy(alpha=.12f), Color.Transparent)), orbit*1.15f, Offset(cx,cy))

            // Three tilted orbital rings create obvious perspective and depth.
            for (i in 0..2) {
                val phase = a + i * 2.0943952f
                val yScale = .18f + .70f * abs(sin(phase).toFloat())
                val w = orbit * (1.05f + i*.08f)
                val h = w * yScale
                drawOval(C.copy(alpha=(.45f+i*.12f)*glow), topLeft=Offset(cx-w,cy-h), size=androidx.compose.ui.geometry.Size(w*2f,h*2f), style=Stroke(if(i==1)2.4f else 1.25f))
            }

            // Spherical grid: latitude and longitude lines move continuously.
            drawCircle(C.copy(alpha=.16f*glow), radius*1.08f, Offset(cx,cy), style=Stroke(1.1f))
            for (lat in -5..5) {
                val y = lat/5f
                val yy = cy + radius*y*.94f
                val rr = radius*sqrt(maxOf(0f,1f-y*y))
                val thickness = if(lat==0) 1.5f else .8f
                drawOval(C.copy(alpha=(.22f+.05f*(5-abs(lat)))*glow), topLeft=Offset(cx-rr,yy-radius*.055f), size=androidx.compose.ui.geometry.Size(rr*2f,radius*.11f), style=Stroke(thickness))
            }
            for (lon in 0 until 16) {
                val p = b + lon*.39269908f
                val x = radius*abs(cos(p).toFloat())
                drawOval(C.copy(alpha=(.20f+.18f*(lon%4==0).compareTo(false))*glow), topLeft=Offset(cx-x,cy-radius), size=androidx.compose.ui.geometry.Size(x*2f,radius*2f), style=Stroke(if(lon%4==0)1.25f else .7f))
            }

            // Bright rotating energy core.
            val core = radius*(.38f + .16f*wave)*pulse
            drawCircle(Brush.radialGradient(listOf(Color.White,C.copy(alpha=.95f),C2.copy(alpha=.55f),Color.Transparent)),core,Offset(cx,cy))
            for(i in 0 until 24) {
                val p = a*2.2f + i*.2617994f
                val r1 = radius*.42f
                val r2 = radius*(.72f + .18f*sin(wave*6.2831855f+i*.7f))
                drawLine(C.copy(alpha=.35f+.45f*glow), Offset(cx+r1*cos(p),cy+r1*sin(p)), Offset(cx+r2*cos(p),cy+r2*sin(p)), if(i%4==0)2f else 1f)
            }

            // Floating particles orbiting at different depths.
            for(i in 0 until 48) {
                val p = a*1.4f + i*.1308997f
                val depth = .55f + ((i*37)%45)/100f
                val px = cx + orbit*depth*cos(p)
                val py = cy + orbit*.62f*depth*sin(p)
                val pr = if(i%9==0)2.8f else 1.15f
                drawCircle(C.copy(alpha=(.25f+.55f*glow)*(.55f+.45f*depth)),pr,Offset(px,py))
            }

            // Scanning beam and base projection make the hologram read as volumetric.
            val scan = cy + sin(wave*6.2831855f)*radius*.9f
            drawLine(C.copy(alpha=.38f*glow),Offset(cx-orbit,scan),Offset(cx+orbit,scan),1.2f)
            drawOval(C.copy(alpha=.55f*glow),topLeft=Offset(cx-orbit*.78f,cy+radius*.90f),size=androidx.compose.ui.geometry.Size(orbit*1.56f,radius*.25f),style=Stroke(1.5f))
            drawOval(C.copy(alpha=.25f*glow),topLeft=Offset(cx-orbit*.52f,cy+radius*1.02f),size=androidx.compose.ui.geometry.Size(orbit*1.04f,radius*.14f),style=Stroke(1f))
        }
        Column(horizontalAlignment=Alignment.CenterHorizontally) {
            Text("J", color=C, fontSize=46.sp, fontWeight=FontWeight.Black)
            Text(if(status=="THINKING")"PROCESSING" else if(status=="LISTENING"||status=="HEARING")"LISTENING" else "ONLINE", color=C, fontSize=7.sp, fontWeight=FontWeight.Bold, letterSpacing=2.sp)
        }
    }
}
