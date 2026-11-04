package com.brainrot.rebuilt.domain.diagnostic

/**
 * Diagnostic event metadata record (Finding 2).
 *
 * Captures structural accessibility telemetry without exposing private user messages,
 * captions, account names, or arbitrary content descriptions.
 */
data class DiagnosticEventRecord(
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: Int,
    val eventTypeName: String,
    val packageName: String,
    val className: String?,
    val scrollDeltaX: Int,
    val scrollDeltaY: Int,
    val scrollPositionBefore: Int?,
    val scrollPositionAfter: Int?,
    val sourceNodeClass: String?,
    val sourceNodeResourceId: String?,
    val windowId: Int,
    val windowType: Int,
    val rootWindowPackage: String?,
    val isRootInActiveWindowNull: Boolean,
    val nodeHierarchyDepth: Int,
    val nodesInspectedCount: Int,
    val feedClassifierResult: String,
    val rejectionReason: String?,
    val containsVideoPager: Boolean,
    val exposedPagerClassOrId: String?
) {
    /**
     * Serializes this record to a single-line redacted JSON representation for logging.
     */
    fun toJsonString(): String {
        val escape = { s: String? -> if (s == null) "null" else "\"${s.replace("\"", "\\\"")}\"" }
        return "{" +
                "\"timestamp\":$timestamp," +
                "\"eventType\":$eventType," +
                "\"eventTypeName\":${escape(eventTypeName)}," +
                "\"packageName\":${escape(packageName)}," +
                "\"className\":${escape(className)}," +
                "\"scrollDeltaX\":$scrollDeltaX," +
                "\"scrollDeltaY\":$scrollDeltaY," +
                "\"scrollPositionBefore\":${scrollPositionBefore ?: "null"}," +
                "\"scrollPositionAfter\":${scrollPositionAfter ?: "null"}," +
                "\"sourceNodeClass\":${escape(sourceNodeClass)}," +
                "\"sourceNodeResourceId\":${escape(sourceNodeResourceId)}," +
                "\"windowId\":$windowId," +
                "\"windowType\":$windowType," +
                "\"rootWindowPackage\":${escape(rootWindowPackage)}," +
                "\"isRootInActiveWindowNull\":$isRootInActiveWindowNull," +
                "\"nodeHierarchyDepth\":$nodeHierarchyDepth," +
                "\"nodesInspectedCount\":$nodesInspectedCount," +
                "\"feedClassifierResult\":${escape(feedClassifierResult)}," +
                "\"rejectionReason\":${escape(rejectionReason)}," +
                "\"containsVideoPager\":$containsVideoPager," +
                "\"exposedPagerClassOrId\":${escape(exposedPagerClassOrId)}" +
                "}"
    }

    /**
     * Formatted multi-line summary for debugging and audit reporting.
     */
    fun toSummaryString(): String {
        return buildString {
            append("[$eventTypeName] pkg=$packageName class=$className dx=$scrollDeltaX dy=$scrollDeltaY\n")
            append("  source: class=$sourceNodeClass id=$sourceNodeResourceId | window: id=$windowId type=$windowType rootNull=$isRootInActiveWindowNull\n")
            append("  hierarchy: depth=$nodeHierarchyDepth inspected=$nodesInspectedCount | pager: exposed=$containsVideoPager ($exposedPagerClassOrId)\n")
            append("  verdict: classifier=$feedClassifierResult rejection=$rejectionReason")
        }
    }
}

