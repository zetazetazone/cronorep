package com.cronorep.mobile

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cronorep.BackgroundKind
import com.cronorep.Billing
import com.cronorep.Gradient
import com.cronorep.Look
import com.cronorep.LookSettings
import com.cronorep.RestTimer
import com.cronorep.Trial
import com.cronorep.formatTime
import kotlinx.coroutines.delay

/** The rest timer. It works the same as the watch timer: tap anywhere to start or restart. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimerScreen(onTap: () -> Unit, onOpenSettings: () -> Unit) {
    val startedAt = RestTimer.startedAt
    val look = Look.settings
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    LaunchedEffect(startedAt, look.saveBattery) {
        while (startedAt != 0L) {
            now = SystemClock.elapsedRealtime()
            // Save battery mode: one screen update each second, on the whole second since the tap,
            // so "REST 0:01" never stays on screen after the end vibration.
            delay(if (look.saveBattery) 1000L - (SystemClock.elapsedRealtime() - startedAt) % 1000L else 100L)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Start rest timer",
                onLongClickLabel = "Open settings",
                onClick = onTap,
                onLongClick = onOpenSettings,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Sizes come from the screen width. Dividing by the font scale (toSp) keeps the drawn size fixed in dp,
        // so the digits always fit inside the ring.
        val idleTimeSize = with(LocalDensity.current) { (maxWidth * 0.25f).toSp() }
        val timeLeftSize = with(LocalDensity.current) { (maxWidth * 0.26f).toSp() }
        val overtimeSize = with(LocalDensity.current) { (maxWidth * 0.21f).toSp() }

        LookBackground(look)

        // A new install, before any trial use: say at once that the trial time goes to our server.
        if (!Billing.unlocked && Trial.usedMs == 0L) {
            Text(
                Trial.SERVER_NOTE,
                color = Dim,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 24.dp, vertical = 24.dp),
            )
        }

        if (startedAt == 0L) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TAP TO START", color = Green, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    formatTime(RestTimer.restSeconds.toLong()),
                    color = Color.White,
                    fontSize = idleTimeSize,
                    maxLines = 1,
                    softWrap = false,
                )
                Spacer(Modifier.height(8.dp))
                Text("Hold to set time", color = Dim, fontSize = 16.sp)
            }
        } else {
            val totalMs = RestTimer.runSeconds * 1000L
            val elapsedMs = (now - startedAt).coerceAtLeast(0L)
            val resting = elapsedMs < totalMs

            if (!look.minimalist) {
                ProgressRing(
                    progress = if (resting) 1f - elapsedMs.toFloat() / totalMs else 1f,
                    color = if (resting) Green else Orange,
                    modifier = Modifier.fillMaxWidth().padding(20.dp).aspectRatio(1f),
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (resting) {
                    val remainingSeconds = (totalMs - elapsedMs + 999) / 1000
                    Text("REST", color = Green, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(
                        formatTime(remainingSeconds),
                        color = Color.White,
                        fontSize = timeLeftSize,
                        maxLines = 1,
                        softWrap = false,
                    )
                } else {
                    Text("GO", color = Orange, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "+" + formatTime((elapsedMs - totalMs) / 1000),
                        color = Orange,
                        fontSize = overtimeSize,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                if (!look.minimalist) {
                    Text("total " + formatTime(elapsedMs / 1000), color = Dim, fontSize = 18.sp)
                }
            }
        }

        TextButton(
            onClick = onOpenSettings,
            modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(8.dp),
        ) { Text("Settings", color = Dim, fontSize = 16.sp) }
    }
}

@Composable
private fun ProgressRing(progress: Float, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = 10.dp.toPx()
        val topLeft = Offset(stroke / 2, stroke / 2)
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(Color(0xFF1F2937), -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
        drawArc(color, -90f, 360f * progress, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

/** Behind the idle screen and the running timer. */
@Composable
private fun LookBackground(look: LookSettings) {
    when (look.effectiveBackground) {
        BackgroundKind.GRADIENT -> GradientBackground(look.gradient)
        BackgroundKind.GIF_RANDOM, BackgroundKind.GIF_FIXED -> Look.gifUrl?.let { GifBackground(it) }
        BackgroundKind.BLACK -> Unit
    }
}

@Composable
private fun GradientBackground(gradient: Gradient) {
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(gradient.top), Color(gradient.bottom))))
    )
    DarkLayer()
}

@Composable
private fun GifBackground(url: String) {
    // The app-wide loader from CronoRepApp plays the GIF.
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
    )
    DarkLayer()
}

/** The timer text must stay easy to read on each background. */
@Composable
private fun DarkLayer() {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
}
