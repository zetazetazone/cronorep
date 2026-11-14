package com.cronorep.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cronorep.RestTimer
import com.cronorep.formatTime

@Composable
fun SettingsScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("Rest time", color = Dim, fontSize = 16.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { RestTimer.changeRest(context, -RestTimer.STEP_SECONDS) }) { Text("-", fontSize = 26.sp) }
            Text(
                formatTime(RestTimer.restSeconds.toLong()),
                color = Color.White,
                fontSize = 48.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Button(onClick = { RestTimer.changeRest(context, RestTimer.STEP_SECONDS) }) { Text("+", fontSize = 26.sp) }
        }
        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.Black),
        ) { Text("Done", fontWeight = FontWeight.Bold) }
    }
}
