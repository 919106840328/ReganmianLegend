package com.regannian.legend.game

import com.regannian.legend.game.config.CustomerSpawnConfig
import com.regannian.legend.game.config.DayConfig
import com.regannian.legend.game.config.DayConfigParser
import com.regannian.legend.game.config.RecipeCatalog
import com.regannian.legend.game.model.CustomerStatus
import com.regannian.legend.game.model.IngredientId
import com.regannian.legend.game.state.GameState
import com.regannian.legend.game.state.starCount
import com.regannian.legend.game.tick.TickEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DayConfigAndSpawnTest {

    private val tick = TickEngine()

    @Test
    fun parse_day01_json_fields() {
        val json = """
            {
              "dayId": 1,
              "durationSec": 100,
              "customerSpawn": { "intervalMs": 8000, "maxSlots": 3 },
              "patienceMul": 1.2,
              "recipePool": ["classic"],
              "toppingBar": ["SesameSauce", "SesameOil", "Scallion"],
              "initialFilled": 1,
              "targetSatisfaction": { "star1": 0.45, "star2": 0.65, "star3": 0.85 }
            }
        """.trimIndent()
        val config = DayConfigParser.parse(json)
        assertEquals(1, config.dayId)
        assertEquals(8_000L, config.spawnIntervalMs)
        assertEquals(1, config.initialFilled)
        assertEquals(listOf(IngredientId.SesameSauce, IngredientId.SesameOil, IngredientId.Scallion),
            RecipeCatalog.toppings(config.toppingBar))
    }

    @Test
    fun shorter_spawn_interval_fills_empty_seats_faster() {
        val slow = DayConfig(
            dayId = 1,
            durationSec = 100,
            customerSpawn = CustomerSpawnConfig(intervalMs = 8_000L, maxSlots = 3),
            recipePool = listOf("classic"),
            toppingBar = listOf("SesameSauce", "SesameOil", "Scallion"),
            initialFilled = 1,
        )
        val fast = slow.copy(
            dayId = 10,
            customerSpawn = CustomerSpawnConfig(intervalMs = 2_000L, maxSlots = 3),
        )
        var slowState = GameState.initialForDay(slow)
        var fastState = GameState.initialForDay(fast)
        assertEquals(1, slowState.customers.count { it.status == CustomerStatus.WaitingServe })

        slowState = tick.advance(slowState, 4_000L).state
        fastState = tick.advance(fastState, 4_000L).state

        assertEquals(1, slowState.customers.count { it.status == CustomerStatus.WaitingServe })
        assertEquals(3, fastState.customers.count { it.status == CustomerStatus.WaitingServe })
    }

    @Test
    fun star_thresholds_from_config() {
        val config = DayConfig(dayId = 1)
        assertEquals(0, starCount(0.2f, config.targetSatisfaction))
        assertEquals(1, starCount(0.5f, config.targetSatisfaction))
        assertEquals(2, starCount(0.7f, config.targetSatisfaction))
        assertEquals(3, starCount(0.9f, config.targetSatisfaction))
    }

    @Test
    fun day10_has_denser_spawn_than_day1_when_parsed() {
        val day1 = DayConfigParser.parse(
            """{"dayId":1,"customerSpawn":{"intervalMs":8000,"maxSlots":3},"recipePool":["classic"],"toppingBar":["SesameSauce"],"initialFilled":1}""",
        )
        val day10 = DayConfigParser.parse(
            """{"dayId":10,"customerSpawn":{"intervalMs":3000,"maxSlots":3},"recipePool":["classic","chili","bean"],"toppingBar":["SesameSauce","ChiliOil"],"initialFilled":3}""",
        )
        assertTrue(day10.spawnIntervalMs < day1.spawnIntervalMs)
        assertEquals(3, day10.recipePool.size)
    }
}
