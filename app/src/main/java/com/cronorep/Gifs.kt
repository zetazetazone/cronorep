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

    /** GIPHY IDs. The app owner chose this list. */
    private val GIF_IDS = listOf(
        "HSBsApUGF4erUp7NHV",
        "hWddcO7xQWCqIew5rH",
        "6uPX6ZNoCiPNxVmAUB",
        "7kbEhOfZzxNPTerRRz",
        "lOqNS2HUyN8OJV0CuF",
        "H4TsgO30w3qZZeMmlL",
        "WbsQEnBiIWKIM",
        "cuXLiXhdgOuJi",
        "3oriNZNU6VDrDxM7sI",
        "gElZT8LKDPJeg",
        "zy5evPW7VPCKI",
        "TyuwvnAzEPVFC",
        "0py1N0feFG64kbG2r3",
        "kzqdxYKp2yy31ROpUM",
        "39lwENv9kD5nO",
        "ehsEjVlJeXiqBqEpGc",
        "oShObTfbg3S5G",
        "0YvEGKIctG6nNOwdLd",
        "FSO0qBQ9Dwno4",
        "XMJb1k3GuouMfXSPIJ",
        "VIMDNQUn0rQ0L4ojIr",
        "bdpIDWZFkI67S",
        "XDSJtVcyGNXXrcCDnr",
        "VRGeARH0HUgr6",
        "Nf9kT8LvdkkStAMGEp",
        "3oz8xDnHOUs1Zmgcyk",
        "Y7O3LHmhllEk",
        "KHhaSdqhakNxa884ea",
        "3o6Ztq2Ed2cSSwAg8M",
        "xUPGcKoAYCn5fHK0Zq",
        "7ueSsRAZsOvDST7Q8p",
        "nFyfPbGijyKMZrF65y",
        "shuybsYkLzyUomNI17",
        "IuZ47lDMjN9yXx4C41",
        "h6M6Dfp5eXCjoeBzJC",
        "HQ9dS2mcEiqYw",
        "pPZ0wbiNbwJMDEzFxm",
        "YpZh61QjwuXq15FhV7",
        "KxKU0W6iBy5M88MwnY",
        "tskVbKESeJPSpVgf57",
        "wVKWhHbI3afhx3MW1e",
        "VFDeGtRSHswfe",
        "NJQubFbs35Vug",
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
