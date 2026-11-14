package com.cronorep

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The current look of the timer, for the whole app. */
object Look {
    var settings by mutableStateOf(LookSettings())
        private set

    /** URL of the GIF to show. Null = no GIF. */
    var gifUrl by mutableStateOf<String?>(null)
        private set

    private var gifId: String? = null

    fun load(context: Context) {
        apply(LookStore.load(context))
    }

    fun update(context: Context, newSettings: LookSettings) {
        LookStore.save(context, newSettings)
        apply(newSettings)
    }

    /** Call at each timer start. Random mode then shows a different GIF. */
    fun onTimerStart() {
        if (settings.effectiveBackground == BackgroundKind.GIF_RANDOM) showRandomGif()
    }

    private fun apply(newSettings: LookSettings) {
        // A change that stays in random mode (for example minimalist on) keeps the GIF.
        val wasRandom = settings.effectiveBackground == BackgroundKind.GIF_RANDOM && gifId != null
        settings = newSettings
        when (newSettings.effectiveBackground) {
            // Show a GIF at once, also after a fixed GIF. The user must not wait for the next tap.
            BackgroundKind.GIF_RANDOM -> if (!wasRandom) showRandomGif()
            BackgroundKind.GIF_FIXED -> showGif(newSettings.gifId ?: return)
            BackgroundKind.BLACK, BackgroundKind.GRADIENT -> {
                gifId = null
                gifUrl = null
            }
        }
    }

    /** Picks a random GIF that is not the current one. */
    private fun showRandomGif() = showGif(GifCatalog.IDS.filter { it != gifId }.random())

    private fun showGif(id: String) {
        gifId = id
        gifUrl = GifCatalog.url(id)
    }
}
