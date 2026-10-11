package com.example.recipes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.withFrameNanos
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val LIFE_MS = 1700
private const val TOTAL_MS = 3600L

private val palette = listOf(
    Color(0xFFFF5252), Color(0xFFFFD740), Color(0xFF69F0AE),
    Color(0xFF40C4FF), Color(0xFFE040FB), Color(0xFFFF9100)
)

private class Spark(val angle: Float, val speed: Float, val color: Color, val radius: Float)
private class Burst(val cx: Float, val cy: Float, val delayMs: Int, val sparks: List<Spark>)

@Composable
fun Fireworks(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val bursts = remember {
        val rnd = Random(System.nanoTime())
        List(7) { i ->
            val c1 = palette.random(rnd)
            val c2 = palette.random(rnd)
            Burst(
                cx = 0.15f + rnd.nextFloat() * 0.7f,
                cy = 0.12f + rnd.nextFloat() * 0.38f,
                delayMs = i * 260 + rnd.nextInt(120),
                sparks = List(44) { k ->
                    Spark(
                        angle = k / 44f * 2f * PI.toFloat() + rnd.nextFloat() * 0.1f,
                        speed = 90f + rnd.nextFloat() * 70f,   // в dp
                        color = if (k % 2 == 0) c1 else c2,
                        radius = 2f + rnd.nextFloat() * 2f     // в dp
                    )
                }
            )
        }
    }

    var timeMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            val t = (withFrameNanos { it } - start) / 1_000_000L
            timeMs = t
            if (t >= TOTAL_MS) break
        }
        onFinished()
    }

    Canvas(modifier.fillMaxSize()) {
        bursts.forEach { b ->
            val local = timeMs - b.delayMs
            if (local < 0 || local > LIFE_MS) return@forEach
            val p = local / LIFE_MS.toFloat()

            b.sparks.forEach { s ->
                // Сама искра и два следа за ней
                for (k in 0..2) {
                    val pk = p - k * 0.03f
                    if (pk < 0f) continue
                    val eased = 1f - (1f - pk) * (1f - pk)
                    val seconds = pk * LIFE_MS / 1000f
                    val dist = s.speed * density * eased
                    val drop = 40f * density * seconds * seconds
                    val alpha = (1f - pk).coerceIn(0f, 1f) * (1f - k * 0.35f)

                    drawCircle(
                        color = s.color.copy(alpha = alpha),
                        radius = s.radius * density * (1f - 0.5f * pk),
                        center = Offset(
                            b.cx * size.width + cos(s.angle) * dist,
                            b.cy * size.height + sin(s.angle) * dist + drop
                        )
                    )
                }
            }
        }
    }
}