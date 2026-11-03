package com.brainrot.rebuilt.domain

import androidx.annotation.DrawableRes
import com.brainrot.rebuilt.R
import com.brainrot.rebuilt.domain.model.MascotStage
import kotlin.math.max

object MascotStageCalculator {

    fun calculateStage(count: Int, target: Int): MascotStage {
        val safeTarget = max(target, 1)
        val safeCount = max(count, 0)
        val ratio = safeCount.toFloat() / safeTarget.toFloat()
        return MascotStage.fromRatio(ratio)
    }

    @DrawableRes
    fun getDrawableResId(stage: MascotStage): Int {
        return when (stage) {
            MascotStage.FRESH -> R.drawable.br_brain_standing_1
            MascotStage.HAPPY -> R.drawable.br_brain_standing_2
            MascotStage.TIRED -> R.drawable.br_brain_standing_3
            MascotStage.EXHAUSTED -> R.drawable.br_brain_standing_4
            MascotStage.ROASTED -> R.drawable.br_brain_standing_5
        }
    }
}

