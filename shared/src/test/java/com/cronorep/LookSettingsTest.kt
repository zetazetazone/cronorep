package com.cronorep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LookSettingsTest {
    private val knownGif = GifCatalog.IDS.first()

    @Test
    fun nullValuesGiveTheDefaults() {
        assertEquals(LookSettings(), LookSettings.fromRaw(null, null, null, null, null))
    }

    @Test
    fun oldGifSwitchBecomesRandomGif() {
        val look = LookSettings.fromRaw(null, null, null, null, null, legacyGifsEnabled = true)
        assertEquals(BackgroundKind.GIF_RANDOM, look.background)
    }

    @Test
    fun savedBackgroundWinsOverTheOldGifSwitch() {
        val look = LookSettings.fromRaw(null, null, "BLACK", null, null, legacyGifsEnabled = true)
        assertEquals(BackgroundKind.BLACK, look.background)
    }

    @Test
    fun unknownBackgroundNameGivesBlack() {
        assertEquals(BackgroundKind.BLACK, LookSettings.fromRaw(null, null, "PLASMA", null, null).background)
    }

    @Test
    fun unknownBackgroundNameGivesBlackAlsoWithTheOldGifSwitch() {
        val look = LookSettings.fromRaw(null, null, "PLASMA", null, null, legacyGifsEnabled = true)
        assertEquals(BackgroundKind.BLACK, look.background)
    }

    @Test
    fun gradientFallsBackToTheFirstOne() {
        assertEquals(Gradients.ALL[2], LookSettings(gradientId = 2).gradient)
        assertEquals(Gradients.ALL[0], LookSettings(gradientId = 99).gradient)
        assertEquals(Gradients.ALL[0], LookSettings(gradientId = -1).gradient)
    }

    @Test
    fun gradientOutOfRangeGivesZero() {
        assertEquals(0, LookSettings.fromRaw(null, null, "GRADIENT", 99, null).gradientId)
        assertEquals(0, LookSettings.fromRaw(null, null, "GRADIENT", -1, null).gradientId)
        assertEquals(5, LookSettings.fromRaw(null, null, "GRADIENT", 5, null).gradientId)
    }

    @Test
    fun unknownGifIdIsDroppedAndFixedFallsBackToRandom() {
        val look = LookSettings.fromRaw(null, null, "GIF_FIXED", null, "not-a-real-id")
        assertNull(look.gifId)
        assertEquals(BackgroundKind.GIF_RANDOM, look.effectiveBackground)
    }

    @Test
    fun knownGifIdIsKept() {
        val look = LookSettings.fromRaw(true, false, "GIF_FIXED", 2, knownGif)
        assertEquals(LookSettings(true, false, BackgroundKind.GIF_FIXED, 2, knownGif), look)
        assertEquals(BackgroundKind.GIF_FIXED, look.effectiveBackground)
    }

    @Test
    fun saveBatteryForcesBlack() {
        val look = LookSettings(saveBattery = true, background = BackgroundKind.GRADIENT)
        assertEquals(BackgroundKind.BLACK, look.effectiveBackground)
        assertEquals(BackgroundKind.GRADIENT, look.background)
    }

    @Test
    fun nextBackgroundSkipsFixedGifWhenNoGifIsChosen() {
        var look = LookSettings()
        look = look.withNextBackground(); assertEquals(BackgroundKind.GRADIENT, look.background)
        look = look.withNextBackground(); assertEquals(BackgroundKind.GIF_RANDOM, look.background)
        look = look.withNextBackground(); assertEquals(BackgroundKind.BLACK, look.background)
    }

    @Test
    fun nextBackgroundIncludesFixedGifWhenAGifIsChosen() {
        var look = LookSettings(background = BackgroundKind.GIF_RANDOM, gifId = knownGif)
        look = look.withNextBackground(); assertEquals(BackgroundKind.GIF_FIXED, look.background)
        look = look.withNextBackground(); assertEquals(BackgroundKind.BLACK, look.background)
    }

    @Test
    fun catalogsAreSane() {
        assertEquals(6, Gradients.ALL.size)
        assertTrue(GifCatalog.IDS.isNotEmpty())
        assertEquals(GifCatalog.IDS.size, GifCatalog.IDS.toSet().size)
        assertEquals("https://media.giphy.com/media/$knownGif/200.gif", GifCatalog.url(knownGif))
        assertEquals("https://media.giphy.com/media/$knownGif/100_s.gif", GifCatalog.thumbUrl(knownGif))
    }
}
