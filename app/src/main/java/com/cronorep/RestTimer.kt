package com.cronorep

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue

/** Timer state for the whole app. It stays alive when the activity is recreated. */
object RestTimer {
    const val DEFAULT_REST_SECONDS = 90
    const val MIN_REST_SECONDS = 10
    const val MAX_REST_SECONDS = 600
    const val STEP_SECONDS = 5
    val PRESETS_SECONDS = listOf(30, 60, 90, 120)

    private const val PREFS = "cronorep"
    private const val KEY_REST = "rest_seconds"

    /** SystemClock.elapsedRealtime() of the last tap. 0 = not started. */
    var startedAt by mutableLongStateOf(0L)

    /** Rest time of the current run. A settings change applies on the next tap. */
    var runSeconds by mutableIntStateOf(DEFAULT_REST_SECONDS)

    /** Configured rest time. */
    var restSeconds by mutableIntStateOf(DEFAULT_REST_SECONDS)
        private set

    fun load(context: Context) {
        restSeconds = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_REST, DEFAULT_REST_SECONDS)
            .coerceIn(MIN_REST_SECONDS, MAX_REST_SECONDS)
    }

    fun changeRest(context: Context, deltaSeconds: Int) = setRest(context, restSeconds + deltaSeconds)

    fun setRest(context: Context, seconds: Int) {
        restSeconds = seconds.coerceIn(MIN_REST_SECONDS, MAX_REST_SECONDS)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_REST, restSeconds).apply()
    }
}

fun formatTime(totalSeconds: Long): String = "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
