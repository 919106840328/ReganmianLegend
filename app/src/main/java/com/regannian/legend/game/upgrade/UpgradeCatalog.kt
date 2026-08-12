package com.regannian.legend.game.upgrade

/**
 * 版本 1.0 全升级树 A–F（对齐 GDD §3.4）。
 * 旧存档名 FierceStove/WideGreen/ClassicCombo 在 [parseUpgradeId] 迁移为 A2/B1/D3。
 */
enum class UpgradeLine { A, B, C, D, E, F }

enum class UpgradeId {
    // A 掸面/煮面
    A1, A2, A3, A4, A5, A6, A7,
    // B 酱
    B1, B2, B3, B4, B5,
    // C 烫面/出餐
    C1, C2, C3, C4, C5,
    // D 配料
    D1, D2, D3, D4, D5, D6,
    // E 辅食（D3 会话落地玩法；此处可购解锁）
    E1, E2, E3, E4, E5, E6,
    // F 经营
    F1, F2, F3, F4, F5, F6, F7,
}

data class UpgradeDef(
    val id: UpgradeId,
    val line: UpgradeLine,
    val name: String,
    val price: Int,
    val blurb: String,
    val requires: Set<UpgradeId> = emptySet(),
    val suggestDay: Int = 1,
)

object UpgradeCatalog {
    val ALL: List<UpgradeDef> = listOf(
        // A
        UpgradeDef(UpgradeId.A1, UpgradeLine.A, "大号煮锅", 280, "面碗 3→4", suggestDay = 2),
        UpgradeDef(UpgradeId.A2, UpgradeLine.A, "猛火灶", 450, "掸面/烫面时间 ×0.73", suggestDay = 3),
        UpgradeDef(UpgradeId.A3, UpgradeLine.A, "宽煮窗口", 600, "过煮红区再延后", suggestDay = 5),
        UpgradeDef(UpgradeId.A4, UpgradeLine.A, "自动拌油", 900, "热面入碗自动加香油", suggestDay = 8),
        UpgradeDef(UpgradeId.A5, UpgradeLine.A, "工业风扇", 1200, "凉面等待更耐放", suggestDay = 10),
        UpgradeDef(UpgradeId.A6, UpgradeLine.A, "自动掸面机 I", 2400, "空闲时每 2.5s 自动开工", suggestDay = 15),
        UpgradeDef(UpgradeId.A7, UpgradeLine.A, "自动掸面机 II", 4200, "自动间隔 1.6s", requires = setOf(UpgradeId.A6), suggestDay = 25),
        // B
        UpgradeDef(UpgradeId.B1, UpgradeLine.B, "宽绿区", 250, "过煮/过烫更宽容", suggestDay = 2),
        UpgradeDef(UpgradeId.B2, UpgradeLine.B, "标准酱一键", 800, "营业日可一键芝麻酱", suggestDay = 7),
        UpgradeDef(UpgradeId.B3, UpgradeLine.B, "酱碟扩容", 700, "预留库存位（展示）", suggestDay = 8),
        UpgradeDef(UpgradeId.B4, UpgradeLine.B, "红油酱解锁", 1100, "配料栏常驻红油", suggestDay = 12),
        UpgradeDef(UpgradeId.B5, UpgradeLine.B, "自动调酱机", 2800, "热面入碗自动加芝麻酱", suggestDay = 20),
        // C
        UpgradeDef(UpgradeId.C1, UpgradeLine.C, "第二笊篱", 1000, "烫锅忙碌时凉面可排队", suggestDay = 9),
        UpgradeDef(UpgradeId.C2, UpgradeLine.C, "快烫", 850, "烫面时间更短", suggestDay = 8),
        UpgradeDef(UpgradeId.C3, UpgradeLine.C, "防过烫铃", 500, "出绿区时提醒", suggestDay = 6),
        UpgradeDef(UpgradeId.C4, UpgradeLine.C, "自动入碗", 1600, "烫好自动进空碗", suggestDay = 14),
        UpgradeDef(UpgradeId.C5, UpgradeLine.C, "递碗滑轨", 2200, "点顾客即可出餐", suggestDay = 18),
        // D
        UpgradeDef(UpgradeId.D1, UpgradeLine.D, "底料预置", 650, "入碗自动加卤水", suggestDay = 6),
        UpgradeDef(UpgradeId.D2, UpgradeLine.D, "配料手速 I", 250, "加料手感更快（展示）", suggestDay = 3),
        UpgradeDef(UpgradeId.D3, UpgradeLine.D, "经典套餐键", 2100, "一键芝麻酱+香葱(+萝卜)", suggestDay = 16),
        UpgradeDef(UpgradeId.D4, UpgradeLine.D, "酸豆角解锁", 500, "配料栏常驻酸豆角", suggestDay = 8),
        UpgradeDef(UpgradeId.D5, UpgradeLine.D, "辣椒油解锁", 750, "配料栏常驻红油", suggestDay = 11),
        UpgradeDef(UpgradeId.D6, UpgradeLine.D, "帮工加小料", 2400, "出餐时 50% 补次要缺料", suggestDay = 20),
        // E（玩法在 D3；此处解锁标记）
        UpgradeDef(UpgradeId.E1, UpgradeLine.E, "豆浆机", 500, "解锁豆浆工位与豆浆订单", suggestDay = 5),
        UpgradeDef(UpgradeId.E2, UpgradeLine.E, "豆浆快注", 800, "豆浆制作更快（需 E1）", requires = setOf(UpgradeId.E1), suggestDay = 10),
        UpgradeDef(UpgradeId.E3, UpgradeLine.E, "面窝炉", 1600, "解锁面窝（未买防糊会糊）", suggestDay = 18),
        UpgradeDef(UpgradeId.E4, UpgradeLine.E, "面窝防糊", 1800, "面窝不再糊（需 E3）", requires = setOf(UpgradeId.E3), suggestDay = 18),
        UpgradeDef(UpgradeId.E5, UpgradeLine.E, "蛋酒", 2000, "解锁蛋酒工位与订单", suggestDay = 22),
        UpgradeDef(UpgradeId.E6, UpgradeLine.E, "DIY 减配", 3100, "订单少要香葱但涨价", suggestDay = 28),
        // F
        UpgradeDef(UpgradeId.F1, UpgradeLine.F, "加凳 I", 1200, "座位 3→4", suggestDay = 10),
        UpgradeDef(UpgradeId.F2, UpgradeLine.F, "加凳 II", 3500, "座位 4→5", requires = setOf(UpgradeId.F1), suggestDay = 24),
        UpgradeDef(UpgradeId.F3, UpgradeLine.F, "涨价装修 I", 650, "售价 ×1.15", suggestDay = 7),
        UpgradeDef(UpgradeId.F4, UpgradeLine.F, "涨价装修 II", 3500, "售价 ×1.3", requires = setOf(UpgradeId.F3), suggestDay = 25),
        UpgradeDef(UpgradeId.F5, UpgradeLine.F, "防偷铃", 1000, "偷碗贼一键驱离", suggestDay = 20),
        UpgradeDef(UpgradeId.F6, UpgradeLine.F, "自动收钱", 3100, "柜台金币自动入账", suggestDay = 22),
        UpgradeDef(UpgradeId.F7, UpgradeLine.F, "帮工补货", 2400, "预留补货（展示）", suggestDay = 16),
    )

    val byLine: Map<UpgradeLine, List<UpgradeDef>> =
        UpgradeLine.entries.associateWith { line -> ALL.filter { it.line == line } }

    fun def(id: UpgradeId): UpgradeDef = ALL.first { it.id == id }

    fun lineTitle(line: UpgradeLine): String = when (line) {
        UpgradeLine.A -> "A · 掸面/煮面"
        UpgradeLine.B -> "B · 芝麻酱"
        UpgradeLine.C -> "C · 烫面/出餐"
        UpgradeLine.D -> "D · 配料"
        UpgradeLine.E -> "E · 辅食"
        UpgradeLine.F -> "F · 经营"
    }

    /** 旧 C4 存档名 → 新 ID */
    fun parseUpgradeId(raw: String): UpgradeId? = when (raw) {
        "FierceStove" -> UpgradeId.A2
        "WideGreen" -> UpgradeId.B1
        "ClassicCombo" -> UpgradeId.D3
        else -> runCatching { UpgradeId.valueOf(raw) }.getOrNull()
    }
}
