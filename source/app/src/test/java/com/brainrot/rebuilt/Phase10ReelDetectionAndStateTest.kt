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
        engine = ReelDetectionEngine(debounceWindowMs = 800L)
    }

    // 1. YouTube Shorts confirmed vertical navigation increments once
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

        val result = engine.evaluateEvent(context)
        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, result)
    }

    // 2. Multiple events belonging to one swipe increment only once
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
        val res1 = engine.evaluateEvent(baseContext)
        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, res1)

        // Rapid follow-up events from the same swipe (e.g. at +80ms, +150ms, +250ms) -> DEDUPLICATED
        val res2 = engine.evaluateEvent(baseContext.copy(eventTimestamp = 1080L))
        assertEquals(DetectionResult.REJECT_DEDUPLICATE_BURST, res2)

        val res3 = engine.evaluateEvent(baseContext.copy(eventTimestamp = 1150L))
        assertEquals(DetectionResult.REJECT_DEDUPLICATE_BURST, res3)

        val res4 = engine.evaluateEvent(baseContext.copy(eventTimestamp = 1250L))
        assertEquals(DetectionResult.REJECT_DEDUPLICATE_BURST, res4)
    }

    // 3. A confirmed horizontal Instagram scroll does not increment
    @Test
    fun testConfirmedHorizontalInstagramScroll_doesNotIncrement() {
        // Swiping a carousel post in Instagram feed horizontally
        val context = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_INSTAGRAM,
            scrollDeltaX = 500, // Significant horizontal delta
            scrollDeltaY = 40,
            className = "androidx.recyclerview.widget.RecyclerView",
            windowHierarchyKeywords = setOf("reels"),
            eventTimestamp = 1000L
        )

        val result = engine.evaluateEvent(context)
        assertEquals(DetectionResult.REJECT_HORIZONTAL_SCROLL, result)
    }

    // 4. An Instagram click does not increment
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

    // 5. A scroll on the Instagram home feed does not increment
    @Test
    fun testInstagramHomeFeedScroll_doesNotIncrement() {
        val homeContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_INSTAGRAM,
            scrollDeltaX = 0,
            scrollDeltaY = 300,
            className = "android.widget.ListView",
            // Neutral feed keywords without Reels indicator
            windowHierarchyKeywords = setOf("Stories", "MainFeed", "ProfileGrid"),
            eventTimestamp = 1000L
        )

        val result = engine.evaluateEvent(homeContext)
        assertEquals(DetectionResult.REJECT_AMBIGUOUS_FEED, result)

        // Scrolling comments or DMs on Instagram
        val commentContext = homeContext.copy(
            windowHierarchyKeywords = setOf("Reels", "active_comment_composer")
        )
        assertEquals(DetectionResult.REJECT_NON_REELS_CONTEXT, engine.evaluateEvent(commentContext))

        val dmContext = homeContext.copy(
            windowHierarchyKeywords = setOf("active_message_composer", "direct_thread")
        )
        assertEquals(DetectionResult.REJECT_NON_REELS_CONTEXT, engine.evaluateEvent(dmContext))
    }

    // 6. A scroll on a non-Shorts YouTube screen does not increment
    @Test
    fun testYouTubeRegularFeedScroll_doesNotIncrement() {
        val watchContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_YOUTUBE,
            scrollDeltaX = 0,
            scrollDeltaY = 400,
            windowHierarchyKeywords = setOf("Watch_panel", "Subscriptions", "Comments"),
            eventTimestamp = 1000L
        )

        val result = engine.evaluateEvent(watchContext)
        assertEquals(DetectionResult.REJECT_NON_SHORTS_CONTEXT, result)
    }

    // 7. An ambiguous event does not increment
    @Test
    fun testAmbiguousOrZeroDeltaEvent_doesNotIncrement() {
        val zeroDeltaContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_INSTAGRAM,
            scrollDeltaX = 0,
            scrollDeltaY = 0,
            className = "android.widget.FrameLayout",
            windowHierarchyKeywords = setOf("Stories", "MainFeed"),
            eventTimestamp = 1000L
        )

        val result = engine.evaluateEvent(zeroDeltaContext)
        assertEquals(DetectionResult.REJECT_AMBIGUOUS_FEED, result)
    }

    // 8. A legitimate second swipe increments again after deduplication allows it
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

        // First swipe
        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, engine.evaluateEvent(swipe1))

        // Legitimate second swipe 900ms later (> 800ms debounce)
        val swipe2 = swipe1.copy(eventTimestamp = 1900L)
        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, engine.evaluateEvent(swipe2))
    }

    // 9. Changing the target immediately recalculates the mascot stage
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

    // 10. Deterministic mascot aging boundaries and safety checks
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

        // Safe handling for 0 or negative targets (requirement 6: handle target values of zero or invalid safely)
        val stageZeroTarget = MascotStageCalculator.calculateStage(10, 0)
        assertNotNull(stageZeroTarget)
        assertEquals(MascotStage.ROASTED, stageZeroTarget)

        val stageNegativeTarget = MascotStageCalculator.calculateStage(5, -20)
        assertNotNull(stageNegativeTarget)
        assertEquals(MascotStage.ROASTED, stageNegativeTarget)

        // Safe handling for negative count
        val stageNegativeCount = MascotStageCalculator.calculateStage(-5, 50)
        assertEquals(MascotStage.FRESH, stageNegativeCount)
    }

    // 11. Instagram Reels vertical swipe confirmed
    @Test
    fun testInstagramReelsVerticalSwipe_incrementsOnce() {
        val context = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_INSTAGRAM,
            scrollDeltaX = 0,
            scrollDeltaY = 550,
            className = "androidx.viewpager2.widget.ViewPager2",
            windowHierarchyKeywords = setOf("Reels", "Original audio", "Remix"),
            eventTimestamp = 2000L
        )

        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, engine.evaluateEvent(context))
    }

    // 12. Untracked application scroll does not increment
    @Test
    fun testUntrackedAppScroll_doesNotIncrement() {
        val context = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = "com.whatsapp",
            scrollDeltaX = 0,
            scrollDeltaY = 300,
            eventTimestamp = 1000L
        )

        assertEquals(DetectionResult.REJECT_UNTRACKED_APP, engine.evaluateEvent(context))
    }

    // 13. Real-world Instagram Reel with Comment button and Direct Share button increments successfully
    @Test
    fun testRealWorldInstagramReelWithCommentAndShareButtons_increments() {
        val reelContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_INSTAGRAM,
            scrollDeltaX = 0,
            scrollDeltaY = 0, // FrameLayout on real phone reports 0
            className = "android.widget.FrameLayout",
            windowHierarchyKeywords = setOf("Reels", "Friends", "1,540 comments", "Comment", "Share", "Direct", "Remix", "Original audio"),
            eventTimestamp = 1000L
        )

        val result = engine.evaluateEvent(reelContext)
        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, result)
    }

    // 14. Real-world YouTube Shorts with Subscriptions chip and Comments button increments successfully
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

        val result = engine.evaluateEvent(shortsContext)
        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, result)
    }

    // 15. Instagram Reel with open comment drawer does not increment
    @Test
    fun testInstagramReelWithActiveCommentDrawer_doesNotIncrement() {
        val openCommentContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_INSTAGRAM,
            scrollDeltaX = 0,
            scrollDeltaY = 200,
            className = "androidx.recyclerview.widget.RecyclerView",
            windowHierarchyKeywords = setOf("Reels", "active_comment_composer", "1,540 comments"),
            eventTimestamp = 1000L
        )

        val result = engine.evaluateEvent(openCommentContext)
        assertEquals(DetectionResult.REJECT_NON_REELS_CONTEXT, result)
    }
}
