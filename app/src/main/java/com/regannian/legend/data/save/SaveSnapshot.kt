package com.regannian.legend.data.save

import com.regannian.legend.game.meta.AchievementId
import com.regannian.legend.game.meta.ReputationRules
import com.regannian.legend.game.upgrade.UpgradeId

/** 日终进度快照。 */
data class SaveSnapshot(
    val walletCoins: Int = 0,
    val upgrades: Set<UpgradeId> = emptySet(),
    /** 下次默认进入的 Day（1..10）。 */
    val nextDayId: Int = 1,
    val muted: Boolean = false,
    val bgmMuted: Boolean = false,
    val tutorialDone: Boolean = false,
    val reputation: Int = ReputationRules.DEFAULT,
    val achievements: Set<AchievementId> = emptySet(),
    val seenEnding: Boolean = false,
    val bestDayCoins: Int = 0,
    val lastAdBonusDayId: Int = 0,
)
