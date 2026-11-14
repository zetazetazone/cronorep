package com.cronorep

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Optional background: one gym GIF for each timer start, from a fixed list.
 * No API call. The image loader keeps downloaded GIFs on the watch, so a
 * GIF downloads only the first time it shows.
 */
object Gifs {
    private const val PREFS = "cronorep"
    private const val KEY_ENABLED = "gifs_enabled"

    /** GIPHY IDs. Chosen by hand: no celebrities, films or TV characters. */
    private val GIF_IDS = listOf(
        "10cylTGU0KcAsE",
        "3o7btMPzJrsdyWVHeo",
        "cIyGi8mYQeiudC3IxK",
        "XYVT2rKkaN7GdiE2eN",
        "13Xp7ZZiRU4Hzq",
        "HSBsApUGF4erUp7NHV",
        "hWddcO7xQWCqIew5rH",
        "6uPX6ZNoCiPNxVmAUB",
        "7kbEhOfZzxNPTerRRz",
        "lOqNS2HUyN8OJV0CuF",
        "kcEN2XhJNHtvKGEG5q",
        "H4TsgO30w3qZZeMmlL",
        "M5kXUwhern0mQ",
        "WbsQEnBiIWKIM",
        "cuXLiXhdgOuJi",
    )

    var enabled by mutableStateOf(false)
        private set

    /** URL of the GIF to show. Null = show the plain black background. */
    var currentUrl by mutableStateOf<String?>(null)
        private set

    private var currentId: String? = null

    fun load(context: Context) {
        enabled = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)
        if (enabled && currentUrl == null) showNext()
    }

    fun toggle(context: Context) {
        enabled = !enabled
        if (enabled) {
            // Show a GIF at once. The user must not wait for the next tap.
            showNext()
        } else {
            currentId = null
            currentUrl = null
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** Picks a random GIF that is not the current one. */
    fun showNext() {
        val id = GIF_IDS.filter { it != currentId }.random()
        currentId = id
        // 200.gif = 200 px high: sharp enough for a watch and a small download.
        currentUrl = "https://media.giphy.com/media/$id/200.gif"
    }
}
