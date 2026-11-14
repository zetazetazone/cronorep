package com.cronorep.mobile

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cronorep.Look
import com.cronorep.LookSettings
import com.cronorep.LookSync
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable

/** The link to the watch. A phone with no watch is normal: nothing here can fail loudly. */
object WatchLink {
    private const val PREFS = "cronorep"

    /** True from a send until the Data Layer accepts it. A phone with no watch makes the put fail. */
    private const val KEY_SEND_PENDING = "look_send_pending"

    /** True when a watch with CronoRep is reachable. */
    var connected by mutableStateOf(false)
        private set

    /** [resend]: send the look again if the last put failed and a watch is there now. */
    fun check(context: Context, resend: Boolean = true) {
        val appContext = context.applicationContext
        Wearable.getCapabilityClient(appContext)
            .getCapability(LookSync.WATCH_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
            .addOnSuccessListener { info ->
                connected = info.nodes.isNotEmpty()
                if (connected && resend && isPending(appContext)) send(appContext, Look.settings)
            }
            .addOnFailureListener { connected = false }
    }

    fun send(context: Context, settings: LookSettings) {
        val appContext = context.applicationContext
        setPending(appContext, true)
        LookSync.send(appContext, settings).addOnCompleteListener { task ->
            if (task.isSuccessful) setPending(appContext, false)
            // No resend from here: a put that fails each time must not loop.
            check(appContext, resend = false)
        }
    }

    private fun isPending(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SEND_PENDING, false)

    private fun setPending(context: Context, pending: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_SEND_PENDING, pending).apply()
    }
}
