package com.regannian.legend

import android.app.Application
import com.regannian.legend.data.progress.PlayerProgress
import com.regannian.legend.data.save.SaveRepository
import com.regannian.legend.data.settings.GameSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReganmianApp : Application() {
    lateinit var saveRepository: SaveRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        saveRepository = SaveRepository(this)
        appScope.launch {
            val snap = saveRepository.load()
            withContext(Dispatchers.Main) {
                PlayerProgress.hydrate(snap)
                GameSettings.hydrate(
                    sfxMuted = snap.muted,
                    bgmMuted = snap.bgmMuted,
                    tutorialDone = snap.tutorialDone,
                )
            }
            PlayerProgress.attachSaver { next ->
                appScope.launch {
                    saveRepository.save(
                        next.copy(
                            muted = GameSettings.muted.value,
                            bgmMuted = GameSettings.bgmMuted.value,
                            tutorialDone = GameSettings.tutorialDone.value,
                        ),
                    )
                }
            }
            GameSettings.attachListener {
                appScope.launch {
                    saveRepository.save(
                        PlayerProgress.snapshot(
                            muted = GameSettings.muted.value,
                            bgmMuted = GameSettings.bgmMuted.value,
                            tutorialDone = GameSettings.tutorialDone.value,
                        ),
                    )
                }
            }
        }
    }
}
