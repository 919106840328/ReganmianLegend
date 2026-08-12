package com.regannian.legend.data.levels

import android.content.Context
import com.regannian.legend.game.config.DayConfig
import com.regannian.legend.game.config.DayConfigParser

/**
 * 从 assets/levels/day_XX.json 读关卡；纯解析在 [DayConfigParser]。
 */
class LevelRepository(
    private val openDayJson: (dayId: Int) -> String,
) {
    fun load(dayId: Int): DayConfig {
        require(dayId in 1..MAX_DAY) { "dayId 须在 1..$MAX_DAY" }
        return DayConfigParser.parse(openDayJson(dayId))
    }

    companion object {
        const val MAX_DAY: Int = 10

        fun fromAssets(context: Context): LevelRepository {
            val assets = context.applicationContext.assets
            return LevelRepository { dayId ->
                val path = "levels/day_%02d.json".format(dayId)
                assets.open(path).bufferedReader().use { it.readText() }
            }
        }
    }
}
