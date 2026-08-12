package com.regannian.legend.game

import com.regannian.legend.game.intent.GameIntent
import com.regannian.legend.game.model.IngredientId
import com.regannian.legend.game.model.StationId
import com.regannian.legend.game.reducer.GameEffect
import com.regannian.legend.game.reducer.GameReducer
import com.regannian.legend.game.reducer.SfxId
import com.regannian.legend.game.state.GameState
import com.regannian.legend.game.tick.TickEngine
import org.junit.Assert.assertTrue
import org.junit.Test

class SfxEffectTest {

    private val tick = TickEngine()

    @Test
    fun tap_dust_emits_click_sfx() {
        val t = GameReducer.reduce(
            GameState.initialGraybox(),
            GameIntent.TapStation(StationId.DustCook),
        )
        assertTrue(t.effects.any { it is GameEffect.PlaySfx && it.id == SfxId.Click })
    }

    @Test
    fun serve_emits_serve_sfx() {
        var state = GameState.initialGraybox()
        state = GameReducer.reduce(state, GameIntent.TapStation(StationId.DustCook)).state
        state = tick.advance(state, GrayboxConfig.DUST_COOK_MS).state
        state = GameReducer.reduce(state, GameIntent.DragCoolToBlanch()).state
        state = tick.advance(state, GrayboxConfig.BLANCH_MS).state
        val bowlId = state.firstEmptyBowlId()!!
        state = GameReducer.reduce(state, GameIntent.DragHotToBowl(bowlId)).state
        GrayboxConfig.CLASSIC.required.forEach { id ->
            state = GameReducer.reduce(state, GameIntent.DragIngredient(id, bowlId)).state
        }
        val t = GameReducer.reduce(
            state,
            GameIntent.DragServe(bowlId, state.customers[0].id),
        )
        assertTrue(t.effects.any { it is GameEffect.PlaySfx && it.id == SfxId.Serve })
        // 避免未使用警告
        assertTrue(IngredientId.SesameSauce in GrayboxConfig.CLASSIC.required)
    }

    @Test
    fun leave_emits_leave_sfx() {
        val t = tick.advance(GameState.initialGraybox(), GrayboxConfig.DEFAULT_PATIENCE_MS)
        assertTrue(t.effects.any { it is GameEffect.PlaySfx && it.id == SfxId.Leave })
    }
}
