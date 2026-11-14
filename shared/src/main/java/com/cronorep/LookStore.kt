package com.cronorep

import android.content.Context

/** Keeps the look settings on the device. */
object LookStore {
    private const val PREFS = "cronorep"
    private const val KEY_MINIMALIST = "look_minimalist"
    private const val KEY_SAVE_BATTERY = "look_save_battery"
    private const val KEY_BACKGROUND = "look_background"
    private const val KEY_GRADIENT = "look_gradient"
    private const val KEY_GIF_ID = "look_gif_id"

    /** The old "GIF on" switch from version 1.0.0 of the watch app. */
    private const val KEY_LEGACY_GIFS = "gifs_enabled"

    /** Watch side: send time of the last look the phone sent that the watch applied. */
    private const val KEY_SYNCED_AT = "look_synced_at"

    fun syncedAt(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_SYNCED_AT, 0L)

    fun setSyncedAt(context: Context, sentAt: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_SYNCED_AT, sentAt).apply()
    }

    fun load(context: Context): LookSettings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return LookSettings.fromRaw(
            minimalist = prefs.getBoolean(KEY_MINIMALIST, false),
            saveBattery = prefs.getBoolean(KEY_SAVE_BATTERY, false),
            background = prefs.getString(KEY_BACKGROUND, null),
            gradientId = prefs.getInt(KEY_GRADIENT, 0),
            gifId = prefs.getString(KEY_GIF_ID, null),
            legacyGifsEnabled = prefs.getBoolean(KEY_LEGACY_GIFS, false),
        )
    }

    fun save(context: Context, settings: LookSettings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_MINIMALIST, settings.minimalist)
            .putBoolean(KEY_SAVE_BATTERY, settings.saveBattery)
            .putString(KEY_BACKGROUND, settings.background.name)
            .putInt(KEY_GRADIENT, settings.gradientId)
            .putString(KEY_GIF_ID, settings.gifId)
            .apply()
    }
}
