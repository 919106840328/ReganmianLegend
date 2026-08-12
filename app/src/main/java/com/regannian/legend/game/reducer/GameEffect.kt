package com.regannian.legend.game.reducer

/**
 * 一次性副作用：UI 层消费（音效等），不回写 GameState。
 */
sealed interface GameEffect {
    data class Toast(val message: String) : GameEffect
    data object ServedOk : GameEffect
    data object RejectServe : GameEffect
    data class PlaySfx(val id: SfxId) : GameEffect
}

enum class SfxId {
    Click,
    Serve,
    Leave,
}
