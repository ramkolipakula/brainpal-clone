package com.brainrot.rebuilt.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.brainrot.rebuilt.BlockOverlayActivity
import com.brainrot.rebuilt.BrainPalApp
import com.brainrot.rebuilt.domain.DetectionResult
import com.brainrot.rebuilt.domain.MascotStageCalculator
import com.brainrot.rebuilt.domain.ReelDetectionEngine
import com.brainrot.rebuilt.domain.ScrollEvaluationContext
import com.brainrot.rebuilt.domain.diagnostic.DiagnosticEventRecord
import com.brainrot.rebuilt.domain.diagnostic.ReelDiagnosticLogger
import com.brainrot.rebuilt.domain.model.MascotStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ReelsAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var floatingBadgeView: FloatingBadgeView? = null
    private var badgeParams: WindowManager.LayoutParams? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val reelDetectionEngine = ReelDetectionEngine(debounceWindowMs = 350L)
    private var currentActivePackage: String? = null

    companion object {
        private const val TAG = "BrainPalA11y"
        private const val DEBUG_LOGGING = false // Toggle for safe debug-only logging
        var isServiceConnected = false
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceConnected = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        Log.i(TAG, "ReelsAccessibilityService connected. WindowManager ready: ${windowManager != null}")

        // Initialize diagnostic event logging (Finding 2)
        ReelDiagnosticLogger.initialize(this)

        val app = BrainPalApp.instance

        // Observe reactive StateFlows for live synchronization
        serviceScope.launch {
            app.prefsRepository.floatingBubbleEnabledFlow.collect { enabled ->
                if (!enabled) {
                    hideFloatingBadge()
                } else {
                    currentActivePackage?.let { pkg ->
                        if (app.prefsRepository.isAppReelMonitored(pkg)) {
                            showOrUpdateFloatingBadge(pkg)
                        }
                    }
                }
            }
        }

        serviceScope.launch {
            app.prefsRepository.todayReelCountFlow.collect { count ->
                val target = currentActivePackage?.let { app.prefsRepository.getAppDailyReelLimit(it) }
                    ?: app.prefsRepository.getDailyReelLimit()
                val isLarge = app.prefsRepository.getBubbleSize() == "large"
                floatingBadgeView?.updateData(count, target, isLarge)
            }
        }

        serviceScope.launch {
            app.prefsRepository.dailyReelLimitFlow.collect { globalTarget ->
                val count = app.prefsRepository.getTodayReelCount()
                val target = currentActivePackage?.let { app.prefsRepository.getAppDailyReelLimit(it) }
                    ?: globalTarget
                val isLarge = app.prefsRepository.getBubbleSize() == "large"
                floatingBadgeView?.updateData(count, target, isLarge)
            }
        }

        serviceScope.launch {
            app.prefsRepository.perAppReelLimitsFlow.collect { _ ->
                val count = app.prefsRepository.getTodayReelCount()
                val target = currentActivePackage?.let { app.prefsRepository.getAppDailyReelLimit(it) }
                    ?: app.prefsRepository.getDailyReelLimit()
                val isLarge = app.prefsRepository.getBubbleSize() == "large"
                floatingBadgeView?.updateData(count, target, isLarge)
            }
        }

        serviceScope.launch {
            app.prefsRepository.bubbleSizeFlow.collect { size ->
                val count = app.prefsRepository.getTodayReelCount()
                val target = currentActivePackage?.let { app.prefsRepository.getAppDailyReelLimit(it) }
                    ?: app.prefsRepository.getDailyReelLimit()
                floatingBadgeView?.updateData(count, target, size == "large")
            }
        }

        // Check if a monitored app is already open upon service connection
        try {
            val activeRootPkg = try { rootInActiveWindow?.packageName?.toString() } catch (e: Exception) { null }
            val currentPkg = activeRootPkg ?: app.usageRepository.getForegroundApp()
            Log.d(TAG, "onServiceConnected: detected active pkg: $currentPkg")
            if (currentPkg != null && app.prefsRepository.isAppReelMonitored(currentPkg) && app.prefsRepository.isFloatingBubbleEnabled()) {
                currentActivePackage = currentPkg
                mainHandler.post { showOrUpdateFloatingBadge(currentPkg) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking active app on connection", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString() ?: return
        val app = BrainPalApp.instance

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                Log.d(TAG, "TYPE_WINDOW_STATE_CHANGED: pkg=$pkg")
                handleWindowStateChanged(pkg)
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (app.prefsRepository.isAppReelMonitored(pkg) && app.prefsRepository.isFloatingBubbleEnabled()) {
                    currentActivePackage = pkg
                    if (floatingBadgeView == null || !floatingBadgeView!!.isAttachedToWindow || floatingBadgeView?.visibility != View.VISIBLE) {
                        mainHandler.post { showOrUpdateFloatingBadge(pkg) }
                    }
                }
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                // Finding 1: Check independent reel monitor configuration
                if (app.prefsRepository.isAppReelMonitored(pkg)) {
                    currentActivePackage = pkg
                    if (app.prefsRepository.isFloatingBubbleEnabled()) {
                        mainHandler.post { showOrUpdateFloatingBadge(pkg) }
                    }

                    val deltaX = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) event.scrollDeltaX else 0
                    val deltaY = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) event.scrollDeltaY else 0

                    val root = rootInActiveWindow ?: try {
                        var p = event.source
                        while (p?.parent != null) {
                            p = p.parent
                        }
                        p
                    } catch (e: Exception) { null }

                    val inspection = inspectHierarchy(root)
                    val fromIdx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && event.fromIndex >= 0) event.fromIndex else null
                    val toIdx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && event.toIndex >= 0) event.toIndex else null

                    val evalContext = ScrollEvaluationContext(
                        eventType = event.eventType,
                        packageName = pkg,
                        scrollDeltaX = deltaX,
                        scrollDeltaY = deltaY,
                        className = event.className,
                        sourceText = event.text.joinToString(" "),
                        contentDescription = event.contentDescription,
                        viewIdResourceName = try { event.source?.viewIdResourceName } catch (e: Exception) { null },
                        windowHierarchyKeywords = inspection.keywords,
                        eventTimestamp = android.os.SystemClock.uptimeMillis(),
                        scrollPositionBefore = fromIdx,
                        scrollPositionAfter = toIdx,
                        isSourcePager = inspection.containsVideoPager
                    )

                    val result = reelDetectionEngine.evaluateEvent(evalContext)
                    val classifiedFeed = reelDetectionEngine.classifyFeed(pkg, inspection.keywords)

                    if (DEBUG_LOGGING) {
                        Log.d(TAG, "ReelEvaluation: pkg=$pkg result=$result dx=$deltaX dy=$deltaY feed=$classifiedFeed")
                    }

                    // Finding 2: Record redacted diagnostic metadata for forensic verification
                    if (app.prefsRepository.isDiagnosticModeEnabled()) {
                        val record = DiagnosticEventRecord(
                            timestamp = System.currentTimeMillis(),
                            eventType = event.eventType,
                            eventTypeName = AccessibilityEvent.eventTypeToString(event.eventType),
                            packageName = pkg,
                            className = event.className?.toString(),
                            scrollDeltaX = deltaX,
                            scrollDeltaY = deltaY,
                            scrollPositionBefore = fromIdx,
                            scrollPositionAfter = toIdx,
                            sourceNodeClass = event.source?.className?.toString(),
                            sourceNodeResourceId = try { event.source?.viewIdResourceName } catch (e: Exception) { null },
                            windowId = event.windowId,
                            windowType = try { event.source?.window?.type ?: -1 } catch (e: Exception) { -1 },
                            rootWindowPackage = try { rootInActiveWindow?.packageName?.toString() } catch (e: Exception) { null },
                            isRootInActiveWindowNull = rootInActiveWindow == null,
                            nodeHierarchyDepth = inspection.maxDepth,
                            nodesInspectedCount = inspection.inspectedCount,
                            feedClassifierResult = classifiedFeed.name,
                            rejectionReason = result.name,
                            containsVideoPager = inspection.containsVideoPager,
                            exposedPagerClassOrId = inspection.exposedPagerClassOrId ?: "NONE_EXPOSED"
                        )
                        ReelDiagnosticLogger.recordEvent(record)
                    }

                    if (result == DetectionResult.CONFIRMED_REEL_INCREMENT) {
                        val newCount = app.prefsRepository.incrementTodayReelCount()
                        if (DEBUG_LOGGING) {
                            Log.i(TAG, "Confirmed reel navigation for $pkg. New count: $newCount")
                        }

                        // Concept 2: Only trigger full-screen block overlay if the app is guarded for usage-time/quota limits
                        val isGuarded = app.prefsRepository.isAppBlocked(pkg)
                        val limit = app.prefsRepository.getAppDailyReelLimit(pkg)
                        val isBedtime = app.prefsRepository.isWithinBedtime()

                        if ((newCount >= limit || isBedtime) && isGuarded && !app.prefsRepository.isSnoozed()) {
                            if (DEBUG_LOGGING) {
                                Log.w(TAG, "Reel quota or bedtime reached ($newCount/$limit). Launching overlay!")
                            }
                            val intent = Intent(this, BlockOverlayActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                putExtra("TARGET_PACKAGE", pkg)
                                putExtra("REEL_COUNT", newCount)
                                putExtra("IS_STRICT_MODE", app.prefsRepository.isStrictMode())
                            }
                            startActivity(intent)
                        }
                    }
                }
            }

            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> {
                // Strict requirement: Clicks must never increment reel count
                Log.d(TAG, "Ignored click event in $pkg (will never increment reels)")

                if (app.prefsRepository.isDiagnosticModeEnabled() && app.prefsRepository.isAppReelMonitored(pkg)) {
                    val record = DiagnosticEventRecord(
                        timestamp = System.currentTimeMillis(),
                        eventType = event.eventType,
                        eventTypeName = AccessibilityEvent.eventTypeToString(event.eventType),
                        packageName = pkg,
                        className = event.className?.toString(),
                        scrollDeltaX = 0,
                        scrollDeltaY = 0,
                        scrollPositionBefore = null,
                        scrollPositionAfter = null,
                        sourceNodeClass = event.source?.className?.toString(),
                        sourceNodeResourceId = try { event.source?.viewIdResourceName } catch (e: Exception) { null },
                        windowId = event.windowId,
                        windowType = try { event.source?.window?.type ?: -1 } catch (e: Exception) { -1 },
                        rootWindowPackage = try { rootInActiveWindow?.packageName?.toString() } catch (e: Exception) { null },
                        isRootInActiveWindowNull = rootInActiveWindow == null,
                        nodeHierarchyDepth = 0,
                        nodesInspectedCount = 1,
                        feedClassifierResult = "IGNORED_CLICK",
                        rejectionReason = DetectionResult.REJECT_CLICK.name,
                        containsVideoPager = false,
                        exposedPagerClassOrId = "NONE_EXPOSED"
                    )
                    ReelDiagnosticLogger.recordEvent(record)
                }
            }
        }
    }

    private fun handleWindowStateChanged(pkg: String) {
        val app = BrainPalApp.instance

        if (app.prefsRepository.isAppReelMonitored(pkg)) {
            currentActivePackage = pkg
            if (app.prefsRepository.isFloatingBubbleEnabled()) {
                mainHandler.post { showOrUpdateFloatingBadge(pkg) }
            }
            return
        }

        val activeRootPkg = try {
            rootInActiveWindow?.packageName?.toString()
        } catch (e: Exception) {
            null
        }

        if (activeRootPkg != null && app.prefsRepository.isAppReelMonitored(activeRootPkg)) {
            currentActivePackage = activeRootPkg
            if (app.prefsRepository.isFloatingBubbleEnabled()) {
                mainHandler.post { showOrUpdateFloatingBadge(activeRootPkg) }
            }
            return
        }

        if (isLauncherOrHomeScreen(pkg)) {
            currentActivePackage = null
            mainHandler.post { hideFloatingBadge() }
        } else if (isTransientSystemOverlay(pkg) || (activeRootPkg != null && isTransientSystemOverlay(activeRootPkg))) {
            Log.d(TAG, "Ignoring transient system overlay: pkg=$pkg activeRoot=$activeRootPkg")
        } else {
            Log.d(TAG, "Switched away from monitored app to unmonitored app: $pkg")
            currentActivePackage = null
            mainHandler.post { hideFloatingBadge() }
        }
    }

    private fun isLauncherOrHomeScreen(pkg: String): Boolean {
        return pkg.contains("launcher", ignoreCase = true) ||
               pkg == "com.android.launcher3" ||
               pkg == "com.bbk.launcher2" ||
               pkg.contains("nexuslauncher", ignoreCase = true)
    }

    private fun isTransientSystemOverlay(pkg: String): Boolean {
        return pkg == "android" ||
               pkg == "com.android.systemui" ||
               pkg.startsWith("com.vivo.") ||
               pkg.startsWith("com.bbk.") ||
               pkg.contains("inputmethod", ignoreCase = true) ||
               pkg.contains("keyboard", ignoreCase = true) ||
               pkg.contains("ime", ignoreCase = true) ||
               pkg == packageName
    }

    data class HierarchyInspection(
        val keywords: Set<String>,
        val maxDepth: Int,
        val inspectedCount: Int,
        val containsVideoPager: Boolean,
        val exposedPagerClassOrId: String?
    )

    private fun inspectHierarchy(root: AccessibilityNodeInfo?): HierarchyInspection {
        if (root == null) return HierarchyInspection(emptySet(), 0, 0, false, null)
        val keywords = mutableSetOf<String>()
        val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        queue.add(Pair(root, 1))
        var inspectedCount = 0
        var maxObservedDepth = 1
        var containsVideoPager = false
        var exposedPagerClassOrId: String? = null

        while (queue.isNotEmpty() && inspectedCount < 100) {
            val (node, depth) = queue.removeFirst()
            inspectedCount++
            if (depth > maxObservedDepth) maxObservedDepth = depth

            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            val resId = try { node.viewIdResourceName?.trim() } catch (e: Exception) { null }
            val className = node.className?.toString()?.trim() ?: ""

            if (!text.isNullOrBlank()) keywords.add(text)
            if (!desc.isNullOrBlank()) keywords.add(desc)
            if (!resId.isNullOrBlank()) keywords.add(resId)

            // Detect video pager exposing accessibility node
            val isPagerClass = className.contains("ViewPager", ignoreCase = true) ||
                    className.contains("RecyclerView", ignoreCase = true)
            val isPagerResId = !resId.isNullOrBlank() && (
                    resId.contains("pager", ignoreCase = true) ||
                    resId.contains("reel_player", ignoreCase = true) ||
                    resId.contains("clips_video", ignoreCase = true) ||
                    resId.contains("spotlight_player", ignoreCase = true) ||
                    resId.contains("shorts_container", ignoreCase = true)
            )

            if ((isPagerClass || isPagerResId) && !containsVideoPager) {
                containsVideoPager = true
                exposedPagerClassOrId = resId ?: className
            }

            if (node.isSelected) {
                if (!desc.isNullOrBlank()) keywords.add("selected_${desc.lowercase().replace(" ", "_")}")
                if (!text.isNullOrBlank()) keywords.add("selected_${text.lowercase().replace(" ", "_")}")
                if (!resId.isNullOrBlank()) keywords.add("selected_${resId.lowercase()}")
            }

            // Check if node is an active text input or comment composer
            if (node.isEditable || className == "android.widget.EditText") {
                keywords.add("has_active_text_input")
                val hint = node.hintText?.toString() ?: ""
                val fullText = "$text $hint"
                if (fullText.contains("comment", ignoreCase = true) || fullText.contains("reply", ignoreCase = true)) {
                    keywords.add("active_comment_composer")
                }
                if (fullText.contains("message", ignoreCase = true)) {
                    keywords.add("active_message_composer")
                }
                if (fullText.contains("search", ignoreCase = true)) {
                    keywords.add("active_search_input")
                }
            }

            if (!resId.isNullOrBlank()) {
                val idLower = resId.lowercase()
                if (idLower.contains("comment") && (idLower.contains("thread") || idLower.contains("composer") || idLower.contains("sheet") || idLower.contains("list") || idLower.contains("row") || idLower.contains("recycler"))) {
                    keywords.add("active_comment_composer")
                    keywords.add("comments_sheet")
                }
                if (idLower.contains("direct") && (idLower.contains("thread") || idLower.contains("composer") || idLower.contains("message"))) {
                    keywords.add("active_message_composer")
                    keywords.add("direct_thread")
                }
                if (idLower.contains("camera") || idLower.contains("capture")) {
                    keywords.add("selected_camera")
                }
                if (idLower.contains("spotlight")) {
                    keywords.add("spotlight")
                }
                if (idLower.contains("chat") && (idLower.contains("feed") || idLower.contains("view"))) {
                    keywords.add("selected_chat")
                }
            }

            for (i in 0 until node.childCount) {
                val child = try { node.getChild(i) } catch (e: Exception) { null }
                if (child != null) {
                    queue.add(Pair(child, depth + 1))
                }
            }
        }
        return HierarchyInspection(keywords, maxObservedDepth, inspectedCount, containsVideoPager, exposedPagerClassOrId)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOrUpdateFloatingBadge(pkg: String) {
        val wm = windowManager ?: (getSystemService(Context.WINDOW_SERVICE) as? WindowManager).also { windowManager = it }
        if (wm == null) {
            Log.e(TAG, "WindowManager is null! Cannot show badge.")
            return
        }

        val app = BrainPalApp.instance
        if (!app.prefsRepository.isFloatingBubbleEnabled()) {
            hideFloatingBadge()
            return
        }

        val limit = app.prefsRepository.getAppDailyReelLimit(pkg)
        val reelCount = app.prefsRepository.getTodayReelCount()
        val isLarge = app.prefsRepository.getBubbleSize() == "large"

        val existingView = floatingBadgeView
        if (existingView == null || !existingView.isAttachedToWindow) {
            if (existingView != null) {
                try {
                    wm.removeViewImmediate(existingView)
                } catch (ignored: Exception) {}
                floatingBadgeView = null
            }

            val badgeView = FloatingBadgeView(this)
            badgeView.updateData(reelCount, limit, isLarge)

            val primaryLayoutType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            val fallbackLayoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                primaryLayoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = 40
                y = 220
            }

            // Draggable touch listener
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f

            badgeView.setOnTouchListener { v, motionEvent ->
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = motionEvent.rawX
                        initialTouchY = motionEvent.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX - (motionEvent.rawX - initialTouchX).toInt()
                        params.y = initialY + (motionEvent.rawY - initialTouchY).toInt()
                        try {
                            wm.updateViewLayout(v, params)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to update layout during drag", e)
                        }
                        true
                    }
                    else -> false
                }
            }

            try {
                wm.addView(badgeView, params)
                floatingBadgeView = badgeView
                badgeParams = params
                Log.i(TAG, "Badge view added with TYPE_ACCESSIBILITY_OVERLAY for $pkg. Count: $reelCount/$limit")
            } catch (e1: Exception) {
                Log.w(TAG, "TYPE_ACCESSIBILITY_OVERLAY failed, attempting fallback to $fallbackLayoutType...")
                try {
                    params.type = fallbackLayoutType
                    wm.addView(badgeView, params)
                    floatingBadgeView = badgeView
                    badgeParams = params
                } catch (e2: Exception) {
                    Log.e(TAG, "All attempts to add floating badge view failed!", e2)
                }
            }
        } else {
            existingView.visibility = View.VISIBLE
            existingView.updateData(reelCount, limit, isLarge)
            try {
                badgeParams?.let { wm.updateViewLayout(existingView, it) }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update layout of attached view, recreating: ${e.message}")
                try {
                    wm.removeViewImmediate(existingView)
                } catch (ignored: Exception) {}
                floatingBadgeView = null
                showOrUpdateFloatingBadge(pkg)
            }
        }
    }

    private fun hideFloatingBadge() {
        floatingBadgeView?.let { view ->
            if (view.visibility != View.GONE) {
                view.visibility = View.GONE
                Log.d(TAG, "Floating badge hidden")
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "ReelsAccessibilityService onInterrupt")
        isServiceConnected = false
        mainHandler.post { hideFloatingBadge() }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "ReelsAccessibilityService onDestroy")
        isServiceConnected = false
        serviceScope.cancel()
        mainHandler.post {
            try {
                floatingBadgeView?.let { windowManager?.removeView(it) }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing badge view during onDestroy", e)
            }
            floatingBadgeView = null
        }
    }

    // ----------------------------------------------------
    // Floating Reel Badge View Component (Phase 2 UI Reference)
    // ----------------------------------------------------
    private class FloatingBadgeView(context: Context) : LinearLayout(context) {
        private val mascotImageView: ImageView
        private val countTextView: TextView
        private var currentStage: MascotStage? = null

        init {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padH = dpToPx(14f)
            val padV = dpToPx(6f)
            setPadding(padH, padV, padH, padV)
            elevation = 20f

            // Dark purple/near-black background with subtle purple outline
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(24f).toFloat()
                setColor(android.graphics.Color.parseColor("#EE161322"))
                setStroke(dpToPx(1.5f), android.graphics.Color.parseColor("#FF7C5CE7"))
            }

            mascotImageView = ImageView(context).apply {
                val size = dpToPx(24f)
                layoutParams = LayoutParams(size, size).apply {
                    marginEnd = dpToPx(8f)
                }
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            addView(mascotImageView)

            countTextView = TextView(context).apply {
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                setTextColor(android.graphics.Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = Typeface.DEFAULT_BOLD
                includeFontPadding = false
            }
            addView(countTextView)
        }

        fun updateData(count: Int, target: Int, isLarge: Boolean) {
            val safeTarget = kotlin.math.max(target, 1)
            val safeCount = kotlin.math.max(count, 0)
            val newStage = MascotStageCalculator.calculateStage(safeCount, safeTarget)

            // Mascot Aging Animation (Phase 2)
            val resId = MascotStageCalculator.getDrawableResId(newStage)
            if (currentStage != newStage) {
                currentStage = newStage
                mascotImageView.setImageResource(resId)
                // Short, tasteful scale pulse on boundary crossing
                mascotImageView.animate()
                    .scaleX(1.3f)
                    .scaleY(1.3f)
                    .setDuration(120)
                    .withEndAction {
                        mascotImageView.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(120)
                            .start()
                    }
                    .start()
            } else if (mascotImageView.drawable == null) {
                mascotImageView.setImageResource(resId)
            }

            countTextView.text = "$safeCount / $safeTarget"
            val textSizeSp = if (isLarge) 15f else 13f
            countTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)

            val mascotSize = if (isLarge) dpToPx(28f) else dpToPx(24f)
            val lp = mascotImageView.layoutParams as LayoutParams
            if (lp.width != mascotSize) {
                lp.width = mascotSize
                lp.height = mascotSize
                mascotImageView.layoutParams = lp
            }

            val padH = if (isLarge) dpToPx(18f) else dpToPx(14f)
            val padV = if (isLarge) dpToPx(8f) else dpToPx(6f)
            setPadding(padH, padV, padH, padV)
        }

        private fun dpToPx(dp: Float): Int {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
        }
    }
}
