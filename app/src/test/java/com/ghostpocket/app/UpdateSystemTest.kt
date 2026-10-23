package com.ghostpocket.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.regex.Pattern

class UpdateSystemTest {

    @Test
    fun testSemanticVersionComparison() {
        fun isSemanticVersionNewer(remote: String, current: String): Boolean {
            val remoteParts = remote.trim().removePrefix("v").removePrefix("V").split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = current.trim().removePrefix("v").removePrefix("V").split(".").mapNotNull { it.toIntOrNull() }
            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        }

        assertTrue(isSemanticVersionNewer("1.4.1", "1.4.0"))
        assertTrue(isSemanticVersionNewer("v2.0.0", "1.4.0"))
        assertTrue(isSemanticVersionNewer("1.5.0", "1.4.9"))
        assertTrue(isSemanticVersionNewer("1.4.0.1", "1.4.0"))

        assertFalse(isSemanticVersionNewer("1.4.0", "1.4.0"))
        assertFalse(isSemanticVersionNewer("1.3.9", "1.4.0"))
        assertFalse(isSemanticVersionNewer("1.0.0", "2.0.0"))
    }

    @Test
    fun testVersionCodeExtractionFromReleaseNotes() {
        fun extractVersionCode(releaseNotes: String): Int? {
            val cleanNotes = releaseNotes.replace("*", "").replace("`", "")
            val pattern = Pattern.compile("(?i)(?:version[_\\s-]?code|build[_\\s-]?number|vc)\\s*[:=]\\s*(\\d+)")
            val matcher = pattern.matcher(cleanNotes)
            return if (matcher.find()) matcher.group(1)?.toIntOrNull() else null
        }

        assertEquals(3, extractVersionCode("- **versionCode**: 3"))
        assertEquals(3, extractVersionCode("### Changelog\nversionCode: 3\n- Fixed crash"))
        assertEquals(15, extractVersionCode("Release notes:\nBuild_Number: 15\nNew features"))
        assertEquals(7, extractVersionCode("vc=7"))
        assertEquals(null, extractVersionCode("Regular release notes without version code"))
    }

    @Test
    fun testSha256ChecksumExtractionFromReleaseNotes() {
        fun extractSha256(releaseNotes: String): String? {
            val pattern = Pattern.compile("(?i)(?:sha-?256|checksum)[:=\\s]+([a-fA-F0-9]{64})")
            val matcher = pattern.matcher(releaseNotes)
            return if (matcher.find()) matcher.group(1)?.lowercase() else null
        }

        val testHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        val notes = "### GhostPocket Release\nSHA256: $testHash\nEnjoy!"
        assertEquals(testHash, extractSha256(notes))

        val notesWithHyphen = "Checksum: $testHash"
        assertEquals(testHash, extractSha256(notesWithHyphen))

        val notesWithoutHash = "Just some random notes"
        assertEquals(null, extractSha256(notesWithoutHash))
    }

    @Test
    fun testMandatoryUpdateDetection() {
        fun isMandatory(notes: String): Boolean {
            return notes.contains("[mandatory]", ignoreCase = true) ||
                   notes.contains("mandatory: true", ignoreCase = true) ||
                   notes.contains("mandatory:true", ignoreCase = true)
        }

        assertTrue(isMandatory("Critical security update! [mandatory]"))
        assertTrue(isMandatory("mandatory: true\nDatabase schema migration required"))
        assertFalse(isMandatory("Regular update with minor UI polish"))
    }

    @Test
    fun testSemanticVersioningIncrementValidation() {
        fun isValidNextVersion(current: String, target: String): Boolean {
            val semver = Regex("""^(\d+)\.(\d+)\.(\d+)$""")
            val cMatch = semver.matchEntire(current) ?: return false
            val tMatch = semver.matchEntire(target) ?: return false

            val (cMaj, cMin, cPat) = cMatch.destructured.let { Triple(it.component1().toInt(), it.component2().toInt(), it.component3().toInt()) }
            val (tMaj, tMin, tPat) = tMatch.destructured.let { Triple(it.component1().toInt(), it.component2().toInt(), it.component3().toInt()) }

            // Valid increments:
            // 1. Patch bump: same major & minor, patch = current + 1
            val isPatch = (tMaj == cMaj && tMin == cMin && tPat == cPat + 1)
            // 2. Minor bump: same major, minor = current + 1, patch reset to 0
            val isMinor = (tMaj == cMaj && tMin == cMin + 1 && tPat == 0)
            // 3. Major bump: major = current + 1, minor reset to 0, patch reset to 0
            val isMajor = (tMaj == cMaj + 1 && tMin == 0 && tPat == 0)

            return isPatch || isMinor || isMajor
        }

        // Current release is 2.0.0
        assertTrue("Patch bump 2.0.0 -> 2.0.1 must be valid", isValidNextVersion("2.0.0", "2.0.1"))
        assertTrue("Minor bump 2.0.0 -> 2.1.0 must be valid", isValidNextVersion("2.0.0", "2.1.0"))
        assertTrue("Major bump 2.0.0 -> 3.0.0 must be valid", isValidNextVersion("2.0.0", "3.0.0"))

        // Invalid progressions
        assertFalse("Downgrade 2.0.0 -> 1.9.0 is invalid", isValidNextVersion("2.0.0", "1.9.0"))
        assertFalse("Same version 2.0.0 -> 2.0.0 is invalid", isValidNextVersion("2.0.0", "2.0.0"))
        assertFalse("Minor bump without resetting patch 2.0.0 -> 2.1.1 is invalid", isValidNextVersion("2.0.0", "2.1.1"))
        assertFalse("Major bump without resetting minor/patch 2.0.0 -> 3.1.0 is invalid", isValidNextVersion("2.0.0", "3.1.0"))
        assertFalse("Non-semver strings are invalid", isValidNextVersion("2.0.0", "2.0.0-beta"))
    }
}
