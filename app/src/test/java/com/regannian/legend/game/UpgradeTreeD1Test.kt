package com.regannian.legend.game

import com.regannian.legend.data.progress.PlayerProgress
import com.regannian.legend.game.intent.GameIntent
import com.regannian.legend.game.model.BowlState
import com.regannian.legend.game.model.IngredientId
import com.regannian.legend.game.model.StationId
import com.regannian.legend.game.model.StationState
import com.regannian.legend.game.reducer.GameReducer
import com.regannian.legend.game.state.GameState
import com.regannian.legend.game.upgrade.StationTimings
import com.regannian.legend.game.upgrade.UpgradeCatalog
import com.regannian.legend.game.upgrade.UpgradeEffects
import com.regannian.legend.game.upgrade.UpgradeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpgradeTreeD1Test {

    @Before
    fun reset() {
        PlayerProgress.resetForTest()
    }

    @Test
    fun legacy_save_ids_migrate() {
        assertEquals(UpgradeId.A2, UpgradeCatalog.parseUpgradeId("FierceStove"))
        assertEquals(UpgradeId.B1, UpgradeCatalog.parseUpgradeId("WideGreen"))
        assertEquals(UpgradeId.D3, UpgradeCatalog.parseUpgradeId("ClassicCombo"))
    }

    @Test
    fun a2_shortens_cook_c2_shortens_blanch() {
        val base = StationTimings.from(emptySet())
        val a2 = StationTimings.from(setOf(UpgradeId.A2))
        val c2 = StationTimings.from(setOf(UpgradeId.C2))
        assertTrue(a2.dustCookMs < base.dustCookMs)
        assertTrue(c2.blanchMs < base.blanchMs)
    }

    @Test
    fun a1_adds_bowl_f1_adds_seat() {
        assertEquals(4, UpgradeEffects.bowlCount(setOf(UpgradeId.A1)))
        assertEquals(1, UpgradeEffects.seatBonus(setOf(UpgradeId.F1)))
        assertEquals(2, UpgradeEffects.seatBonus(setOf(UpgradeId.F1, UpgradeId.F2)))
        val state = GameState.initialGraybox(upgrades = setOf(UpgradeId.A1, UpgradeId.F1))
        assertEquals(4, state.bowls.size)
        assertEquals(4, state.customers.size)
    }

    @Test
    fun f3_scales_price() {
        assertEquals(11, UpgradeEffects.scaledReward(10, setOf(UpgradeId.F3)))
        assertEquals(13, UpgradeEffects.scaledReward(10, setOf(UpgradeId.F4)))
    }

    @Test
    fun buy_requires_prerequisite() {
        PlayerProgress.resetForTest(coins = 10_000)
        assertEquals("需先购买：自动掸面机 I", PlayerProgress.tryBuy(UpgradeId.A7))
        assertEquals(null, PlayerProgress.tryBuy(UpgradeId.A6))
        assertEquals(null, PlayerProgress.tryBuy(UpgradeId.A7))
    }

    @Test
    fun d3_combo_and_c1_queue() {
        var state = GameState.initialGraybox(upgrades = setOf(UpgradeId.D3, UpgradeId.C1))
        val bowlId = state.firstEmptyBowlId()!!
        state = state.copy(
            bowls = state.bowls + (bowlId to BowlState.Assembling()),
            focusedBowlId = bowlId,
        )
        state = GameReducer.reduce(state, GameIntent.ApplyClassicCombo).state
        val toppings = when (val bowl = state.bowls[bowlId]) {
            is BowlState.Assembling -> bowl.toppings
            is BowlState.ReadyToServe -> bowl.toppings
            else -> emptySet()
        }
        assertTrue(IngredientId.SesameSauce in toppings)

        // C1 queue: dust ready + blanch busy
        state = GameState.initialGraybox(upgrades = setOf(UpgradeId.C1))
        state = GameReducer.reduce(state, GameIntent.TapStation(StationId.DustCook)).state
        state = GameReducer.reduce(state, GameIntent.TapStation(StationId.DustCook)).state
        val blanchWorking = StationState.Working(
            remainingMs = 500,
            totalMs = 500,
            output = com.regannian.legend.game.model.StationOutput.HotNoodles,
            attention = com.regannian.legend.game.model.AttentionWindow(500, 1000, 0),
        )
        state = state.copy(
            stations = state.stations + (StationId.Blanch to blanchWorking),
        )
        state = GameReducer.reduce(state, GameIntent.DragCoolToBlanch()).state
        assertNotNull(state.pendingCoolQuality)
        assertTrue(state.stations[StationId.DustCook] is StationState.Idle)
    }

    @Test
    fun catalog_has_full_tree() {
        assertEquals(36, UpgradeCatalog.ALL.size)
    }
}
