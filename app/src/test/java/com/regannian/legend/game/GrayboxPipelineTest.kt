package com.regannian.legend.game

import com.regannian.legend.game.intent.GameIntent
import com.regannian.legend.game.model.BowlState
import com.regannian.legend.game.model.CustomerStatus
import com.regannian.legend.game.model.StationId
import com.regannian.legend.game.reducer.GameEffect
import com.regannian.legend.game.reducer.GameReducer
import com.regannian.legend.game.state.GameState
import com.regannian.legend.game.tick.TickEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GrayboxPipelineTest {

    private val tick = TickEngine()

    private fun reduce(state: GameState, intent: GameIntent): GameState =
        GameReducer.reduce(state, intent).state

    private fun hotNoodlesInBowl(state: GameState): Pair<GameState, com.regannian.legend.game.model.BowlId> {
        var s = reduce(state, GameIntent.TapStation(StationId.DustCook))
        s = tick.advance(s, GrayboxConfig.DUST_COOK_MS).state
        s = reduce(s, GameIntent.DragCoolToBlanch())
        s = tick.advance(s, GrayboxConfig.BLANCH_MS).state
        val bowlId = s.firstEmptyBowlId()!!
        s = reduce(s, GameIntent.DragHotToBowl(bowlId))
        assertTrue(s.bowls[bowlId] is BowlState.Assembling)
        return s to bowlId
    }

    @Test
    fun classic_full_order_serves() {
        var state = GameState.initialGraybox()
        val customer = state.customers[0]
        val (bowled, bowlId) = hotNoodlesInBowl(state)
        state = bowled
        customer.order.required.forEach {
            state = reduce(state, GameIntent.DragIngredient(it, bowlId))
        }
        val served = GameReducer.reduce(state, GameIntent.DragServe(bowlId, customer.id))
        assertTrue(served.effects.contains(GameEffect.ServedOk))
        assertEquals(CustomerStatus.Served, served.state.customers[0].status)
        assertEquals(customer.order.reward, served.state.coinsOnCounter.first().amount)
    }
}
