package com.cronorep.mobile

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cronorep.LookSettings
import com.cronorep.LookSync
import com.google.android.gms.wearable.Wearable

/** The link to the watch. A phone with no watch is normal: nothing here can fail loudly. */
object WatchLink {
    var connected by mutableStateOf(false)
        private set

    fun check(context: Context) {
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes -> connected = nodes.isNotEmpty() }
            .addOnFailureListener { connected = false }
    }

    fun send(context: Context, settings: LookSettings) {
        LookSync.send(context, settings).addOnFailureListener { connected = false }
        check(context)
    }
}
