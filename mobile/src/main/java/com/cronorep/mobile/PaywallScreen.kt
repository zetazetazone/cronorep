package com.cronorep.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cronorep.Billing

@Composable
fun PaywallScreen(onUnlock: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Free trial ended", color = Orange, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Pay one time.\nUse it forever.",
            color = Color.White,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onUnlock,
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.Black),
        ) {
            Text(listOfNotNull("Unlock", Billing.priceText).joinToString(" "), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Billing.message?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = Dim, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Privacy policy",
            color = Dim,
            fontSize = 14.sp,
            modifier = Modifier.clickable { uriHandler.openUri(PRIVACY_POLICY_URL) }.padding(8.dp),
        )
    }
}
