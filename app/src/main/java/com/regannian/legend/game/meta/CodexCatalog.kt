package com.regannian.legend.game.meta

import com.regannian.legend.game.upgrade.UpgradeId

data class CodexEntry(
    val id: String,
    val title: String,
    val lockedHint: String,
    val body: String,
)

object CodexCatalog {
    val ALL: List<CodexEntry> = listOf(
        CodexEntry(
            id = "commuter",
            title = "赶早客",
            lockedHint = "开张即见",
            body = "要一碗热干面赶路。图标以后可换成上班族立绘。",
        ),
        CodexEntry(
            id = "uncle",
            title = "过早市大爷",
            lockedHint = "完成「伺候大爷」",
            body = "只要芝麻酱的素面。点他拍桌付钱。",
        ),
        CodexEntry(
            id = "thief",
            title = "偷碗贼",
            lockedHint = "完成「看店有方」",
            body = "Day 8 起盯碗或柜台。连点或防偷铃可赶。",
        ),
        CodexEntry(
            id = "soy",
            title = "豆浆",
            lockedHint = "买下豆浆机",
            body = "辅食工位：点开工再拖进碗。",
        ),
        CodexEntry(
            id = "mianwo",
            title = "面窝",
            lockedHint = "买下面窝炉",
            body = "没买防糊会糊。图标可后换。",
        ),
        CodexEntry(
            id = "eggwine",
            title = "蛋酒",
            lockedHint = "买下蛋酒",
            body = "后期辅食订单。",
        ),
        CodexEntry(
            id = "legend",
            title = "热干面传奇",
            lockedHint = "达成结局",
            body = "巷子里都知道这碗面。烟花演出可后换正式动画。",
        ),
    )

    fun isUnlocked(
        id: String,
        achievements: Set<AchievementId>,
        upgrades: Set<UpgradeId>,
        seenEnding: Boolean,
    ): Boolean = when (id) {
        "commuter" -> true
        "uncle" -> AchievementId.UNCLE_TIP in achievements
        "thief" -> AchievementId.THIEF_SHOO in achievements
        "soy" -> UpgradeId.E1 in upgrades
        "mianwo" -> UpgradeId.E3 in upgrades
        "eggwine" -> UpgradeId.E5 in upgrades
        "legend" -> seenEnding || AchievementId.LEGEND in achievements
        else -> false
    }
}
