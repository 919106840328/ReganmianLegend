package com.regannian.legend.ui.art

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.regannian.legend.ui.theme.SauceBrown
import com.regannian.legend.ui.theme.WarmYellow

/**
 * 状态变化时播一次「倒入/落下」特效（面进锅、料进碗等）。
 */
@Composable
fun PourBurst(
    triggerKey: Any?,
    color: Color = WarmYellow,
    modifier: Modifier = Modifier,
) {
    var playKey by remember { mutableStateOf<Any?>(null) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(triggerKey) {
        if (triggerKey == null || triggerKey == playKey) return@LaunchedEffect
        playKey = triggerKey
        // 只在「倒入内容」时播，清空不播
        if (triggerKey == "empty") return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(380, easing = LinearEasing))
    }
    val p = progress.value
    if (p <= 0f || p >= 1f) return
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 1f - p },
        ) {
            val cx = size.width * 0.5f
            val startY = size.height * 0.05f
            val endY = size.height * 0.45f
            val y = startY + (endY - startY) * p
            // 下落团
            drawCircle(color, radius = 10f * (1.1f - p * 0.3f), center = Offset(cx, y))
            // 溅开
            if (p > 0.45f) {
                val s = (p - 0.45f) / 0.55f
                repeat(6) { i ->
                    val ang = i * 60f * (Math.PI.toFloat() / 180f)
                    drawCircle(
                        color = color.copy(alpha = 0.55f * (1f - s)),
                        radius = 3.5f,
                        center = Offset(
                            cx + kotlin.math.cos(ang) * 18f * s,
                            endY + kotlin.math.sin(ang) * 10f * s,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun SauceDripBurst(
    triggerKey: Any?,
    color: Color = SauceBrown,
    modifier: Modifier = Modifier,
) {
    PourBurst(triggerKey = triggerKey, color = color, modifier = modifier)
}
