package com.regannian.legend.game.meta

import com.regannian.legend.game.state.DaySummary
import com.regannian.legend.game.upgrade.UpgradeId

enum class AchievementId {
    FIRST_UPGRADE,
    AUTO_DUST,
    BUSY_TILL,
    GOLDEN_TILL,
    THREE_STARS,
    UNCLE_TIP,
    THIEF_SHOO,
    TRASH_RUN,
    GOOD_BOWL,
    DAY10_OPEN,
    SOY_SIDE,
    FULL_TREE,
    LEGEND,
}

data class AchievementDef(
    val id: AchievementId,
    val title: String,
    val body: String,
)

object AchievementCatalog {
    val ALL: List<AchievementDef> = listOf(
        AchievementDef(AchievementId.FIRST_UPGRADE, "开张装修", "买下第一件升级。"),
        AchievementDef(AchievementId.AUTO_DUST, "机器掸面", "购入自动掸面机 I。"),
        AchievementDef(AchievementId.BUSY_TILL, "早市小忙", "单日收入达到 50 金。"),
        AchievementDef(AchievementId.GOLDEN_TILL, "柜台上金", "单日收入达到 100 金。"),
        AchievementDef(AchievementId.THREE_STARS, "好评如潮", "任意一日打出三星。"),
        AchievementDef(AchievementId.UNCLE_TIP, "伺候大爷", "给过早市大爷素面并收钱。"),
        AchievementDef(AchievementId.THIEF_SHOO, "看店有方", "赶走一次偷碗贼。"),
        AchievementDef(AchievementId.TRASH_RUN, "错了就扔", "把一碗面拖进垃圾桶。"),
        AchievementDef(AchievementId.GOOD_BOWL, "刚好一碗", "以良品出过一餐。"),
        AchievementDef(AchievementId.DAY10_OPEN, "忙完十天", "打烊 Day 10。"),
        AchievementDef(AchievementId.SOY_SIDE, "豆浆配面", "买下豆浆机。"),
        AchievementDef(AchievementId.FULL_TREE, "店面全满", "买齐 A–F 全部升级。"),
        AchievementDef(AchievementId.LEGEND, "热干面传奇", "全升级且钱包达到结局门槛。"),
    )

    fun def(id: AchievementId): AchievementDef = ALL.first { it.id == id }
}

data class AchievementInput(
    val upgrades: Set<UpgradeId>,
    val walletCoins: Int,
    val bestDayCoins: Int,
    val day: DaySummary? = null,
)

object AchievementEvaluator {
    fun unlock(
        already: Set<AchievementId>,
        input: AchievementInput,
    ): Set<AchievementId> {
        val next = already.toMutableSet()
        val day = input.day
        if (input.upgrades.isNotEmpty()) next += AchievementId.FIRST_UPGRADE
        if (UpgradeId.A6 in input.upgrades) next += AchievementId.AUTO_DUST
        if (UpgradeId.E1 in input.upgrades) next += AchievementId.SOY_SIDE
        if (UpgradeId.entries.all { it in input.upgrades }) next += AchievementId.FULL_TREE
        if (EndingRules.isMet(input.upgrades, input.walletCoins)) next += AchievementId.LEGEND
        val peak = maxOf(input.bestDayCoins, day?.coins ?: 0)
        if (peak >= 50) next += AchievementId.BUSY_TILL
        if (peak >= 100) next += AchievementId.GOLDEN_TILL
        if (day != null) {
            if (day.stars >= 3) next += AchievementId.THREE_STARS
            if (day.servedUncle) next += AchievementId.UNCLE_TIP
            if (day.chasedThief) next += AchievementId.THIEF_SHOO
            if (day.discarded) next += AchievementId.TRASH_RUN
            if (day.servedGood) next += AchievementId.GOOD_BOWL
            if (day.dayId >= 10) next += AchievementId.DAY10_OPEN
        }
        return next
    }
}
