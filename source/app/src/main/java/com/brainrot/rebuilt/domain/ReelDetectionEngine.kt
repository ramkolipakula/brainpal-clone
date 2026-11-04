package com.brainrot.rebuilt.domain

import android.view.accessibility.AccessibilityEvent
import kotlin.math.abs

enum class FeedType {
    INSTAGRAM_REELS,
    YOUTUBE_SHORTS,
    SNAPCHAT_SPOTLIGHT,
    NON_FEED_OR_UNTRACKED
}

enum class DetectionResult {
    CONFIRMED_REEL_INCREMENT,
    REJECT_CLICK,
    REJECT_NON_SCROLL,
    REJECT_UNTRACKED_APP,
    REJECT_HORIZONTAL_SCROLL,
    REJECT_ZERO_DELTA,
    REJECT_NON_REELS_CONTEXT,
    REJECT_NON_SHORTS_CONTEXT,
    REJECT_NON_SPOTLIGHT_CONTEXT,
    REJECT_INTERACTIVE_CONTROL,
    REJECT_AMBIGUOUS_FEED,
    REJECT_DEDUPLICATE_BURST
}

data class ScrollEvaluationContext(
    val eventType: Int,
    val packageName: String,
    val scrollDeltaX: Int = 0,
    val scrollDeltaY: Int = 0,
    val className: CharSequence? = null,
    val sourceText: CharSequence? = null,
    val contentDescription: CharSequence? = null,
    val viewIdResourceName: String? = null,
    val windowHierarchyKeywords: Set<String> = emptySet(),
    val eventTimestamp: Long = System.currentTimeMillis()
)

