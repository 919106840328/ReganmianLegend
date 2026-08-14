package com.regannian.legend.data.save

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.regannian.legend.data.levels.LevelRepository
import com.regannian.legend.game.meta.AchievementId
import com.regannian.legend.game.meta.ReputationRules
import com.regannian.legend.game.upgrade.UpgradeCatalog
import com.regannian.legend.game.upgrade.UpgradeId
import kotlinx.coroutines.flow.first

private val Context.saveDataStore: DataStore<Preferences> by preferencesDataStore(name = "reganmian_save")

/**
 * DataStore 日终存档：钱包 / 升级 / 下一关 / 设置 / 成就。
 */
class SaveRepository(context: Context) {
    private val dataStore = context.applicationContext.saveDataStore

    suspend fun load(): SaveSnapshot {
        val prefs = dataStore.data.first()
        val upgrades = prefs[KEY_UPGRADES]
            .orEmpty()
            .mapNotNull { UpgradeCatalog.parseUpgradeId(it) }
            .toSet()
        val achievements = prefs[KEY_ACHIEVEMENTS]
            .orEmpty()
            .mapNotNull { raw -> runCatching { AchievementId.valueOf(raw) }.getOrNull() }
            .toSet()
        return SaveSnapshot(
            walletCoins = prefs[KEY_COINS] ?: 0,
            upgrades = upgrades,
            nextDayId = (prefs[KEY_NEXT_DAY] ?: 1).coerceIn(1, LevelRepository.MAX_DAY),
            muted = prefs[KEY_MUTED] ?: false,
            bgmMuted = prefs[KEY_BGM_MUTED] ?: false,
            tutorialDone = prefs[KEY_TUTORIAL] ?: false,
            reputation = (prefs[KEY_REPUTATION] ?: ReputationRules.DEFAULT).coerceIn(0, 100),
            achievements = achievements,
            seenEnding = prefs[KEY_SEEN_ENDING] ?: false,
            bestDayCoins = (prefs[KEY_BEST_DAY] ?: 0).coerceAtLeast(0),
            lastAdBonusDayId = prefs[KEY_AD_DAY] ?: 0,
        )
    }

    suspend fun save(snapshot: SaveSnapshot) {
        dataStore.edit { prefs ->
            prefs[KEY_COINS] = snapshot.walletCoins.coerceAtLeast(0)
            prefs[KEY_UPGRADES] = snapshot.upgrades.map { it.name }.toSet()
            prefs[KEY_NEXT_DAY] = snapshot.nextDayId.coerceIn(1, LevelRepository.MAX_DAY)
            prefs[KEY_MUTED] = snapshot.muted
            prefs[KEY_BGM_MUTED] = snapshot.bgmMuted
            prefs[KEY_TUTORIAL] = snapshot.tutorialDone
            prefs[KEY_REPUTATION] = snapshot.reputation.coerceIn(0, 100)
            prefs[KEY_ACHIEVEMENTS] = snapshot.achievements.map { it.name }.toSet()
            prefs[KEY_SEEN_ENDING] = snapshot.seenEnding
            prefs[KEY_BEST_DAY] = snapshot.bestDayCoins.coerceAtLeast(0)
            prefs[KEY_AD_DAY] = snapshot.lastAdBonusDayId
        }
    }

    companion object {
        private val KEY_COINS = intPreferencesKey("wallet_coins")
        private val KEY_UPGRADES = stringSetPreferencesKey("upgrades")
        private val KEY_NEXT_DAY = intPreferencesKey("next_day_id")
        private val KEY_MUTED = booleanPreferencesKey("muted")
        private val KEY_BGM_MUTED = booleanPreferencesKey("bgm_muted")
        private val KEY_TUTORIAL = booleanPreferencesKey("tutorial_done")
        private val KEY_REPUTATION = intPreferencesKey("reputation")
        private val KEY_ACHIEVEMENTS = stringSetPreferencesKey("achievements")
        private val KEY_SEEN_ENDING = booleanPreferencesKey("seen_ending")
        private val KEY_BEST_DAY = intPreferencesKey("best_day_coins")
        private val KEY_AD_DAY = intPreferencesKey("last_ad_bonus_day")
    }
}
