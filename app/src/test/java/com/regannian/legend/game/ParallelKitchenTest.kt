package com.regannian.legend.game

import com.regannian.legend.game.intent.GameIntent
import com.regannian.legend.game.model.CustomerStatus
import com.regannian.legend.game.model.StationId
import com.regannian.legend.game.model.StationState
import com.regannian.legend.game.reducer.GameReducer
import com.regannian.legend.game.state.GameState
import com.regannian.legend.game.tick.TickEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParallelKitchenTest {

    private val tick = TickEngine()

    private fun reduce(state: GameState, intent: GameIntent): GameState =
        GameReducer.reduce(state, intent).state

    @Test
    fun dust_and_blanch_can_overlap_via_drag() {
        var state = GameState.initialGraybox()
        state = reduce(state, GameIntent.TapStation(StationId.DustCook))
        state = tick.advance(state, GrayboxConfig.DUST_COOK_MS).state
        state = reduce(state, GameIntent.DragCoolToBlanch())
        assertTrue(state.stations[StationId.Blanch] is StationState.Working)

        // 烫面同时再开掸面
        state = reduce(state, GameIntent.TapStation(StationId.DustCook))
        assertTrue(state.stations[StationId.DustCook] is StationState.Working)
        assertTrue(state.stations[StationId.Blanch] is StationState.Working)
    }

    @Test
    fun patience_exhausted_leaves_with_sat_zero() {
        var state = GameState.initialGraybox()
        state = tick.advance(state, GrayboxConfig.DEFAULT_PATIENCE_MS).state
        assertTrue(state.customers.all { it.status == CustomerStatus.LeftAngry })
        assertEquals(3, state.daySatCount)
        assertEquals(0f, state.daySatSum, 0.001f)
    }
}
