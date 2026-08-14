package com.regannian.legend.game.meta

import com.regannian.legend.game.upgrade.UpgradeId

/**
 * 结局门槛（GDD：全升级 + 累计金币）。美术可后换烟花。
 */
object EndingRules {
    const val WALLET_THRESHOLD: Int = 20_000

    fun isMet(upgrades: Set<UpgradeId>, walletCoins: Int): Boolean =
        UpgradeId.entries.all { it in upgrades } && walletCoins >= WALLET_THRESHOLD
}
