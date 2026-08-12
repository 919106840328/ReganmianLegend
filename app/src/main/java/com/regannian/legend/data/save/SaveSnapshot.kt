package com.regannian.legend.data.save

import com.regannian.legend.game.upgrade.UpgradeId

/** 日终进度快照（C5）。 */
data class SaveSnapshot(
    val walletCoins: Int = 0,
    val upgrades: Set<UpgradeId> = emptySet(),
    /** 下次默认进入的 Day（1..10）。 */
    val nextDayId: Int = 1,
    val muted: Boolean = false,
)
