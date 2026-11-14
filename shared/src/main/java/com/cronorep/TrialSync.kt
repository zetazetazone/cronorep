package com.cronorep

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.cronorep.shared.BuildConfig
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sends the used trial time to the trial server and gets the biggest value it
 * has for this device. A new install of the app gets the old used time back.
 * Every error is ignored: the timer never waits for the network.
 */
object TrialSync {

    private const val TIMEOUT_MS = 5_000

    /** onResult runs on the main thread, and only when the server answered. */
    fun sync(context: Context, platform: String, onResult: (Long) -> Unit) {
        val id = deviceTrialId(context) ?: return
        val usedMs = Trial.usedMs
        val main = Handler(Looper.getMainLooper())
        Thread {
            val serverMs = post(TrialMath.requestBody(id, platform, usedMs)) ?: return@Thread
            main.post { onResult(serverMs) }
        }.start()
    }

    /** ANDROID_ID stays the same after a new install. Only its hash is sent. */
    @SuppressLint("HardwareIds")
    private fun deviceTrialId(context: Context): String? {
        val raw = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return if (raw.isNullOrBlank()) null else TrialMath.trialId(raw)
    }

    private fun post(body: String): Long? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(BuildConfig.TRIAL_API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode != 200) return null
            val answer = connection.inputStream.bufferedReader().use { it.readText() }
            TrialMath.parseUsedMs(answer)
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
