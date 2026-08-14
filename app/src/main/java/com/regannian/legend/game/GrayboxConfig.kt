package com.regannian.legend.game

import com.regannian.legend.game.model.BowlId
import com.regannian.legend.game.model.CustomerId
import com.regannian.legend.game.model.IngredientId
import com.regannian.legend.game.model.ItemQuality
import com.regannian.legend.game.model.Order
import com.regannian.legend.game.model.RecipeId
import com.regannian.legend.game.model.ServeVerdict
import com.regannian.legend.game.model.SideId
import com.regannian.legend.game.upgrade.UpgradeId
import kotlin.math.roundToInt

/**
 * 玩法 MVP（C1）数值：注意力窗 + 主/次要料规则。
 */
object GrayboxConfig {
    const val DAY_DURATION_MS: Long = 100_000L
    const val TICK_STEP_MS: Long = 100L

    /**
     * 掸面时间轴：
     * - COOK：煮到可出锅（Working→Ready）
     * - GOOD_HOLD：出锅后仍算「好面」的宽限
     * - RED / BURNT：软面 / 粘面分界
     */
    const val DUST_COOK_MS: Long = 2_000L
    const val DUST_GOOD_HOLD_MS: Long = 1_800L
    const val DUST_RED_MS: Long = 6_000L
    const val DUST_BURNT_MS: Long = 9_000L

    const val BLANCH_MS: Long = 600L
    const val BLANCH_GOOD_HOLD_MS: Long = 1_200L
    const val BLANCH_RED_MS: Long = 2_500L
    const val BLANCH_BURNT_MS: Long = 4_000L

    const val DEFAULT_PATIENCE_MS: Long = 60_000L
    const val DISCARD_PENALTY: Int = 2
    /** 大爷互动收款（幽默轻冒犯，中额）。 */
    const val UNCLE_TIP_REWARD: Int = 28

    /** 芝麻酱为主料；缺主料拒收。 */
    val MAIN_INGREDIENTS: Set<IngredientId> = setOf(IngredientId.SesameSauce)

    val BOWL_IDS: List<BowlId> = listOf(
        BowlId("bowl-1"),
        BowlId("bowl-2"),
        BowlId("bowl-3"),
    )

    val CUSTOMER_IDS: List<CustomerId> = listOf(
        CustomerId("c1"),
        CustomerId("c2"),
        CustomerId("c3"),
    )

    val TOPPING_BAR: List<IngredientId> = listOf(
        IngredientId.SesameSauce,
        IngredientId.SesameOil,
        IngredientId.SaladOil,
        IngredientId.ChiliOil,
        IngredientId.Garlic,
        IngredientId.Scallion,
        IngredientId.Radish,
        IngredientId.PickledBean,
        IngredientId.Brine,
    )

    val CLASSIC: Order = Order(
        recipeId = RecipeId("classic"),
        required = setOf(
            IngredientId.SesameSauce,
            IngredientId.SesameOil,
            IngredientId.Scallion,
        ),
        reward = 10,
    )

    val CHILI: Order = Order(
        recipeId = RecipeId("chili"),
        required = setOf(
            IngredientId.SesameSauce,
            IngredientId.ChiliOil,
            IngredientId.Garlic,
        ),
        reward = 12,
    )

    val BEAN: Order = Order(
        recipeId = RecipeId("bean"),
        required = setOf(
            IngredientId.SesameSauce,
            IngredientId.SesameOil,
            IngredientId.PickledBean,
        ),
        reward = 11,
    )

    /** 大爷要的素面：只要芝麻酱。 */
    val PLAIN_VEG: Order = Order(
        recipeId = RecipeId("plain_veg"),
        required = setOf(IngredientId.SesameSauce),
        reward = 4,
    )

    val DAY_ORDERS: List<Order> = listOf(CLASSIC, CHILI, BEAN)

