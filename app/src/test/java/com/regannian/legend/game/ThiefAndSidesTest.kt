package com.regannian.legend.game

import com.regannian.legend.game.config.CustomerSpawnConfig
import com.regannian.legend.game.config.DayConfig
import com.regannian.legend.game.intent.GameIntent
import com.regannian.legend.game.model.BowlState
import com.regannian.legend.game.model.IngredientId
import com.regannian.legend.game.model.ItemQuality
import com.regannian.legend.game.model.SideId
import com.regannian.legend.game.model.SideStationState
import com.regannian.legend.game.model.ThiefState
import com.regannian.legend.game.model.ThiefTarget
import com.regannian.legend.game.reducer.GameReducer
import com.regannian.legend.game.state.GameState
import com.regannian.legend.game.tick.TickEngine
import com.regannian.legend.game.upgrade.UpgradeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThiefAndSidesTest {

    private val tick = TickEngine()

    private fun day8(upgrades: Set<UpgradeId> = emptySet()): GameState =
        GameState.initialForDay(
            DayConfig(
                dayId = 8,
                durationSec = 100,
                customerSpawn = CustomerSpawnConfig(intervalMs = 99_000L, maxSlots = 3),
                recipePool = listOf("classic"),
                toppingBar = listOf("SesameSauce", "SesameOil", "Scallion"),
                initialFilled = 1,
            ),
            upgrades = upgrades,
        )

    @Test
    fun thief_spawns_after_cooldown_and_f5_one_tap() {
        var state = day8(setOf(UpgradeId.F5)).copy(thiefCooldownMs = 0L, coins = 20)
        state = tick.advance(state, GrayboxConfig.TICK_STEP_MS).state
        assertTrue(state.thief is ThiefState.Present)
        val present = state.thief as ThiefState.Present
        assertEquals(1, present.tapsNeeded)

        state = GameReducer.reduce(state, GameIntent.TapThief).state
        assertTrue(state.thief is ThiefState.Absent)
    }

    @Test
    fun thief_steals_bowl_if_not_tapped() {
        var state = day8()
        val bowlId = state.firstEmptyBowlId()!!
        state = state.copy(
            thiefCooldownMs = 0L,
            bowls = state.bowls + (
                bowlId to BowlState.ReadyToServe(
                    quality = ItemQuality.Good,
                    toppings = setOf(IngredientId.SesameSauce),
                )
                ),
        )
        state = tick.advance(state, GrayboxConfig.TICK_STEP_MS).state
        assertTrue(state.thief is ThiefState.Present)

        state = tick.advance(state, GrayboxConfig.THIEF_ESCAPE_MS + 200L).state
        assertTrue(state.thief is ThiefState.Absent)
        assertTrue(state.bowls[bowlId] is BowlState.Empty)
    }

    @Test
    fun soy_side_cook_and_drag_to_bowl() {
        var state = day8(setOf(UpgradeId.E1))
        assertTrue(SideId.Soy in state.sideStations)
        state = GameReducer.reduce(state, GameIntent.TapSide(SideId.Soy)).state
        assertTrue(state.sideStations[SideId.Soy] is SideStationState.Working)
        state = GameReducer.reduce(state, GameIntent.TapSide(SideId.Soy)).state
        assertTrue(state.sideStations[SideId.Soy] is SideStationState.Ready)

        val bowlId = state.firstEmptyBowlId()!!
        state = state.copy(
            bowls = state.bowls + (bowlId to BowlState.Assembling()),
            focusedBowlId = bowlId,
        )
        state = GameReducer.reduce(state, GameIntent.DragSideToBowl(SideId.Soy, bowlId)).state
        val bowl = state.bowls[bowlId]
        val toppings = when (bowl) {
            is BowlState.Assembling -> bowl.toppings
            is BowlState.ReadyToServe -> bowl.toppings
            else -> emptySet()
        }
        assertTrue(IngredientId.SoyMilk in toppings)
        assertTrue(state.sideStations[SideId.Soy] is SideStationState.Idle)
    }

    @Test
    fun mianwo_burns_without_e4() {
        var state = day8(setOf(UpgradeId.E3))
        state = state.copy(
            sideStations = state.sideStations + (
                SideId.MianWo to SideStationState.Ready(ItemQuality.Good, 0L)
                ),
        )
        state = tick.advance(state, 4_100L).state
        val ready = state.sideStations[SideId.MianWo] as SideStationState.Ready
        assertEquals(ItemQuality.Burnt, ready.quality)
    }

    @Test
    fun e1_augments_recipe_pool() {
        val state = day8(setOf(UpgradeId.E1))
        assertTrue(state.recipePool.any { IngredientId.SoyMilk in it.required })
    }
}
