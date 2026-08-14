package com.regannian.legend.game.upgrade

import com.regannian.legend.game.GrayboxConfig

/**
 * 根据已购升级计算当日出锅/烫面注意力窗（纯函数，便于单测）。
 */
data class StationTimings(
    val dustCookMs: Long,
    val dustGoodHoldMs: Long,
    val dustRedMs: Long,
    val dustBurntMs: Long,
    val blanchMs: Long,
    val blanchGoodHoldMs: Long,
    val blanchRedMs: Long,
    val blanchBurntMs: Long,
) {
    fun dustGoodUntilMs(): Long = dustCookMs + dustGoodHoldMs

    fun blanchGoodUntilMs(): Long = blanchMs + blanchGoodHoldMs

    companion object {
        fun from(upgrades: Set<UpgradeId>): StationTimings {
            var dustCook = GrayboxConfig.DUST_COOK_MS
            var dustHold = GrayboxConfig.DUST_GOOD_HOLD_MS
            var dustRed = GrayboxConfig.DUST_RED_MS
            var dustBurnt = GrayboxConfig.DUST_BURNT_MS
            var blanch = GrayboxConfig.BLANCH_MS
            var blanchHold = GrayboxConfig.BLANCH_GOOD_HOLD_MS
            var blanchRed = GrayboxConfig.BLANCH_RED_MS
            var blanchBurnt = GrayboxConfig.BLANCH_BURNT_MS

            // A2 猛火灶：整体加速
            if (UpgradeId.A2 in upgrades) {
                dustCook = (dustCook * 73L) / 100L
                dustHold = (dustHold * 73L) / 100L
                dustRed = (dustRed * 73L) / 100L
                dustBurnt = (dustBurnt * 73L) / 100L
                blanch = (blanch * 73L) / 100L
                blanchHold = (blanchHold * 73L) / 100L
                blanchRed = (blanchRed * 73L) / 100L
                blanchBurnt = (blanchBurnt * 73L) / 100L
            }
            // C2 快烫
            if (UpgradeId.C2 in upgrades) {
                blanch = (blanch * 5L) / 8L
                blanchHold = (blanchHold * 5L) / 8L
                blanchRed = (blanchRed * 5L) / 8L
                blanchBurnt = (blanchBurnt * 5L) / 8L
            }
            // B1 宽好限
            if (UpgradeId.B1 in upgrades) {
                dustHold = (dustHold * 3L) / 2L
                blanchHold = (blanchHold * 3L) / 2L
                dustRed = dustCook + dustHold + ((dustRed - dustCook) * 3L) / 2L
                dustBurnt = dustRed + ((dustBurnt - dustRed) * 3L) / 2L
                blanchRed = blanch + blanchHold + ((blanchRed - blanch) * 3L) / 2L
                blanchBurnt = blanchRed + ((blanchBurnt - blanchRed) * 3L) / 2L
            }
            // A3 宽煮窗口
            if (UpgradeId.A3 in upgrades) {
                dustHold += 400L
                dustRed += 400L
                dustBurnt += 400L
            }
            // A5 工业风扇：Ready 更耐放
            if (UpgradeId.A5 in upgrades) {
                dustHold += 800L
                dustRed += 800L
                dustBurnt += 1_200L
                blanchHold += 400L
            }

            dustCook = dustCook.coerceAtLeast(400L)
            blanch = blanch.coerceAtLeast(200L)
            dustHold = dustHold.coerceAtLeast(600L)
            blanchHold = blanchHold.coerceAtLeast(400L)
            dustRed = dustRed.coerceAtLeast(dustCook + dustHold + 200L)
            dustBurnt = dustBurnt.coerceAtLeast(dustRed + 200L)
            blanchRed = blanchRed.coerceAtLeast(blanch + blanchHold + 100L)
            blanchBurnt = blanchBurnt.coerceAtLeast(blanchRed + 100L)

            return StationTimings(
                dustCookMs = dustCook,
                dustGoodHoldMs = dustHold,
                dustRedMs = dustRed,
                dustBurntMs = dustBurnt,
                blanchMs = blanch,
                blanchGoodHoldMs = blanchHold,
                blanchRedMs = blanchRed,
                blanchBurntMs = blanchBurnt,
            )
        }
    }
}
