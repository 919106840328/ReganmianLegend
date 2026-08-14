package com.regannian.legend.game

import com.regannian.legend.game.intent.GameIntent
import com.regannian.legend.game.model.BowlState
import com.regannian.legend.game.model.ItemQuality
import com.regannian.legend.game.model.StationId
import com.regannian.legend.game.model.StationState
import com.regannian.legend.game.reducer.GameEffect
import com.regannian.legend.game.reducer.GameReducer
import com.regannian.legend.game.state.GameState
import com.regannian.legend.game.tick.TickEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 煮面/烫面须等满格自动 Ready，再点运烫/入碗；煮着时点锅不提前出锅。 */
class TapToFinishTest {

    private val tick = TickEngine()

    private fun reduce(state: GameState, intent: GameIntent) =
        GameReducer.reduce(state, intent)

    @Test
    fun tap_dust_while_working_does_not_finish_early() {
        var state = GameState.initialGraybox()
        state = reduce(state, GameIntent.TapStation(StationId.DustCook)).state
        assertTrue(state.stations[StationId.DustCook] is StationState.Working)

        state = tick.advance(state, 400L).state
        val t = reduce(state, GameIntent.TapStation(StationId.DustCook))
        assertTrue(t.state.stations[StationId.DustCook] is StationState.Working)
        assertTrue(t.effects.any { it is GameEffect.Toast })
    }

    @Test
    fun dust_auto_ready_then_tap_to_blanch() {
        var state = GameState.initialGraybox()
        state = reduce(state, GameIntent.TapStation(StationId.DustCook)).state
        state = tick.advance(state, GrayboxConfig.DUST_COOK_MS).state
        val dustReady = state.stations[StationId.DustCook] as StationState.Ready
        assertEquals(ItemQuality.Good, dustReady.quality)

        state = reduce(state, GameIntent.TapStation(StationId.DustCook)).state
        assertTrue(state.stations[StationId.DustCook] is StationState.Idle)
        assertTrue(state.stations[StationId.Blanch] is StationState.Working)
    }

    @Test
    fun blanch_auto_ready_then_tap_into_bowl() {
        var state = GameState.initialGraybox()
        state = reduce(state, GameIntent.TapStation(StationId.DustCook)).state
        state = tick.advance(state, GrayboxConfig.DUST_COOK_MS).state
        state = reduce(state, GameIntent.TapStation(StationId.DustCook)).state
        assertTrue(state.stations[StationId.Blanch] is StationState.Working)

        state = tick.advance(state, GrayboxConfig.BLANCH_MS).state
        assertTrue(state.stations[StationId.Blanch] is StationState.Ready)

        state = reduce(state, GameIntent.TapStation(StationId.Blanch)).state
        assertTrue(state.stations[StationId.Blanch] is StationState.Idle)
        assertTrue(state.bowls.values.any { it is BowlState.Assembling })
    }
}
