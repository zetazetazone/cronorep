package com.cronorep

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

/** Sends the look settings from the phone to the watch through the Data Layer. */
object LookSync {
    const val PATH = "/look"

    /** The watch app says it has CronoRep with this name. It must match `app/src/main/res/values/wear.xml`. */
    const val WATCH_CAPABILITY = "cronorep_watch"

    private const val KEY_MINIMALIST = "minimalist"
    private const val KEY_SAVE_BATTERY = "save_battery"
    private const val KEY_BACKGROUND = "background"
    private const val KEY_GRADIENT = "gradient"
    private const val KEY_GIF_ID = "gif_id"

    /** Phone clock time of the send. Each put is then a real change, and the watch can skip old items. */
    private const val KEY_SENT_AT = "sent_at"

    fun toMap(settings: LookSettings, sentAt: Long): Map<String, Any> = mapOf(
        KEY_MINIMALIST to settings.minimalist,
        KEY_SAVE_BATTERY to settings.saveBattery,
        KEY_BACKGROUND to settings.background.name,
        KEY_GRADIENT to settings.gradientId,
        // The Data Layer has no null strings: empty = no GIF chosen.
        KEY_GIF_ID to (settings.gifId ?: ""),
        KEY_SENT_AT to sentAt,
    )

    /** Absent values and values of the wrong type fall back to the defaults. */
    fun fromMap(map: Map<String, Any?>): LookSettings = LookSettings.fromRaw(
        minimalist = map[KEY_MINIMALIST] as? Boolean,
        saveBattery = map[KEY_SAVE_BATTERY] as? Boolean,
        background = map[KEY_BACKGROUND] as? String,
        gradientId = map[KEY_GRADIENT] as? Int,
        gifId = map[KEY_GIF_ID] as? String,
    )

    /** 0 when the item has no send time. */
    fun sentAt(map: Map<String, Any?>): Long = map[KEY_SENT_AT] as? Long ?: 0L

    fun send(context: Context, settings: LookSettings): Task<DataItem> {
        val request = PutDataMapRequest.create(PATH)
            .apply { dataMap.putAll(toMap(settings, System.currentTimeMillis())) }
            .asPutDataRequest()
            .setUrgent()
        return Wearable.getDataClient(context).putDataItem(request)
    }

    /**
     * Watch side. Applies a received item only when it is newer than the last one applied,
     * so a look change made on the watch stays until the phone sends a newer one.
     * The change runs on the main thread, as all other [Look] changes do.
     */
    fun receive(context: Context, map: DataMap) {
        val values = map.toValues()
        val sentAt = sentAt(values)
        val settings = fromMap(values)
        val appContext = context.applicationContext
        Handler(Looper.getMainLooper()).post {
            if (sentAt > LookStore.syncedAt(appContext)) {
                LookStore.setSyncedAt(appContext, sentAt)
                Look.update(appContext, settings)
            }
        }
    }

    /** Watch side. Reads the item the phone sent while the watch app was not listening. */
    fun fetchLatest(context: Context) {
        val appContext = context.applicationContext
        Wearable.getDataClient(appContext)
            .getDataItems(Uri.parse("wear://*$PATH"))
            .addOnSuccessListener { buffer ->
                try {
                    buffer.map { DataMapItem.fromDataItem(it).dataMap }
                        .maxByOrNull { sentAt(it.toValues()) }
                        ?.let { receive(appContext, it) }
                } finally {
                    buffer.release()
                }
            }
            // A watch with no phone is normal. Nothing to do.
            .addOnFailureListener { }
    }

    private fun DataMap.putAll(values: Map<String, Any>) {
        for ((key, value) in values) {
            when (value) {
                is Boolean -> putBoolean(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is String -> putString(key, value)
            }
        }
    }

    private fun DataMap.toValues(): Map<String, Any?> = keySet().associateWith { get<Any?>(it) }
}
