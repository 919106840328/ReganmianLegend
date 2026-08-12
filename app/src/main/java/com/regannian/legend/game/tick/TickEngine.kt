package com.regannian.legend.game.tick

import com.regannian.legend.game.GrayboxConfig
import com.regannian.legend.game.intent.GameIntent
import com.regannian.legend.game.reducer.GameEffect
import com.regannian.legend.game.reducer.GameReducer
import com.regannian.legend.game.reducer.Transition
import com.regannian.legend.game.state.GameState

/**
 * 固定步长逻辑时钟（默认 100ms）。
 * A2 提供可测推进；A4 由 ViewModel 用协程 delay 驱动。
 */
class TickEngine(
    private val stepMs: Long = GrayboxConfig.TICK_STEP_MS,
) {
    /**
     * 将墙钟 [deltaMs] 折算为若干逻辑 Tick 并依次 reduce。
     */
    fun advance(state: GameState, deltaMs: Long): Transition {
        if (deltaMs <= 0L) return Transition.none(state)

        var current = state
        val effects = mutableListOf<GameEffect>()
        var elapsed = 0L
        while (elapsed + stepMs <= deltaMs) {
            val t = GameReducer.reduce(current, GameIntent.Tick)
            current = t.state
            effects += t.effects
            elapsed += stepMs
        }
        return Transition(current, effects)
    }

    /** 快进恰好 [ticks] 个逻辑步（单测友好）。 */
    fun advanceTicks(state: GameState, ticks: Int): Transition {
        require(ticks >= 0)
        var current = state
        val effects = mutableListOf<GameEffect>()
        repeat(ticks) {
            val t = GameReducer.reduce(current, GameIntent.Tick)
            current = t.state
            effects += t.effects
        }
        return Transition(current, effects)
    }
}
