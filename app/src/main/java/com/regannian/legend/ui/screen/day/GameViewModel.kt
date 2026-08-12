package com.regannian.legend.ui.screen.day

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.regannian.legend.data.progress.PlayerProgress
import com.regannian.legend.game.GrayboxConfig
import com.regannian.legend.game.config.DayConfig
import com.regannian.legend.game.intent.GameIntent
import com.regannian.legend.game.model.DayPhase
import com.regannian.legend.game.reducer.GameEffect
import com.regannian.legend.game.reducer.GameReducer
import com.regannian.legend.game.state.GameState
import com.regannian.legend.game.upgrade.UpgradeId
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 持有 [GameState]，UI 只 [dispatch] Intent；[effects] 供音效等副作用消费。
 */
class GameViewModel(
    private val dayConfig: DayConfig,
) : ViewModel() {

    private val upgrades: Set<UpgradeId> get() = PlayerProgress.ownedUpgrades.value

    private val _state = MutableStateFlow(GameState.initialForDay(dayConfig, upgrades))
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<GameEffect>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val effects: SharedFlow<GameEffect> = _effects.asSharedFlow()

    private var tickJob: Job? = null

    init {
        startTick()
    }

    fun dispatch(intent: GameIntent) {
        val current = _state.value
        if (current.phase == DayPhase.Result && intent !is GameIntent.Tick) {
            return
        }
        val transition = GameReducer.reduce(current, intent)
        _state.value = transition.state
        transition.effects.forEach { effect ->
            _effects.tryEmit(effect)
        }
    }

    fun restartDay() {
        tickJob?.cancel()
        _state.value = GameState.initialForDay(dayConfig, upgrades)
        startTick()
    }

    private fun startTick() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive) {
                delay(GrayboxConfig.TICK_STEP_MS)
                if (_state.value.phase == DayPhase.Playing) {
                    dispatch(GameIntent.Tick)
                }
                if (_state.value.phase == DayPhase.Result) {
                    break
                }
            }
        }
    }

    companion object {
        fun factory(dayConfig: DayConfig): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GameViewModel(dayConfig) as T
                }
            }
    }
}
