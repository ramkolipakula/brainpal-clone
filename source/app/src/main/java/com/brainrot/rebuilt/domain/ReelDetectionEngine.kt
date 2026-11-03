package com.brainrot.rebuilt.domain

import android.view.accessibility.AccessibilityEvent
import kotlin.math.abs

enum class DetectionResult {
    CONFIRMED_REEL_INCREMENT,
    REJECT_CLICK,
    REJECT_NON_SCROLL,
    REJECT_UNTRACKED_APP,
    REJECT_HORIZONTAL_SCROLL,
    REJECT_ZERO_DELTA,
    REJECT_NON_REELS_CONTEXT,
    REJECT_NON_SHORTS_CONTEXT,
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
    private val debounceWindowMs: Long = 800L
) {
    private var lastConfirmedSwipeTimestamp: Long = 0L
    private var lastActivePackage: String? = null

    companion object {
        const val PKG_INSTAGRAM = "com.instagram.android"
        const val PKG_YOUTUBE = "com.google.android.youtube"

        // Instagram Reels positive keywords
        private val INSTA_REELS_POSITIVE = setOf(
            "reels", "reel", "clips", "remix", "audio", "original audio",
            "camera, double tap to record", "clips_view_pager", "clips_video_container",
            "reel_viewer", "selected_reels", "selected_clips"
        )

        // Instagram negative context indicators (Active comment composer, DM thread, Profile edit, Home feed)
        private val INSTA_NEGATIVE = setOf(
            "active_comment_composer", "comment_composer", "layout_comment_thread",
            "active_message_composer", "direct_thread", "direct_composer",
            "active_search_input", "search_edit_text",
            "edit profile", "edit_profile", "change profile photo",
            "selected_home", "row_feed_carousel"
        )

        // YouTube Shorts positive keywords
        private val YT_SHORTS_POSITIVE = setOf(
            "shorts", "short", "reel_player", "shorts_container", "remix", "sound",
            "selected_shorts"
        )

        // YouTube negative context indicators (Active comments sheet, Watch panel, Home feed)
        private val YT_NEGATIVE = setOf(
            "active_comment_composer", "comment_composer", "comments_sheet",
            "watch_panel", "player_fragment_container", "selected_home"
        )
    }

    fun evaluateEvent(context: ScrollEvaluationContext): DetectionResult {
        // A. Strict Event-Type Filtering
        when (context.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> return DetectionResult.REJECT_CLICK

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> { /* proceed */ }

            else -> return DetectionResult.REJECT_NON_SCROLL
        }

        // B. Application Package Verification
        val pkg = context.packageName
        if (pkg != PKG_INSTAGRAM && pkg != PKG_YOUTUBE) {
            return DetectionResult.REJECT_UNTRACKED_APP
        }

        // C. Directional Analysis (Vertical vs Horizontal)
        val dx = context.scrollDeltaX
        val dy = context.scrollDeltaY

        if (dx != 0 && abs(dx) > abs(dy)) {
            // Confirmed horizontal navigation (e.g., photo carousel swipe or tab swipe)
            return DetectionResult.REJECT_HORIZONTAL_SCROLL
        }

        // D. Feed-Context Verification
        val allContextTerms = mutableSetOf<String>()
        context.className?.let { allContextTerms.add(it.toString().lowercase()) }
        context.sourceText?.let { allContextTerms.add(it.toString().lowercase()) }
        context.contentDescription?.let { allContextTerms.add(it.toString().lowercase()) }
        context.viewIdResourceName?.let { allContextTerms.add(it.toString().lowercase()) }
        allContextTerms.addAll(context.windowHierarchyKeywords.map { it.lowercase() })

        val contextString = allContextTerms.joinToString(" ")

        when (pkg) {
            PKG_INSTAGRAM -> {
                // Check negative indicators first
                for (neg in INSTA_NEGATIVE) {
                    if (contextString.contains(neg)) {
                        return DetectionResult.REJECT_NON_REELS_CONTEXT
                    }
                }

                // Check positive indicators
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
                // Check negative indicators first
                for (neg in YT_NEGATIVE) {
                    if (contextString.contains(neg)) {
                        return DetectionResult.REJECT_NON_SHORTS_CONTEXT
                    }
                }

                // Check positive indicators
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
        }

        // E. Swipe Deduplication
        val now = context.eventTimestamp
        val timeSinceLastSwipe = now - lastConfirmedSwipeTimestamp

        if (timeSinceLastSwipe in 1 until debounceWindowMs && lastActivePackage == pkg) {
            return DetectionResult.REJECT_DEDUPLICATE_BURST
        }

        // Confirmed valid swipe!
        lastConfirmedSwipeTimestamp = now
        lastActivePackage = pkg
        return DetectionResult.CONFIRMED_REEL_INCREMENT
    }

    fun resetState() {
        lastConfirmedSwipeTimestamp = 0L
        lastActivePackage = null
    }
}

