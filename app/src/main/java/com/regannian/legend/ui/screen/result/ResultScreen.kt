package com.regannian.legend.ui.screen.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
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
import com.regannian.legend.data.progress.PlayerProgress
import com.regannian.legend.ui.theme.BambooGreen
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
) {
    val wallet by PlayerProgress.walletCoins.collectAsStateWithLifecycle()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BambooGreen.copy(alpha = 0.12f))
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Day $dayId 打烊",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "星级：${"★".repeat(stars)}${"☆".repeat((3 - stars).coerceAtLeast(0))}",
                style = MaterialTheme.typography.titleLarge,
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
            Spacer(modifier = Modifier.height(32.dp))
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
