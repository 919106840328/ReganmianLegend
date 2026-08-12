package com.regannian.legend.ui.screen.day

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.regannian.legend.data.audio.SfxPlayer
import com.regannian.legend.data.levels.LevelRepository
import com.regannian.legend.data.settings.GameSettings
import com.regannian.legend.game.GrayboxConfig
import com.regannian.legend.game.intent.GameIntent
import com.regannian.legend.game.model.BowlState
import com.regannian.legend.game.model.CustomerKind
import com.regannian.legend.game.model.CustomerStatus
import com.regannian.legend.game.model.DayPhase
import com.regannian.legend.game.model.IngredientId
import com.regannian.legend.game.model.ItemQuality
import com.regannian.legend.game.model.SideStationState
import com.regannian.legend.game.model.StationId
import com.regannian.legend.game.model.StationState
import com.regannian.legend.game.reducer.GameEffect
import com.regannian.legend.game.state.DaySummary
import com.regannian.legend.game.state.GameState
import com.regannian.legend.game.state.toDaySummary
import com.regannian.legend.game.upgrade.UpgradeEffects
import com.regannian.legend.game.upgrade.UpgradeId
import com.regannian.legend.ui.component.ActiveDrag
import com.regannian.legend.ui.component.DragPayload
import com.regannian.legend.ui.component.DraggableHitBlock
import com.regannian.legend.ui.component.DropTargetId
import com.regannian.legend.ui.component.DropTargetRegistry
import com.regannian.legend.ui.component.HitBlock
import com.regannian.legend.ui.component.HitBlockLabel
import com.regannian.legend.ui.component.dropTarget
import com.regannian.legend.ui.component.rememberDropTargetRegistry
import com.regannian.legend.ui.theme.BambooGreen
import com.regannian.legend.ui.theme.BrickRed
import com.regannian.legend.ui.theme.ChiliRed
import com.regannian.legend.ui.theme.CreamBg
import com.regannian.legend.ui.theme.InkText
import com.regannian.legend.ui.theme.ReganmianLegendTheme
import com.regannian.legend.ui.theme.SauceBrown
import com.regannian.legend.ui.theme.WarmYellow
import kotlin.math.roundToInt

@Composable
fun DayScreen(
    dayId: Int,
    onEndDay: (DaySummary) -> Unit,
) {
    val context = LocalContext.current
    val dayConfig = remember(dayId) {
        LevelRepository.fromAssets(context).load(dayId)
    }
    val viewModel: GameViewModel = viewModel(
        key = "day-$dayId",
        factory = GameViewModel.factory(dayConfig),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val muted by GameSettings.muted.collectAsStateWithLifecycle()
    val sfxPlayer = remember { SfxPlayer(context.applicationContext) }

    DisposableEffect(sfxPlayer) {
        onDispose { sfxPlayer.release() }
    }
    LaunchedEffect(muted) { sfxPlayer.muted = muted }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            if (effect is GameEffect.PlaySfx) sfxPlayer.play(effect.id)
        }
    }
    LaunchedEffect(dayId) { viewModel.restartDay() }
    LaunchedEffect(state.phase) {
        if (state.phase == DayPhase.Result) onEndDay(state.toDaySummary())
    }

    DayScreenContent(
        state = state,
        muted = muted,
        onToggleMute = GameSettings::toggleMute,
        onIntent = viewModel::dispatch,
        onEndDay = { onEndDay(state.toDaySummary()) },
    )
}

