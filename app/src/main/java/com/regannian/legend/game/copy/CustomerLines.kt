package com.regannian.legend.game.copy

import com.regannian.legend.game.model.CustomerStatus

/**
 * 客人台词（武汉口语可再改）。情绪阈值只给 UI 用，不改金币/耐心数值。
 */
object CustomerLines {
    const val HOPE: String = "来碗热干面撒"
    const val IMPATIENT: String = "搞莫斯"
    const val FED_UP: String = "信了你的邪"
    const val HAPPY: String = "面来咯！"
    const val RESIGNED: String = "蒜了蒜了都不容易"
    const val UNCLE_WAIT_PAY: String = "拍桌子，算账！"

    fun waitingLine(patiencePct: Int): String = when {
        patiencePct > 62 -> HOPE
        patiencePct > 32 -> IMPATIENT
        else -> FED_UP
    }

    fun serveLine(good: Boolean): String = if (good) HAPPY else RESIGNED

    fun moodOf(status: CustomerStatus, patiencePct: Int, bark: String?): SeatMood = when (status) {
        CustomerStatus.Empty -> SeatMood.Empty
        CustomerStatus.WaitingInteract -> SeatMood.WaitPay
        CustomerStatus.Served ->
            if (bark == RESIGNED) SeatMood.Resigned else SeatMood.Happy
        CustomerStatus.LeftAngry -> SeatMood.Angry
        CustomerStatus.WaitingServe -> when {
            patiencePct > 62 -> SeatMood.Hopeful
            patiencePct > 32 -> SeatMood.Impatient
            else -> SeatMood.FedUp
        }
    }
}

enum class SeatMood {
    Empty,
    Hopeful,
    Impatient,
    FedUp,
    Happy,
    Resigned,
    Angry,
    WaitPay,
}
