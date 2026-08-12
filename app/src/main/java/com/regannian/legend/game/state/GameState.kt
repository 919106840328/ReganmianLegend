package com.regannian.legend.game.state

import com.regannian.legend.game.GrayboxConfig
import com.regannian.legend.game.config.CustomerSpawnConfig
import com.regannian.legend.game.config.DayConfig
import com.regannian.legend.game.config.RecipeCatalog
import com.regannian.legend.game.config.TargetSatisfaction
import com.regannian.legend.game.model.BowlId
import com.regannian.legend.game.model.BowlState
import com.regannian.legend.game.model.CoinPile
import com.regannian.legend.game.model.CustomerId
import com.regannian.legend.game.model.CustomerKind
import com.regannian.legend.game.model.CustomerState
import com.regannian.legend.game.model.CustomerStatus
import com.regannian.legend.game.model.DayPhase
import com.regannian.legend.game.model.IngredientId
import com.regannian.legend.game.model.ItemQuality
import com.regannian.legend.game.model.Order
import com.regannian.legend.game.model.SideId
import com.regannian.legend.game.model.SideStationState
import com.regannian.legend.game.model.StationId
import com.regannian.legend.game.model.StationState
import com.regannian.legend.game.model.ThiefState
import com.regannian.legend.game.upgrade.StationTimings
import com.regannian.legend.game.upgrade.UpgradeEffects
import com.regannian.legend.game.upgrade.UpgradeId

data class GameState(
    val phase: DayPhase,
    val dayId: Int,
    val clockMs: Long,
    val dayDurationMs: Long,
    val coins: Int,
    val daySatSum: Float,
    val daySatCount: Int,
    val bowls: Map<BowlId, BowlState>,
    val focusedBowlId: BowlId?,
    val stations: Map<StationId, StationState>,
    val customers: List<CustomerState>,
    val coinsOnCounter: List<CoinPile>,
    val nextCoinPileSeq: Int,
    val spawnIntervalMs: Long,
    val spawnAccMs: Long,
    val nextCustomerSeq: Int,
    val recipePool: List<Order>,
    val toppingBar: List<IngredientId>,
    val patienceMaxMs: Long,
    val targetSatisfaction: TargetSatisfaction,
    val ownedUpgrades: Set<UpgradeId>,
    val timings: StationTimings,
    /** C1：烫锅忙时排队的凉面品质 */
    val pendingCoolQuality: ItemQuality? = null,
    /** A6/A7 自动掸面积分 */
    val autoDustAccMs: Long = 0L,
    /** D2 偷碗贼 */
    val thief: ThiefState = ThiefState.Absent,
    val thiefCooldownMs: Long = 0L,
    /** D3 辅食工位 */
    val sideStations: Map<SideId, SideStationState> = emptyMap(),
) {
    fun firstEmptyBowlId(): BowlId? =
        bowls.keys.firstOrNull { bowls[it] is BowlState.Empty }

    fun matchesAnyWaitingOrder(toppings: Set<IngredientId>): Boolean {
        return customers.any { c ->
            c.status == CustomerStatus.WaitingServe &&
                c.order.required
                    .intersect(GrayboxConfig.MAIN_INGREDIENTS)
                    .all { it in toppings }
        }
    }

    companion object {
        fun initialGraybox(
            upgrades: Set<UpgradeId> = emptySet(),
        ): GameState = initialForDay(
            DayConfig(
                dayId = 0,
                durationSec = (GrayboxConfig.DAY_DURATION_MS / 1000L).toInt(),
                customerSpawn = CustomerSpawnConfig(
                    intervalMs = Long.MAX_VALUE / 4,
                    maxSlots = 3,
                ),
                patienceMul = 1f,
                recipePool = listOf("classic", "chili", "bean"),
                toppingBar = GrayboxConfig.TOPPING_BAR.map { it.name },
                initialFilled = 3,
            ),
            upgrades = upgrades,
        )

        fun initialForDay(
            config: DayConfig,
            upgrades: Set<UpgradeId> = emptySet(),
        ): GameState {
            val pool = UpgradeEffects.augmentRecipePool(
                config.recipePool.map { RecipeCatalog.order(it) }.ifEmpty {
                    listOf(GrayboxConfig.CLASSIC)
                },
                upgrades,
            )
            val toppings = UpgradeEffects.mergeToppingBar(
                RecipeCatalog.toppings(config.toppingBar).ifEmpty {
                    listOf(IngredientId.SesameSauce, IngredientId.SesameOil, IngredientId.Scallion)
                },
                upgrades,
            )
            val patience = (GrayboxConfig.DEFAULT_PATIENCE_MS * config.patienceMul)
                .toLong()
                .coerceAtLeast(5_000L)
            val maxSlots = (config.maxSlots + UpgradeEffects.seatBonus(upgrades)).coerceAtMost(5)
            val filled = config.initialFilled.coerceIn(0, maxSlots)
            val spawnUncle = config.dayId == 10
            val customers = (0 until maxSlots).map { index ->
                val id = CustomerId("c${index + 1}")
                if (index < filled) {
                    if (spawnUncle && index == 0) {
                        CustomerState(
                            id = id,
                            status = CustomerStatus.WaitingServe,
                            order = GrayboxConfig.PLAIN_VEG,
                            patienceMs = patience,
                            patienceMaxMs = patience,
                            kind = CustomerKind.EarlyMarketUncle,
                        )
                    } else {
                        val order = pool[index % pool.size]
                        CustomerState(
                            id = id,
                            status = CustomerStatus.WaitingServe,
                            order = order,
                            patienceMs = patience,
                            patienceMaxMs = patience,
                            kind = CustomerKind.Normal,
                        )
                    }
                } else {
                    CustomerState(
                        id = id,
                        status = CustomerStatus.Empty,
                        order = pool.first(),
                        patienceMs = 0L,
                        patienceMaxMs = 0L,
                        kind = CustomerKind.Normal,
                    )
                }
            }
            val bowlIds = UpgradeEffects.bowlIds(upgrades)
            val bowls = bowlIds.associateWith { BowlState.Empty }
            return GameState(
                phase = DayPhase.Playing,
                dayId = config.dayId,
                clockMs = 0L,
                dayDurationMs = config.durationMs,
                coins = 0,
                daySatSum = 0f,
                daySatCount = 0,
                bowls = bowls,
                focusedBowlId = bowlIds.first(),
                stations = mapOf(
                    StationId.DustCook to StationState.Idle,
                    StationId.Blanch to StationState.Idle,
                ),
                customers = customers,
                coinsOnCounter = emptyList(),
                nextCoinPileSeq = 1,
                spawnIntervalMs = config.spawnIntervalMs,
                spawnAccMs = 0L,
                nextCustomerSeq = filled,
                recipePool = pool,
                toppingBar = toppings,
                patienceMaxMs = patience,
                targetSatisfaction = config.targetSatisfaction,
                ownedUpgrades = upgrades,
                timings = StationTimings.from(upgrades),
                pendingCoolQuality = null,
                autoDustAccMs = 0L,
                thief = ThiefState.Absent,
                thiefCooldownMs = if (config.dayId >= GrayboxConfig.THIEF_MIN_DAY) 20_000L else Long.MAX_VALUE / 4,
                sideStations = UpgradeEffects.unlockedSides(upgrades)
                    .associateWith { SideStationState.Idle },
            )
        }
    }
}
