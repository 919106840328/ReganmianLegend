package com.regannian.legend.game.model

/** 工位：掸面含煮 / 烫面。 */
enum class StationId {
    DustCook,
    Blanch,
}

/**
 * 热干面配料。
 * 主料：芝麻酱；其余为油香/小料（次要）。
 */
enum class IngredientId {
    SesameSauce, // 芝麻酱（主料）
    SesameOil,   // 香油
    SaladOil,    // 色拉油
    ChiliOil,    // 红油
    Garlic,      // 大蒜
    Scallion,    // 香葱
    Radish,      // 萝卜丁
    PickledBean, // 酸豆角
    Brine,       // 卤水汁
    // D3 辅食（作订单次要需求；在辅食工位制作后拖入碗）
    SoyMilk,     // 豆浆
    MianWo,      // 面窝
    EggWine,     // 蛋酒
}

/** 辅食工位（D3）。 */
enum class SideId {
    Soy,
    MianWo,
    EggWine,
}

sealed interface SideStationState {
    data object Idle : SideStationState
    data class Working(val remainingMs: Long, val totalMs: Long) : SideStationState
    data class Ready(
        val quality: ItemQuality = ItemQuality.Good,
        val elapsedMs: Long = 0L,
    ) : SideStationState
}

/**
 * 偷碗贼（D2）。
 */
sealed interface ThiefState {
    data object Absent : ThiefState

    data class Present(
        val tapsDone: Int,
        val tapsNeeded: Int,
        val escapeMs: Long,
        val target: ThiefTarget,
    ) : ThiefState
}

sealed interface ThiefTarget {
    data object CounterCoins : ThiefTarget
    data class Bowl(val bowlId: BowlId) : ThiefTarget
}

enum class DayPhase {
    Playing,
    Paused,
    Result,
}

enum class ItemQuality {
    Good,
    Soft,
    Sticky,
    Burnt,
}

@JvmInline
value class CustomerId(val value: String)

@JvmInline
value class BowlId(val value: String)

@JvmInline
value class CoinPileId(val value: String)

@JvmInline
value class RecipeId(val value: String)

sealed interface StationOutput {
    data object CoolNoodles : StationOutput
    data object HotNoodles : StationOutput
}

/**
 * 注意力窗：绿区终点可捞；越过红区品质变差。
 * elapsed 自进入 Working 起算。
 */
data class AttentionWindow(
    val greenEndMs: Long,
    val redEndMs: Long,
    val elapsedMs: Long,
)

sealed interface StationState {
    data object Idle : StationState

    data class Working(
        val remainingMs: Long,
        val totalMs: Long,
        val output: StationOutput,
        val attention: AttentionWindow,
        val quality: ItemQuality = ItemQuality.Good,
    ) : StationState

    data class Ready(
        val output: StationOutput,
        val quality: ItemQuality,
        val attention: AttentionWindow,
    ) : StationState
}

sealed interface BowlState {
    data object Empty : BowlState

    data class Assembling(
        val quality: ItemQuality = ItemQuality.Good,
        val toppings: Set<IngredientId> = emptySet(),
    ) : BowlState

    data class ReadyToServe(
        val quality: ItemQuality,
        val toppings: Set<IngredientId>,
    ) : BowlState
}

data class Order(
    val recipeId: RecipeId,
    val required: Set<IngredientId>,
    val reward: Int,
)

enum class CustomerStatus {
    Empty,
    WaitingServe,
    /** 大爷：已给面，等待点一下互动收款 */
    WaitingInteract,
    Served,
    LeftAngry,
}

enum class CustomerKind {
    Normal,
    /** Day10 过早市大爷 */
    EarlyMarketUncle,
}

data class CustomerState(
    val id: CustomerId,
    val status: CustomerStatus,
    val order: Order,
    val patienceMs: Long,
    val patienceMaxMs: Long,
    val kind: CustomerKind = CustomerKind.Normal,
    /** 出餐/离店台词；等面台词由 UI 按耐心算。 */
    val bark: String? = null,
)

data class CoinPile(
    val id: CoinPileId,
    val amount: Int,
)

/** 出餐判定结果（逻辑层）。 */
sealed interface ServeVerdict {
    data class Accept(val reward: Int, val satFactor: Float) : ServeVerdict
    data object Reject : ServeVerdict
}
