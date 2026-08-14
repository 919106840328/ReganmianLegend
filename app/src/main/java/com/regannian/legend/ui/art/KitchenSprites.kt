package com.regannian.legend.ui.art

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.regannian.legend.game.copy.SeatMood
import com.regannian.legend.game.model.ItemQuality
import com.regannian.legend.ui.theme.BambooGreen
import com.regannian.legend.ui.theme.BrickRed
import com.regannian.legend.ui.theme.ChiliRed
import com.regannian.legend.ui.theme.CreamBg
import com.regannian.legend.ui.theme.InkText
import com.regannian.legend.ui.theme.WarmYellow
import kotlin.math.cos
import kotlin.math.sin

private val Wood = Color(0xFF8B5A2B)
private val WokIron = Color(0xFF3A322C)
private val Ceramic = Color(0xFFF3E6D0)
private val Skin = Color(0xFFE8C4A8)
private val Shirt = Color(0xFF4A7C59)
private val Steam = Color(0xCCFFFFFF)

@Composable
fun SpeechBubble(text: String, modifier: Modifier = Modifier) {
    if (text.isBlank()) return
    Box(
        modifier = modifier
            .background(CreamBg, RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = InkText,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

/**
 * 锅具烹饪阶段（仅表现层）。
 */
enum class CookVisual {
    Idle,
    Cooking,
    Ready,
}

@Composable
fun SeatCustomerArt(
    mood: SeatMood,
    uncle: Boolean,
    bark: String?,
    modifier: Modifier = Modifier,
    /** 客人身份变化时触发入场（如 customerId）。 */
    arriveKey: String? = null,
) {
    val occupied = mood != SeatMood.Empty
    val slot = when {
        uncle && occupied -> ArtSlots.CUSTOMER_UNCLE
        mood == SeatMood.Empty -> ArtSlots.SEAT_EMPTY
        mood == SeatMood.Hopeful -> ArtSlots.CUSTOMER_HOPEFUL
        mood == SeatMood.Impatient -> ArtSlots.CUSTOMER_IMPATIENT
        mood == SeatMood.FedUp -> ArtSlots.CUSTOMER_FEDUP
        mood == SeatMood.Happy -> ArtSlots.CUSTOMER_HAPPY
        mood == SeatMood.Resigned -> ArtSlots.CUSTOMER_RESIGNED
        mood == SeatMood.Angry -> ArtSlots.CUSTOMER_ANGRY
        else -> ArtSlots.CUSTOMER_HOPEFUL
    }
    val enterX = remember { Animatable(0f) }
    val enterAlpha = remember { Animatable(1f) }
    var seenKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(arriveKey, occupied) {
        if (!occupied || arriveKey == null) {
            enterX.snapTo(0f)
            enterAlpha.snapTo(1f)
            if (!occupied) seenKey = null
            return@LaunchedEffect
        }
        if (arriveKey != seenKey) {
            seenKey = arriveKey
            enterX.snapTo(56f)
            enterAlpha.snapTo(0f)
            enterAlpha.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
            enterX.animateTo(
                0f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
            )
        }
    }
    val idle = rememberInfiniteTransition(label = "seat-idle")
    val bob by idle.animateFloat(
        initialValue = 0f,
        targetValue = when (mood) {
            SeatMood.Hopeful, SeatMood.Happy, SeatMood.WaitPay -> -4f
            SeatMood.Impatient -> -2f
            else -> 0f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(if (mood == SeatMood.Impatient) 280 else 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "seat-bob",
    )
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (!bark.isNullOrBlank() && occupied) {
            SpeechBubble(
                bark,
                modifier = Modifier.graphicsLayer { alpha = enterAlpha.value },
            )
        }
        Box(
            modifier = Modifier
                .size(80.dp, 96.dp)
                .offset(x = enterX.value.dp, y = bob.dp)
                .graphicsLayer {
                    alpha = if (occupied) enterAlpha.value else 1f
                    val squash = when (mood) {
                        SeatMood.Happy -> 1.04f
                        SeatMood.FedUp, SeatMood.Angry -> 0.97f
                        else -> 1f
                    }
                    scaleX = squash
                    scaleY = if (mood == SeatMood.Happy) 0.98f else 1f
                },
            contentAlignment = Alignment.BottomCenter,
        ) {
            ArtOrFallback(slot) {
                Canvas(modifier = Modifier.size(72.dp, 88.dp)) {
                    val w = size.width
                    val h = size.height
                    // 圆凳
                    drawOval(
                        color = Wood,
                        topLeft = Offset(w * 0.18f, h * 0.72f),
                        size = Size(w * 0.64f, h * 0.18f),
                    )
                    drawRect(
                        color = Wood.copy(alpha = 0.85f),
                        topLeft = Offset(w * 0.42f, h * 0.82f),
                        size = Size(w * 0.16f, h * 0.14f),
                    )
                    if (mood == SeatMood.Empty) return@Canvas
                    val body = if (uncle) BrickRed else Shirt
                    // 身体
                    drawOval(
                        color = body,
                        topLeft = Offset(w * 0.28f, h * 0.38f),
                        size = Size(w * 0.44f, h * 0.38f),
                    )
                    // 头
                    drawCircle(color = Skin, radius = w * 0.16f, center = Offset(w * 0.5f, h * 0.28f))
                    if (uncle) {
                        drawArc(
                            color = Color(0xFFE8E0D4),
                            startAngle = 200f,
                            sweepAngle = 140f,
                            useCenter = true,
                            topLeft = Offset(w * 0.32f, h * 0.12f),
                            size = Size(w * 0.36f, h * 0.22f),
                        )
                    }
                    val eyeY = h * 0.26f
                    val mouthY = h * 0.34f
                    val eyeColor = InkText
                    when (mood) {
                        SeatMood.Hopeful, SeatMood.WaitPay -> {
                            drawCircle(eyeColor, w * 0.025f, Offset(w * 0.44f, eyeY))
                            drawCircle(eyeColor, w * 0.025f, Offset(w * 0.56f, eyeY))
                            drawArc(
                                color = InkText,
                                startAngle = 20f,
                                sweepAngle = 140f,
                                useCenter = false,
                                topLeft = Offset(w * 0.42f, mouthY - 4f),
                                size = Size(w * 0.16f, h * 0.08f),
                                style = Stroke(width = 2.5f),
                            )
                        }
                        SeatMood.Happy -> {
                            drawCircle(eyeColor, w * 0.025f, Offset(w * 0.44f, eyeY))
                            drawCircle(eyeColor, w * 0.025f, Offset(w * 0.56f, eyeY))
                            drawArc(
                                color = BrickRed,
                                startAngle = 15f,
                                sweepAngle = 150f,
                                useCenter = false,
                                topLeft = Offset(w * 0.40f, mouthY - 6f),
                                size = Size(w * 0.20f, h * 0.10f),
                                style = Stroke(width = 3f),
                            )
                        }
                        SeatMood.Impatient -> {
                            drawLine(eyeColor, Offset(w * 0.38f, eyeY), Offset(w * 0.46f, eyeY + 3f), strokeWidth = 3f)
                            drawLine(eyeColor, Offset(w * 0.62f, eyeY), Offset(w * 0.54f, eyeY + 3f), strokeWidth = 3f)
                            drawLine(eyeColor, Offset(w * 0.44f, mouthY + 4f), Offset(w * 0.56f, mouthY + 4f), strokeWidth = 2.5f)
                        }
                        SeatMood.FedUp, SeatMood.Angry -> {
                            drawLine(eyeColor, Offset(w * 0.38f, eyeY + 4f), Offset(w * 0.46f, eyeY), strokeWidth = 3f)
                            drawLine(eyeColor, Offset(w * 0.62f, eyeY + 4f), Offset(w * 0.54f, eyeY), strokeWidth = 3f)
                            drawArc(
                                color = InkText,
                                startAngle = 200f,
                                sweepAngle = 140f,
                                useCenter = false,
                                topLeft = Offset(w * 0.42f, mouthY),
                                size = Size(w * 0.16f, h * 0.07f),
                                style = Stroke(width = 2.5f),
                            )
                        }
                        SeatMood.Resigned -> {
                            drawCircle(eyeColor, w * 0.02f, Offset(w * 0.44f, eyeY + 2f))
                            drawCircle(eyeColor, w * 0.02f, Offset(w * 0.56f, eyeY + 2f))
                            drawLine(eyeColor, Offset(w * 0.44f, mouthY + 2f), Offset(w * 0.56f, mouthY + 6f), strokeWidth = 2.5f)
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}

@Composable
fun WokArt(
    working: Boolean,
    hasNoodles: Boolean,
    modifier: Modifier = Modifier,
    cookProgress: Float = 0f,
    quality: ItemQuality = ItemQuality.Good,
) {
    val visual = when {
        working -> CookVisual.Cooking
        hasNoodles -> CookVisual.Ready
        else -> CookVisual.Idle
    }
    CookPropShell(
        emptySlot = ArtSlots.WOK,
        noodleSlot = ArtSlots.WOK_NOODLES,
        visual = visual,
        cookProgress = cookProgress,
        quality = quality,
        modifier = modifier,
        emptyFallback = { WokFallback(hasNoodles = false) },
        blanchStyle = false,
    )
}

@Composable
fun BlanchPotArt(
    working: Boolean,
    hasNoodles: Boolean,
    modifier: Modifier = Modifier,
    cookProgress: Float = 0f,
    quality: ItemQuality = ItemQuality.Good,
) {
    val visual = when {
        working -> CookVisual.Cooking
        hasNoodles -> CookVisual.Ready
        else -> CookVisual.Idle
    }
    CookPropShell(
        emptySlot = ArtSlots.BLANCH,
        noodleSlot = ArtSlots.BLANCH_NOODLES,
        visual = visual,
        cookProgress = cookProgress,
        quality = quality,
        modifier = modifier,
        emptyFallback = { BlanchFallback(hasNoodles = false) },
        blanchStyle = true,
    )
}

@Composable
private fun CookPropShell(
    emptySlot: String,
    noodleSlot: String,
    visual: CookVisual,
    cookProgress: Float,
    quality: ItemQuality,
    modifier: Modifier,
    emptyFallback: @Composable () -> Unit,
    blanchStyle: Boolean,
) {
    val infinite = rememberInfiniteTransition(label = "cook-$emptySlot")
    val shake by infinite.animateFloat(
        initialValue = -1.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(90), RepeatMode.Reverse),
        label = "shake",
    )
    val steamPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "steam",
    )
    val readyPulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "ready-pulse",
    )
    val pop = remember { Animatable(1f) }
    LaunchedEffect(visual) {
        if (visual == CookVisual.Ready) {
            pop.snapTo(0.86f)
            pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 380f))
        } else {
            pop.snapTo(1f)
        }
    }
    val progress = cookProgress.coerceIn(0f, 1.4f)
    val hasNoodles = visual != CookVisual.Idle
    val noodleKey = if (hasNoodles) "noodles" else "empty"
    val noodleAlpha by animateFloatAsState(
        targetValue = if (hasNoodles) 1f else 0f,
        animationSpec = tween(380),
        label = "vessel-noodle",
    )
    Box(
        modifier = modifier
            .size(96.dp, 96.dp)
            .graphicsLayer {
                val cooking = visual == CookVisual.Cooking
                translationX = if (cooking) shake else 0f
                val scale = when (visual) {
                    CookVisual.Ready -> readyPulse * pop.value
                    CookVisual.Cooking -> 1f + progress * 0.03f
                    CookVisual.Idle -> 1f
                }
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center,
    ) {
        ArtOrFallback(emptySlot) { emptyFallback() }
        if (noodleAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = noodleAlpha },
            ) {
                ArtOrFallback(noodleSlot) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawNoodlesInVessel(blanchStyle, quality)
                    }
                }
            }
        }
        PourBurst(
            triggerKey = noodleKey,
            color = if (blanchStyle) Color(0xFF6EC4C8) else WarmYellow,
        )
        if (visual == CookVisual.Cooking) {
            SteamOverlay(phase = steamPhase, intensity = 0.45f + progress * 0.55f)
            DonenessRing(progress = progress)
        }
        if (visual == CookVisual.Ready) {
            ReadySparkles(quality = quality, phase = steamPhase)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNoodlesInVessel(
    blanch: Boolean,
    quality: ItemQuality,
) {
    val w = size.width
    val h = size.height
    val noodle = when (quality) {
        ItemQuality.Good -> WarmYellow
        ItemQuality.Soft -> WarmYellow.copy(red = 0.95f)
        ItemQuality.Sticky -> BrickRed.copy(alpha = 0.85f)
        ItemQuality.Burnt -> InkText.copy(alpha = 0.75f)
    }
    if (blanch) {
        drawOval(noodle, Offset(w * 0.28f, h * 0.26f), Size(w * 0.44f, h * 0.16f))
        drawLine(noodle.copy(alpha = 0.55f), Offset(w * 0.32f, h * 0.30f), Offset(w * 0.68f, h * 0.34f), strokeWidth = 2.5f)
        drawLine(noodle.copy(alpha = 0.45f), Offset(w * 0.34f, h * 0.35f), Offset(w * 0.66f, h * 0.38f), strokeWidth = 2f)
    } else {
        drawOval(noodle, Offset(w * 0.22f, h * 0.48f), Size(w * 0.56f, h * 0.20f))
        drawLine(noodle.copy(alpha = 0.55f), Offset(w * 0.28f, h * 0.54f), Offset(w * 0.72f, h * 0.58f), strokeWidth = 3f)
        drawLine(noodle.copy(alpha = 0.4f), Offset(w * 0.30f, h * 0.60f), Offset(w * 0.70f, h * 0.63f), strokeWidth = 2.5f)
    }
}

@Composable
private fun SteamOverlay(phase: Float, intensity: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        repeat(3) { i ->
            val t = (phase + i * 0.33f) % 1f
            val x = w * (0.32f + i * 0.18f) + sin((t + i) * 6.28) * 4f
            val y = h * (0.42f - t * 0.34f)
            val r = (5f + i * 2f) * (0.6f + intensity * 0.5f) * (1f - t * 0.35f)
            drawCircle(
                color = Steam.copy(alpha = (0.55f * intensity * (1f - t)).coerceIn(0f, 0.7f)),
                radius = r.toFloat(),
                center = Offset(x.toFloat(), y),
            )
        }
    }
}

@Composable
private fun DonenessRing(progress: Float) {
    val color = when {
        progress < 0.85f -> WarmYellow.copy(alpha = 0.55f)
        progress < 1.05f -> BambooGreen.copy(alpha = 0.75f)
        else -> ChiliRed.copy(alpha = 0.8f)
    }
    val label = when {
        progress < 0.85f -> "煮着"
        progress < 1.05f -> "将满"
        else -> "满格"
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Canvas(modifier = Modifier.size(36.dp).padding(top = 2.dp)) {
            drawArc(
                color = color.copy(alpha = 0.25f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 4f),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceAtMost(1.2f) / 1.2f,
                useCenter = false,
                style = Stroke(width = 4f),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = InkText,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp)
                .background(CreamBg.copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun ReadySparkles(quality: ItemQuality, phase: Float) {
    val tint = when (quality) {
        ItemQuality.Good -> BambooGreen
        ItemQuality.Soft, ItemQuality.Sticky -> WarmYellow
        ItemQuality.Burnt -> ChiliRed
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height * 0.28f
            repeat(5) { i ->
                val a = (phase * 6.28f + i * 1.2f)
                val r = 16f + i * 3f
                drawCircle(
                    color = tint.copy(alpha = 0.55f),
                    radius = 3.5f,
                    center = Offset(cx + cos(a) * r, cy + sin(a) * r * 0.55f),
                )
            }
        }
        Text(
            text = when (quality) {
                ItemQuality.Good -> "好了！"
                ItemQuality.Soft -> "软了"
                ItemQuality.Sticky -> "黏了"
                ItemQuality.Burnt -> "糊了"
            },
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = tint,
            modifier = Modifier
                .padding(top = 4.dp)
                .background(CreamBg.copy(alpha = 0.92f), RoundedCornerShape(6.dp))
                .padding(horizontal = 5.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun WokFallback(hasNoodles: Boolean) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawOval(WokIron, Offset(w * 0.08f, h * 0.38f), Size(w * 0.84f, h * 0.48f))
        drawOval(Color(0xFF2A2420), Offset(w * 0.16f, h * 0.44f), Size(w * 0.68f, h * 0.32f))
        if (hasNoodles) {
            drawOval(WarmYellow, Offset(w * 0.24f, h * 0.50f), Size(w * 0.52f, h * 0.18f))
        }
    }
}

@Composable
private fun BlanchFallback(hasNoodles: Boolean) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(BrickRed, Offset(w * 0.18f, h * 0.28f), Size(w * 0.64f, h * 0.52f))
        drawOval(BrickRed, Offset(w * 0.14f, h * 0.18f), Size(w * 0.72f, h * 0.28f))
        drawOval(Color(0xFF6EC4C8), Offset(w * 0.22f, h * 0.22f), Size(w * 0.56f, h * 0.18f))
        if (hasNoodles) {
            drawOval(WarmYellow, Offset(w * 0.30f, h * 0.28f), Size(w * 0.40f, h * 0.12f))
        }
    }
}

@Composable
fun JarArt(
    fill: Color,
    modifier: Modifier = Modifier,
    ingredient: com.regannian.legend.game.model.IngredientId? = null,
) {
    Box(modifier = modifier.size(64.dp, 64.dp)) {
        val slot = ingredient?.let { ArtSlots.toppingSlot(it) } ?: ArtSlots.JAR
        ArtOrFallback(slot) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                // 金属料槽底，贴近沙威玛式柜台融入感
                drawRoundRect(
                    Color(0xFF8A9098),
                    Offset(w * 0.08f, h * 0.18f),
                    Size(w * 0.84f, h * 0.72f),
                    androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                )
                drawRoundRect(
                    fill,
                    Offset(w * 0.16f, h * 0.28f),
                    Size(w * 0.68f, h * 0.48f),
                    androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                )
            }
        }
    }
}

@Composable
fun TrashBinArt(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(64.dp, 56.dp)) {
        ArtOrFallback(ArtSlots.TRASH) {
            Canvas(modifier = Modifier.size(64.dp, 56.dp)) {
                val w = size.width
                val h = size.height
                drawRect(Color(0xFF4A5560), Offset(w * 0.22f, h * 0.28f), Size(w * 0.56f, h * 0.62f))
                drawRoundRect(Color(0xFF2F3A44), Offset(w * 0.14f, h * 0.12f), Size(w * 0.72f, h * 0.22f), androidx.compose.ui.geometry.CornerRadius(4f, 4f))
            }
        }
    }
}

@Composable
fun CoinArt(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(48.dp, 48.dp)) {
        ArtOrFallback(ArtSlots.COIN) {
            Canvas(modifier = Modifier.size(48.dp, 48.dp)) {
                drawCircle(WarmYellow, size.minDimension * 0.42f, Offset(size.width / 2f, size.height / 2f))
                drawCircle(Color(0xFFD4A017), size.minDimension * 0.28f, Offset(size.width / 2f, size.height / 2f), style = Stroke(3f))
            }
        }
    }
}

@Composable
fun ThiefArt(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(72.dp, 72.dp)) {
        ArtOrFallback(ArtSlots.THIEF) {
            Canvas(modifier = Modifier.size(72.dp, 72.dp)) {
                val w = size.width
                val h = size.height
                drawCircle(Color(0xFF2B2118), w * 0.22f, Offset(w * 0.5f, h * 0.28f))
                drawOval(Color(0xFF3D3A38), Offset(w * 0.28f, h * 0.42f), Size(w * 0.44f, h * 0.40f))
                drawCircle(Color(0xFFE8C4A8), w * 0.06f, Offset(w * 0.42f, h * 0.30f))
                drawCircle(Color(0xFFE8C4A8), w * 0.06f, Offset(w * 0.58f, h * 0.30f))
            }
        }
    }
}

@Composable
fun SidePanArt(working: Boolean, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "side")
    val steamPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "side-steam",
    )
    val shake by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(100), RepeatMode.Reverse),
        label = "side-shake",
    )
    Box(
        modifier = modifier
            .size(72.dp, 64.dp)
            .graphicsLayer { translationX = if (working) shake else 0f },
    ) {
        ArtOrFallback(ArtSlots.SIDE) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawOval(Color(0xFF6B5344), Offset(w * 0.10f, h * 0.40f), Size(w * 0.80f, h * 0.42f))
                drawOval(if (working) WarmYellow else BambooGreen, Offset(w * 0.22f, h * 0.46f), Size(w * 0.56f, h * 0.22f))
            }
        }
        if (working) {
            SteamOverlay(phase = steamPhase, intensity = 0.7f)
        }
    }
}
