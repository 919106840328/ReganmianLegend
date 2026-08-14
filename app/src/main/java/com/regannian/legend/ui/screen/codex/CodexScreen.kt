package com.regannian.legend.ui.screen.codex

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.regannian.legend.data.progress.PlayerProgress
import com.regannian.legend.game.meta.AchievementCatalog
import com.regannian.legend.game.meta.CodexCatalog
import com.regannian.legend.ui.component.ShopBackdrop
import com.regannian.legend.ui.component.ShopPanel
import com.regannian.legend.ui.component.ShopSignBoard
import com.regannian.legend.ui.theme.InkText

/**
 * 成就 + 图鉴。
 */
@Composable
fun CodexScreen(
    onBack: () -> Unit,
) {
    val achievements by PlayerProgress.achievements.collectAsStateWithLifecycle()
    val upgrades by PlayerProgress.ownedUpgrades.collectAsStateWithLifecycle()
    val seenEnding by PlayerProgress.seenEnding.collectAsStateWithLifecycle()

    ShopBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShopSignBoard(
                title = "图鉴 / 成就",
                subtitle = "已解锁 ${achievements.size}/${AchievementCatalog.ALL.size}",
            )
            Spacer(modifier = Modifier.height(14.dp))
            ShopPanel(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("巷口人物", style = MaterialTheme.typography.titleMedium, color = InkText)
                    Spacer(modifier = Modifier.height(8.dp))
                    CodexCatalog.ALL.forEach { entry ->
                        val open = CodexCatalog.isUnlocked(entry.id, achievements, upgrades, seenEnding)
                        Text(
                            text = if (open) entry.title else "？？ · ${entry.lockedHint}",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkText,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(
                            text = if (open) entry.body else "尚未解锁",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkText.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("成就", style = MaterialTheme.typography.titleMedium, color = InkText)
                    AchievementCatalog.ALL.forEach { def ->
                        val open = def.id in achievements
                        Text(
                            text = if (open) "★ ${def.title}" else "☆ ${def.title}",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkText,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(
                            text = def.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkText.copy(alpha = if (open) 0.8f else 0.45f),
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .sizeIn(minWidth = 200.dp, minHeight = 48.dp),
                    ) {
                        Text("返回")
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
