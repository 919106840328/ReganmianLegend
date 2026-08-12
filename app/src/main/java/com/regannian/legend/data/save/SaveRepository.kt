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
import com.regannian.legend.game.upgrade.UpgradeCatalog
import com.regannian.legend.game.upgrade.UpgradeId
import kotlinx.coroutines.flow.first

private val Context.saveDataStore: DataStore<Preferences> by preferencesDataStore(name = "reganmian_save")

/**
 * DataStore 日终存档：钱包 / 升级 / 下一关 / 静音。
 */
class SaveRepository(context: Context) {
    private val dataStore = context.applicationContext.saveDataStore

    suspend fun load(): SaveSnapshot {
        val prefs = dataStore.data.first()
        val upgrades = prefs[KEY_UPGRADES]
            .orEmpty()
            .mapNotNull { UpgradeCatalog.parseUpgradeId(it) }
            .toSet()
        return SaveSnapshot(
            walletCoins = prefs[KEY_COINS] ?: 0,
            upgrades = upgrades,
            nextDayId = (prefs[KEY_NEXT_DAY] ?: 1).coerceIn(1, LevelRepository.MAX_DAY),
            muted = prefs[KEY_MUTED] ?: false,
        )
    }

    suspend fun save(snapshot: SaveSnapshot) {
        dataStore.edit { prefs ->
            prefs[KEY_COINS] = snapshot.walletCoins.coerceAtLeast(0)
            prefs[KEY_UPGRADES] = snapshot.upgrades.map { it.name }.toSet()
            prefs[KEY_NEXT_DAY] = snapshot.nextDayId.coerceIn(1, LevelRepository.MAX_DAY)
            prefs[KEY_MUTED] = snapshot.muted
        }
    }

    companion object {
        private val KEY_COINS = intPreferencesKey("wallet_coins")
        private val KEY_UPGRADES = stringSetPreferencesKey("upgrades")
        private val KEY_NEXT_DAY = intPreferencesKey("next_day_id")
        private val KEY_MUTED = booleanPreferencesKey("muted")
    }
}
