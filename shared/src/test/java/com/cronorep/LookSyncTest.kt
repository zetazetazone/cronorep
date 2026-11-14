package com.cronorep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LookSyncTest {
    private val knownGif = GifCatalog.IDS[3]

    @Test
    fun roundTripKeepsEachSetting() {
        val look = LookSettings(
            minimalist = true,
            saveBattery = true,
            background = BackgroundKind.GIF_FIXED,
            gradientId = 4,
            gifId = knownGif,
        )
        val map = LookSync.toMap(look, sentAt = 1234L)
        assertEquals(look, LookSync.fromMap(map))
        assertEquals(1234L, LookSync.sentAt(map))
    }

    @Test
    fun noGifGoesOutAsEmptyStringAndComesBackNull() {
        val map = LookSync.toMap(LookSettings(background = BackgroundKind.GIF_RANDOM), sentAt = 1L)
        assertEquals("", map["gif_id"])
        assertNull(LookSync.fromMap(map).gifId)
    }

    @Test
    fun absentKeysGiveTheDefaults() {
        assertEquals(LookSettings(), LookSync.fromMap(emptyMap()))
    }

    @Test
    fun wrongTypeValuesFallBackToTheDefaults() {
        val map = mapOf(
            "minimalist" to "yes",
            "save_battery" to 1,
            "background" to 7,
            "gradient" to "2",
            "gif_id" to 5L,
            "sent_at" to "now",
        )
        assertEquals(LookSettings(), LookSync.fromMap(map))
        assertEquals(0L, LookSync.sentAt(map))
    }

    @Test
    fun sentAtIsZeroWhenAbsent() {
        assertEquals(0L, LookSync.sentAt(emptyMap()))
        assertEquals(0L, LookSync.sentAt(mapOf("sent_at" to null)))
    }
}
