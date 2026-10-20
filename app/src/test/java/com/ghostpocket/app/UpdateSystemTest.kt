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
}
