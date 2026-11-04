package com.brainrot.rebuilt.domain.diagnostic

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.util.ArrayDeque

/**
 * Diagnostic logger for Reel and Short Video Accessibility events (Finding 2).
 *
 * Persists redacted event metadata to both an in-memory buffer and a rolling file,
 * allowing precise forensic comparison of vertical transitions across target apps.
 */
object ReelDiagnosticLogger {

    private const val TAG = "BrainPalDiagnostic"
    private const val MAX_IN_MEMORY_RECORDS = 200
    private const val LOG_FILE_NAME = "reels_diagnostic.jsonl"

    private val recordsLock = Any()
    private val memoryBuffer = ArrayDeque<DiagnosticEventRecord>(MAX_IN_MEMORY_RECORDS)
    private var diagnosticFile: File? = null

    fun initialize(context: Context) {
        synchronized(recordsLock) {
            try {
                val dir = File(context.filesDir, "diagnostics")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                diagnosticFile = File(dir, LOG_FILE_NAME)
                Log.i(TAG, "ReelDiagnosticLogger initialized at ${diagnosticFile?.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize diagnostic file", e)
            }
        }
    }

    fun recordEvent(record: DiagnosticEventRecord) {
        val json = record.toJsonString()
        Log.i(TAG, "[CAPTURE] $json")

        synchronized(recordsLock) {
            if (memoryBuffer.size >= MAX_IN_MEMORY_RECORDS) {
                memoryBuffer.removeFirst()
            }
            memoryBuffer.addLast(record)

            diagnosticFile?.let { file ->
                try {
                    FileWriter(file, true).use { writer ->
                        writer.appendLine(json)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to append to diagnostic file: ${e.message}")
                }
            }
        }
    }

    fun getRecentRecords(): List<DiagnosticEventRecord> {
        synchronized(recordsLock) {
            return memoryBuffer.toList()
        }
    }

    fun clear() {
        synchronized(recordsLock) {
            memoryBuffer.clear()
            try {
                diagnosticFile?.let { file ->
                    if (file.exists()) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear diagnostic file: ${e.message}")
            }
        }
    }

    fun exportFormattedLog(): String {
        val records = getRecentRecords()
        if (records.isEmpty()) return "No diagnostic events captured."
        return buildString {
            append("=== BrainPal Reel Diagnostic Log (${records.size} events) ===\n")
            records.forEachIndexed { idx, r ->
                append("#${idx + 1} ${r.toSummaryString()}\n")
            }
        }
    }
}

