package com.regannian.legend.game

import com.regannian.legend.game.intent.GameIntent
import com.regannian.legend.game.model.BowlState
import com.regannian.legend.game.model.CustomerStatus
import com.regannian.legend.game.model.IngredientId
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

class AttentionAndOrderRulesTest {

    private val tick = TickEngine()

    private fun reduce(state: GameState, intent: GameIntent): GameState =
        GameReducer.reduce(state, intent).state

    private fun toReadyDust(state: GameState): GameState {
        var s = reduce(state, GameIntent.TapStation(StationId.DustCook))
        s = tick.advance(s, GrayboxConfig.DUST_COOK_MS).state
        val dust = s.stations.getValue(StationId.DustCook)
        assertTrue(dust is StationState.Ready)
        assertEquals(ItemQuality.Good, (dust as StationState.Ready).quality)
        return s
    }

    @Test
    fun good_hold_keeps_good_quality() {
        var state = toReadyDust(GameState.initialGraybox())
        // 出锅后仍在好面宽限内
        state = tick.advance(state, GrayboxConfig.DUST_GOOD_HOLD_MS - 200L).state
        val dust = state.stations.getValue(StationId.DustCook) as StationState.Ready
        assertEquals(ItemQuality.Good, dust.quality)
    }

    @Test
    fun overcook_dust_becomes_soft() {
        var state = toReadyDust(GameState.initialGraybox())
        // 越过好面宽限、未出红区终点 → Soft
        state = tick.advance(state, GrayboxConfig.DUST_GOOD_HOLD_MS + 200L).state
        val dust = state.stations.getValue(StationId.DustCook) as StationState.Ready
        assertEquals(ItemQuality.Soft, dust.quality)
    }

    @Test
    fun soft_quality_lowers_reward() {
        var state = toReadyDust(GameState.initialGraybox())
        state = tick.advance(state, GrayboxConfig.DUST_GOOD_HOLD_MS + 200L).state
        state = reduce(state, GameIntent.DragCoolToBlanch())
        state = tick.advance(state, GrayboxConfig.BLANCH_MS).state
        val bowlId = state.firstEmptyBowlId()!!
        state = reduce(state, GameIntent.DragHotToBowl(bowlId))
        GrayboxConfig.CLASSIC.required.forEach {
            state = reduce(state, GameIntent.DragIngredient(it, bowlId))
        }
        val t = GameReducer.reduce(
            state,
            GameIntent.DragServe(bowlId, state.customers[0].id),
        )
        assertTrue(t.effects.contains(GameEffect.ServedOk))
        val reward = t.state.coinsOnCounter.first().amount
        assertTrue(reward < GrayboxConfig.CLASSIC.reward)
        assertTrue(reward >= 1)
    }

    @Test
    fun missing_sesame_sauce_rejected() {
        var state = toReadyDust(GameState.initialGraybox())
        state = reduce(state, GameIntent.DragCoolToBlanch())
        state = tick.advance(state, GrayboxConfig.BLANCH_MS).state
        val bowlId = state.firstEmptyBowlId()!!
        state = reduce(state, GameIntent.DragHotToBowl(bowlId))
        // 只有香油+葱，缺芝麻酱（主料）
        state = reduce(state, GameIntent.DragIngredient(IngredientId.SesameOil, bowlId))
        state = reduce(state, GameIntent.DragIngredient(IngredientId.Scallion, bowlId))
        val t = GameReducer.reduce(
            state,
            GameIntent.DragServe(bowlId, state.customers[0].id),
        )
        assertTrue(t.effects.contains(GameEffect.RejectServe))
        assertEquals(CustomerStatus.WaitingServe, t.state.customers[0].status)
    }

    @Test
    fun missing_secondary_accepts_at_low_price() {
        var state = toReadyDust(GameState.initialGraybox())
        state = reduce(state, GameIntent.DragCoolToBlanch())
        state = tick.advance(state, GrayboxConfig.BLANCH_MS).state
        val bowlId = state.firstEmptyBowlId()!!
        state = reduce(state, GameIntent.DragHotToBowl(bowlId))
        // 只有芝麻酱（主料齐，次要全缺）
        state = reduce(state, GameIntent.DragIngredient(IngredientId.SesameSauce, bowlId))
        assertTrue(state.bowls[bowlId] is BowlState.ReadyToServe)

        val t = GameReducer.reduce(
            state,
            GameIntent.DragServe(bowlId, state.customers[0].id),
        )
        assertTrue(t.effects.contains(GameEffect.ServedOk))
        val reward = t.state.coinsOnCounter.first().amount
        assertTrue(reward <= GrayboxConfig.CLASSIC.reward / 2 + 1)
        assertEquals(CustomerStatus.Served, t.state.customers[0].status)
    }

    @Test
    fun discard_bowl_costs_two_coins() {
        var state = GameState.initialGraybox().copy(coins = 5)
        state = toReadyDust(state)
        state = reduce(state, GameIntent.DragCoolToBlanch())
        state = tick.advance(state, GrayboxConfig.BLANCH_MS).state
        val bowlId = state.firstEmptyBowlId()!!
        state = reduce(state, GameIntent.DragHotToBowl(bowlId))
        state = reduce(state, GameIntent.DiscardBowl(bowlId))
        assertTrue(state.bowls[bowlId] is BowlState.Empty)
        assertEquals(3, state.coins)
    }
}
