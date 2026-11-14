package com.cronorep

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue

/**
 * Free trial clock. It counts only the time that the app is on screen
 * with a started timer. The lock screen and the purchase come later.
 */
object Trial {
    const val LIMIT_MS = 10 * 60 * 60 * 1000L

    private const val PREFS = "cronorep"
    private const val KEY_USED_MS = "trial_used_ms"

    var usedMs by mutableLongStateOf(0L)
        private set

    /** SystemClock.elapsedRealtime() of the last flush. 0 = not counting. */
    private var countingSince = 0L

    val expired: Boolean
        get() = usedMs >= LIMIT_MS

    val remainingMinutes: Long
        get() = ((LIMIT_MS - usedMs).coerceAtLeast(0L) + 59_999L) / 60_000L

    /** Example: "9 h 58 min" or "42 min". */
    val remainingText: String
        get() {
            val minutes = remainingMinutes
            return if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "$minutes min"
        }

    fun load(context: Context) {
        usedMs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_USED_MS, 0L)
    }

    /** Takes the server value when it is bigger, for example after a new install. */
    fun merge(context: Context, serverMs: Long) {
        val merged = TrialMath.mergeUsed(usedMs, serverMs)
        if (merged == usedMs) return
        usedMs = merged
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_USED_MS, usedMs).apply()
    }

    fun startCounting() {
        if (countingSince == 0L) countingSince = SystemClock.elapsedRealtime()
    }

    /** Adds the time since the last flush to the total and saves it. */
    fun flush(context: Context) {
        if (countingSince == 0L) return
        val now = SystemClock.elapsedRealtime()
        usedMs += now - countingSince
        countingSince = now
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_USED_MS, usedMs).apply()
    }

    fun stopCounting(context: Context) {
        flush(context)
        countingSince = 0L
    }
}
