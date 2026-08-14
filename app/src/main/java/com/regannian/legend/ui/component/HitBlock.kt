package com.regannian.legend.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.regannian.legend.game.model.BowlId
import com.regannian.legend.game.model.CustomerId
import com.regannian.legend.game.model.IngredientId
import com.regannian.legend.game.model.SideId

/**
 * 灰盒可点色块：热区 ≥48dp。
 */
@Composable
fun HitBlock(
    title: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glyph: String? = null,
    pulse: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val infinite = rememberInfiniteTransition(label = "hit-pulse")
    val pulseScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (pulse) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(520),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse-scale",
    )
    Box(
        modifier = modifier
            .sizeIn(minWidth = 96.dp, minHeight = 72.dp)
            .graphicsLayer {
                val s = (if (pressed && enabled) 0.94f else 1f) * pulseScale
                scaleX = s
                scaleY = s
            }
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = if (enabled) 0.9f else 0.4f))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        enabled = enabled,
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        HitBlockLabel(title = title, subtitle = subtitle, glyph = glyph)
    }
}

@Composable
fun HitBlockLabel(title: String, subtitle: String, glyph: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (!glyph.isNullOrBlank()) {
            Text(
                text = glyph,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
        )
    }
}

/** 拖放载荷（UI 层，松手后转成 GameIntent）。 */
sealed interface DragPayload {
    data class Ingredient(val id: IngredientId) : DragPayload
    data class ReadyBowl(val id: BowlId) : DragPayload
    data object CoolNoodles : DragPayload
    data object HotNoodles : DragPayload
    data class SideReady(val sideId: SideId) : DragPayload
}

sealed interface DropTargetId {
    data class Bowl(val id: BowlId) : DropTargetId
    data class Customer(val id: CustomerId) : DropTargetId
    data object Blanch : DropTargetId
    data object Trash : DropTargetId
}

class DropTargetRegistry {
    private val targets = mutableMapOf<DropTargetId, Rect>()

    fun update(id: DropTargetId, bounds: Rect) {
        targets[id] = bounds
    }

    fun clear(id: DropTargetId) {
        targets.remove(id)
    }

    /**
     * 命中检测；[prefer] 用于同点多目标时优先（避免烫锅挡住碗）。
     */
    fun hit(
        point: Offset,
        prefer: ((DropTargetId) -> Boolean)? = null,
    ): DropTargetId? {
        val hits = targets.filter { (_, rect) -> rect.contains(point) }.keys
        if (hits.isEmpty()) return null
        if (prefer != null) {
            hits.firstOrNull(prefer)?.let { return it }
        }
        // 碗优先于烫锅，避免热面拖入碗失败
        hits.firstOrNull { it is DropTargetId.Bowl }?.let { return it }
        return hits.firstOrNull()
    }
}

@Composable
fun rememberDropTargetRegistry(): DropTargetRegistry = remember { DropTargetRegistry() }

fun Modifier.dropTarget(
    registry: DropTargetRegistry,
    id: DropTargetId,
    enabled: Boolean = true,
): Modifier = composed {
    DisposableEffect(id, enabled) {
        if (!enabled) {
            registry.clear(id)
        }
        onDispose { registry.clear(id) }
    }
    if (enabled) {
        this.onGloballyPositioned { coords ->
            registry.update(id, coords.boundsInRoot())
        }
    } else {
        this
    }
}

data class ActiveDrag(
    val payload: DragPayload,
    val title: String,
    val subtitle: String,
    val color: Color,
    val fingerInRoot: Offset,
    val grabOffset: Offset,
)

/**
 * 可拖拽色块：上报 [ActiveDrag]，幽灵由上层全屏层绘制。
 */
@Composable
fun DraggableHitBlock(
    title: String,
    subtitle: String,
    color: Color,
    payload: DragPayload,
    enabled: Boolean,
    onActiveDrag: (ActiveDrag?) -> Unit,
    onDrop: (fingerInRoot: Offset, payload: DragPayload) -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .sizeIn(minWidth = 96.dp, minHeight = 72.dp)
            .onGloballyPositioned { coords ->
                rootOrigin = coords.boundsInRoot().topLeft
            }
            .graphicsLayer { alpha = if (dragging) 0.35f else 1f }
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = if (enabled) 0.9f else 0.4f))
            // 轻点=点击（出锅/入碗）；超过 touchSlop 才开始拖，避免点不动
            .pointerInput(enabled, payload, title, subtitle, color, onClick) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val grab = down.position
                    var lastFinger = rootOrigin + grab
                    fun publishDrag() {
                        onActiveDrag(
                            ActiveDrag(
                                payload = payload,
                                title = title,
                                subtitle = subtitle,
                                color = color,
                                fingerInRoot = lastFinger,
                                grabOffset = grab,
                            ),
                        )
                    }
                    val slopChange = awaitTouchSlopOrCancellation(down.id) { change, _ ->
                        change.consume()
                        lastFinger = rootOrigin + change.position
                    }
                    if (slopChange == null) {
                        // 未超过滑移阈值就抬起 → 视为点击
                        onClick?.invoke()
                        return@awaitEachGesture
                    }
                    dragging = true
                    lastFinger = rootOrigin + slopChange.position
                    publishDrag()
                    val completed = drag(slopChange.id) { change ->
                        change.consume()
                        lastFinger = rootOrigin + change.position
                        publishDrag()
                    }
                    if (completed) {
                        onDrop(lastFinger, payload)
                    }
                    dragging = false
                    onActiveDrag(null)
                }
            }
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        HitBlockLabel(title = title, subtitle = subtitle)
    }
}
