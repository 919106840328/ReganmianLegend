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
import com.regannian.legend.game.upgrade.UpgradeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 兼容旧 C4 测试：ID 已迁到 A2/B1/D3。 */
class UpgradeC4Test {

    @Before
    fun reset() {
        PlayerProgress.resetForTest()
    }

    @Test
    fun fierce_stove_shortens_dust_cook() {
        val base = StationTimings.from(emptySet())
        val boosted = StationTimings.from(setOf(UpgradeId.A2))
        assertTrue(boosted.dustCookMs < base.dustCookMs)
        assertTrue(boosted.blanchMs < base.blanchMs)
    }

    @Test
    fun wide_green_extends_red_window() {
        val base = StationTimings.from(emptySet())
        val wide = StationTimings.from(setOf(UpgradeId.B1))
        assertTrue(wide.dustRedMs - wide.dustCookMs > base.dustRedMs - base.dustCookMs)
    }

    @Test
    fun buy_upgrade_spends_wallet() {
        PlayerProgress.resetForTest(coins = 500)
        assertEquals(null, PlayerProgress.tryBuy(UpgradeId.B1))
        assertTrue(PlayerProgress.owns(UpgradeId.B1))
        assertEquals(250, PlayerProgress.walletCoins.value)
        assertEquals("已拥有", PlayerProgress.tryBuy(UpgradeId.B1))
    }

    @Test
    fun classic_combo_adds_toppings_to_focused_bowl() {
        var state = GameState.initialGraybox(upgrades = setOf(UpgradeId.D3))
        val bowlId = state.firstEmptyBowlId()!!
        state = state.copy(
            bowls = state.bowls + (bowlId to BowlState.Assembling()),
            focusedBowlId = bowlId,
        )
        state = GameReducer.reduce(state, GameIntent.ApplyClassicCombo).state
        val bowl = state.bowls[bowlId] as BowlState
        val toppings = when (bowl) {
            is BowlState.Assembling -> bowl.toppings
            is BowlState.ReadyToServe -> bowl.toppings
            else -> emptySet()
        }
        assertTrue(IngredientId.SesameSauce in toppings)
        assertTrue(IngredientId.Scallion in toppings)
    }

    @Test
    fun dust_working_uses_upgrade_timing() {
        val state = GameState.initialGraybox(upgrades = setOf(UpgradeId.A2))
        val after = GameReducer.reduce(state, GameIntent.TapStation(StationId.DustCook)).state
        val working = after.stations[StationId.DustCook] as StationState.Working
        assertEquals(state.timings.dustCookMs, working.totalMs)
        assertNotNull(working)
    }
}
