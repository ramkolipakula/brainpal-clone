package com.brainrot.rebuilt

import android.view.accessibility.AccessibilityEvent
import com.brainrot.rebuilt.domain.DetectionResult
import com.brainrot.rebuilt.domain.MascotStageCalculator
import com.brainrot.rebuilt.domain.ReelDetectionEngine
import com.brainrot.rebuilt.domain.ScrollEvaluationContext
import com.brainrot.rebuilt.domain.model.MascotStage
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class Phase10ReelDetectionAndStateTest {

    private lateinit var engine: ReelDetectionEngine

    @Before
    fun setUp() {
        engine = ReelDetectionEngine(debounceWindowMs = 600L)
    }

    // 1. One confirmed Instagram Reel navigation increments exactly once
    @Test
    fun testInstagramReelsVerticalSwipe_incrementsOnce() {
        val context = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_INSTAGRAM,
            scrollDeltaX = 0,
            scrollDeltaY = 550,
            className = "androidx.viewpager2.widget.ViewPager2",
            windowHierarchyKeywords = setOf("Reels", "Original audio", "Remix"),
            eventTimestamp = 1000L
        )

        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, engine.evaluateEvent(context))
    }

    // 2. An Instagram button click does not increment
    @Test
    fun testInstagramClickEvent_doesNotIncrement() {
        val clickContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_CLICKED,
            packageName = ReelDetectionEngine.PKG_INSTAGRAM,
            contentDescription = "Like",
            windowHierarchyKeywords = setOf("Reels"),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_CLICK, engine.evaluateEvent(clickContext))

        val longClickContext = clickContext.copy(eventType = AccessibilityEvent.TYPE_VIEW_LONG_CLICKED)
        assertEquals(DetectionResult.REJECT_CLICK, engine.evaluateEvent(longClickContext))
    }

    // 2b. An Instagram interactive control / button generating a scroll event is rejected
    @Test
    fun testInstagramButtonScrolledEvent_doesNotIncrement() {
        val buttonScrolled = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_INSTAGRAM,
            className = "android.widget.ImageButton",
            contentDescription = "Like",
            viewIdResourceName = "com.instagram.android:id/like_button",
            windowHierarchyKeywords = setOf("Reels", "Original audio"),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_INTERACTIVE_CONTROL, engine.evaluateEvent(buttonScrolled))

        val shareButtonScrolled = buttonScrolled.copy(
            contentDescription = "Share",
            viewIdResourceName = "com.instagram.android:id/direct_share_button"
        )
        assertEquals(DetectionResult.REJECT_INTERACTIVE_CONTROL, engine.evaluateEvent(shareButtonScrolled))
    }

    // 3. Instagram horizontal scrolling does not increment
    @Test
    fun testConfirmedHorizontalInstagramScroll_doesNotIncrement() {
        // Swiping a carousel post in Instagram feed horizontally with delta
        val context = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_INSTAGRAM,
            scrollDeltaX = 500,
            scrollDeltaY = 40,
            className = "androidx.recyclerview.widget.RecyclerView",
            windowHierarchyKeywords = setOf("reels"),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_HORIZONTAL_SCROLL, engine.evaluateEvent(context))

        // Swiping horizontal scroll view or stories carousel
        val carouselContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_INSTAGRAM,
            scrollDeltaX = 0,
            scrollDeltaY = 0,
            className = "android.widget.HorizontalScrollView",
            viewIdResourceName = "com.instagram.android:id/feed_carousel",
            windowHierarchyKeywords = setOf("reels"),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_HORIZONTAL_SCROLL, engine.evaluateEvent(carouselContext))
    }

    // 4. Scrolling Instagram comments or the home feed does not increment
    @Test
    fun testInstagramHomeFeedScroll_doesNotIncrement() {
        val homeContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_INSTAGRAM,
            scrollDeltaX = 0,
            scrollDeltaY = 300,
            className = "android.widget.ListView",
            windowHierarchyKeywords = setOf("Stories", "MainFeed", "ProfileGrid"),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_AMBIGUOUS_FEED, engine.evaluateEvent(homeContext))

        // Scrolling comments on Instagram
        val commentContext = homeContext.copy(
            windowHierarchyKeywords = setOf("Reels", "active_comment_composer")
        )
        assertEquals(DetectionResult.REJECT_NON_REELS_CONTEXT, engine.evaluateEvent(commentContext))

        val commentSheetContext = homeContext.copy(
            windowHierarchyKeywords = setOf("Reels", "comments_sheet")
        )
        assertEquals(DetectionResult.REJECT_NON_REELS_CONTEXT, engine.evaluateEvent(commentSheetContext))

        // Direct messages on Instagram
        val dmContext = homeContext.copy(
            windowHierarchyKeywords = setOf("active_message_composer", "direct_thread")
        )
        assertEquals(DetectionResult.REJECT_NON_REELS_CONTEXT, engine.evaluateEvent(dmContext))
    }

    // 5. One confirmed YouTube Shorts navigation increments exactly once
    @Test
    fun testYouTubeShortsVerticalSwipe_incrementsOnce() {
        val context = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_YOUTUBE,
            scrollDeltaX = 0,
            scrollDeltaY = 600,
            className = "androidx.viewpager2.widget.ViewPager2",
            windowHierarchyKeywords = setOf("Shorts", "Remix", "Sound"),
            eventTimestamp = 1000L
        )

        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, engine.evaluateEvent(context))
    }

    // 6. Ordinary YouTube video-page scrolling does not increment
    @Test
    fun testYouTubeRegularFeedScroll_doesNotIncrement() {
        val watchContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_YOUTUBE,
            scrollDeltaX = 0,
            scrollDeltaY = 400,
            windowHierarchyKeywords = setOf("watch_panel", "Subscriptions", "Comments"),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_NON_SHORTS_CONTEXT, engine.evaluateEvent(watchContext))

        val homeFeedContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_YOUTUBE,
            scrollDeltaX = 0,
            scrollDeltaY = 300,
            windowHierarchyKeywords = setOf("selected_home", "rich_grid"),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_NON_SHORTS_CONTEXT, engine.evaluateEvent(homeFeedContext))
    }

    // 7. One confirmed Snapchat Spotlight navigation increments exactly once
    @Test
    fun testSnapchatSpotlightVerticalSwipe_incrementsOnce() {
        val context = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_SNAPCHAT,
            scrollDeltaX = 0,
            scrollDeltaY = 500,
            className = "androidx.viewpager2.widget.ViewPager2",
            windowHierarchyKeywords = setOf("Spotlight", "Remix Snap", "Sound"),
            eventTimestamp = 1000L
        )

        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, engine.evaluateEvent(context))
    }

    // 8. Snapchat navigation outside Spotlight does not increment
    @Test
    fun testSnapchatCameraOrChatScroll_doesNotIncrement() {
        // Camera capture screen
        val cameraContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_SNAPCHAT,
            scrollDeltaX = 0,
            scrollDeltaY = 300,
            windowHierarchyKeywords = setOf("selected_camera", "take picture", "lenses"),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_NON_SPOTLIGHT_CONTEXT, engine.evaluateEvent(cameraContext))

        // Chat & messaging screen
        val chatContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_SNAPCHAT,
            scrollDeltaX = 0,
            scrollDeltaY = 250,
            windowHierarchyKeywords = setOf("selected_chat", "messages", "friend_feed"),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_NON_SPOTLIGHT_CONTEXT, engine.evaluateEvent(chatContext))

        // Snap Map
        val mapContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_SNAPCHAT,
            scrollDeltaX = 0,
            scrollDeltaY = 200,
            windowHierarchyKeywords = setOf("selected_map", "snap map"),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_NON_SPOTLIGHT_CONTEXT, engine.evaluateEvent(mapContext))
    }

    // 9. Multiple accessibility events from one navigation do not double-count
    @Test
    fun testSingleSwipeMultipleEventsBurst_incrementsOnlyOnce() {
        val baseContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_YOUTUBE,
            scrollDeltaX = 0,
            scrollDeltaY = 450,
            className = "androidx.viewpager2.widget.ViewPager2",
            windowHierarchyKeywords = setOf("Shorts"),
            eventTimestamp = 1000L
        )

        // First event in burst -> CONFIRMED
        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, engine.evaluateEvent(baseContext))

        // Follow-up events within 600ms debounce window -> DEDUPLICATED
        assertEquals(DetectionResult.REJECT_DEDUPLICATE_BURST, engine.evaluateEvent(baseContext.copy(eventTimestamp = 1080L)))
        assertEquals(DetectionResult.REJECT_DEDUPLICATE_BURST, engine.evaluateEvent(baseContext.copy(eventTimestamp = 1250L)))
        assertEquals(DetectionResult.REJECT_DEDUPLICATE_BURST, engine.evaluateEvent(baseContext.copy(eventTimestamp = 1450L)))
    }

    // 10. Two separate rapid navigations can increment separately
    @Test
    fun testLegitimateSecondSwipeAfterDebounce_incrementsAgain() {
        val swipe1 = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_INSTAGRAM,
            scrollDeltaX = 0,
            scrollDeltaY = 500,
            className = "androidx.viewpager2.widget.ViewPager2",
            windowHierarchyKeywords = setOf("Reels", "Clips"),
            eventTimestamp = 1000L
        )

        // First swipe at 1000ms
        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, engine.evaluateEvent(swipe1))

        // Second genuine swipe 700ms later (> 600ms debounce)
        val swipe2 = swipe1.copy(eventTimestamp = 1700L)
        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, engine.evaluateEvent(swipe2))

        // Third genuine swipe 750ms later
        val swipe3 = swipe1.copy(eventTimestamp = 2450L)
        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, engine.evaluateEvent(swipe3))
    }

    // 11. Ambiguous or unsupported events do not increment
    @Test
    fun testAmbiguousOrZeroDeltaEvent_doesNotIncrement() {
        val ambiguousContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_INSTAGRAM,
            scrollDeltaX = 0,
            scrollDeltaY = 0,
            className = "android.widget.FrameLayout",
            windowHierarchyKeywords = setOf("Stories", "MainFeed"),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_AMBIGUOUS_FEED, engine.evaluateEvent(ambiguousContext))

        val untrackedContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = "com.whatsapp",
            scrollDeltaX = 0,
            scrollDeltaY = 300,
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_UNTRACKED_APP, engine.evaluateEvent(untrackedContext))
    }

    // 12. Real-world Instagram Reel with Comment button and Direct Share button increments successfully
    @Test
    fun testRealWorldInstagramReelWithCommentAndShareButtons_increments() {
        val reelContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_INSTAGRAM,
            scrollDeltaX = 0,
            scrollDeltaY = 0,
            className = "android.widget.FrameLayout",
            windowHierarchyKeywords = setOf("Reels", "Friends", "1,540 comments", "Comment", "Share", "Direct", "Remix", "Original audio"),
            eventTimestamp = 1000L
        )

        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, engine.evaluateEvent(reelContext))
    }

    // 13. Real-world YouTube Shorts with Subscriptions chip and Comments button increments successfully
    @Test
    fun testRealWorldYouTubeShortsWithSubscriptionsAndComments_increments() {
        val shortsContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_YOUTUBE,
            scrollDeltaX = 0,
            scrollDeltaY = 350,
            className = "android.view.ViewGroup",
            windowHierarchyKeywords = setOf("Shorts", "Subscriptions", "Live", "Comments", "Remix"),
            eventTimestamp = 1000L
        )

        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, engine.evaluateEvent(shortsContext))
    }

    // 14. Target change immediately recalculates mascot stage
    @Test
    fun testTargetChange_immediatelyRecalculatesMascotStage() {
        val count = 40
        var target = 100

        // 40 / 100 = 40% -> HAPPY (25% - 50%)
        assertEquals(MascotStage.HAPPY, MascotStageCalculator.calculateStage(count, target))

        // User reduces target from 100 to 50
        target = 50
        // 40 / 50 = 80% -> EXHAUSTED (75% - 100%)
        assertEquals(MascotStage.EXHAUSTED, MascotStageCalculator.calculateStage(count, target))

        // User reduces target to 40
        target = 40
        // 40 / 40 = 100% -> ROASTED (>= 100%)
        assertEquals(MascotStage.ROASTED, MascotStageCalculator.calculateStage(count, target))
    }

    // 15. Mascot stage boundaries and safety checks
    @Test
    fun testMascotStageCalculation_boundariesAndSafeHandling() {
        val target = 100

        assertEquals(MascotStage.FRESH, MascotStageCalculator.calculateStage(0, target))
        assertEquals(MascotStage.FRESH, MascotStageCalculator.calculateStage(24, target))
        assertEquals(MascotStage.HAPPY, MascotStageCalculator.calculateStage(25, target))
        assertEquals(MascotStage.HAPPY, MascotStageCalculator.calculateStage(49, target))
        assertEquals(MascotStage.TIRED, MascotStageCalculator.calculateStage(50, target))
        assertEquals(MascotStage.TIRED, MascotStageCalculator.calculateStage(74, target))
        assertEquals(MascotStage.EXHAUSTED, MascotStageCalculator.calculateStage(75, target))
        assertEquals(MascotStage.EXHAUSTED, MascotStageCalculator.calculateStage(99, target))
        assertEquals(MascotStage.ROASTED, MascotStageCalculator.calculateStage(100, target))
        assertEquals(MascotStage.ROASTED, MascotStageCalculator.calculateStage(140, target))

        // Safe handling for 0 or negative targets
        assertEquals(MascotStage.ROASTED, MascotStageCalculator.calculateStage(10, 0))
        assertEquals(MascotStage.ROASTED, MascotStageCalculator.calculateStage(5, -20))

        // Safe handling for negative count
        assertEquals(MascotStage.FRESH, MascotStageCalculator.calculateStage(-5, 50))
    }

    // 16. Grammatical streak formatting test (1 Day vs X Days)
    @Test
    fun testGrammaticalStreakFormatting() {
        fun formatStreak(days: Int): String = if (days == 1) "1 Day" else "$days Days"

        assertEquals("1 Day", formatStreak(1))
        assertEquals("2 Days", formatStreak(2))
        assertEquals("14 Days", formatStreak(14))
        assertEquals("100 Days", formatStreak(100))
    }

    // 17. Per-app override precedence vs global default
    @Test
    fun testPerAppOverridePrecedence() {
        val globalLimit = 25
        val appOverrides = mutableMapOf<String, Int>()

        fun resolveLimit(pkg: String): Int = appOverrides[pkg] ?: globalLimit

        // Initially no override -> uses global default
        assertEquals(25, resolveLimit("com.instagram.android"))
        assertEquals(25, resolveLimit("com.google.android.youtube"))

        // Set custom override for Instagram only
        appOverrides["com.instagram.android"] = 50
        assertEquals(50, resolveLimit("com.instagram.android"))
        assertEquals(25, resolveLimit("com.google.android.youtube")) // YouTube untouched

        // Remove override for Instagram
        appOverrides.remove("com.instagram.android")
        assertEquals(25, resolveLimit("com.instagram.android")) // back to global
    }

    // 18. Daily rollover reset logic
    @Test
    fun testDailyRolloverResetLogic() {
        var todayKey = "2026-11-03"
        var lastActiveDate = "2026-11-02"
        var todayCount = 45
        var streak = 5
        var passes = 1

        fun performMidnightCheck(currentDate: String) {
            if (lastActiveDate != currentDate) {
                // New day detected
                todayCount = 0
                passes = 3
                streak++
                lastActiveDate = currentDate
            }
        }

        performMidnightCheck(todayKey)
        assertEquals(0, todayCount)
        assertEquals(3, passes)
        assertEquals(6, streak)
        assertEquals("2026-11-03", lastActiveDate)

        // Same day check does not re-reset
        todayCount = 10
        performMidnightCheck(todayKey)
        assertEquals(10, todayCount)
        assertEquals(6, streak)
    }
}
