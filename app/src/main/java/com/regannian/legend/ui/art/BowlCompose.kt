package com.regannian.legend.ui.art

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.regannian.legend.game.model.IngredientId
import com.regannian.legend.game.model.ItemQuality
import com.regannian.legend.ui.theme.BambooGreen
import com.regannian.legend.ui.theme.BrickRed
import com.regannian.legend.ui.theme.ChiliRed
import com.regannian.legend.ui.theme.InkText
import com.regannian.legend.ui.theme.SauceBrown
import com.regannian.legend.ui.theme.WarmYellow

private val Ceramic = Color(0xFFF3E6D0)

/**
 * 碗分层贴图：空碗 → 光面 → 加酱 → 成品，带渐显。
 */
@Composable
fun BowlArt(
    filled: Boolean,
    modifier: Modifier = Modifier,
    toppings: Set<IngredientId> = emptySet(),
    quality: ItemQuality = ItemQuality.Good,
) {
    val hasNoodles = filled
    val hasSauce = IngredientId.SesameSauce in toppings || IngredientId.Brine in toppings
    val contentKey = buildString {
        append(if (hasNoodles) "n" else "e")
        append('|')
        append(toppings.sortedBy { it.name }.joinToString(",") { it.name })
        append('|')
        append(quality.name)
    }
    val noodleAlpha by animateFloatAsState(
        targetValue = if (hasNoodles) 1f else 0f,
        animationSpec = tween(420),
        label = "bowl-noodle",
    )
    val sauceAlpha by animateFloatAsState(
        targetValue = when {
            hasSauce -> 1f
            toppings.isNotEmpty() && hasNoodles -> 0.45f
            else -> 0f
        },
        animationSpec = tween(380),
        label = "bowl-sauce",
    )
    val toppingAlpha by animateFloatAsState(
        targetValue = if (toppings.isNotEmpty()) 1f else 0f,
        animationSpec = tween(360),
        label = "bowl-toppings",
    )
    val pop = remember { Animatable(1f) }
    val splash = remember { Animatable(0f) }
    LaunchedEffect(contentKey) {
        pop.snapTo(0.88f)
        splash.snapTo(1f)
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 420f))
        splash.animateTo(0f, tween(420))
    }
    Box(
        modifier = modifier
            .size(88.dp, 88.dp)
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            },
        contentAlignment = Alignment.Center,
    ) {
        ArtOrFallback(ArtSlots.BOWL_EMPTY) { EmptyBowlShell() }
        if (noodleAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = noodleAlpha },
            ) {
                ArtOrFallback(ArtSlots.BOWL_NOODLES) {
                    Canvas(modifier = Modifier.fillMaxSize()) { drawNoodleFill(quality) }
                }
            }
        }
        if (sauceAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = sauceAlpha * noodleAlpha },
            ) {
                ArtOrFallback(ArtSlots.BOWL_SAUCED) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    SauceBrown.copy(alpha = 0.75f),
                                    SauceBrown.copy(alpha = 0.25f),
                                    Color.Transparent,
                                ),
                                center = Offset(w * 0.5f, h * 0.38f),
                                radius = w * 0.32f,
                            ),
                            topLeft = Offset(w * 0.22f, h * 0.28f),
                            size = Size(w * 0.56f, h * 0.24f),
                        )
                    }
                }
            }
        }
        // 每种配料叠独立贴图，便于辨认（无贴图时回退色块）
        if (toppingAlpha > 0.01f && toppings.isNotEmpty()) {
            val ordered = toppings.sortedBy { it.name }
            ordered.forEachIndexed { index, id ->
                val ox = ((index % 3) - 1) * 10.dp
                val oy = (index / 3) * 8.dp - 4.dp
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = ox, y = oy)
                        .size(34.dp)
                        .graphicsLayer { alpha = toppingAlpha * noodleAlpha },
                ) {
                    ArtOrFallback(ArtSlots.toppingSlot(id)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawToppingBits(setOf(id), size.width, size.height)
                        }
                    }
                }
            }
        }
        val s = splash.value
        if (s > 0.05f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = WarmYellow.copy(alpha = 0.35f * s),
                    radius = size.minDimension * 0.22f * (1.2f - s * 0.4f),
                    center = Offset(size.width * 0.5f, size.height * 0.38f),
                )
            }
        }
    }
}

