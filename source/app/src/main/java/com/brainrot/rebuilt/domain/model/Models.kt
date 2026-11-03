package com.brainrot.rebuilt.domain.model

enum class BuddyMood {
    EXCELLENT, // < 25% of quota
    GOOD,      // 25% - 75% of quota
    WARN,      // 75% - 100% of quota
    ROASTED    // > 100% of quota
}

enum class MascotStage(
    val stageNumber: Int,
    val description: String,
    val assetName: String
) {
    FRESH(1, "Fresh & Energetic", "br_brain_standing_1"),
    HAPPY(2, "Happy & Focused", "br_brain_standing_2"),
    TIRED(3, "Slightly Worn", "br_brain_standing_3"),
    EXHAUSTED(4, "Fatigued", "br_brain_standing_4"),
    ROASTED(5, "Completely Cooked", "br_brain_standing_5");

    companion object {
        fun fromRatio(ratio: Float): MascotStage {
            return when {
                ratio < 0.25f -> FRESH
                ratio < 0.50f -> HAPPY
                ratio < 0.75f -> TIRED
                ratio < 1.00f -> EXHAUSTED
                else -> ROASTED
            }
        }
    }
}

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val usageTimeMillis: Long,
    val dailyLimitMinutes: Int = 30,
    val isBlocked: Boolean = false,
    val launchesToday: Int = 0
)

data class RoastMeme(
    val id: String,
    val source: String,
    val url: String,
    val caption: String,
    val width: Int,
    val height: Int,
    val language: String
)

data class DailyDetoxSummary(
    val date: String,
    val totalTimeSpentMillis: Long,
    val allowedTimeLimitMillis: Long,
    val totalReelsCount: Int,
    val allowedReelsLimit: Int,
    val streakDays: Int,
    val mood: BuddyMood
)
