package com.regannian.legend.game

import com.regannian.legend.data.progress.PlayerProgress
import com.regannian.legend.data.save.SaveSnapshot
import com.regannian.legend.game.config.CustomerSpawnConfig
import com.regannian.legend.game.config.DayConfig
import com.regannian.legend.game.intent.GameIntent
import com.regannian.legend.game.model.BowlState
import com.regannian.legend.game.model.CustomerKind
import com.regannian.legend.game.model.CustomerStatus
import com.regannian.legend.game.model.IngredientId
import com.regannian.legend.game.reducer.GameReducer
import com.regannian.legend.game.state.GameState
import com.regannian.legend.game.upgrade.UpgradeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UncleAndSaveTest {

    @Before
    fun reset() {
        PlayerProgress.resetForTest()
    }

    private fun day10State(): GameState = GameState.initialForDay(
        DayConfig(
            dayId = 10,
            durationSec = 100,
            customerSpawn = CustomerSpawnConfig(intervalMs = 99_000L, maxSlots = 3),
            recipePool = listOf("classic"),
            toppingBar = listOf("SesameSauce", "SesameOil", "Scallion"),
            initialFilled = 3,
        ),
    )

    @Test
    fun day10_seat0_is_uncle_wanting_plain() {
        val state = day10State()
        val uncle = state.customers[0]
        assertEquals(CustomerKind.EarlyMarketUncle, uncle.kind)
        assertEquals(setOf(IngredientId.SesameSauce), uncle.order.required)
    }

    @Test
    fun uncle_serve_then_tap_collects_tip() {
        var state = day10State()
        val uncle = state.customers[0]
        val bowlId = state.firstEmptyBowlId()!!
        state = state.copy(
            bowls = state.bowls + (
                bowlId to BowlState.ReadyToServe(
                    quality = com.regannian.legend.game.model.ItemQuality.Good,
                    toppings = setOf(IngredientId.SesameSauce),
                )
                ),
            focusedBowlId = bowlId,
        )
        state = GameReducer.reduce(state, GameIntent.DragServe(bowlId, uncle.id)).state
        assertEquals(CustomerStatus.WaitingInteract, state.customers[0].status)
        assertTrue(state.coinsOnCounter.isEmpty())

        state = GameReducer.reduce(state, GameIntent.TapCustomer(uncle.id)).state
        assertEquals(CustomerStatus.Served, state.customers[0].status)
        assertEquals(GrayboxConfig.UNCLE_TIP_REWARD, state.coinsOnCounter.first().amount)
    }

    @Test
    fun day_earnings_advance_next_day_and_persist_callback() {
        var saved: SaveSnapshot? = null
        PlayerProgress.resetForTest(coins = 10, nextDayId = 1)
        PlayerProgress.attachSaver { saved = it }
        PlayerProgress.addDayEarnings(coins = 20, completedDayId = 3)
        assertEquals(30, PlayerProgress.walletCoins.value)
        assertEquals(4, PlayerProgress.nextDayId.value)
        assertEquals(30, saved?.walletCoins)
        assertEquals(4, saved?.nextDayId)
    }

    @Test
    fun buy_upgrade_persists() {
        var saved: SaveSnapshot? = null
        PlayerProgress.resetForTest(coins = 500)
        PlayerProgress.attachSaver { saved = it }
        assertEquals(null, PlayerProgress.tryBuy(UpgradeId.B1))
        assertTrue(UpgradeId.B1 in (saved?.upgrades ?: emptySet()))
    }
}
