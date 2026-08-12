package com.regannian.legend.data.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 静音设置；变更可经 [attachListener] 写入存档。
 */
object GameSettings {
    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted.asStateFlow()

    @Volatile
    private var listener: (() -> Unit)? = null

    fun attachListener(listener: () -> Unit) {
        this.listener = listener
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
}
