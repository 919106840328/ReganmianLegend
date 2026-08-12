package com.regannian.legend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.regannian.legend.data.progress.PlayerProgress
import com.regannian.legend.ui.navigation.ReganmianNavHost
import com.regannian.legend.ui.theme.ReganmianLegendTheme

/**
 * 入口：等 DataStore hydrate 后再进导航。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReganmianLegendTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val ready by PlayerProgress.hydrated.collectAsStateWithLifecycle()
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
}
