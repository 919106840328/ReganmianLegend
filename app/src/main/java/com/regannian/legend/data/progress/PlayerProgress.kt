package com.regannian.legend.data.progress

import com.regannian.legend.data.levels.LevelRepository
import com.regannian.legend.data.save.SaveSnapshot
import com.regannian.legend.game.meta.AchievementEvaluator
import com.regannian.legend.game.meta.AchievementId
import com.regannian.legend.game.meta.AchievementInput
import com.regannian.legend.game.meta.EndingRules
import com.regannian.legend.game.meta.ReputationRules
import com.regannian.legend.game.state.DaySummary
import com.regannian.legend.game.upgrade.UpgradeCatalog
import com.regannian.legend.game.upgrade.UpgradeId
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 进程内进度；经 [attachSaver] 写入 DataStore。
 */
object PlayerProgress {
    private val _walletCoins = MutableStateFlow(0)
    val walletCoins: StateFlow<Int> = _walletCoins.asStateFlow()

    private val _owned = MutableStateFlow<Set<UpgradeId>>(emptySet())
    val ownedUpgrades: StateFlow<Set<UpgradeId>> = _owned.asStateFlow()

    private val _nextDayId = MutableStateFlow(1)
    val nextDayId: StateFlow<Int> = _nextDayId.asStateFlow()

    private val _reputation = MutableStateFlow(ReputationRules.DEFAULT)
    val reputation: StateFlow<Int> = _reputation.asStateFlow()

    private val _achievements = MutableStateFlow<Set<AchievementId>>(emptySet())
    val achievements: StateFlow<Set<AchievementId>> = _achievements.asStateFlow()

    private val _seenEnding = MutableStateFlow(false)
    val seenEnding: StateFlow<Boolean> = _seenEnding.asStateFlow()

    private val _bestDayCoins = MutableStateFlow(0)
    val bestDayCoins: StateFlow<Int> = _bestDayCoins.asStateFlow()

    private val _recentUnlocks = MutableStateFlow<List<AchievementId>>(emptyList())
    val recentUnlocks: StateFlow<List<AchievementId>> = _recentUnlocks.asStateFlow()

    private val _hydrated = MutableStateFlow(false)
    val hydrated: StateFlow<Boolean> = _hydrated.asStateFlow()

    private var lastAdBonusDayId: Int = 0

    @Volatile
    private var saver: ((SaveSnapshot) -> Unit)? = null

    fun attachSaver(saver: (SaveSnapshot) -> Unit) {
        this.saver = saver
    }

    fun hydrate(snapshot: SaveSnapshot) {
        _walletCoins.value = snapshot.walletCoins.coerceAtLeast(0)
        _owned.value = snapshot.upgrades
        _nextDayId.value = snapshot.nextDayId.coerceIn(1, LevelRepository.MAX_DAY)
        _reputation.value = snapshot.reputation.coerceIn(0, 100)
        _achievements.value = snapshot.achievements
        _seenEnding.value = snapshot.seenEnding
        _bestDayCoins.value = snapshot.bestDayCoins.coerceAtLeast(0)
        lastAdBonusDayId = snapshot.lastAdBonusDayId
        _recentUnlocks.value = emptyList()
        _hydrated.value = true
    }

    fun snapshot(
        muted: Boolean = false,
        bgmMuted: Boolean = false,
        tutorialDone: Boolean = false,
    ): SaveSnapshot = SaveSnapshot(
        walletCoins = _walletCoins.value,
        upgrades = _owned.value,
        nextDayId = _nextDayId.value,
        muted = muted,
        bgmMuted = bgmMuted,
        tutorialDone = tutorialDone,
        reputation = _reputation.value,
        achievements = _achievements.value,
        seenEnding = _seenEnding.value,
        bestDayCoins = _bestDayCoins.value,
        lastAdBonusDayId = lastAdBonusDayId,
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

    fun recordDay(summary: DaySummary) {
        addDayEarnings(summary.coins, summary.dayId)
        _bestDayCoins.update { maxOf(it, summary.coins) }
        _reputation.value = ReputationRules.apply(_reputation.value, summary.satPercent)
        refreshMeta(summary)
        persist()
    }

    /**
     * 结算占位广告 ×1.5；同一日只能点一次。返回额外金币，失败为 null。
     */
    fun tryAdBonus(dayId: Int, baseCoins: Int): Int? {
        if (dayId == lastAdBonusDayId) return null
        if (baseCoins <= 0) return null
        val extra = (baseCoins * 0.5f).roundToInt().coerceAtLeast(1)
        lastAdBonusDayId = dayId
        _walletCoins.update { it + extra }
        refreshMeta(day = null)
        persist()
        return extra
    }

    fun canAdBonus(dayId: Int): Boolean = dayId != lastAdBonusDayId

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
        refreshMeta(day = null)
        persist()
        return null
    }

    fun markEndingSeen() {
        if (_seenEnding.value) return
        _seenEnding.value = true
        persist()
    }

    private fun refreshMeta(day: DaySummary?) {
        if (EndingRules.isMet(_owned.value, _walletCoins.value)) {
            _seenEnding.value = true
        }
        val next = AchievementEvaluator.unlock(
            already = _achievements.value,
            input = AchievementInput(
                upgrades = _owned.value,
                walletCoins = _walletCoins.value,
                bestDayCoins = _bestDayCoins.value,
                day = day,
            ),
        )
        val newly = (next - _achievements.value).toList()
        _achievements.value = next
        if (newly.isNotEmpty()) {
            _recentUnlocks.value = newly
        }
    }

    private fun persist() {
        saver?.invoke(snapshot())
    }

    /** 单测用 */
    fun resetForTest(
        coins: Int = 0,
        upgrades: Set<UpgradeId> = emptySet(),
        nextDayId: Int = 1,
        reputation: Int = ReputationRules.DEFAULT,
        achievements: Set<AchievementId> = emptySet(),
        seenEnding: Boolean = false,
        bestDayCoins: Int = 0,
    ) {
        saver = null
        _walletCoins.value = coins
        _owned.value = upgrades
        _nextDayId.value = nextDayId
        _reputation.value = reputation
        _achievements.value = achievements
        _seenEnding.value = seenEnding
        _bestDayCoins.value = bestDayCoins
        lastAdBonusDayId = 0
        _recentUnlocks.value = emptyList()
        _hydrated.value = true
    }
}