    fun displayName(id: IngredientId): String = when (id) {
        IngredientId.SesameSauce -> "芝麻酱"
        IngredientId.SesameOil -> "香油"
        IngredientId.SaladOil -> "色拉油"
        IngredientId.ChiliOil -> "红油"
        IngredientId.Garlic -> "大蒜"
        IngredientId.Scallion -> "香葱"
        IngredientId.Radish -> "萝卜丁"
        IngredientId.PickledBean -> "酸豆角"
        IngredientId.Brine -> "卤水"
        IngredientId.SoyMilk -> "豆浆"
        IngredientId.MianWo -> "面窝"
        IngredientId.EggWine -> "蛋酒"
    }

    const val THIEF_ESCAPE_MS: Long = 4_500L
    const val THIEF_TAPS_DEFAULT: Int = 5
    const val THIEF_COOLDOWN_MS: Long = 35_000L
    /** Day≥该值开始可能刷贼（10 日内容压缩自对标 Day20）。 */
    const val THIEF_MIN_DAY: Int = 8

    fun sideCookMs(side: SideId, upgrades: Set<UpgradeId>): Long {
        val base = when (side) {
            SideId.Soy -> 1_200L
            SideId.MianWo -> 1_800L
            SideId.EggWine -> 1_500L
        }
        return if (side == SideId.Soy && UpgradeId.E2 in upgrades) {
            (base * 65L) / 100L
        } else {
            base
        }
    }

    fun orderLabel(order: Order): String =
        order.required.joinToString("+") { displayName(it) }

    fun qualityLabel(q: ItemQuality): String = when (q) {
        ItemQuality.Good -> "好面"
        ItemQuality.Soft -> "软面"
        ItemQuality.Sticky -> "粘面"
        ItemQuality.Burnt -> "糊面"
    }

    /** 好面说明（UI 展示）。 */
    const val QUALITY_HINT: String = "好面=出锅后在宽限内捞走；黄区变软、红区更差"

    fun qualityMultiplier(q: ItemQuality): Float = when (q) {
        ItemQuality.Good -> 1f
        ItemQuality.Soft -> 0.6f
        ItemQuality.Sticky -> 0.7f
        ItemQuality.Burnt -> 0.3f
    }

    /**
     * @param readyMs Working→Ready 的时刻（满格出锅）
     * @param goodUntilMs 仍算好面的截止（ready + hold）
     */
    fun qualityFromElapsed(
        readyMs: Long,
        goodUntilMs: Long,
        redEnd: Long,
        burntEnd: Long,
        elapsed: Long,
    ): ItemQuality {
        val goodCap = goodUntilMs.coerceAtLeast(readyMs)
        return when {
            elapsed <= goodCap -> ItemQuality.Good
            elapsed <= redEnd -> ItemQuality.Soft
            elapsed <= burntEnd -> ItemQuality.Sticky
            else -> ItemQuality.Burnt
        }
    }

    fun toppingsLine(toppings: Set<IngredientId>): String =
        if (toppings.isEmpty()) "配料：无"
        else "配料：" + toppings.joinToString("+") { displayName(it) }

    /**
     * 主料（芝麻酱）缺失 → 拒收；
     * 主料齐、次要缺 → 可交，奖励约半价、满意度打折；
     * 全齐 → 全价，再乘面质系数。
     */
    fun judgeServe(
        order: Order,
        toppings: Set<IngredientId>,
        quality: ItemQuality,
    ): ServeVerdict {
        val requiredMain = order.required.intersect(MAIN_INGREDIENTS)
        if (requiredMain.any { it !in toppings }) {
            return ServeVerdict.Reject
        }
        val qMul = qualityMultiplier(quality)
        val missingSecondary = order.required
            .filterNot { it in MAIN_INGREDIENTS }
            .filterNot { it in toppings }
        return if (missingSecondary.isEmpty()) {
            ServeVerdict.Accept(
                reward = (order.reward * qMul).roundToInt().coerceAtLeast(1),
                satFactor = 0.85f * qMul + 0.15f,
            )
        } else {
            ServeVerdict.Accept(
                reward = (order.reward * 0.5f * qMul).roundToInt().coerceAtLeast(1),
                satFactor = 0.4f * qMul,
            )
        }
    }
}
