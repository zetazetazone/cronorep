package com.cronorep.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cronorep.BackgroundKind
import com.cronorep.Billing
import com.cronorep.GifCatalog
import com.cronorep.Gradients
import com.cronorep.Look
import com.cronorep.LookSettings
import com.cronorep.RestTimer
import com.cronorep.Trial
import com.cronorep.formatTime

/** Rest time and the look of the timer. Look changes also go to the watch. */
@Composable
fun SettingsScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val look = Look.settings
    // Save battery mode forces a black background, so the choices are off.
    val canChooseBackground = !look.saveBattery

    LaunchedEffect(Unit) { WatchLink.check(context) }

    fun change(newLook: LookSettings) {
        Look.update(context, newLook)
        WatchLink.send(context, newLook)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        fullRow {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Settings", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.weight(1f))
                Button(
                    onClick = onDone,
                    colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.Black),
                ) { Text("Done", fontWeight = FontWeight.Bold) }
            }
        }

        fullRow { SectionTitle("Rest time") }
        fullRow {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { RestTimer.changeRest(context, -RestTimer.STEP_SECONDS) },
                    colors = ButtonDefaults.buttonColors(containerColor = Panel, contentColor = Color.White),
                ) { Text("-", fontSize = 26.sp) }
                Text(
                    formatTime(RestTimer.restSeconds.toLong()),
                    color = Color.White,
                    fontSize = 44.sp,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Button(
                    onClick = { RestTimer.changeRest(context, RestTimer.STEP_SECONDS) },
                    colors = ButtonDefaults.buttonColors(containerColor = Panel, contentColor = Color.White),
                ) { Text("+", fontSize = 26.sp) }
            }
        }
        fullRow {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                RestTimer.PRESETS_SECONDS.forEach { seconds ->
                    FilterChip(
                        selected = seconds == RestTimer.restSeconds,
                        onClick = { RestTimer.setRest(context, seconds) },
                        label = { Text(formatTime(seconds.toLong()), fontSize = 16.sp) },
                    )
                }
            }
        }

        fullRow { SectionTitle("Look") }
        fullRow {
            SwitchRow("Minimalist mode", "No progress ring. Only the time.", look.minimalist) {
                change(look.copy(minimalist = it))
            }
        }
        fullRow {
            SwitchRow("Save battery mode", "Black background. One screen update each second.", look.saveBattery) {
                change(look.copy(saveBattery = it))
            }
        }

        fullRow { SectionTitle("Background") }
        if (look.saveBattery) {
            fullRow { Text("Save battery mode is on. The background is black.", color = Orange, fontSize = 14.sp) }
        }
        fullRow {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = look.background == BackgroundKind.BLACK,
                    enabled = canChooseBackground,
                    onClick = { change(look.copy(background = BackgroundKind.BLACK)) },
                    label = { Text("Black") },
                )
                FilterChip(
                    selected = look.background == BackgroundKind.GIF_RANDOM,
                    enabled = canChooseBackground,
                    onClick = { change(look.copy(background = BackgroundKind.GIF_RANDOM)) },
                    label = { Text("Random GIF") },
                )
            }
        }
        fullRow {
            Row(
                modifier = Modifier.fillMaxWidth().alpha(if (canChooseBackground) 1f else 0.35f),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Gradients.ALL.forEachIndexed { index, gradient ->
                    val selected = look.background == BackgroundKind.GRADIENT && look.gradientId == index
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Brush.verticalGradient(listOf(Color(gradient.top), Color(gradient.bottom))))
                            .border(3.dp, if (selected) Color.White else Color.Transparent, CircleShape)
                            .clickable(enabled = canChooseBackground) {
                                change(look.copy(background = BackgroundKind.GRADIENT, gradientId = index))
                            }
                    )
                }
            }
        }
        fullRow { Text("Or choose one GIF", color = Dim, fontSize = 14.sp) }
        items(GifCatalog.IDS) { id ->
            val selected = look.background == BackgroundKind.GIF_FIXED && look.gifId == id
            AsyncImage(
                model = GifCatalog.thumbUrl(id),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .aspectRatio(1f)
                    .alpha(if (canChooseBackground) 1f else 0.35f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Panel)
                    .border(3.dp, if (selected) Green else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable(enabled = canChooseBackground) {
                        change(look.copy(background = BackgroundKind.GIF_FIXED, gifId = id))
                    },
            )
        }

        fullRow { SectionTitle("Watch") }
        fullRow {
            Text(
                if (WatchLink.connected) {
                    "Watch connected. It uses the same look."
                } else {
                    "No watch found. Your look settings go to the watch when it connects."
                },
                color = if (WatchLink.connected) Green else Dim,
                fontSize = 14.sp,
            )
        }
        fullRow {
            Text(
                if (Billing.unlocked) {
                    "Unlocked. Thank you!"
                } else if (Trial.remainingMinutes > 0) {
                    "Free trial: ${Trial.remainingText} left"
                } else {
                    "Free trial ended"
                },
                color = Dim,
                fontSize = 14.sp,
            )
        }
        fullRow {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Privacy policy",
                    color = Dim,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { uriHandler.openUri(PRIVACY_POLICY_URL) },
                )
                Text("GIFs by GIPHY", color = Dim, fontSize = 13.sp)
            }
        }
    }
}

/** One item that uses the full width of the grid. */
private fun LazyGridScope.fullRow(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        color = Green,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 10.dp),
    )
}

@Composable
private fun SwitchRow(title: String, note: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Panel)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 17.sp)
            Text(note, color = Dim, fontSize = 13.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