@Composable
fun DayScreenContent(
    state: GameState,
    onIntent: (GameIntent) -> Unit,
    onEndDay: () -> Unit,
    muted: Boolean = false,
    onToggleMute: () -> Unit = {},
) {
    val remainSec = ((state.dayDurationMs - state.clockMs).coerceAtLeast(0L) / 1000L).toInt()
    val waiting = state.customers.count { it.status == CustomerStatus.WaitingServe }
    val dropRegistry = rememberDropTargetRegistry()
    var activeDrag by remember { mutableStateOf<ActiveDrag?>(null) }
    val dragging = activeDrag != null
    val dust = state.stations.getValue(StationId.DustCook)
    val blanch = state.stations.getValue(StationId.Blanch)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CreamBg)
                .padding(16.dp)
                .verticalScroll(rememberScrollState(), enabled = !dragging),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HudBar(state.dayId, remainSec, state.coins, muted, onToggleMute)

            Text(
                text = "等餐 $waiting/${state.customers.size}",
                style = MaterialTheme.typography.bodyMedium,
            )
            when (val thief = state.thief) {
                is com.regannian.legend.game.model.ThiefState.Present -> {
                    val targetHint = when (val t = thief.target) {
                        is com.regannian.legend.game.model.ThiefTarget.Bowl -> "盯着碗"
                        is com.regannian.legend.game.model.ThiefTarget.CounterCoins -> "盯着钱"
                    }
                    HitBlock(
                        title = "偷碗贼！",
                        subtitle = "$targetHint · 点${thief.tapsDone}/${thief.tapsNeeded} · ${(thief.escapeMs / 1000).coerceAtLeast(0)}s",
                        color = ChiliRed,
                        onClick = { onIntent(GameIntent.TapThief) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> Unit
            }
            Text(
                text = "流程：点掸面开工→再点出锅→点/拖去烫→再点停烫→点/拖入碗→加料→出餐",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )

            Text("顾客", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.customers.forEachIndexed { index, customer ->
                    val patiencePct = if (customer.patienceMaxMs <= 0L) 0
                    else ((customer.patienceMs * 100) / customer.patienceMaxMs).toInt()
                    val orderText = if (customer.kind == CustomerKind.EarlyMarketUncle) {
                        "大爷·素面"
                    } else {
                        GrayboxConfig.orderLabel(customer.order)
                    }
                    val subtitle = when (customer.status) {
                        CustomerStatus.Empty -> "空座·等客"
                        CustomerStatus.WaitingServe -> "$orderText · $patiencePct%"
                        CustomerStatus.WaitingInteract -> "点一下收钱！"
                        CustomerStatus.Served -> "已出餐"
                        CustomerStatus.LeftAngry -> "已离店"
                    }
                    HitBlock(
                        title = when {
                            customer.kind == CustomerKind.EarlyMarketUncle -> "大爷"
                            else -> "座${index + 1}"
                        },
                        subtitle = subtitle,
                        color = when (customer.status) {
                            CustomerStatus.Empty -> InkText.copy(alpha = 0.35f)
                            CustomerStatus.WaitingServe -> BambooGreen
                            CustomerStatus.WaitingInteract -> ChiliRed
                            CustomerStatus.Served -> WarmYellow
                            CustomerStatus.LeftAngry -> ChiliRed
                        },
                        enabled = customer.status == CustomerStatus.WaitingServe ||
                            customer.status == CustomerStatus.WaitingInteract,
                        onClick = {
                            onIntent(GameIntent.TapCustomer(customer.id))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .dropTarget(
                                dropRegistry,
                                DropTargetId.Customer(customer.id),
                                enabled = customer.status == CustomerStatus.WaitingServe,
                            ),
                    )
                }
            }

            Text("工位（少点多拖）", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    when (dust) {
                        is StationState.Idle -> HitBlock(
                            title = "掸面锅",
                            subtitle = "点一下开工",
                            color = WarmYellow,
                            onClick = { onIntent(GameIntent.TapStation(StationId.DustCook)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        is StationState.Working -> HitBlock(
                            title = "掸面锅",
                            subtitle = "点一下出锅",
                            color = WarmYellow,
                            onClick = { onIntent(GameIntent.TapStation(StationId.DustCook)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        is StationState.Ready -> DraggableHitBlock(
                            title = "凉面·${GrayboxConfig.qualityLabel(dust.quality)}",
                            subtitle = "点一下去烫 / 可拖",
                            color = qualityColor(dust.quality, WarmYellow),
                            payload = DragPayload.CoolNoodles,
                            enabled = true,
                            onActiveDrag = { activeDrag = it },
                            onDrop = { finger, payload ->
                                handleDrop(finger, payload, dropRegistry, onIntent)
                                activeDrag = null
                            },
                            onClick = {
                                activeDrag = null
                                onIntent(GameIntent.TapStation(StationId.DustCook))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    AttentionProgressBar(
                        station = dust,
                        greenEnd = state.timings.dustCookMs,
                        redEnd = state.timings.dustRedMs,
                        burntEnd = state.timings.dustBurntMs,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    when (blanch) {
                        is StationState.Idle -> HitBlock(
                            title = "烫面锅",
                            subtitle = "等凉面",
                            color = BrickRed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .dropTarget(dropRegistry, DropTargetId.Blanch, enabled = true),
                        )
                        is StationState.Working -> HitBlock(
                            title = "烫面锅",
                            subtitle = "点一下停烫",
                            color = BrickRed,
                            onClick = { onIntent(GameIntent.TapStation(StationId.Blanch)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .dropTarget(dropRegistry, DropTargetId.Blanch, enabled = false),
                        )
                        is StationState.Ready -> DraggableHitBlock(
                            title = "热面·${GrayboxConfig.qualityLabel(blanch.quality)}",
                            subtitle = "点一下入碗 / 可拖",
                            color = qualityColor(blanch.quality, BrickRed),
                            payload = DragPayload.HotNoodles,
                            enabled = true,
                            onActiveDrag = { activeDrag = it },
                            onDrop = { finger, payload ->
                                handleDrop(finger, payload, dropRegistry, onIntent)
                                activeDrag = null
                            },
                            onClick = {
                                activeDrag = null
                                onIntent(GameIntent.TapStation(StationId.Blanch))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .dropTarget(dropRegistry, DropTargetId.Blanch, enabled = false),
                        )
                    }
                    AttentionProgressBar(
                        station = blanch,
                        greenEnd = state.timings.blanchMs,
                        redEnd = state.timings.blanchRedMs,
                        burntEnd = state.timings.blanchBurntMs,
                    )
                }
            }

            HitBlock(
                title = "垃圾桶",
                subtitle = "错碗拖进来 −2金",
                color = InkText,
                modifier = Modifier
                    .fillMaxWidth()
                    .dropTarget(dropRegistry, DropTargetId.Trash),
            )

            Text("碗（热面拖入；配料拖入；匹配后拖给客人）", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.bowls.keys.sortedBy { it.value }.forEachIndexed { index, bowlId ->
                    val bowl = state.bowls.getValue(bowlId)
                    val focused = state.focusedBowlId == bowlId
                    val toppings = when (bowl) {
                        is BowlState.Assembling -> bowl.toppings
                        is BowlState.ReadyToServe -> bowl.toppings
                        is BowlState.Empty -> emptySet()
                    }
                    val matched = toppings.isNotEmpty() && state.matchesAnyWaitingOrder(toppings)
                    val color = when {
                        bowl is BowlState.Empty -> BambooGreen.copy(alpha = 0.55f)
                        matched -> BrickRed
                        else -> WarmYellow
                    }
                    val borderMod = Modifier.border(
                        width = if (focused) 3.dp else 0.dp,
                        color = if (focused) InkText else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                    )
                    val baseMod = Modifier
                        .weight(1f)
                        .then(borderMod)
                        .dropTarget(dropRegistry, DropTargetId.Bowl(bowlId))

                    if (toppings.isNotEmpty()) {
                        DraggableHitBlock(
                            title = "碗${index + 1}",
                            subtitle = if (matched) "拖给对的人" else bowlLabel(bowl),
                            color = color,
                            payload = DragPayload.ReadyBowl(bowlId),
                            enabled = true,
                            onActiveDrag = { activeDrag = it },
                            onDrop = { finger, payload ->
                                handleDrop(finger, payload, dropRegistry, onIntent)
                            },
                            onClick = { onIntent(GameIntent.TapBowl(bowlId)) },
                            modifier = baseMod,
                        )
                    } else {
                        HitBlock(
                            title = "碗${index + 1}",
                            subtitle = bowlLabel(bowl),
                            color = color,
                            onClick = { onIntent(GameIntent.TapBowl(bowlId)) },
                            modifier = baseMod.sizeIn(minHeight = 88.dp),
                        )
                    }
                }
            }

            if (state.sideStations.isNotEmpty()) {
                Text("辅食（点开工→再点出锅→拖入碗）", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.sideStations.forEach { (sideId, side) ->
                        val name = GrayboxConfig.displayName(UpgradeEffects.sideIngredient(sideId))
                        when (side) {
                            is SideStationState.Idle -> HitBlock(
                                title = name,
                                subtitle = "点开工",
                                color = BambooGreen,
                                onClick = { onIntent(GameIntent.TapSide(sideId)) },
                                modifier = Modifier.weight(1f),
                            )
                            is SideStationState.Working -> HitBlock(
                                title = name,
                                subtitle = "点一下出锅",
                                color = WarmYellow,
                                onClick = { onIntent(GameIntent.TapSide(sideId)) },
                                modifier = Modifier.weight(1f),
                            )
                            is SideStationState.Ready -> DraggableHitBlock(
                                title = "$name·${GrayboxConfig.qualityLabel(side.quality)}",
                                subtitle = "拖进碗",
                                color = if (side.quality == ItemQuality.Burnt) InkText else BambooGreen,
                                payload = DragPayload.SideReady(sideId),
                                enabled = true,
                                onActiveDrag = { activeDrag = it },
                                onDrop = { finger, payload ->
                                    handleDrop(finger, payload, dropRegistry, onIntent)
                                    activeDrag = null
                                },
                                onClick = { onIntent(GameIntent.TapSide(sideId)) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            Text("配料（直接拖到碗，含芝麻酱）", style = MaterialTheme.typography.titleMedium)
            if (UpgradeId.D3 in state.ownedUpgrades || UpgradeId.B2 in state.ownedUpgrades) {
                Button(
                    onClick = { onIntent(GameIntent.ApplyClassicCombo) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .sizeIn(minHeight = 48.dp),
                ) {
                    Text(
                        if (UpgradeId.D3 in state.ownedUpgrades) "经典套餐键"
                        else "标准酱一键",
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.toppingBar.chunked(3).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowItems.forEach { ingredient ->
                            DraggableHitBlock(
                                title = GrayboxConfig.displayName(ingredient),
                                subtitle = "拖到碗",
                                color = toppingColor(ingredient),
                                payload = DragPayload.Ingredient(ingredient),
                                enabled = true,
                                onActiveDrag = { activeDrag = it },
                                onDrop = { finger, payload ->
                                    handleDrop(finger, payload, dropRegistry, onIntent)
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (state.coinsOnCounter.isNotEmpty()) {
                Text("柜台金币", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.coinsOnCounter.forEach { pile ->
                        HitBlock(
                            title = "+${pile.amount}金",
                            subtitle = "点收取",
                            color = ChiliRed,
                            onClick = { onIntent(GameIntent.TapCoin(pile.id)) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onEndDay,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .sizeIn(minWidth = 200.dp, minHeight = 48.dp),
            ) {
                Text("结束一天")
            }
        }

        activeDrag?.let { drag ->
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (drag.fingerInRoot.x - drag.grabOffset.x).roundToInt(),
                            (drag.fingerInRoot.y - drag.grabOffset.y).roundToInt(),
                        )
                    }
                    .sizeIn(minWidth = 96.dp, minHeight = 72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(drag.color.copy(alpha = 0.92f))
                    .padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                HitBlockLabel(title = drag.title, subtitle = drag.subtitle)
            }
        }
    }
}

private fun handleDrop(
    fingerInRoot: Offset,
    payload: DragPayload,
    registry: DropTargetRegistry,
    onIntent: (GameIntent) -> Unit,
) {
    val prefer: ((DropTargetId) -> Boolean)? = when (payload) {
        is DragPayload.CoolNoodles -> { t -> t is DropTargetId.Blanch }
        is DragPayload.HotNoodles -> { t -> t is DropTargetId.Bowl }
        is DragPayload.Ingredient -> { t -> t is DropTargetId.Bowl }
        is DragPayload.SideReady -> { t -> t is DropTargetId.Bowl }
        is DragPayload.ReadyBowl -> { t -> t is DropTargetId.Customer || t is DropTargetId.Trash }
    }
    val target = registry.hit(fingerInRoot, prefer) ?: return
    when (payload) {
        is DragPayload.CoolNoodles -> {
            if (target is DropTargetId.Blanch) onIntent(GameIntent.DragCoolToBlanch())
        }
        is DragPayload.HotNoodles -> {
            val bowlId = (target as? DropTargetId.Bowl)?.id ?: return
            onIntent(GameIntent.DragHotToBowl(bowlId))
        }
        is DragPayload.Ingredient -> {
            val bowlId = (target as? DropTargetId.Bowl)?.id ?: return
            onIntent(GameIntent.DragIngredient(payload.id, bowlId))
        }
        is DragPayload.SideReady -> {
            val bowlId = (target as? DropTargetId.Bowl)?.id ?: return
            onIntent(GameIntent.DragSideToBowl(payload.sideId, bowlId))
        }
        is DragPayload.ReadyBowl -> {
            when (target) {
                is DropTargetId.Customer ->
                    onIntent(GameIntent.DragServe(payload.id, target.id))
                is DropTargetId.Trash ->
                    onIntent(GameIntent.DiscardBowl(payload.id))
                else -> Unit
            }
        }
    }
}

@Composable
private fun AttentionProgressBar(
    station: StationState,
    greenEnd: Long,
    redEnd: Long,
    burntEnd: Long,
) {
    val (elapsed, active) = when (station) {
        is StationState.Working -> station.attention.elapsedMs to true
        is StationState.Ready -> station.attention.elapsedMs to true
        is StationState.Idle -> 0L to false
    }
    if (!active) {
        Spacer(modifier = Modifier.height(10.dp))
        return
    }
    val progress = (elapsed.toFloat() / burntEnd.toFloat()).coerceIn(0f, 1f)
    val barColor = when {
        elapsed <= greenEnd -> BambooGreen
        elapsed <= redEnd -> WarmYellow
        else -> ChiliRed
    }
    val label = when {
        elapsed <= greenEnd -> "绿区 ${(progress * 100).toInt()}%"
        elapsed <= redEnd -> "软了！快捞"
        else -> "糊了！"
    }
    Column(modifier = Modifier.padding(top = 6.dp)) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = barColor,
            trackColor = InkText.copy(alpha = 0.12f),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = barColor,
        )
    }
}

@Composable
private fun HudBar(
    dayId: Int,
    remainSec: Int,
    coins: Int,
    muted: Boolean,
    onToggleMute: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WarmYellow.copy(alpha = 0.35f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (dayId > 0) "Day $dayId · ${remainSec}s" else "剩余 ${remainSec}s",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(text = "金币 $coins", style = MaterialTheme.typography.titleMedium)
        Button(onClick = onToggleMute, modifier = Modifier.sizeIn(minHeight = 48.dp)) {
            Text(if (muted) "音效:关" else "音效:开")
        }
    }
}

private fun bowlLabel(bowl: BowlState): String = when (bowl) {
    is BowlState.Empty -> "空·拖热面"
    is BowlState.Assembling -> {
        val q = GrayboxConfig.qualityLabel(bowl.quality)
        if (bowl.toppings.isEmpty()) "$q·待加料"
        else "$q·" + bowl.toppings.joinToString("+") { GrayboxConfig.displayName(it) }
    }
    is BowlState.ReadyToServe -> {
        val q = GrayboxConfig.qualityLabel(bowl.quality)
        "$q·" + bowl.toppings.joinToString("+") { GrayboxConfig.displayName(it) }
    }
}

private fun qualityColor(quality: ItemQuality, base: Color): Color = when (quality) {
    ItemQuality.Good -> base
    ItemQuality.Soft -> ChiliRed.copy(alpha = 0.85f)
    ItemQuality.Sticky -> ChiliRed
    ItemQuality.Burnt -> InkText
}

private fun toppingColor(id: IngredientId): Color = when (id) {
    IngredientId.SesameSauce -> SauceBrown
    IngredientId.SesameOil -> WarmYellow
    IngredientId.SaladOil -> WarmYellow.copy(green = 0.75f)
    IngredientId.ChiliOil -> ChiliRed
    IngredientId.Garlic -> BambooGreen.copy(red = 0.55f)
    IngredientId.Scallion -> BambooGreen
    IngredientId.Radish -> WarmYellow.copy(red = 0.95f)
    IngredientId.PickledBean -> BambooGreen.copy(blue = 0.35f)
    IngredientId.Brine -> BrickRed.copy(alpha = 0.85f)
    IngredientId.SoyMilk -> WarmYellow.copy(alpha = 0.95f)
    IngredientId.MianWo -> SauceBrown.copy(alpha = 0.75f)
    IngredientId.EggWine -> ChiliRed.copy(alpha = 0.65f)
}

@Preview(showBackground = true, heightDp = 960)
@Composable
private fun DayScreenPreview() {
    ReganmianLegendTheme {
        DayScreenContent(
            state = GameState.initialGraybox(),
            onIntent = {},
            onEndDay = {},
        )
    }
}
