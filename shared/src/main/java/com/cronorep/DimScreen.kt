package com.cronorep

import android.app.Activity
import android.view.WindowManager

/** Screen brightness in dim mode, from 0 (darkest) to 1. */
private const val DIM_BRIGHTNESS = 0.1f

/**
 * Dim mode ([LookSettings.saveBattery]) turns the screen brightness down while this app is
 * on screen. Off gives the brightness back to the system.
 */
fun Activity.applyDim(dim: Boolean) {
    val wanted = if (dim) DIM_BRIGHTNESS else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    if (window.attributes.screenBrightness == wanted) return
    window.attributes = window.attributes.apply { screenBrightness = wanted }
}
