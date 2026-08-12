package com.regannian.legend.game.state

import com.regannian.legend.game.config.TargetSatisfaction
import kotlin.math.roundToInt

/** 日终摘要（供结算屏展示）。 */
data class DaySummary(
    val dayId: Int,
    val coins: Int,
    val satPercent: Int,
    val orderCount: Int,
    val stars: Int,
)

fun GameState.toDaySummary(): DaySummary {
    val satRatio = if (daySatCount <= 0) {
        0f
    } else {
        (daySatSum / daySatCount).coerceIn(0f, 1f)
    }
    val satPercent = (satRatio * 100f).roundToInt().coerceIn(0, 100)
    return DaySummary(
        dayId = dayId,
        coins = coins,
        satPercent = satPercent,
        orderCount = daySatCount,
        stars = starCount(satRatio, targetSatisfaction),
    )
}

fun starCount(satRatio: Float, targets: TargetSatisfaction): Int = when {
    satRatio >= targets.star3 -> 3
    satRatio >= targets.star2 -> 2
    satRatio >= targets.star1 -> 1
    else -> 0
}
