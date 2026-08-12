package com.regannian.legend.game.reducer

import com.regannian.legend.game.state.GameState

data class Transition(
    val state: GameState,
    val effects: List<GameEffect> = emptyList(),
) {
    companion object {
        fun none(state: GameState): Transition = Transition(state)
    }
}
