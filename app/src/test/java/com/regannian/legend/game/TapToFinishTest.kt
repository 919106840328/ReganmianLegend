package com.regannian.legend.game

import com.regannian.legend.game.intent.GameIntent
import com.regannian.legend.game.model.BowlState
import com.regannian.legend.game.model.ItemQuality
import com.regannian.legend.game.model.StationId
import com.regannian.legend.game.model.StationState
import com.regannian.legend.game.reducer.GameReducer
import com.regannian.legend.game.state.GameState
import com.regannian.legend.game.tick.TickEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 点一下停掸/停烫，再点运烫/入碗。 */
class TapToFinishTest {

    private val tick = TickEngine()

    private fun reduce(state: GameState, intent: GameIntent): GameState =
        GameReducer.reduce(state, intent).state

    @Test
    fun tap_dust_working_finishes_ready_then_tap_to_blanch() {
        var state = GameState.initialGraybox()
        state = reduce(state, GameIntent.TapStation(StationId.DustCook))
        assertTrue(state.stations[StationId.DustCook] is StationState.Working)

        state = tick.advance(state, 400L).state
        state = reduce(state, GameIntent.TapStation(StationId.DustCook))
        val dustReady = state.stations[StationId.DustCook] as StationState.Ready
        assertEquals(ItemQuality.Good, dustReady.quality)

        state = reduce(state, GameIntent.TapStation(StationId.DustCook))
        assertTrue(state.stations[StationId.DustCook] is StationState.Idle)
        assertTrue(state.stations[StationId.Blanch] is StationState.Working)
    }

    @Test
    fun tap_blanch_working_finishes_then_tap_into_bowl() {
        var state = GameState.initialGraybox()
        state = reduce(state, GameIntent.TapStation(StationId.DustCook))
        state = reduce(state, GameIntent.TapStation(StationId.DustCook)) // 提前出锅
        state = reduce(state, GameIntent.TapStation(StationId.DustCook)) // 送烫
        assertTrue(state.stations[StationId.Blanch] is StationState.Working)

        state = tick.advance(state, 200L).state
        state = reduce(state, GameIntent.TapStation(StationId.Blanch))
        assertTrue(state.stations[StationId.Blanch] is StationState.Ready)

        state = reduce(state, GameIntent.TapStation(StationId.Blanch))
        assertTrue(state.stations[StationId.Blanch] is StationState.Idle)
        assertTrue(state.bowls.values.any { it is BowlState.Assembling })
    }
}
