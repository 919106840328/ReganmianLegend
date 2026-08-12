package com.regannian.legend.game.upgrade

import com.regannian.legend.game.GrayboxConfig

/**
 * 根据已购升级计算当日出锅/烫面注意力窗（纯函数，便于单测）。
 */
data class StationTimings(
    val dustCookMs: Long,
    val dustRedMs: Long,
    val dustBurntMs: Long,
    val blanchMs: Long,
    val blanchRedMs: Long,
    val blanchBurntMs: Long,
) {
    companion object {
        fun from(upgrades: Set<UpgradeId>): StationTimings {
            var dustCook = GrayboxConfig.DUST_COOK_MS
            var dustRed = GrayboxConfig.DUST_RED_MS
            var dustBurnt = GrayboxConfig.DUST_BURNT_MS
            var blanch = GrayboxConfig.BLANCH_MS
            var blanchRed = GrayboxConfig.BLANCH_RED_MS
            var blanchBurnt = GrayboxConfig.BLANCH_BURNT_MS

            // A2 猛火灶
            if (UpgradeId.A2 in upgrades) {
                dustCook = (dustCook * 73L) / 100L
                dustRed = (dustRed * 73L) / 100L
                dustBurnt = (dustBurnt * 73L) / 100L
                blanch = (blanch * 73L) / 100L
                blanchRed = (blanchRed * 73L) / 100L
                blanchBurnt = (blanchBurnt * 73L) / 100L
            }
            // C2 快烫
            if (UpgradeId.C2 in upgrades) {
                blanch = (blanch * 5L) / 8L
                blanchRed = (blanchRed * 5L) / 8L
                blanchBurnt = (blanchBurnt * 5L) / 8L
            }
            // B1 宽绿区
            if (UpgradeId.B1 in upgrades) {
                dustRed = dustCook + ((dustRed - dustCook) * 3L) / 2L
                dustBurnt = dustRed + ((dustBurnt - dustRed) * 3L) / 2L
                blanchRed = blanch + ((blanchRed - blanch) * 3L) / 2L
                blanchBurnt = blanchRed + ((blanchBurnt - blanchRed) * 3L) / 2L
            }
            // A3 宽煮窗口：红区再 +400ms
            if (UpgradeId.A3 in upgrades) {
                dustRed += 400L
                dustBurnt += 400L
            }
            // A5 工业风扇：Ready 阶段更耐放（拉长红/糊）
            if (UpgradeId.A5 in upgrades) {
                dustRed += 800L
                dustBurnt += 1_200L
            }

            return StationTimings(
                dustCookMs = dustCook.coerceAtLeast(400L),
                dustRedMs = dustRed.coerceAtLeast(dustCook + 200L),
                dustBurntMs = dustBurnt.coerceAtLeast(dustRed + 200L),
                blanchMs = blanch.coerceAtLeast(200L),
                blanchRedMs = blanchRed.coerceAtLeast(blanch + 100L),
                blanchBurntMs = blanchBurnt.coerceAtLeast(blanchRed + 100L),
            )
        }
    }
}
