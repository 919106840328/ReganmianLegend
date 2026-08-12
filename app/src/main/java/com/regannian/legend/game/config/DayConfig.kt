package com.regannian.legend.game.config

import kotlinx.serialization.Serializable

/**
 * 单日关卡配置（C3）：改 JSON 即可调节奏，无需改 Reducer 大规则。
 */
@Serializable
data class DayConfig(
    val dayId: Int,
    val durationSec: Int = 100,
    val customerSpawn: CustomerSpawnConfig = CustomerSpawnConfig(),
    val patienceMul: Float = 1f,
    val recipePool: List<String> = listOf("classic"),
    val toppingBar: List<String> = listOf("SesameSauce", "SesameOil", "Scallion"),
    /** 开局已坐满的座位数（其余空座等刷客）。 */
    val initialFilled: Int = 1,
    val targetSatisfaction: TargetSatisfaction = TargetSatisfaction(),
) {
    val durationMs: Long get() = durationSec * 1000L
    val spawnIntervalMs: Long get() = customerSpawn.intervalMs
    val maxSlots: Int get() = customerSpawn.maxSlots
}

@Serializable
data class CustomerSpawnConfig(
    val intervalMs: Long = 6_000L,
    val maxSlots: Int = 3,
)

@Serializable
data class TargetSatisfaction(
    val star1: Float = 0.45f,
    val star2: Float = 0.65f,
    val star3: Float = 0.85f,
)
