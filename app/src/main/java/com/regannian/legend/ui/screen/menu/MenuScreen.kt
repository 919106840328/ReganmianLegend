package com.regannian.legend.ui.screen.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.regannian.legend.data.levels.LevelRepository
import com.regannian.legend.data.progress.PlayerProgress
import com.regannian.legend.data.settings.GameSettings
import com.regannian.legend.ui.theme.ReganmianLegendTheme
import com.regannian.legend.ui.theme.WarmYellow

/**
 * 店外菜单：选 Day 1–10 + 音效开关。
 */
@Composable
fun MenuScreen(
    onStartDay: (dayId: Int) -> Unit,
    onOpenUpgrade: () -> Unit = {},
) {
    val muted by GameSettings.muted.collectAsStateWithLifecycle()
    val wallet by PlayerProgress.walletCoins.collectAsStateWithLifecycle()
    val nextDay by PlayerProgress.nextDayId.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmYellow.copy(alpha = 0.35f))
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "热干面传奇",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "玩法 MVP · 钱包 $wallet 金 · 进度 Day $nextDay",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = { onStartDay(nextDay) },
                modifier = Modifier.sizeIn(minWidth = 200.dp, minHeight = 48.dp),
            ) {
                Text(text = "继续 Day $nextDay")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onStartDay(1) },
                modifier = Modifier.sizeIn(minWidth = 200.dp, minHeight = 48.dp),
            ) {
                Text(text = "从 Day 1 开张")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onOpenUpgrade,
                modifier = Modifier.sizeIn(minWidth = 200.dp, minHeight = 48.dp),
            ) {
                Text(text = "升级店")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "选日（改 JSON 可调难度）",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            (1..LevelRepository.MAX_DAY).chunked(5).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    row.forEach { day ->
                        Button(
                            onClick = { onStartDay(day) },
                            modifier = Modifier.sizeIn(minWidth = 56.dp, minHeight = 48.dp),
                        ) {
                            Text(text = "$day")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = GameSettings::toggleMute,
                modifier = Modifier.sizeIn(minWidth = 200.dp, minHeight = 48.dp),
            ) {
                Text(text = if (muted) "音效：关" else "音效：开")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MenuScreenPreview() {
    ReganmianLegendTheme {
        MenuScreen(onStartDay = {})
    }
}
