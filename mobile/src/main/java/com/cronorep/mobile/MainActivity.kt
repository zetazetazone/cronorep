package com.cronorep.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.cronorep.Billing
import com.cronorep.Look
import com.cronorep.RestTimer
import com.cronorep.RestTimerService
import com.cronorep.Trial
import com.cronorep.applyDim
import com.cronorep.TrialSync

class MainActivity : ComponentActivity() {

    /** The answer does not matter: the timer works with and without the notification. */
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The app is always dark, so the system bar icons must be light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        // The screen stays on, so one tap always restarts the timer.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        RestTimer.load(this)
        Trial.load(this)
        Billing.init(this)
        // A new install gets the used trial time back from the server. A paid user needs no guard.
        if (!Billing.unlocked) TrialSync.sync(this, "phone") { Trial.merge(this, it) }
        Look.load(this)
        // Debug only: adb shell am start -n com.cronorep/com.cronorep.mobile.MainActivity --ez paywall true
        val previewPaywall = BuildConfig.DEBUG && intent.getBooleanExtra("paywall", false)

        setContent {
            SideEffect { applyDim(Look.settings.saveBattery) }
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Green,
                    onPrimary = Color.Black,
                    secondary = Orange,
                    background = Color.Black,
                    surface = Panel,
                )
            ) {
                var showSettings by rememberSaveable { mutableStateOf(false) }
                var showPaywall by rememberSaveable { mutableStateOf(previewPaywall) }
                if (showPaywall && !Billing.unlocked) {
                    BackHandler { showPaywall = false }
                    PaywallScreen(onUnlock = { Billing.buy(this) })
                } else if (showSettings) {
                    BackHandler { showSettings = false }
                    SettingsScreen(onDone = { showSettings = false })
                } else {
                    // Android 8 to 11 finish the activity on Back, and that stops a running timer.
                    BackHandler(enabled = RestTimer.startedAt != 0L) { moveTaskToBack(true) }
                    TimerScreen(
                        onTap = {
                            Trial.flush(this)
                            if (isLocked()) showPaywall = true else restartTimer()
                        },
                        onOpenSettings = {
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

    override fun onResume() {
        super.onResume()
        Billing.refresh()
        if (RestTimer.startedAt != 0L) Trial.startCounting()
        // A watch may have connected since the last look change: send it then.
        WatchLink.check(this)
    }

    override fun onPause() {
        Trial.stopCounting(this)
        if (!Billing.unlocked) TrialSync.sync(this, "phone") { Trial.merge(this, it) }
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
        Look.onTimerStart()
        // Short tick: the tap worked.
        getSystemService(Vibrator::class.java)
            .vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        askForNotificationsOnce()
    }

    /** Android 13 and later: the "Rest timer is running" notification needs a permission. */
    private fun askForNotificationsOnce() {
        if (Build.VERSION.SDK_INT < 33) return
        // Saved, so the question does not come back after each app start.
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ASKED_NOTIFICATIONS, false)) return
        prefs.edit().putBoolean(KEY_ASKED_NOTIFICATIONS, true).apply()
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private companion object {
        const val PREFS = "cronorep"
        const val KEY_ASKED_NOTIFICATIONS = "asked_notifications"
    }
}
