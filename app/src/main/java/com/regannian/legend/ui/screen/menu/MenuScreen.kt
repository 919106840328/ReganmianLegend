package com.regannian.legend.ui.screen.menu

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.regannian.legend.data.levels.LevelRepository
import com.regannian.legend.data.progress.PlayerProgress
import com.regannian.legend.data.settings.GameSettings
import com.regannian.legend.ui.component.ShopBackdrop
import com.regannian.legend.ui.component.ShopPanel
import com.regannian.legend.ui.component.ShopSignBoard
import com.regannian.legend.ui.theme.BrickRed
import com.regannian.legend.ui.theme.InkText
import com.regannian.legend.ui.theme.ReganmianLegendTheme

/**
 * 店外菜单：招牌 + 选日 + 入口。
 */
@Composable
fun MenuScreen(
    onStartDay: (dayId: Int) -> Unit,
    onOpenUpgrade: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenCodex: () -> Unit = {},
    onOpenEnding: () -> Unit = {},
) {
    val muted by GameSettings.muted.collectAsStateWithLifecycle()
    val wallet by PlayerProgress.walletCoins.collectAsStateWithLifecycle()
    val nextDay by PlayerProgress.nextDayId.collectAsStateWithLifecycle()
    val reputation by PlayerProgress.reputation.collectAsStateWithLifecycle()
    val seenEnding by PlayerProgress.seenEnding.collectAsStateWithLifecycle()

    ShopBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShopSignBoard(
                title = "热干面传奇",
                subtitle = "钱包 $wallet 金 · 口碑 $reputation · 进度 Day $nextDay",
            )
            Spacer(modifier = Modifier.height(16.dp))
            ShopPanel(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "今日过早，面来咯",
                        style = MaterialTheme.typography.titleMedium,
                        color = BrickRed,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onStartDay(nextDay) },
                        modifier = Modifier.sizeIn(minWidth = 220.dp, minHeight = 48.dp),
                    ) {
                        Text(text = "继续 Day $nextDay")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onStartDay(1) },
                        modifier = Modifier.sizeIn(minWidth = 220.dp, minHeight = 48.dp),
                    ) {
                        Text(text = "从 Day 1 开张")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onOpenUpgrade,
                        modifier = Modifier.sizeIn(minWidth = 220.dp, minHeight = 48.dp),
                    ) {
                        Text(text = "升级店")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onOpenCodex,
                        modifier = Modifier.sizeIn(minWidth = 220.dp, minHeight = 48.dp),
                    ) {
                        Text(text = "图鉴 / 成就")
                    }
                    if (seenEnding) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onOpenEnding,
                            modifier = Modifier.sizeIn(minWidth = 220.dp, minHeight = 48.dp),
                        ) {
                            Text(text = "再看结局")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            ShopPanel(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "选日（改 JSON 可调难度）",
                        style = MaterialTheme.typography.labelLarge,
                        color = InkText.copy(alpha = 0.7f),
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
                    Button(
                        onClick = onOpenSettings,
                        modifier = Modifier.sizeIn(minWidth = 220.dp, minHeight = 48.dp),
                    ) {
                        Text(text = "设置")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = GameSettings::toggleMute,
                        modifier = Modifier.sizeIn(minWidth = 220.dp, minHeight = 48.dp),
                    ) {
                        Text(text = if (muted) "音效：关" else "音效：开")
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
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
