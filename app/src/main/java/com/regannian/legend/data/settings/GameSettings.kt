package com.regannian.legend.data.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 设置（D4）：音效 / BGM / 引导完成。变更经 [attachListener] 写入存档。
 */
object GameSettings {
    private val _muted = MutableStateFlow(false)
    /** 短音效静音。 */
    val muted: StateFlow<Boolean> = _muted.asStateFlow()

    private val _bgmMuted = MutableStateFlow(false)
    val bgmMuted: StateFlow<Boolean> = _bgmMuted.asStateFlow()

    private val _tutorialDone = MutableStateFlow(false)
    val tutorialDone: StateFlow<Boolean> = _tutorialDone.asStateFlow()

    @Volatile
    private var listener: (() -> Unit)? = null

    fun attachListener(listener: () -> Unit) {
        this.listener = listener
    }

    fun hydrate(sfxMuted: Boolean, bgmMuted: Boolean, tutorialDone: Boolean) {
        val prev = listener
        listener = null
        _muted.value = sfxMuted
        _bgmMuted.value = bgmMuted
        _tutorialDone.value = tutorialDone
        listener = prev
    }

    fun toggleMute() {
        _muted.value = !_muted.value
        listener?.invoke()
    }

    fun setMuted(value: Boolean) {
        if (_muted.value == value) return
        _muted.value = value
        listener?.invoke()
    }

    fun toggleBgm() {
        _bgmMuted.value = !_bgmMuted.value
        listener?.invoke()
    }

    fun setBgmMuted(value: Boolean) {
        if (_bgmMuted.value == value) return
        _bgmMuted.value = value
        listener?.invoke()
    }

    fun markTutorialDone() {
        if (_tutorialDone.value) return
        _tutorialDone.value = true
        listener?.invoke()
    }

    fun resetTutorial() {
        _tutorialDone.value = false
        listener?.invoke()
    }
}
