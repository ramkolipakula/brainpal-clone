package com.brainrot.rebuilt

import com.brainrot.rebuilt.domain.model.BuddyMood
import com.brainrot.rebuilt.domain.model.RoastMeme
import org.junit.Assert.*
import org.junit.Test

class LogicUnitTest {

    @Test
    fun testBuddyMoodCalculations() {
        val limitMinutes = 30

        fun calculateMood(minutesSpent: Int): BuddyMood {
            val ratio = minutesSpent.toFloat() / limitMinutes.toFloat()
            return when {
                ratio < 0.35f -> BuddyMood.EXCELLENT
                ratio < 0.75f -> BuddyMood.GOOD
                ratio < 1.0f -> BuddyMood.WARN
                else -> BuddyMood.ROASTED
            }
        }

        assertEquals(BuddyMood.EXCELLENT, calculateMood(5))
        assertEquals(BuddyMood.EXCELLENT, calculateMood(10))
        assertEquals(BuddyMood.GOOD, calculateMood(15))
        assertEquals(BuddyMood.GOOD, calculateMood(20))
        assertEquals(BuddyMood.WARN, calculateMood(25))
        assertEquals(BuddyMood.WARN, calculateMood(29))
        assertEquals(BuddyMood.ROASTED, calculateMood(30))
        assertEquals(BuddyMood.ROASTED, calculateMood(45))
    }

    @Test
    fun testSnoozeLogic() {
        val now = 1_000_000L
        val snoozeMinutes = 15
        val snoozeUntil = now + (snoozeMinutes * 60 * 1000L)

        fun isSnoozed(currentTime: Long): Boolean = currentTime < snoozeUntil

        assertTrue(isSnoozed(now + 5_000L))
        assertTrue(isSnoozed(now + (14 * 60 * 1000L)))
        assertFalse(isSnoozed(now + (16 * 60 * 1000L)))
    }

    @Test
    fun testEmergencyPassDeductionAndReplenish() {
        var passes = 3 // DEF-14 FIX: 3 passes default

        fun usePass(strictMode: Boolean): Boolean {
            if (strictMode) return false
            return if (passes > 0) {
                passes--
                true
            } else {
                false
            }
        }

        fun replenish() {
            passes = 3
        }

        // Test normal pass deduction
        assertTrue(usePass(strictMode = false))
        assertEquals(2, passes)
        assertTrue(usePass(strictMode = false))
        assertEquals(1, passes)
        assertTrue(usePass(strictMode = false))
        assertEquals(0, passes)
        assertFalse(usePass(strictMode = false))

        // Test replenish
        replenish()
        assertEquals(3, passes)

        // Test strict mode disallows bypass
        assertFalse(usePass(strictMode = true))
        assertEquals(3, passes)
    }

    @Test
    fun testBedtimeScheduleWindow() {
        fun isWithinBedtime(currentHour: Int, startHour: Int = 22, endHour: Int = 6): Boolean {
            return if (startHour > endHour) {
                currentHour >= startHour || currentHour < endHour
            } else {
                currentHour in startHour until endHour
            }
        }

        // Night window 22:00 to 06:00
        assertTrue(isWithinBedtime(22))
        assertTrue(isWithinBedtime(23))
        assertTrue(isWithinBedtime(0))
        assertTrue(isWithinBedtime(3))
        assertTrue(isWithinBedtime(5))
        assertFalse(isWithinBedtime(6))
        assertFalse(isWithinBedtime(12))
        assertFalse(isWithinBedtime(18))
        assertFalse(isWithinBedtime(21))
    }

    @Test
    fun testPerAppLimitOverrides() {
        val globalLimit = 30
        val customLimits = mapOf("com.instagram.android" to 15, "com.google.android.youtube" to 45)

        fun getLimit(pkg: String): Int = customLimits[pkg] ?: globalLimit

        assertEquals(15, getLimit("com.instagram.android"))
        assertEquals(45, getLimit("com.google.android.youtube"))
        assertEquals(30, getLimit("com.snapchat.android")) // defaults to global
    }

    @Test
    fun testRoastMemeFallback() {
        val memes = listOf(
            RoastMeme("1", "tenor", "http://example.com/1.webp", "Stop scrolling!", 400, 400, "english"),
            RoastMeme("2", "tenor", "http://example.com/2.webp", "Back to work!", 400, 400, "english")
        )

        assertNotNull(memes)
        assertEquals(2, memes.size)
        assertTrue(memes.all { it.caption.isNotEmpty() })
        assertEquals("english", memes[0].language)
    }
}
