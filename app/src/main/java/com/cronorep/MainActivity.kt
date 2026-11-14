package com.cronorep

import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import kotlinx.coroutines.delay

private val Green = Color(0xFF4ADE80)
private val Orange = Color(0xFFFB923C)
private val Dim = Color(0xFF9CA3AF)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The screen stays on, so one tap always restarts the timer.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        RestTimer.load(this)
        Trial.load(this)
        Billing.init(this)
        Gifs.load(this)
        // Debug only: adb shell am start -n com.cronorep/.MainActivity --ez paywall true
        val previewPaywall = BuildConfig.DEBUG && intent.getBooleanExtra("paywall", false)

        setContent {
            MaterialTheme {
                var showSettings by rememberSaveable { mutableStateOf(false) }
                var showPaywall by rememberSaveable { mutableStateOf(previewPaywall) }
                if (showPaywall && !Billing.unlocked) {
                    BackHandler { showPaywall = false }
                    PaywallScreen(onUnlock = { Billing.buy(this) })
                } else if (showSettings) {
                    BackHandler { showSettings = false }
                    SettingsScreen(
                        onDone = { showSettings = false },
                        onGifToggle = {
                            if (gifsAllowed()) Gifs.toggle(this) else showPaywall = true
                        },
                    )
                } else {
                    TimerScreen(
                        onTap = {
                            Trial.flush(this)
                            if (isLocked()) showPaywall = true else restartTimer()
                        },
                        onLongPress = {
                            Trial.flush(this)
                            showSettings = true
                        },
                    )
                }
            }
        }
    }

    /** Test builds never lock, so development is not blocked. */
    private fun isLocked() = !Billing.unlocked && Trial.expired && !BuildConfig.DEBUG

    /** GIF backgrounds are a paid feature. Test builds have it for development. */
    private fun gifsAllowed() = Billing.unlocked || BuildConfig.DEBUG

    override fun onResume() {
        super.onResume()
        Billing.refresh()
        if (RestTimer.startedAt != 0L) Trial.startCounting()
    }

    override fun onPause() {
        Trial.stopCounting(this)
        super.onPause()
    }

    override fun onDestroy() {
        if (isFinishing) {
            RestTimerService.stop(this)
            RestTimer.startedAt = 0L
        }
        super.onDestroy()
    }

    private fun restartTimer() {
        RestTimer.runSeconds = RestTimer.restSeconds
        RestTimer.startedAt = SystemClock.elapsedRealtime()
        RestTimerService.start(this, RestTimer.runSeconds * 1000L)
        Trial.startCounting()
        if (Gifs.enabled && gifsAllowed()) Gifs.loadNext()
        // Short tick: the tap worked.
        getSystemService(Vibrator::class.java)
            .vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimerScreen(onTap: () -> Unit, onLongPress: () -> Unit) {
    val startedAt = RestTimer.startedAt
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    LaunchedEffect(startedAt) {
        while (startedAt != 0L) {
            now = SystemClock.elapsedRealtime()
            delay(100)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
                onLongClick = onLongPress,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (startedAt == 0L) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TAP TO START", color = Green, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(formatTime(RestTimer.restSeconds.toLong()), color = Color.White, fontSize = 44.sp)
                Spacer(Modifier.height(6.dp))
                Text("Hold to set time", color = Dim, fontSize = 12.sp)
            }
        } else {
            val totalMs = RestTimer.runSeconds * 1000L
            val elapsedMs = (now - startedAt).coerceAtLeast(0L)
            val resting = elapsedMs < totalMs

            Gifs.currentUrl?.let { gifUrl ->
                GifBackground(gifUrl)
                // GIPHY rule: show this mark where their GIFs show.
                Text(
                    "Powered by GIPHY",
                    color = Color.White,
                    fontSize = 8.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                )
            }

            CircularProgressIndicator(
                progress = if (resting) 1f - elapsedMs.toFloat() / totalMs else 1f,
                modifier = Modifier.fillMaxSize().padding(4.dp),
                indicatorColor = if (resting) Green else Orange,
                trackColor = Color(0xFF1F2937),
                strokeWidth = 6.dp,
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (resting) {
                    val remainingSeconds = (totalMs - elapsedMs + 999) / 1000
                    Text("REST", color = Green, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(formatTime(remainingSeconds), color = Color.White, fontSize = 60.sp)
                } else {
                    Text("GO", color = Orange, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("+" + formatTime((elapsedMs - totalMs) / 1000), color = Orange, fontSize = 52.sp)
                }
                Text("total " + formatTime(elapsedMs / 1000), color = Dim, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun GifBackground(url: String) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(ImageDecoderDecoder.Factory()) }
            .crossfade(true)
            .build()
    }
    AsyncImage(
        model = url,
        contentDescription = null,
        imageLoader = imageLoader,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
    )
    // Dark layer: the timer text must stay easy to read.
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
}

@Composable
private fun SettingsScreen(onDone: () -> Unit, onGifToggle: () -> Unit) {
    val context = LocalContext.current
    // Page 0: the +/- control. Page 1 (swipe): the preset bubbles.
    // Each page fills the screen, so its content is always in the center.
    VerticalPager(
        state = rememberPagerState(pageCount = { 2 }),
        modifier = Modifier.fillMaxSize().background(Color.Black),
    ) { page ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (page == 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Rest time", color = Dim, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { RestTimer.changeRest(context, -RestTimer.STEP_SECONDS) },
                            colors = ButtonDefaults.secondaryButtonColors(),
                        ) { Text("-", fontSize = 26.sp) }
                        Text(
                            formatTime(RestTimer.restSeconds.toLong()),
                            color = Color.White,
                            fontSize = 34.sp,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                        Button(
                            onClick = { RestTimer.changeRest(context, RestTimer.STEP_SECONDS) },
                            colors = ButtonDefaults.secondaryButtonColors(),
                        ) { Text("+", fontSize = 26.sp) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onGifToggle,
                            modifier = Modifier.width(70.dp).height(40.dp),
                            colors = if (Gifs.enabled) {
                                ButtonDefaults.buttonColors(backgroundColor = Orange, contentColor = Color.Black)
                            } else {
                                ButtonDefaults.secondaryButtonColors()
                            },
                        ) {
                            Text(
                                if (Gifs.enabled) "GIF on" else "GIF off",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Button(
                            onClick = onDone,
                            modifier = Modifier.width(70.dp).height(40.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Green, contentColor = Color.Black),
                        ) { Text("OK", fontWeight = FontWeight.Bold) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (Billing.unlocked) {
                            "Unlocked. Thank you!"
                        } else if (Trial.remainingMinutes > 0) {
                            "Free trial: ${Trial.remainingText} left"
                        } else {
                            "Free trial ended"
                        },
                        color = Dim,
                        fontSize = 11.sp,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RestTimer.PRESETS_SECONDS.chunked(2).forEach { rowPresets ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowPresets.forEach { seconds ->
                                PresetBubble(
                                    seconds = seconds,
                                    selected = seconds == RestTimer.restSeconds,
                                    onClick = {
                                        RestTimer.setRest(context, seconds)
                                        onDone()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaywallScreen(onUnlock: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Free trial ended", color = Orange, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Pay one time.\nUse it forever.",
            color = Color.White,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onUnlock,
            modifier = Modifier.width(150.dp).height(44.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Green, contentColor = Color.Black),
        ) {
            Text("Unlock " + (Billing.priceText ?: ""), fontWeight = FontWeight.Bold)
        }
        Billing.message?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = Dim, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun PresetBubble(seconds: Int, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(64.dp),
        colors = if (selected) {
            ButtonDefaults.buttonColors(backgroundColor = Green, contentColor = Color.Black)
        } else {
            ButtonDefaults.secondaryButtonColors()
        },
    ) { Text(formatTime(seconds.toLong()), fontSize = 17.sp, fontWeight = FontWeight.Bold) }
}
