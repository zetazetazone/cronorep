package com.cronorep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrialMathTest {

    @Test
    fun mergeKeepsTheBiggerValue() {
        assertEquals(5_000L, TrialMath.mergeUsed(5_000L, 1_000L))
        assertEquals(9_000L, TrialMath.mergeUsed(5_000L, 9_000L))
        assertEquals(0L, TrialMath.mergeUsed(0L, 0L))
    }

    @Test
    fun mergeNeverTrustsAServerValueAboveTheLimitOrBelowZero() {
        assertEquals(Trial.LIMIT_MS, TrialMath.mergeUsed(0L, Trial.LIMIT_MS * 5))
        assertEquals(3_000L, TrialMath.mergeUsed(3_000L, -50L))
    }

    @Test
    fun trialIdIsTheSha256OfThePrefixedId() {
        assertEquals(
            "40369e4366938eb4dc6a12d2848d87b50ee0d352e3e584d2be16aaaa57229e7a",
            TrialMath.trialId("abc"),
        )
        assertEquals(64, TrialMath.trialId("9774d56d682e549c").length)
    }

    @Test
    fun requestBodyIsTheServerJson() {
        assertEquals(
            """{"id":"ab","platform":"wear","usedMs":42}""",
            TrialMath.requestBody("ab", "wear", 42L),
        )
    }

    @Test
    fun parseReadsUsedMsFromAnOkAnswer() {
        assertEquals(9_000L, TrialMath.parseUsedMs("""{"ok":true,"usedMs":9000}"""))
        assertEquals(7L, TrialMath.parseUsedMs("""{ "usedMs" : 7, "ok" : true }"""))
    }

    @Test
    fun parseRejectsErrorsAndJunk() {
        assertNull(TrialMath.parseUsedMs("""{"ok":false,"error":"id"}"""))
        assertNull(TrialMath.parseUsedMs("""{"ok":true}"""))
        assertNull(TrialMath.parseUsedMs("<html>502</html>"))
        assertNull(TrialMath.parseUsedMs(""))
    }

    @Test
    fun shortCodeIsTheFirstEightCharactersOfTheTrialId() {
        assertEquals("40369e43", TrialMath.shortCode(TrialMath.trialId("abc")))
    }
}
