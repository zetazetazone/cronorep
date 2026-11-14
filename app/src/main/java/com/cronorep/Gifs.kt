package com.cronorep

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

/**
 * Optional background: one random gym GIF from GIPHY for each timer start.
 * The API key comes from GIPHY_API_KEY in local.properties.
 */
object Gifs {
    private const val PREFS = "cronorep"
    private const val KEY_ENABLED = "gifs_enabled"
    private const val SEARCH_TAG = "gym workout"

    var enabled by mutableStateOf(false)
        private set

    /** URL of the GIF to show. Null = show the plain black background. */
    var currentUrl by mutableStateOf<String?>(null)
        private set

    fun load(context: Context) {
        enabled = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)
    }

    fun toggle(context: Context) {
        enabled = !enabled
        if (!enabled) currentUrl = null
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** Gets a new random GIF. On an error, the last GIF stays. */
    fun loadNext() {
        val key = BuildConfig.GIPHY_API_KEY
        if (key.isBlank()) return
        thread(name = "giphy") {
            runCatching {
                // rating=g: content for all ages only.
                val url = URL(
                    "https://api.giphy.com/v1/gifs/random?api_key=$key" +
                        "&tag=${URLEncoder.encode(SEARCH_TAG, "UTF-8")}&rating=g"
                )
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 8_000
                }
                try {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    JSONObject(body).getJSONObject("data").getJSONObject("images")
                        .getJSONObject("fixed_height").getString("url")
                } finally {
                    connection.disconnect()
                }
            }.onSuccess { if (enabled) currentUrl = it }
        }
    }
}
