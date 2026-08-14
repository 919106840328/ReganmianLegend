package com.regannian.legend.ui.screen.ending

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.regannian.legend.data.progress.PlayerProgress
import com.regannian.legend.game.meta.EndingRules
import com.regannian.legend.ui.component.ShopBackdrop
import com.regannian.legend.ui.theme.ChiliRed
import com.regannian.legend.ui.theme.InkText
import com.regannian.legend.ui.theme.WarmYellow
import kotlin.math.cos
import kotlin.math.sin

/**
 * 结局演出占位：色点烟花，后续可换正式动画。
 */
@Composable
fun EndingScreen(
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) { PlayerProgress.markEndingSeen() }
    val spin by rememberInfiniteTransition(label = "fireworks").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spin",
    )
    ShopBackdrop {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height * 0.38f
                val colors = listOf(WarmYellow, ChiliRed, Color(0xFFE8D44D), Color(0xFF7BC67E))
                repeat(18) { i ->
                    val ang = Math.toRadians((spin + i * 20f).toDouble())
                    val r = size.minDimension * 0.18f
                    drawCircle(
                        color = colors[i % colors.size].copy(alpha = 0.85f),
                        radius = 10f + (i % 4) * 3f,
                        center = Offset(
                            cx + (cos(ang) * r).toFloat(),
                            cy + (sin(ang) * r).toFloat(),
                        ),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "你已经是热干面传奇",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = InkText,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "全店升级完成，钱包 ≥ ${EndingRules.WALLET_THRESHOLD} 金。\n烟花以后可以换成正式演出。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkText.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onBack,
                    modifier = Modifier.sizeIn(minWidth = 200.dp, minHeight = 48.dp),
                ) {
                    Text("回店外")
                }
            }
        }
    }
}
