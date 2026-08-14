package com.regannian.legend.ui.screen.upgrade

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.regannian.legend.data.progress.PlayerProgress
import com.regannian.legend.game.upgrade.UpgradeCatalog
import com.regannian.legend.game.upgrade.UpgradeLine
import com.regannian.legend.ui.component.ShopBackdrop
import com.regannian.legend.ui.component.ShopPanel
import com.regannian.legend.ui.component.ShopSignBoard
import com.regannian.legend.ui.theme.InkText
import com.regannian.legend.ui.theme.ReganmianLegendTheme
import com.regannian.legend.ui.theme.WarmYellow

/**
 * 全升级树商店 A–F。
 */
@Composable
fun UpgradeScreen(
    onBack: () -> Unit,
) {
    val coins by PlayerProgress.walletCoins.collectAsStateWithLifecycle()
    val owned by PlayerProgress.ownedUpgrades.collectAsStateWithLifecycle()
    val nextDay by PlayerProgress.nextDayId.collectAsStateWithLifecycle()
    var toast by remember { mutableStateOf<String?>(null) }

    ShopBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ShopSignBoard(
                title = "升级店",
                subtitle = "钱包 $coins 金 · 进度 Day $nextDay",
            )
            ShopPanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Day≥8 可能出偷碗贼；F5 一键驱离；E 线解锁辅食",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkText.copy(alpha = 0.65f),
                    )
                    toast?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error)
                    }
                    UpgradeLine.entries.forEach { line ->
                        Text(
                            text = UpgradeCatalog.lineTitle(line),
                            style = MaterialTheme.typography.titleMedium,
                            color = InkText,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        UpgradeCatalog.byLine.getValue(line).forEach { def ->
                            val bought = def.id in owned
                            val reqOk = def.requires.all { it in owned }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(WarmYellow.copy(alpha = 0.28f))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "${def.id.name} ${def.name}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = InkText,
                                    )
                                    Text(def.blurb, style = MaterialTheme.typography.bodySmall, color = InkText.copy(alpha = 0.8f))
                                    Text(
                                        text = "${def.price} 金 · 建议 Day ${def.suggestDay}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = InkText.copy(alpha = 0.65f),
                                    )
                                    if (!reqOk && !bought) {
                                        Text(
                                            text = "需：" + def.requires.joinToString { UpgradeCatalog.def(it).name },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        toast = if (bought) {
                                            "已拥有"
                                        } else {
                                            PlayerProgress.tryBuy(def.id) ?: "购买成功：${def.name}"
                                        }
                                    },
                                    enabled = !bought,
                                    modifier = Modifier.sizeIn(minHeight = 48.dp),
                                ) {
                                    Text(if (bought) "已购" else "购买")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
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
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UpgradeScreenPreview() {
    ReganmianLegendTheme {
        UpgradeScreen(onBack = {})
    }
}
