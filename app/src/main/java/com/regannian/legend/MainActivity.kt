package com.regannian.legend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.regannian.legend.data.audio.BgmPlayer
import com.regannian.legend.data.progress.PlayerProgress
import com.regannian.legend.data.settings.GameSettings
import com.regannian.legend.ui.navigation.ReganmianNavHost
import com.regannian.legend.ui.theme.ReganmianLegendTheme

/**
 * 入口：等 DataStore hydrate 后再进导航；BGM 随生命周期启停。
 */
class MainActivity : ComponentActivity() {

    private val bgmPlayer = BgmPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReganmianLegendTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val ready by PlayerProgress.hydrated.collectAsStateWithLifecycle()
                    val bgmMuted by GameSettings.bgmMuted.collectAsStateWithLifecycle()
                    LaunchedEffect(ready, bgmMuted) {
                        bgmPlayer.muted = bgmMuted
                        if (ready && !bgmMuted) bgmPlayer.play()
                    }
                    if (!ready) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        ReganmianNavHost()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (PlayerProgress.hydrated.value && !GameSettings.bgmMuted.value) {
            bgmPlayer.play()
        }
    }

    override fun onPause() {
        bgmPlayer.pause()
        super.onPause()
    }

    override fun onDestroy() {
        bgmPlayer.release()
        super.onDestroy()
    }
}
