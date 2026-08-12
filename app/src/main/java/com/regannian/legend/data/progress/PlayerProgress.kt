package com.regannian.legend.data.progress

import com.regannian.legend.data.levels.LevelRepository
import com.regannian.legend.data.save.SaveSnapshot
import com.regannian.legend.game.upgrade.UpgradeCatalog
import com.regannian.legend.game.upgrade.UpgradeId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 进程内进度；经 [attachSaver] 写入 DataStore（C5）。
 */
object PlayerProgress {
    private val _walletCoins = MutableStateFlow(0)
    val walletCoins: StateFlow<Int> = _walletCoins.asStateFlow()

    private val _owned = MutableStateFlow<Set<UpgradeId>>(emptySet())
    val ownedUpgrades: StateFlow<Set<UpgradeId>> = _owned.asStateFlow()

    private val _nextDayId = MutableStateFlow(1)
    val nextDayId: StateFlow<Int> = _nextDayId.asStateFlow()

    private val _hydrated = MutableStateFlow(false)
    val hydrated: StateFlow<Boolean> = _hydrated.asStateFlow()

    @Volatile
    private var saver: ((SaveSnapshot) -> Unit)? = null

    fun attachSaver(saver: (SaveSnapshot) -> Unit) {
        this.saver = saver
    }

    fun hydrate(snapshot: SaveSnapshot) {
        _walletCoins.value = snapshot.walletCoins.coerceAtLeast(0)
        _owned.value = snapshot.upgrades
        _nextDayId.value = snapshot.nextDayId.coerceIn(1, LevelRepository.MAX_DAY)
        _hydrated.value = true
    }

    fun snapshot(muted: Boolean = false): SaveSnapshot = SaveSnapshot(
        walletCoins = _walletCoins.value,
        upgrades = _owned.value,
        nextDayId = _nextDayId.value,
        muted = muted,
    )

    fun addDayEarnings(coins: Int, completedDayId: Int) {
        if (coins > 0) {
            _walletCoins.update { it + coins }
        }
        if (completedDayId in 1 until LevelRepository.MAX_DAY) {
            _nextDayId.update { current ->
                maxOf(current, completedDayId + 1).coerceAtMost(LevelRepository.MAX_DAY)
            }
        } else if (completedDayId >= LevelRepository.MAX_DAY) {
            _nextDayId.value = LevelRepository.MAX_DAY
        }
        persist()
    }

    fun owns(id: UpgradeId): Boolean = id in _owned.value

    /**
     * @return null 成功；否则失败原因
     */
    fun tryBuy(id: UpgradeId): String? {
        if (id in _owned.value) return "已拥有"
        val def = UpgradeCatalog.def(id)
        if (!def.requires.all { it in _owned.value }) {
            val need = def.requires.joinToString("/") { UpgradeCatalog.def(it).name }
            return "需先购买：$need"
        }
        if (_walletCoins.value < def.price) return "金币不足"
        _walletCoins.update { it - def.price }
        _owned.update { it + id }
        persist()
        return null
    }

    private fun persist() {
        saver?.invoke(snapshot())
    }

    /** 单测用 */
    fun resetForTest(
        coins: Int = 0,
        upgrades: Set<UpgradeId> = emptySet(),
        nextDayId: Int = 1,
    ) {
        saver = null
        _walletCoins.value = coins
        _owned.value = upgrades
        _nextDayId.value = nextDayId
        _hydrated.value = true
    }
}
