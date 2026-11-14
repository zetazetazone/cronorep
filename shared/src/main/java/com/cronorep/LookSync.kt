package com.cronorep

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

/** Sends the look settings from the phone to the watch through the Data Layer. */
object LookSync {
    const val PATH = "/look"

    private const val KEY_MINIMALIST = "minimalist"
    private const val KEY_SAVE_BATTERY = "save_battery"
    private const val KEY_BACKGROUND = "background"
    private const val KEY_GRADIENT = "gradient"
    private const val KEY_GIF_ID = "gif_id"

    fun write(settings: LookSettings, map: DataMap) {
        map.putBoolean(KEY_MINIMALIST, settings.minimalist)
        map.putBoolean(KEY_SAVE_BATTERY, settings.saveBattery)
        map.putString(KEY_BACKGROUND, settings.background.name)
        map.putInt(KEY_GRADIENT, settings.gradientId)
        // The Data Layer has no null strings: empty = no GIF chosen.
        map.putString(KEY_GIF_ID, settings.gifId ?: "")
    }

    fun fromDataMap(map: DataMap): LookSettings = LookSettings.fromRaw(
        minimalist = if (map.containsKey(KEY_MINIMALIST)) map.getBoolean(KEY_MINIMALIST) else null,
        saveBattery = if (map.containsKey(KEY_SAVE_BATTERY)) map.getBoolean(KEY_SAVE_BATTERY) else null,
        background = map.getString(KEY_BACKGROUND),
        gradientId = if (map.containsKey(KEY_GRADIENT)) map.getInt(KEY_GRADIENT) else null,
        gifId = map.getString(KEY_GIF_ID),
    )

    /** The Data Layer keeps the item and delivers it when a watch connects. */
    fun send(context: Context, settings: LookSettings): Task<DataItem> {
        val request = PutDataMapRequest.create(PATH)
            .apply { write(settings, dataMap) }
            .asPutDataRequest()
            .setUrgent()
        return Wearable.getDataClient(context).putDataItem(request)
    }
}
