package com.regannian.legend.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.regannian.legend.data.settings.GameSettings
import com.regannian.legend.ui.component.ShopBackdrop
import com.regannian.legend.ui.component.ShopPanel
import com.regannian.legend.ui.component.ShopSignBoard

/**
 * 设置：音效、BGM、重看引导。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
) {
    val sfxMuted by GameSettings.muted.collectAsStateWithLifecycle()
    val bgmMuted by GameSettings.bgmMuted.collectAsStateWithLifecycle()
    val tutorialDone by GameSettings.tutorialDone.collectAsStateWithLifecycle()

    ShopBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            ShopSignBoard(
                title = "设置",
                subtitle = "音效与音乐分开；引导可重看",
            )
            Spacer(modifier = Modifier.height(16.dp))
            ShopPanel(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = GameSettings::toggleMute,
                        modifier = Modifier.sizeIn(minWidth = 220.dp, minHeight = 48.dp),
                    ) {
                        Text(if (sfxMuted) "短音效：关" else "短音效：开")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = GameSettings::toggleBgm,
                        modifier = Modifier.sizeIn(minWidth = 220.dp, minHeight = 48.dp),
                    ) {
                        Text(if (bgmMuted) "店内 BGM：关" else "店内 BGM：开")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = GameSettings::resetTutorial,
                        modifier = Modifier.sizeIn(minWidth = 220.dp, minHeight = 48.dp),
                    ) {
                        Text(
                            if (tutorialDone) "重看引导（下次进营业日弹出）"
                            else "引导：将在营业日弹出",
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onBack,
                        modifier = Modifier.sizeIn(minWidth = 220.dp, minHeight = 48.dp),
                    ) {
                        Text("返回")
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
