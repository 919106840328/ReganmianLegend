package com.regannian.legend.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.regannian.legend.ui.art.ArtSlots
import com.regannian.legend.ui.theme.AwningRed
import com.regannian.legend.ui.theme.BambooGreen
import com.regannian.legend.ui.theme.BrickRed
import com.regannian.legend.ui.theme.CreamBg
import com.regannian.legend.ui.theme.InkText
import com.regannian.legend.ui.theme.SauceBrown
import com.regannian.legend.ui.theme.WarmYellow

private val SkyTop = Color(0xFFFFC98A)
private val SkyMid = Color(0xFFFFE2B8)
private val WallCream = Color(0xFFFFF1D8)
private val FloorWood = Color(0xFFC48A4A)
private val FloorDark = Color(0xFFA06A35)
private val LanternRed = Color(0xFFD64545)
private val ChiliDeep = Color(0xFF9B2D2D)

/**
 * 早市小店背景：晨曦天空 + 红黄招牌篷 + 暖墙 + 木地板 + 轻蒸汽。
 * [kitchenMode] 营业日：柜台条、挂辣、蒸笼剪影，不挡操作。
 */
@Composable
fun ShopBackdrop(
    rootModifier: Modifier = Modifier,
    showStreetProps: Boolean = true,
    kitchenMode: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val steam by rememberInfiniteTransition(label = "bg-steam").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
        label = "steam-phase",
    )
    Box(modifier = rootModifier.fillMaxSize()) {
        val context = LocalContext.current
        val kitchenBgId = remember(kitchenMode) {
            if (!kitchenMode) 0
            else context.resources.getIdentifier(ArtSlots.KITCHEN_BG, "drawable", context.packageName)
        }
        if (kitchenBgId != 0) {
            Image(
                painter = painterResource(kitchenBgId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            // 轻罩一层，保留柜台纹理，避免道具发白框感
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(InkText.copy(alpha = 0.08f)),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(SkyTop, SkyMid, WallCream, CreamBg))),
            )
        }
        // 左右墙面色带，增加巷口层次
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(0.16f)
                .background(BrickRed.copy(alpha = 0.08f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.16f)
                .background(BambooGreen.copy(alpha = 0.07f)),
        )
        // 轻蒸汽点
        repeat(5) { k ->
            val t = (steam + k * 0.2f) % 1f
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (24 + k * 56).dp, y = (-48 - (t * 90f)).dp)
                    .size((18 + k * 3).dp)
                    .graphicsLayer { alpha = 0.14f * (1f - t) }
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
        // 红黄相间遮阳篷
        Column(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
            Row(modifier = Modifier.fillMaxWidth().height(28.dp)) {
                repeat(14) { i ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .background(if (i % 2 == 0) AwningRed else WarmYellow),
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth().height(10.dp)) {
                repeat(20) { i ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(if (i % 2 == 0) 10.dp else 5.dp)
                            .background(AwningRed.copy(alpha = 0.85f)),
                    )
                }
            }
        }
        // 木地板带
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, FloorWood.copy(alpha = 0.55f), FloorDark.copy(alpha = 0.7f)),
                    ),
                ),
        )
        if (showStreetProps) {
            CornerProps(
                mod = Modifier.align(Alignment.BottomStart).padding(12.dp),
                mirror = false,
            )
            CornerProps(
                mod = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                mirror = true,
            )
            ShopLantern(mod = Modifier.align(Alignment.TopStart).padding(start = 10.dp, top = 36.dp))
            ShopLantern(mod = Modifier.align(Alignment.TopEnd).padding(end = 10.dp, top = 36.dp))
        }
        if (kitchenMode) {
            KitchenDecor()
        }
        Box(
            modifier = Modifier.fillMaxSize().padding(top = 36.dp),
            content = content,
        )
    }
}

/** 营业日厨房氛围：柜台、挂辣、竹蒸笼剪影（纯装饰）。 */
@Composable
private fun BoxScope.KitchenDecor() {
    // 后墙砖纹浅带
    Row(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 40.dp)
            .fillMaxWidth()
            .height(18.dp)
            .padding(horizontal = 8.dp),
    ) {
        repeat(12) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(1.dp)
                    .background(
                        if (i % 2 == 0) BrickRed.copy(alpha = 0.10f) else BrickRed.copy(alpha = 0.06f),
                        RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
    // 左挂辣椒串
    Column(
        modifier = Modifier
            .align(Alignment.CenterStart)
            .padding(start = 6.dp)
            .graphicsLayer { alpha = 0.55f },
        verticalArrangement = Arrangement.spacedBy((-4).dp),
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .size(14.dp, 22.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.verticalGradient(listOf(LanternRed, ChiliDeep))),
            )
        }
    }
    // 右竹蒸笼
    Column(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(end = 8.dp)
            .graphicsLayer { alpha = 0.5f },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .size(36.dp, 14.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(WarmYellow.copy(alpha = 0.55f))
                    .border(1.dp, SauceBrown.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
            )
            SpacerKitchen(2)
        }
    }
    // 操作台木纹条
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(72.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        FloorWood.copy(alpha = 0.28f),
                        FloorDark.copy(alpha = 0.42f),
                    ),
                ),
            ),
    )
}

@Composable
private fun SpacerKitchen(h: Int) {
    Box(modifier = Modifier.height(h.dp))
}

@Composable
private fun ShopLantern(mod: Modifier) {
    val bob by rememberInfiniteTransition(label = "lantern").animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "bob",
    )
    Column(
        modifier = mod.offset(y = bob.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(10.dp)
                .background(InkText.copy(alpha = 0.5f)),
        )
        Box(
            modifier = Modifier
                .size(22.dp, 28.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(listOf(LanternRed, ChiliDeep)))
                .border(1.dp, WarmYellow.copy(alpha = 0.7f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("面", color = WarmYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CornerProps(mod: Modifier, mirror: Boolean) {
    Row(
        modifier = mod,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (!mirror) {
            DecoBowl()
            DecoStool()
        } else {
            DecoStool()
            DecoBowl()
        }
    }
}

@Composable
private fun DecoBowl() {
    Box(modifier = Modifier.size(42.dp, 34.dp), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp, topStart = 4.dp, topEnd = 4.dp))
                .background(Color(0xFFF3E6D0))
                .border(1.dp, SauceBrown.copy(alpha = 0.25f), RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)),
        )
        Box(
            modifier = Modifier
                .padding(bottom = 14.dp)
                .size(28.dp, 10.dp)
                .clip(CircleShape)
                .background(WarmYellow),
        )
        Box(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .size(16.dp, 6.dp)
                .clip(CircleShape)
                .background(SauceBrown),
        )
    }
}

@Composable
private fun DecoStool() {
    Column(
        modifier = Modifier.size(36.dp, 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp, 12.dp)
                .clip(CircleShape)
                .background(FloorDark),
        )
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(14.dp)
                .background(FloorWood),
        )
    }
}

/** 店招木牌标题区。 */
@Composable
fun ShopSignBoard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFFFE7B8), Color(0xFFFFF6E8))))
            .border(2.dp, SauceBrown.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("武汉过早 · 巷口小店", style = MaterialTheme.typography.labelMedium, color = BrickRed)
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = InkText,
            fontSize = 34.sp,
            textAlign = TextAlign.Center,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = InkText.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** 半透明内容底板。 */
@Composable
fun ShopPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(CreamBg.copy(alpha = 0.92f))
            .border(1.5.dp, WarmYellow.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        content()
    }
}
