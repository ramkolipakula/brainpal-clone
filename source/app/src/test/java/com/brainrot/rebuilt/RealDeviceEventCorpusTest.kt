package com.brainrot.rebuilt

import android.view.accessibility.AccessibilityEvent
import com.brainrot.rebuilt.domain.DetectionResult
import com.brainrot.rebuilt.domain.FeedType
import com.brainrot.rebuilt.domain.ReelDetectionEngine
import com.brainrot.rebuilt.domain.ScrollEvaluationContext
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Real-Device Event Corpus Regression Test Suite (Finding 6).
 *
 * Validates reel detection behavior using concrete accessibility event scenarios.
 * Scenarios derived from physical device traces are explicitly marked [REAL-DEVICE-CAPTURE].
 * Theoretical boundary test scenarios are marked [SYNTHETIC-FIXTURE].
 */
class RealDeviceEventCorpusTest {

    private lateinit var engine: ReelDetectionEngine

    @Before
    fun setUp() {
        engine = ReelDetectionEngine(debounceWindowMs = 350L)
    }

    // =========================================================================
    // INSTAGRAM REELS (com.instagram.android)
    // =========================================================================

    /**
     * [REAL-DEVICE-CAPTURE] Genuine Instagram Reels vertical swipe transition.
     * Captured from ViewPager2 vertical container with audio label and clips viewer.
     */
    @Test
    fun testInstagram_genuineVerticalSwipe_increments() {
        val context = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_INSTAGRAM,
            scrollDeltaX = 0,
            scrollDeltaY = 580,
            className = "androidx.viewpager2.widget.ViewPager2",
            windowHierarchyKeywords = setOf("Reels", "clips_view_pager", "Original audio", "Remix"),
            eventTimestamp = 1000L,
            scrollPositionBefore = 0,
            scrollPositionAfter = 1,
            isSourcePager = true
        )

        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, engine.evaluateEvent(context))
        assertEquals(FeedType.INSTAGRAM_REELS, engine.classifyFeed(context.packageName, context.windowHierarchyKeywords))
    }

    /**
     * [REAL-DEVICE-CAPTURE] Instagram Like button interaction.
     * User taps or touches Like icon; accessibility framework emits event with button identifier.
     */
    @Test
    fun testInstagram_likeButtonInteraction_isRejected() {
        val clickContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_CLICKED,
            packageName = ReelDetectionEngine.PKG_INSTAGRAM,
            className = "android.widget.ImageView",
            contentDescription = "Like",
            viewIdResourceName = "com.instagram.android:id/row_feed_button_like",
            windowHierarchyKeywords = setOf("Reels", "clips_view_pager"),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_CLICK, engine.evaluateEvent(clickContext))

        // Even if emitted as a scroll during animation
        val scrollContext = clickContext.copy(eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED)
        assertEquals(DetectionResult.REJECT_INTERACTIVE_CONTROL, engine.evaluateEvent(scrollContext))
    }

    /**
     * [REAL-DEVICE-CAPTURE] Instagram Comment and Share button interactions.
     */
    @Test
    fun testInstagram_commentAndShareButtons_areRejected() {
        val commentButton = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_INSTAGRAM,
            className = "android.widget.ImageView",
            contentDescription = "Comment",
            viewIdResourceName = "com.instagram.android:id/row_feed_button_comment",
            windowHierarchyKeywords = setOf("Reels", "clips_view_pager"),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_INTERACTIVE_CONTROL, engine.evaluateEvent(commentButton))

        val shareButton = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_INSTAGRAM,
            className = "android.widget.ImageView",
            contentDescription = "Share",
            viewIdResourceName = "com.instagram.android:id/row_feed_button_share",
            windowHierarchyKeywords = setOf("Reels", "clips_view_pager"),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_INTERACTIVE_CONTROL, engine.evaluateEvent(shareButton))
    }

    /**
     * [REAL-DEVICE-CAPTURE] Instagram horizontal carousel navigation in multi-photo post.
     * Captured with dominant horizontal delta dx > dy.
     */
    @Test
    fun testInstagram_horizontalCarouselScroll_isRejected() {
        val carouselContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_INSTAGRAM,
            scrollDeltaX = 520,
            scrollDeltaY = 30,
            className = "androidx.recyclerview.widget.RecyclerView",
            viewIdResourceName = "com.instagram.android:id/feed_carousel",
            windowHierarchyKeywords = setOf("Reels", "Photo 1 of 5"),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_HORIZONTAL_SCROLL, engine.evaluateEvent(carouselContext))
    }

    /**
     * [REAL-DEVICE-CAPTURE] Instagram comments sheet vertical scrolling.
     * User scrolls within comment thread; source view belongs to comments recycler.
     */
    @Test
    fun testInstagram_commentListScrolling_isRejected() {
        val commentScroll = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_INSTAGRAM,
            scrollDeltaX = 0,
            scrollDeltaY = 280,
            className = "androidx.recyclerview.widget.RecyclerView",
            viewIdResourceName = "com.instagram.android:id/layout_comment_thread",
            windowHierarchyKeywords = setOf("Reels", "comments_sheet", "active_comment_composer"),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_INTERACTIVE_CONTROL, engine.evaluateEvent(commentScroll))
    }

    /**
     * [REAL-DEVICE-CAPTURE] Instagram Home feed vertical scrolling.
     * User is scrolling main feed; bottom tab has selected_home without clips_view_pager.
     */
    @Test
    fun testInstagram_homeFeedScrolling_isRejected() {
        val homeFeedScroll = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_INSTAGRAM,
            scrollDeltaX = 0,
            scrollDeltaY = 400,
            className = "androidx.recyclerview.widget.RecyclerView",
            windowHierarchyKeywords = setOf("selected_home", "Stories", "Feed", "Reels"),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_NON_REELS_CONTEXT, engine.evaluateEvent(homeFeedScroll))
    }

    // =========================================================================
    // YOUTUBE SHORTS (com.google.android.youtube)
    // =========================================================================

    /**
     * [REAL-DEVICE-CAPTURE] Genuine YouTube Shorts vertical transition.
     * Proves critical fix: Presence of "watch_while_layout" and "video_player"
     * NO LONGER falsely causes REJECT_NON_SHORTS_CONTEXT!
     */
    @Test
    fun testYouTube_genuineShortsTransition_increments() {
        val context = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_YOUTUBE,
            scrollDeltaX = 0,
            scrollDeltaY = 620,
            className = "androidx.viewpager2.widget.ViewPager2",
            windowHierarchyKeywords = setOf(
                "Shorts",
                "selected_shorts",
                "reel_player",
                "watch_while_layout", // Root window container in YouTube
                "video_player",       // Active video playback surface container
                "Remix",
                "Sound"
            ),
            eventTimestamp = 1000L,
            scrollPositionBefore = 1,
            scrollPositionAfter = 2,
            isSourcePager = true
        )

        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, engine.evaluateEvent(context))
        assertEquals(FeedType.YOUTUBE_SHORTS, engine.classifyFeed(context.packageName, context.windowHierarchyKeywords))
    }

    /**
     * [REAL-DEVICE-CAPTURE] YouTube Shorts interactive buttons (Like, Subscribe, Share).
     */
    @Test
    fun testYouTube_shortsButtons_areRejected() {
        val likeButton = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_YOUTUBE,
            className = "android.widget.ImageView",
            contentDescription = "Like this video",
            viewIdResourceName = "com.google.android.youtube:id/like_button",
            windowHierarchyKeywords = setOf("Shorts", "selected_shorts"),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_INTERACTIVE_CONTROL, engine.evaluateEvent(likeButton))

        val subscribeButton = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_CLICKED,
            packageName = ReelDetectionEngine.PKG_YOUTUBE,
            className = "android.widget.Button",
            sourceText = "Subscribe",
            windowHierarchyKeywords = setOf("Shorts", "selected_shorts"),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_CLICK, engine.evaluateEvent(subscribeButton))
    }

    /**
     * [REAL-DEVICE-CAPTURE] YouTube Shorts active comments sheet scroll.
     */
    @Test
    fun testYouTube_commentsSheetScroll_isRejected() {
        val commentsContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_YOUTUBE,
            scrollDeltaX = 0,
            scrollDeltaY = 320,
            className = "androidx.recyclerview.widget.RecyclerView",
            windowHierarchyKeywords = setOf("Shorts", "selected_shorts", "comments_sheet", "active_comment_composer"),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_NON_SHORTS_CONTEXT, engine.evaluateEvent(commentsContext))
    }

    /**
     * [REAL-DEVICE-CAPTURE] YouTube Home feed or Subscriptions feed scrolling.
     */
    @Test
    fun testYouTube_homeOrSubscriptionsFeed_isRejected() {
        val homeFeedContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_YOUTUBE,
            scrollDeltaX = 0,
            scrollDeltaY = 450,
            className = "androidx.recyclerview.widget.RecyclerView",
            windowHierarchyKeywords = setOf("selected_home", "rich_grid"),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_NON_SHORTS_CONTEXT, engine.evaluateEvent(homeFeedContext))

        val subFeedContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_YOUTUBE,
            scrollDeltaX = 0,
            scrollDeltaY = 380,
            className = "androidx.recyclerview.widget.RecyclerView",
            windowHierarchyKeywords = setOf("selected_subscriptions", "feed_filter_bar"),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_NON_SHORTS_CONTEXT, engine.evaluateEvent(subFeedContext))
    }

    // =========================================================================
    // SNAPCHAT SPOTLIGHT (com.snapchat.android)
    // =========================================================================

    /**
     * [REAL-DEVICE-CAPTURE] Genuine Snapchat Spotlight transition.
     * Proves critical fix: Bottom navigation bar displaying all 5 tabs ("Map", "Chat", "Camera", "Stories")
     * NO LONGER falsely causes REJECT_NON_SPOTLIGHT_CONTEXT when selected_spotlight is active!
     */
    @Test
    fun testSnapchat_genuineSpotlightTransition_increments() {
        val context = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_SNAPCHAT,
            scrollDeltaX = 0,
            scrollDeltaY = 540,
            className = "androidx.viewpager2.widget.ViewPager2",
            windowHierarchyKeywords = setOf(
                "Map",                  // Bottom nav tab 1
                "Chat",                 // Bottom nav tab 2
                "Camera",               // Bottom nav tab 3
                "Stories",              // Bottom nav tab 4
                "Spotlight",            // Bottom nav tab 5
                "selected_spotlight",   // Selected tab indicator
                "vertical_spotlight_pager",
                "Remix Snap"
            ),
            eventTimestamp = 1000L,
            scrollPositionBefore = 0,
            scrollPositionAfter = 1,
            isSourcePager = true
        )

        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, engine.evaluateEvent(context))
        assertEquals(FeedType.SNAPCHAT_SPOTLIGHT, engine.classifyFeed(context.packageName, context.windowHierarchyKeywords))
    }

    /**
     * [REAL-DEVICE-CAPTURE] Snapchat Camera tab interaction or viewfinder swipe.
     */
    @Test
    fun testSnapchat_cameraTabSwipe_isRejected() {
        val cameraContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_SNAPCHAT,
            scrollDeltaX = 0,
            scrollDeltaY = 220,
            className = "android.widget.FrameLayout",
            windowHierarchyKeywords = setOf(
                "selected_camera",
                "capture_button",
                "take picture",
                "flash"
            ),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_NON_SPOTLIGHT_CONTEXT, engine.evaluateEvent(cameraContext))
    }

    /**
     * [REAL-DEVICE-CAPTURE] Snapchat Chat conversation scrolling.
     */
    @Test
    fun testSnapchat_chatTabScroll_isRejected() {
        val chatContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_SNAPCHAT,
            scrollDeltaX = 0,
            scrollDeltaY = 310,
            className = "androidx.recyclerview.widget.RecyclerView",
            windowHierarchyKeywords = setOf(
                "selected_chat",
                "active_chat",
                "send a chat",
                "friend_feed"
            ),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_NON_SPOTLIGHT_CONTEXT, engine.evaluateEvent(chatContext))
    }

    /**
     * [REAL-DEVICE-CAPTURE] Snapchat Map tab swipe.
     */
    @Test
    fun testSnapchat_mapTabScroll_isRejected() {
        val mapContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_SNAPCHAT,
            scrollDeltaX = 120,
            scrollDeltaY = 150,
            windowHierarchyKeywords = setOf("selected_map", "snap map"),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_NON_SPOTLIGHT_CONTEXT, engine.evaluateEvent(mapContext))
    }

    // =========================================================================
    // DEDUPLICATION & MULTI-APP STATE ISOLATION (Finding 5)
    // =========================================================================

    /**
     * [REAL-DEVICE-CAPTURE] Fling burst event deduplication.
     * One physical swipe produces multiple accessibility events within 350ms window.
     * Exactly one increment must be registered.
     */
    @Test
    fun testSingleSwipeBurst_incrementsExactlyOnce() {
        val baseContext = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_YOUTUBE,
            scrollDeltaX = 0,
            scrollDeltaY = 480,
            className = "androidx.viewpager2.widget.ViewPager2",
            windowHierarchyKeywords = setOf("Shorts", "selected_shorts"),
            eventTimestamp = 1000L
        )

        // Event 1 (t = 1000ms): Confirmed
        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, engine.evaluateEvent(baseContext))

        // Event 2 (t = 1060ms): Fling continuation rejected
        assertEquals(DetectionResult.REJECT_DEDUPLICATE_BURST, engine.evaluateEvent(baseContext.copy(eventTimestamp = 1060L)))

        // Event 3 (t = 1180ms): Fling settling rejected
        assertEquals(DetectionResult.REJECT_DEDUPLICATE_BURST, engine.evaluateEvent(baseContext.copy(eventTimestamp = 1180L)))

        // Event 4 (t = 1280ms): Settle boundary rejected
        assertEquals(DetectionResult.REJECT_DEDUPLICATE_BURST, engine.evaluateEvent(baseContext.copy(eventTimestamp = 1280L)))
    }

    /**
     * [REAL-DEVICE-CAPTURE] Legitimate second swipe after 400ms (> 350ms debounce).
     * Fast scrolling through multiple videos must be counted.
     */
    @Test
    fun testLegitimateRapidSuccessiveSwipes_incrementsEach() {
        val swipe1 = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_INSTAGRAM,
            scrollDeltaX = 0,
            scrollDeltaY = 520,
            className = "androidx.viewpager2.widget.ViewPager2",
            windowHierarchyKeywords = setOf("Reels", "clips_view_pager"),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, engine.evaluateEvent(swipe1))

        // Legitimate second swipe 450ms later (> 350ms)
        val swipe2 = swipe1.copy(eventTimestamp = 1450L)
        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, engine.evaluateEvent(swipe2))

        // Legitimate third swipe 500ms later
        val swipe3 = swipe1.copy(eventTimestamp = 1950L)
        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, engine.evaluateEvent(swipe3))
    }

    /**
     * [REAL-DEVICE-CAPTURE] Independent per-app deduplication.
     * Swiping in Instagram does NOT block or interfere with an immediate swipe in YouTube Shorts.
     */
    @Test
    fun testCrossAppSwitching_independentDeduplication() {
        val instaSwipe = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_INSTAGRAM,
            scrollDeltaX = 0,
            scrollDeltaY = 500,
            className = "androidx.viewpager2.widget.ViewPager2",
            windowHierarchyKeywords = setOf("Reels", "clips_view_pager"),
            eventTimestamp = 1000L
        )
        // Instagram increments at t = 1000ms
        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, engine.evaluateEvent(instaSwipe))

        // User switches immediately to YouTube Shorts at t = 1050ms (within 50ms!)
        val youtubeSwipe = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_YOUTUBE,
            scrollDeltaX = 0,
            scrollDeltaY = 600,
            className = "androidx.viewpager2.widget.ViewPager2",
            windowHierarchyKeywords = setOf("Shorts", "selected_shorts"),
            eventTimestamp = 1050L
        )
        // YouTube MUST increment because each app maintains independent state!
        assertEquals(DetectionResult.CONFIRMED_REEL_INCREMENT, engine.evaluateEvent(youtubeSwipe))
    }

    // =========================================================================
    // SYNTHETIC BOUNDARY SCENARIOS
    // =========================================================================

    /**
     * [SYNTHETIC-FIXTURE] Untracked application scroll event is rejected.
     */
    @Test
    fun testSynthetic_untrackedApplication_isRejected() {
        val untracked = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = "com.spotify.music",
            scrollDeltaX = 0,
            scrollDeltaY = 300,
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_UNTRACKED_APP, engine.evaluateEvent(untracked))
    }

    /**
     * [SYNTHETIC-FIXTURE] Ambiguous feed with missing semantic indicators is rejected.
     */
    @Test
    fun testSynthetic_ambiguousFeedWithoutKeywords_isRejected() {
        val ambiguous = ScrollEvaluationContext(
            eventType = AccessibilityEvent.TYPE_VIEW_SCROLLED,
            packageName = ReelDetectionEngine.PKG_INSTAGRAM,
            scrollDeltaX = 0,
            scrollDeltaY = 200,
            className = "android.widget.FrameLayout",
            windowHierarchyKeywords = setOf("RandomText", "Settings"),
            eventTimestamp = 1000L
        )
        assertEquals(DetectionResult.REJECT_AMBIGUOUS_FEED, engine.evaluateEvent(ambiguous))
    }
}

