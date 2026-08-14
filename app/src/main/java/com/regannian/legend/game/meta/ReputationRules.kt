package com.regannian.legend.game.meta

/**
 * 跨日口碑：结算满意度改口碑，口碑微调次日耐心。
 */
object ReputationRules {
    const val DEFAULT: Int = 50

    fun apply(current: Int, satPercent: Int): Int {
        val delta = when {
            satPercent >= 85 -> 8
            satPercent >= 65 -> 3
            satPercent >= 45 -> 0
            else -> -8
        }
        return (current + delta).coerceIn(0, 100)
    }

    /** 口碑 0→1.12 耐心倍率约 0.88～1.12。 */
    fun patienceMul(reputation: Int): Float {
        val t = reputation.coerceIn(0, 100) / 100f
        return 0.88f + t * 0.24f
    }
}
