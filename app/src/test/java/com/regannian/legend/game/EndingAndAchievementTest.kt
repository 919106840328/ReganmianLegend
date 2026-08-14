package com.regannian.legend.game

import com.regannian.legend.data.progress.PlayerProgress
import com.regannian.legend.game.intent.GameIntent
import com.regannian.legend.game.meta.AchievementEvaluator
import com.regannian.legend.game.meta.AchievementId
import com.regannian.legend.game.meta.AchievementInput
import com.regannian.legend.game.meta.EndingRules
import com.regannian.legend.game.meta.ReputationRules
import com.regannian.legend.game.model.ThiefState
import com.regannian.legend.game.model.ThiefTarget
import com.regannian.legend.game.reducer.GameReducer
import com.regannian.legend.game.state.DaySummary
import com.regannian.legend.game.state.GameState
import com.regannian.legend.game.upgrade.UpgradeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EndingAndAchievementTest {

    @Before
    fun reset() {
        PlayerProgress.resetForTest()
    }

    @Test
    fun ending_requires_full_tree_and_wallet() {
        assertFalse(EndingRules.isMet(emptySet(), EndingRules.WALLET_THRESHOLD))
        val all = UpgradeId.entries.toSet()
        assertFalse(EndingRules.isMet(all, EndingRules.WALLET_THRESHOLD - 1))
        assertTrue(EndingRules.isMet(all, EndingRules.WALLET_THRESHOLD))
    }

    @Test
    fun reputation_clamps_and_patience_scales() {
        assertEquals(58, ReputationRules.apply(50, 90))
        assertEquals(0, ReputationRules.apply(5, 10))
        assertEquals(100, ReputationRules.apply(97, 90))
        assertTrue(ReputationRules.patienceMul(100) > ReputationRules.patienceMul(0))
    }

    @Test
    fun evaluator_unlocks_from_day_and_upgrades() {
        val unlocked = AchievementEvaluator.unlock(
            already = emptySet(),
            input = AchievementInput(
                upgrades = setOf(UpgradeId.A6, UpgradeId.E1),
                walletCoins = 0,
                bestDayCoins = 0,
                day = DaySummary(
                    dayId = 10,
                    coins = 120,
                    satPercent = 90,
                    orderCount = 8,
                    stars = 3,
                    servedUncle = true,
                    chasedThief = true,
                    discarded = true,
                    servedGood = true,
                ),
            ),
        )
        assertTrue(AchievementId.AUTO_DUST in unlocked)
        assertTrue(AchievementId.SOY_SIDE in unlocked)
        assertTrue(AchievementId.GOLDEN_TILL in unlocked)
        assertTrue(AchievementId.THREE_STARS in unlocked)
        assertTrue(AchievementId.UNCLE_TIP in unlocked)
        assertTrue(AchievementId.THIEF_SHOO in unlocked)
        assertTrue(AchievementId.DAY10_OPEN in unlocked)
        assertFalse(AchievementId.LEGEND in unlocked)
    }

    @Test
    fun record_day_updates_reputation_and_achievements() {
        PlayerProgress.resetForTest(coins = 10, reputation = 50)
        PlayerProgress.recordDay(
            DaySummary(dayId = 1, coins = 60, satPercent = 90, orderCount = 4, stars = 3),
        )
        assertEquals(70, PlayerProgress.walletCoins.value)
        assertEquals(58, PlayerProgress.reputation.value)
        assertTrue(AchievementId.BUSY_TILL in PlayerProgress.achievements.value)
        assertTrue(AchievementId.THREE_STARS in PlayerProgress.achievements.value)
    }

    @Test
    fun ad_bonus_once_per_day() {
        PlayerProgress.resetForTest(coins = 0)
        assertEquals(5, PlayerProgress.tryAdBonus(dayId = 2, baseCoins = 10))
        assertEquals(5, PlayerProgress.walletCoins.value)
        assertEquals(null, PlayerProgress.tryAdBonus(dayId = 2, baseCoins = 10))
    }

    @Test
    fun tap_thief_sets_chased_flag() {
        var state = GameState.initialGraybox()
        state = state.copy(
            thief = ThiefState.Present(
                tapsDone = 4,
                tapsNeeded = 5,
                escapeMs = 3_000L,
                target = ThiefTarget.CounterCoins,
            ),
        )
        val next = GameReducer.reduce(state, GameIntent.TapThief).state
        assertTrue(next.dayStats.chasedThief)
    }
}
