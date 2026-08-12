package com.regannian.legend.game.upgrade

import com.regannian.legend.game.GrayboxConfig
import com.regannian.legend.game.model.BowlId
import com.regannian.legend.game.model.IngredientId
import com.regannian.legend.game.model.Order
import com.regannian.legend.game.model.RecipeId
import com.regannian.legend.game.model.SideId

/**
 * 由已购升级推导的当日规则（纯函数）。
 */
object UpgradeEffects {
    fun bowlCount(upgrades: Set<UpgradeId>): Int =
        if (UpgradeId.A1 in upgrades) 4 else 3

    fun bowlIds(upgrades: Set<UpgradeId>): List<BowlId> =
        (1..bowlCount(upgrades)).map { BowlId("bowl-$it") }

    fun seatBonus(upgrades: Set<UpgradeId>): Int {
        var bonus = 0
        if (UpgradeId.F1 in upgrades) bonus += 1
        if (UpgradeId.F2 in upgrades) bonus += 1
        return bonus
    }

    fun priceMul(upgrades: Set<UpgradeId>): Float = when {
        UpgradeId.F4 in upgrades -> 1.3f
        UpgradeId.F3 in upgrades -> 1.15f
        else -> 1f
    }

    fun autoDustIntervalMs(upgrades: Set<UpgradeId>): Long? = when {
        UpgradeId.A7 in upgrades -> 1_600L
        UpgradeId.A6 in upgrades -> 2_500L
        else -> null
    }

    fun mergeToppingBar(
        base: List<IngredientId>,
        upgrades: Set<UpgradeId>,
    ): List<IngredientId> {
        val extra = buildList {
            if (UpgradeId.B4 in upgrades || UpgradeId.D5 in upgrades) add(IngredientId.ChiliOil)
            if (UpgradeId.D4 in upgrades) add(IngredientId.PickledBean)
            if (UpgradeId.D1 in upgrades) add(IngredientId.Brine)
        }
        return (base + extra).distinct()
    }

    fun unlockedSides(upgrades: Set<UpgradeId>): List<SideId> = buildList {
        if (UpgradeId.E1 in upgrades) add(SideId.Soy)
        if (UpgradeId.E3 in upgrades) add(SideId.MianWo)
        if (UpgradeId.E5 in upgrades) add(SideId.EggWine)
    }

    fun sideIngredient(side: SideId): IngredientId = when (side) {
        SideId.Soy -> IngredientId.SoyMilk
        SideId.MianWo -> IngredientId.MianWo
        SideId.EggWine -> IngredientId.EggWine
    }

    /** 拥有辅食升级时，往配方池追加带辅食的变体。 */
    fun augmentRecipePool(
        base: List<Order>,
        upgrades: Set<UpgradeId>,
    ): List<Order> {
        val extra = buildList {
            if (UpgradeId.E1 in upgrades) {
                add(
                    Order(
                        recipeId = RecipeId("classic_soy"),
                        required = GrayboxConfig.CLASSIC.required + IngredientId.SoyMilk,
                        reward = GrayboxConfig.CLASSIC.reward + 3,
                    ),
                )
            }
            if (UpgradeId.E3 in upgrades) {
                add(
                    Order(
                        recipeId = RecipeId("chili_mianwo"),
                        required = GrayboxConfig.CHILI.required + IngredientId.MianWo,
                        reward = GrayboxConfig.CHILI.reward + 4,
                    ),
                )
            }
            if (UpgradeId.E5 in upgrades) {
                add(
                    Order(
                        recipeId = RecipeId("bean_egg"),
                        required = GrayboxConfig.BEAN.required + IngredientId.EggWine,
                        reward = GrayboxConfig.BEAN.reward + 5,
                    ),
                )
            }
            if (UpgradeId.E6 in upgrades && base.isNotEmpty()) {
                // DIY：少要香葱但涨价
                val src = base.first()
                add(
                    src.copy(
                        recipeId = RecipeId("${src.recipeId.value}_diy"),
                        required = src.required - IngredientId.Scallion,
                        reward = src.reward + 4,
                    ),
                )
            }
        }
        return base + extra
    }

    fun scaledReward(base: Int, upgrades: Set<UpgradeId>): Int =
        (base * priceMul(upgrades)).toInt().coerceAtLeast(1)
}
