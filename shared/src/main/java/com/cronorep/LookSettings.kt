package com.cronorep

enum class BackgroundKind { BLACK, GRADIENT, GIF_RANDOM, GIF_FIXED }

/** How the timer looks. The phone and the watch use the same model. */
data class LookSettings(
    /** No progress ring and no "total" line. */
    val minimalist: Boolean = false,
    /** Black background and one screen update each second. */
    val saveBattery: Boolean = false,
    val background: BackgroundKind = BackgroundKind.BLACK,
    /** Index in [Gradients.ALL]. */
    val gradientId: Int = 0,
    /** GIPHY id for [BackgroundKind.GIF_FIXED]. Null = no GIF chosen. */
    val gifId: String? = null,
) {
    /** The background to draw. */
    val effectiveBackground: BackgroundKind
        get() = when {
            saveBattery -> BackgroundKind.BLACK
            background == BackgroundKind.GIF_FIXED && gifId == null -> BackgroundKind.GIF_RANDOM
            else -> background
        }

    /** The gradient to draw. A bad [gradientId] gives the first one, so drawing never crashes. */
    val gradient: Gradient
        get() = Gradients.ALL.getOrElse(gradientId) { Gradients.ALL[0] }

    /** For the one-button background control on the watch. */
    fun withNextBackground(): LookSettings = copy(
        background = when (background) {
            BackgroundKind.BLACK -> BackgroundKind.GRADIENT
            BackgroundKind.GRADIENT -> BackgroundKind.GIF_RANDOM
            BackgroundKind.GIF_RANDOM -> if (gifId != null) BackgroundKind.GIF_FIXED else BackgroundKind.BLACK
            BackgroundKind.GIF_FIXED -> BackgroundKind.BLACK
        }
    )

    companion object {
        /**
         * Builds settings from stored or received values. Bad values fall back to the
         * defaults. [legacyGifsEnabled] is the old "GIF on" switch: it applies only
         * when no background is saved.
         */
        fun fromRaw(
            minimalist: Boolean?,
            saveBattery: Boolean?,
            background: String?,
            gradientId: Int?,
            gifId: String?,
            legacyGifsEnabled: Boolean = false,
        ): LookSettings = LookSettings(
            minimalist = minimalist ?: false,
            saveBattery = saveBattery ?: false,
            background = when {
                background != null ->
                    BackgroundKind.entries.firstOrNull { it.name == background } ?: BackgroundKind.BLACK
                legacyGifsEnabled -> BackgroundKind.GIF_RANDOM
                else -> BackgroundKind.BLACK
            },
            gradientId = gradientId?.takeIf { it in Gradients.ALL.indices } ?: 0,
            gifId = gifId?.takeIf { it in GifCatalog.IDS },
        )
    }
}
