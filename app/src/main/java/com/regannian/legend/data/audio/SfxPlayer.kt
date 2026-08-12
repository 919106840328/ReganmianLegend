package com.regannian.legend.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.regannian.legend.R
import com.regannian.legend.game.reducer.SfxId

/**
 * B3：短音效 SoundPool。静音由 [muted] 控制，在 UI 层切换。
 */
class SfxPlayer(context: Context) {

    @Volatile
    var muted: Boolean = false

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val soundIds: Map<SfxId, Int> = mapOf(
        SfxId.Click to pool.load(context, R.raw.sfx_click, 1),
        SfxId.Serve to pool.load(context, R.raw.sfx_serve, 1),
        SfxId.Leave to pool.load(context, R.raw.sfx_leave, 1),
    )

    fun play(id: SfxId) {
        if (muted) return
        val sid = soundIds[id] ?: return
        pool.play(sid, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        pool.release()
    }
}
