package com.regannian.legend.game.state

/** 当日事件标记，供成就结算（不进 UI 计算）。 */
data class DayStats(
    val servedUncle: Boolean = false,
    val chasedThief: Boolean = false,
    val discarded: Boolean = false,
    val servedGood: Boolean = false,
)
