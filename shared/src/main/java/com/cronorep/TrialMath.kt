package com.cronorep

import java.security.MessageDigest

/**
 * Pure helpers for the free trial sync. No Android code here, so unit tests
 * run on the JVM. org.json is not used: it is a stub in local unit tests.
 */
object TrialMath {

    /** The used time never goes down. A server value is trusted only from 0 to the limit. */
    fun mergeUsed(local: Long, server: Long): Long =
        maxOf(local, server.coerceIn(0L, Trial.LIMIT_MS))

    /** sha256("cronorep-trial:" + ANDROID_ID) as lowercase hex. The raw ID never leaves the device. */
    fun trialId(androidId: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest("cronorep-trial:$androidId".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    /** The first 8 characters of the trial ID: a short code the user can send us to delete the data. */
    fun shortCode(trialId: String): String = trialId.take(8)

    /** id is hex and platform is a fixed word, so no JSON escaping is needed. */
    fun requestBody(id: String, platform: String, usedMs: Long): String =
        """{"id":"$id","platform":"$platform","usedMs":$usedMs}"""

    private val okRe = Regex(""""ok"\s*:\s*true""")
    private val usedRe = Regex(""""usedMs"\s*:\s*(\d{1,12})""")

    /** The usedMs of a {"ok":true,...} answer, or null for anything else. */
    fun parseUsedMs(body: String): Long? {
        if (!okRe.containsMatchIn(body)) return null
        return usedRe.find(body)?.groupValues?.get(1)?.toLongOrNull()
    }
}