class ReelDetectionEngine(
    private val debounceWindowMs: Long = 600L
) {
    private var lastConfirmedSwipeTimestamp: Long = 0L
    private var lastActivePackage: String? = null

    companion object {
        const val PKG_INSTAGRAM = "com.instagram.android"
        const val PKG_YOUTUBE = "com.google.android.youtube"
        const val PKG_SNAPCHAT = "com.snapchat.android"

        // Instagram Reels positive feed semantic indicators
        private val INSTA_REELS_POSITIVE = setOf(
            "reels", "reel", "clips", "remix", "audio", "original audio",
            "camera, double tap to record", "clips_view_pager", "clips_video_container",
            "reel_viewer", "selected_reels", "selected_clips", "reboundviewpager",
            "clips_swipe_refresh_container"
        )

        // Instagram negative context indicators (Comments modal, DM thread, Profile edit, Search, Home feed)
        private val INSTA_NEGATIVE = setOf(
            "active_comment_composer", "comment_composer", "layout_comment_thread",
            "comments_sheet", "comments_list", "comment_row", "comment_thread",
            "active_message_composer", "direct_thread", "direct_composer", "inbox",
            "active_search_input", "search_edit_text",
            "edit profile", "edit_profile", "change profile photo", "self_profile",
            "selected_home", "row_feed_carousel"
        )

        // YouTube Shorts positive feed semantic indicators
        private val YT_SHORTS_POSITIVE = setOf(
            "shorts", "short", "reel_player", "shorts_container", "remix", "sound",
            "selected_shorts", "shorts_pager", "reel_watch_fragment"
        )

        // YouTube negative context indicators (Active comments sheet, Watch panel, Subscriptions feed, Home feed)
        private val YT_NEGATIVE = setOf(
            "active_comment_composer", "comment_composer", "comments_sheet", "comment_thread",
            "comments_list", "watch_panel", "player_fragment_container", "watch_while_layout",
            "video_player", "selected_home", "selected_subscriptions", "feed_filter_bar",
            "search_results", "search_edit_text"
        )

        // Snapchat Spotlight positive feed semantic indicators
        private val SNAP_SPOTLIGHT_POSITIVE = setOf(
            "spotlight", "spotlight_feed", "spotlight_player", "selected_spotlight",
            "spotlight_tab", "vertical_spotlight_pager", "spotlight_card", "spotlight_viewer",
            "remix snap"
        )

        // Snapchat negative context indicators (Camera, Chat, Map, Stories, Search)
        private val SNAP_NEGATIVE = setOf(
            "selected_camera", "take picture", "record video", "flip camera", "flash", "lenses",
            "capture_button", "selected_chat", "chat", "messages", "send a chat",
            "active_chat", "friend_feed", "selected_map", "snap map", "map",
            "selected_stories", "discover_feed", "story_player", "search_bar", "bitmoji"
        )

        // Button / Interactive control action keywords to reject
        private val INTERACTIVE_CONTROL_KEYWORDS = setOf(
            "like", "dislike", "comment", "comments", "share", "send", "save", "bookmark",
            "remix", "sound", "audio", "more options", "options", "follow", "subscribe",
            "channel", "profile", "camera", "search", "back", "close"
        )
    }

    /**
     * Refactored multi-stage short-video navigation classifier.
     */
    fun evaluateEvent(context: ScrollEvaluationContext): DetectionResult {
        // Stage 1: Validate event type
        when (context.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> return DetectionResult.REJECT_CLICK

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> { /* proceed */ }

            else -> return DetectionResult.REJECT_NON_SCROLL
        }

        // Stage 2: Identify supported foreground package
        val pkg = context.packageName
        if (pkg != PKG_INSTAGRAM && pkg != PKG_YOUTUBE && pkg != PKG_SNAPCHAT) {
            return DetectionResult.REJECT_UNTRACKED_APP
        }

        // Stage 3: Reject interactive button / non-feed controls masquerading as scrolls
        if (isInteractiveControl(context)) {
            return DetectionResult.REJECT_INTERACTIVE_CONTROL
        }

        // Stage 4: Directional analysis (Reject confirmed horizontal scrolling)
        if (isConfirmedHorizontalScroll(context)) {
            return DetectionResult.REJECT_HORIZONTAL_SCROLL
        }

        // Stage 5: App-specific feed-context and modal verification
        val contextTerms = extractNormalizedTerms(context)
        val contextString = contextTerms.joinToString(" ")

        when (pkg) {
            PKG_INSTAGRAM -> {
                // Check negative context (comments drawer, chat, profile edit, home feed)
                for (neg in INSTA_NEGATIVE) {
                    if (contextString.contains(neg)) {
                        return DetectionResult.REJECT_NON_REELS_CONTEXT
                    }
                }

                // Check positive Reels presence
                var hasReelsProof = false
                for (pos in INSTA_REELS_POSITIVE) {
                    if (contextString.contains(pos)) {
                        hasReelsProof = true
                        break
                    }
                }
                if (!hasReelsProof) {
                    return DetectionResult.REJECT_AMBIGUOUS_FEED
                }
            }

            PKG_YOUTUBE -> {
                // Check negative context (comments sheet, main watch panel, home feed)
                for (neg in YT_NEGATIVE) {
                    if (contextString.contains(neg)) {
                        return DetectionResult.REJECT_NON_SHORTS_CONTEXT
                    }
                }

                // Check positive Shorts presence
                var hasShortsProof = false
                for (pos in YT_SHORTS_POSITIVE) {
                    if (contextString.contains(pos)) {
                        hasShortsProof = true
                        break
                    }
                }
                if (!hasShortsProof) {
                    return DetectionResult.REJECT_AMBIGUOUS_FEED
                }
            }

            PKG_SNAPCHAT -> {
                // Check negative context (camera, chat messages, snap map, stories)
                for (neg in SNAP_NEGATIVE) {
                    if (contextString.contains(neg)) {
                        return DetectionResult.REJECT_NON_SPOTLIGHT_CONTEXT
                    }
                }

                // Check positive Spotlight presence
                var hasSpotlightProof = false
                for (pos in SNAP_SPOTLIGHT_POSITIVE) {
                    if (contextString.contains(pos)) {
                        hasSpotlightProof = true
                        break
                    }
                }
                if (!hasSpotlightProof) {
                    return DetectionResult.REJECT_AMBIGUOUS_FEED
                }
            }
        }

        // Stage 6: Swipe burst deduplication
        val now = context.eventTimestamp
        val timeSinceLastSwipe = now - lastConfirmedSwipeTimestamp

        if (timeSinceLastSwipe in 1 until debounceWindowMs && lastActivePackage == pkg) {
            return DetectionResult.REJECT_DEDUPLICATE_BURST
        }

        // Stage 7: Confirmed legitimate video navigation!
        lastConfirmedSwipeTimestamp = now
        lastActivePackage = pkg
        return DetectionResult.CONFIRMED_REEL_INCREMENT
    }

    /**
     * Determines whether the scrolled view is a specific button, icon, or control rather than video feed pager.
     */
    private fun isInteractiveControl(context: ScrollEvaluationContext): Boolean {
        val classNameStr = context.className?.toString() ?: ""
        val resIdStr = context.viewIdResourceName?.lowercase() ?: ""
        val textStr = context.sourceText?.toString()?.trim()?.lowercase() ?: ""
        val descStr = context.contentDescription?.toString()?.trim()?.lowercase() ?: ""

        // Buttons, ImageButtons, and small interactive icons
        val isButtonClass = classNameStr.endsWith("Button") ||
                classNameStr.endsWith("ImageButton") ||
                classNameStr.endsWith("ImageView")

        if (isButtonClass) {
            if (INTERACTIVE_CONTROL_KEYWORDS.any { textStr == it || descStr == it || resIdStr.contains(it) }) {
                return true
            }
            if (resIdStr.contains("button") || resIdStr.contains("_btn") || resIdStr.contains("tab_icon")) {
                return true
            }
        }

        // Check if the scrolled view is an active comment entry or comment row
        if (resIdStr.contains("comment_row") || resIdStr.contains("comment_thread_view") || resIdStr.contains("comments_recycler")) {
            return true
        }

        return false
    }

    /**
     * Identifies horizontal carousels, horizontal scroll views, and verified horizontal gesture deltas.
     */
    private fun isConfirmedHorizontalScroll(context: ScrollEvaluationContext): Boolean {
        val dx = context.scrollDeltaX
        val dy = context.scrollDeltaY

        // Confirmed horizontal scroll delta
        if (dx != 0 && abs(dx) > abs(dy)) {
            return true
        }

        // Horizontal container class
        val classNameStr = context.className?.toString()?.lowercase() ?: ""
        if (classNameStr.contains("horizontalscrollview") || classNameStr.contains("horizontalrecyclerview")) {
            return true
        }

        // Horizontal resource identifiers (stories tray, photo carousel)
        val resIdStr = context.viewIdResourceName?.lowercase() ?: ""
        if (resIdStr.contains("carousel") ||
            resIdStr.contains("horizontal") ||
            resIdStr.contains("story_tray") ||
            resIdStr.contains("stories_tray") ||
            resIdStr.contains("feed_carousel")
        ) {
            return true
        }

        return false
    }

    private fun extractNormalizedTerms(context: ScrollEvaluationContext): Set<String> {
        val terms = mutableSetOf<String>()
        context.className?.let { terms.add(it.toString().lowercase()) }
        context.sourceText?.let { terms.add(it.toString().lowercase()) }
        context.contentDescription?.let { terms.add(it.toString().lowercase()) }
        context.viewIdResourceName?.let { terms.add(it.toString().lowercase()) }
        terms.addAll(context.windowHierarchyKeywords.map { it.lowercase() })
        return terms
    }

    fun classifyFeed(pkg: String, hierarchyKeywords: Set<String>): FeedType {
        val text = hierarchyKeywords.joinToString(" ").lowercase()
        return when (pkg) {
            PKG_INSTAGRAM -> if (INSTA_REELS_POSITIVE.any { text.contains(it) }) FeedType.INSTAGRAM_REELS else FeedType.NON_FEED_OR_UNTRACKED
            PKG_YOUTUBE -> if (YT_SHORTS_POSITIVE.any { text.contains(it) }) FeedType.YOUTUBE_SHORTS else FeedType.NON_FEED_OR_UNTRACKED
            PKG_SNAPCHAT -> if (SNAP_SPOTLIGHT_POSITIVE.any { text.contains(it) }) FeedType.SNAPCHAT_SPOTLIGHT else FeedType.NON_FEED_OR_UNTRACKED
            else -> FeedType.NON_FEED_OR_UNTRACKED
        }
    }

    fun resetState() {
        lastConfirmedSwipeTimestamp = 0L
        lastActivePackage = null
    }
}