@Composable
private fun EmptyBowlShell() {
    // 顶视空碗（对齐参考：白碗 + 深蓝边）
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val r = size.minDimension * 0.46f
        drawCircle(Color(0xFF1E3A5F), r, Offset(cx, cy))
        drawCircle(Color(0xFFF7F7F5), r * 0.88f, Offset(cx, cy))
        drawCircle(Color(0xFF5B9BD5).copy(alpha = 0.55f), r * 0.78f, Offset(cx, cy), style = Stroke(3f))
        drawCircle(InkText.copy(alpha = 0.08f), r * 0.55f, Offset(cx, cy))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNoodleFill(quality: ItemQuality) {
    val w = size.width
    val h = size.height
    val noodle = when (quality) {
        ItemQuality.Good -> WarmYellow
        ItemQuality.Soft -> WarmYellow.copy(red = 0.95f)
        ItemQuality.Sticky -> BrickRed.copy(alpha = 0.85f)
        ItemQuality.Burnt -> InkText.copy(alpha = 0.75f)
    }
    drawOval(noodle, Offset(w * 0.22f, h * 0.30f), Size(w * 0.56f, h * 0.22f))
    drawLine(
        noodle.copy(alpha = 0.55f),
        Offset(w * 0.28f, h * 0.36f),
        Offset(w * 0.70f, h * 0.40f),
        strokeWidth = 3f,
    )
    drawLine(
        noodle.copy(alpha = 0.45f),
        Offset(w * 0.30f, h * 0.42f),
        Offset(w * 0.68f, h * 0.46f),
        strokeWidth = 2.5f,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawToppingBits(
    toppings: Set<IngredientId>,
    w: Float,
    h: Float,
) {
    if (IngredientId.SesameSauce in toppings) {
        drawOval(SauceBrown, Offset(w * 0.34f, h * 0.34f), Size(w * 0.32f, h * 0.12f))
    }
    if (IngredientId.SesameOil in toppings || IngredientId.SaladOil in toppings) {
        drawCircle(WarmYellow.copy(alpha = 0.75f), 5f, Offset(w * 0.30f, h * 0.40f))
        drawCircle(WarmYellow.copy(alpha = 0.65f), 4f, Offset(w * 0.70f, h * 0.42f))
    }
    if (IngredientId.ChiliOil in toppings) {
        drawCircle(ChiliRed, 4.5f, Offset(w * 0.38f, h * 0.38f))
        drawCircle(ChiliRed.copy(alpha = 0.85f), 3.5f, Offset(w * 0.55f, h * 0.36f))
        drawCircle(ChiliRed, 3f, Offset(w * 0.62f, h * 0.44f))
    }
    if (IngredientId.Scallion in toppings) {
        drawRoundRect(
            BambooGreen,
            Offset(w * 0.40f, h * 0.32f),
            Size(w * 0.08f, h * 0.05f),
            CornerRadius(2f, 2f),
        )
        drawRoundRect(
            BambooGreen.copy(green = 0.55f),
            Offset(w * 0.52f, h * 0.35f),
            Size(w * 0.09f, h * 0.04f),
            CornerRadius(2f, 2f),
        )
    }
    if (IngredientId.Garlic in toppings) {
        drawCircle(Color(0xFFF2E6C8), 4f, Offset(w * 0.45f, h * 0.44f))
        drawCircle(Color(0xFFE8D9B0), 3.5f, Offset(w * 0.58f, h * 0.42f))
    }
    if (IngredientId.Radish in toppings) {
        drawCircle(Color(0xFFFF8A65), 4f, Offset(w * 0.34f, h * 0.46f))
        drawCircle(Color(0xFFFF7043), 3.5f, Offset(w * 0.66f, h * 0.45f))
    }
    if (IngredientId.PickledBean in toppings) {
        drawCircle(BambooGreen.copy(blue = 0.3f), 3.5f, Offset(w * 0.42f, h * 0.48f))
        drawCircle(BambooGreen.copy(blue = 0.3f), 3f, Offset(w * 0.50f, h * 0.50f))
        drawCircle(BambooGreen.copy(blue = 0.3f), 3.5f, Offset(w * 0.58f, h * 0.48f))
    }
    if (IngredientId.Brine in toppings) {
        drawOval(BrickRed.copy(alpha = 0.55f), Offset(w * 0.36f, h * 0.40f), Size(w * 0.28f, h * 0.08f))
    }
    if (IngredientId.SoyMilk in toppings) {
        drawCircle(Color(0xFFFFF8E7), 6f, Offset(w * 0.72f, h * 0.34f))
    }
    if (IngredientId.MianWo in toppings) {
        drawOval(SauceBrown.copy(alpha = 0.8f), Offset(w * 0.22f, h * 0.48f), Size(w * 0.18f, h * 0.10f))
    }
    if (IngredientId.EggWine in toppings) {
        drawCircle(ChiliRed.copy(alpha = 0.55f), 5f, Offset(w * 0.26f, h * 0.36f))
    }
}

/** 拖拽幽灵：凉面/热面团。 */
@Composable
fun NoodleClumpArt(hot: Boolean, modifier: Modifier = Modifier) {
    val pop = remember { Animatable(0.9f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 380f))
    }
    Canvas(
        modifier = modifier
            .size(56.dp, 40.dp)
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            },
    ) {
        val base = if (hot) BrickRed.copy(alpha = 0.9f) else WarmYellow
        drawOval(base, Offset(size.width * 0.1f, size.height * 0.2f), Size(size.width * 0.8f, size.height * 0.65f))
        drawLine(
            base.copy(alpha = 0.5f),
            Offset(size.width * 0.2f, size.height * 0.4f),
            Offset(size.width * 0.8f, size.height * 0.5f),
            strokeWidth = 3f,
        )
        if (hot) {
            drawCircle(Color.White.copy(alpha = 0.55f), 4f, Offset(size.width * 0.35f, size.height * 0.15f))
            drawCircle(Color.White.copy(alpha = 0.45f), 5f, Offset(size.width * 0.55f, size.height * 0.08f))
        }
    }
}
