package com.regannian.legend.ui.screen.result

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.regannian.legend.data.progress.PlayerProgress
import com.regannian.legend.game.meta.AchievementCatalog
import com.regannian.legend.game.meta.EndingRules
import androidx.compose.foundation.layout.fillMaxWidth
import com.regannian.legend.ui.component.ShopBackdrop
import com.regannian.legend.ui.component.ShopPanel
import com.regannian.legend.ui.component.ShopSignBoard
import com.regannian.legend.ui.theme.ReganmianLegendTheme

/**
 * 日终结算：展示金币 / 满意度 / 星级；可进下一天。
 */
@Composable
fun ResultScreen(
    dayId: Int,
    coins: Int,
    satPercent: Int,
    orderCount: Int,
    stars: Int,
    onBackToMenu: () -> Unit,
    onNextDay: (() -> Unit)? = null,
    onOpenUpgrade: (() -> Unit)? = null,
    onOpenEnding: (() -> Unit)? = null,
) {
    val wallet by PlayerProgress.walletCoins.collectAsStateWithLifecycle()
    val reputation by PlayerProgress.reputation.collectAsStateWithLifecycle()
    val unlocks by PlayerProgress.recentUnlocks.collectAsStateWithLifecycle()
    val seenEnding by PlayerProgress.seenEnding.collectAsStateWithLifecycle()
    val upgrades by PlayerProgress.ownedUpgrades.collectAsStateWithLifecycle()
    var adExtra by remember { mutableStateOf<Int?>(null) }
    var popped by remember { mutableStateOf(false) }
    LaunchedEffect(stars) { popped = true }
    val starScale by animateFloatAsState(
        targetValue = if (popped) 1f else 0.35f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "star-pop",
    )
    ShopBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ShopSignBoard(
                    title = "Day $dayId 打烊",
                    subtitle = "今日收入 $coins 金 · 口碑 $reputation",
                )
                Spacer(modifier = Modifier.height(14.dp))
                ShopPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "星级：${"★".repeat(stars)}${"☆".repeat((3 - stars).coerceAtLeast(0))}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.graphicsLayer {
                        scaleX = starScale
                        scaleY = starScale
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "今日收入：$coins 金",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "钱包合计：$wallet 金",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                )
                Text(
                    text = "满意度：$satPercent%",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "服务单数：$orderCount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
                Text(
                    text = "口碑：$reputation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
                if (unlocks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("新成就", style = MaterialTheme.typography.titleSmall)
                    unlocks.forEach { id ->
                        Text(
                            text = "★ ${AchievementCatalog.def(id).title}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                if (adExtra == null && PlayerProgress.canAdBonus(dayId) && coins > 0) {
                    Button(
                        onClick = { adExtra = PlayerProgress.tryAdBonus(dayId, coins) },
                        modifier = Modifier.sizeIn(minWidth = 200.dp, minHeight = 48.dp),
                    ) {
                        Text("看广告 ×1.5（占位）")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                } else if (adExtra != null) {
                    Text(
                        text = "广告加成 +$adExtra 金",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (onOpenEnding != null && (seenEnding || EndingRules.isMet(upgrades, wallet))) {
                    Button(
                        onClick = onOpenEnding,
                        modifier = Modifier.sizeIn(minWidth = 200.dp, minHeight = 48.dp),
                    ) {
                        Text(text = "看结局")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (onOpenUpgrade != null) {
                    Button(
                        onClick = onOpenUpgrade,
                        modifier = Modifier.sizeIn(minWidth = 200.dp, minHeight = 48.dp),
                    ) {
                        Text(text = "去升级店")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (onNextDay != null) {
                    Button(
                        onClick = onNextDay,
                        modifier = Modifier.sizeIn(minWidth = 200.dp, minHeight = 48.dp),
                    ) {
                        Text(text = "下一天 Day ${dayId + 1}")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Button(
                    onClick = onBackToMenu,
                    modifier = Modifier.sizeIn(minWidth = 200.dp, minHeight = 48.dp),
                ) {
                    Text(text = "回店外")
                }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultScreenPreview() {
    ReganmianLegendTheme {
        ResultScreen(
            dayId = 1,
            coins = 10,
            satPercent = 86,
            orderCount = 1,
            stars = 2,
            onBackToMenu = {},
            onNextDay = {},
        )
    }
}
