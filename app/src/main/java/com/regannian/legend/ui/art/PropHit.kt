package com.regannian.legend.ui.art

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.regannian.legend.ui.component.ActiveDrag
import com.regannian.legend.ui.component.DragPayload
import com.regannian.legend.ui.theme.CreamBg
import com.regannian.legend.ui.theme.InkText

/**
 * 实物热区：无色块底板，点按仍 ≥48dp。
 */
@Composable
fun PropHit(
    caption: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pulse: Boolean = false,
    onClick: (() -> Unit)? = null,
    art: @Composable () -> Unit,
) {
    val infinite = rememberInfiniteTransition(label = "prop-pulse")
    val pulseScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (pulse) 1.06f else 1f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "prop-scale",
    )
    Column(
        modifier = modifier
            .sizeIn(minWidth = 72.dp, minHeight = 72.dp)
            .graphicsLayer {
                val s = pulseScale
                scaleX = s
                scaleY = s
                alpha = if (enabled) 1f else 0.45f
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        enabled = enabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        art()
        PropCaption(caption)
    }
}

@Composable
private fun PropCaption(caption: String) {
    if (caption.isBlank()) return
    Text(
        text = caption,
        style = MaterialTheme.typography.labelSmall,
        color = CreamBg,
        textAlign = TextAlign.Center,
        maxLines = 2,
        modifier = Modifier
            .padding(top = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(InkText.copy(alpha = 0.55f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
fun DraggableProp(
    caption: String,
    payload: DragPayload,
    enabled: Boolean,
    onActiveDrag: (ActiveDrag?) -> Unit,
    onDrop: (fingerInRoot: Offset, payload: DragPayload) -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    art: @Composable () -> Unit,
) {
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .sizeIn(minWidth = 72.dp, minHeight = 72.dp)
            .onGloballyPositioned { coords ->
                rootOrigin = coords.boundsInRoot().topLeft
            }
            .graphicsLayer { alpha = if (dragging) 0.35f else if (enabled) 1f else 0.45f }
            .pointerInput(enabled, payload, caption, onClick) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val grab = down.position
                    var lastFinger = rootOrigin + grab
                    fun publishDrag() {
                        onActiveDrag(
                            ActiveDrag(
                                payload = payload,
                                title = caption,
                                subtitle = "",
                                color = InkText.copy(alpha = 0.2f),
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
                    if (completed) onDrop(lastFinger, payload)
                    dragging = false
                    onActiveDrag(null)
                }
            }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        art()
        PropCaption(caption)
    }
}
