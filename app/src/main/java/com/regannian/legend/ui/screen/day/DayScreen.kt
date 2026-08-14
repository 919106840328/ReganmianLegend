package com.regannian.legend.ui.screen.day

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.regannian.legend.data.progress.PlayerProgress
import com.regannian.legend.data.settings.GameSettings
import kotlinx.coroutines.delay
import com.regannian.legend.game.GrayboxConfig
import com.regannian.legend.game.copy.CustomerLines
import com.regannian.legend.game.intent.GameIntent
import com.regannian.legend.game.meta.ReputationRules
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
import com.regannian.legend.game.tutorial.TutorialSteps
import com.regannian.legend.game.upgrade.UpgradeEffects
import com.regannian.legend.game.upgrade.UpgradeId
import com.regannian.legend.ui.art.BlanchPotArt
import com.regannian.legend.ui.art.BowlArt
import com.regannian.legend.ui.art.CoinArt
import com.regannian.legend.ui.art.DraggableProp
import com.regannian.legend.ui.art.JarArt
import com.regannian.legend.ui.art.NoodleClumpArt
import com.regannian.legend.ui.art.PropHit
import com.regannian.legend.ui.art.SauceDripBurst
import com.regannian.legend.ui.art.SeatCustomerArt
import com.regannian.legend.ui.art.SidePanArt
import com.regannian.legend.ui.art.ThiefArt
import com.regannian.legend.ui.art.TrashBinArt
import com.regannian.legend.ui.art.WokArt
import com.regannian.legend.ui.component.ActiveDrag
import com.regannian.legend.ui.component.DragPayload
import com.regannian.legend.ui.component.DropTargetId
import com.regannian.legend.ui.component.DropTargetRegistry
import com.regannian.legend.ui.component.ShopBackdrop
import com.regannian.legend.ui.component.TutorialOverlay
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
    val reputation by PlayerProgress.reputation.collectAsStateWithLifecycle()
    val dayConfig = remember(dayId, reputation) {
        val raw = LevelRepository.fromAssets(context).load(dayId)
        raw.copy(patienceMul = raw.patienceMul * ReputationRules.patienceMul(reputation))
    }
    val viewModel: GameViewModel = viewModel(
        key = "day-$dayId",
        factory = GameViewModel.factory(dayConfig),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val muted by GameSettings.muted.collectAsStateWithLifecycle()
    val tutorialDone by GameSettings.tutorialDone.collectAsStateWithLifecycle()
    val sfxPlayer = remember { SfxPlayer(context.applicationContext) }
    var banner by remember { mutableStateOf<String?>(null) }
    var tutorialIndex by remember { mutableIntStateOf(0) }
    // 任意营业日：未完成或设置里点了重看，都会弹出引导
    val showTutorial = !tutorialDone
    LaunchedEffect(tutorialDone) {
        if (!tutorialDone) tutorialIndex = 0
    }

    DisposableEffect(sfxPlayer) {
        onDispose { sfxPlayer.release() }
    }
    LaunchedEffect(muted) { sfxPlayer.muted = muted }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is GameEffect.PlaySfx -> sfxPlayer.play(effect.id)
                is GameEffect.Toast -> banner = effect.message
                GameEffect.ServedOk -> banner = "出餐成功！"
                GameEffect.RejectServe -> banner = "这份对不上！"
            }
        }
    }
    LaunchedEffect(banner) {
        if (banner != null) {
            delay(1600)
            banner = null
        }
    }
    LaunchedEffect(dayId) { viewModel.restartDay() }
    LaunchedEffect(state.phase) {
        if (state.phase == DayPhase.Result) onEndDay(state.toDaySummary())
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DayScreenContent(
            state = state,
            muted = muted,
            onToggleMute = GameSettings::toggleMute,
            onIntent = viewModel::dispatch,
            onEndDay = { onEndDay(state.toDaySummary()) },
            onReplayTutorial = {
                tutorialIndex = 0
                GameSettings.resetTutorial()
            },
        )
        AnimatedVisibility(
            visible = banner != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp, start = 16.dp, end = 16.dp),
        ) {
            Text(
                text = banner.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                color = InkText,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(WarmYellow)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
        if (showTutorial) {
            val steps = TutorialSteps.ALL
            val idx = tutorialIndex.coerceIn(0, steps.lastIndex)
            TutorialOverlay(
                step = steps[idx],
                index = idx + 1,
                total = steps.size,
                onNext = {
                    if (idx >= steps.lastIndex) {
                        GameSettings.markTutorialDone()
                    } else {
                        tutorialIndex = idx + 1
                    }
                },
                onSkip = GameSettings::markTutorialDone,
            )
        }
    }
}

@Composable
fun DayScreenContent(
    state: GameState,
    onIntent: (GameIntent) -> Unit,
    onEndDay: () -> Unit,
    muted: Boolean = false,
    onToggleMute: () -> Unit = {},
    onReplayTutorial: () -> Unit = {},
) {
    val remainSec = ((state.dayDurationMs - state.clockMs).coerceAtLeast(0L) / 1000L).toInt()
    val waiting = state.customers.count { it.status == CustomerStatus.WaitingServe }
    val dropRegistry = rememberDropTargetRegistry()
    var activeDrag by remember { mutableStateOf<ActiveDrag?>(null) }
    val dragging = activeDrag != null
    val dust = state.stations.getValue(StationId.DustCook)
    val blanch = state.stations.getValue(StationId.Blanch)

    val paused = state.phase == DayPhase.Paused
    Box(modifier = Modifier.fillMaxSize()) {
        ShopBackdrop(showStreetProps = false, kitchenMode = true) {
        // 横屏柜台：上顾客、下操作台（参考沙威玛式融入背景）
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            HudBar(
                dayId = state.dayId,
                remainSec = remainSec,
                coins = state.coins,
                muted = muted,
                paused = paused,
                onToggleMute = onToggleMute,
                onPause = { onIntent(GameIntent.Pause) },
            )

            Text(
                text = "等餐 $waiting/${state.customers.size} · ${GrayboxConfig.QUALITY_HINT}",
                style = MaterialTheme.typography.labelMedium,
                color = CreamBg,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(InkText.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
            when (val thief = state.thief) {
                is com.regannian.legend.game.model.ThiefState.Present -> {
                    val targetHint = when (val t = thief.target) {
                        is com.regannian.legend.game.model.ThiefTarget.Bowl -> "盯着碗"
                        is com.regannian.legend.game.model.ThiefTarget.CounterCoins -> "盯着钱"
                    }
                    PropHit(
                        caption = "$targetHint · 点${thief.tapsDone}/${thief.tapsNeeded}",
                        onClick = { onIntent(GameIntent.TapThief) },
                        modifier = Modifier.fillMaxWidth(),
                        art = { ThiefArt() },
                    )
                }
                else -> Unit
            }
            // 上排顾客（柜台对面）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.34f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                state.customers.forEach { customer ->
                    val patiencePct = if (customer.patienceMaxMs <= 0L) 0
                    else ((customer.patienceMs * 100) / customer.patienceMaxMs).toInt()
                    val orderText = if (customer.kind == CustomerKind.EarlyMarketUncle) {
                        "大爷·素面"
                    } else {
                        GrayboxConfig.orderLabel(customer.order)
                    }
                    val bark = when (customer.status) {
                        CustomerStatus.Empty -> null
                        CustomerStatus.WaitingServe -> CustomerLines.waitingLine(patiencePct)
                        CustomerStatus.WaitingInteract ->
                            customer.bark ?: CustomerLines.UNCLE_WAIT_PAY
                        else -> customer.bark
                    }
                    val mood = CustomerLines.moodOf(customer.status, patiencePct, customer.bark)
                    PropHit(
                        caption = when (customer.status) {
                            CustomerStatus.Empty -> "空座"
                            else -> orderText
                        },
                        enabled = customer.status == CustomerStatus.WaitingServe ||
                            customer.status == CustomerStatus.WaitingInteract,
                        onClick = { onIntent(GameIntent.TapCustomer(customer.id)) },
                        modifier = Modifier
                            .weight(1f)
                            .dropTarget(
                                dropRegistry,
                                DropTargetId.Customer(customer.id),
                                enabled = customer.status == CustomerStatus.WaitingServe,
                            ),
                        art = {
                            SeatCustomerArt(
                                mood = mood,
                                uncle = customer.kind == CustomerKind.EarlyMarketUncle,
                                bark = bark,
                                arriveKey = when (customer.status) {
                                    CustomerStatus.Empty -> null
                                    else -> customer.id.value
                                },
                            )
                        },
                    )
                }
            }

            // 下排操作台：工位 | 碗 | 配料（横铺融入柜台）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.66f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.30f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState(), enabled = !dragging && !paused),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                Column(modifier = Modifier.weight(1f)) {
                    when (dust) {
                        is StationState.Idle -> PropHit(
                            caption = "掸面锅 · 点开工",
                            onClick = { onIntent(GameIntent.TapStation(StationId.DustCook)) },
                            modifier = Modifier.fillMaxWidth(),
                            art = { WokArt(working = false, hasNoodles = false) },
                        )
                        is StationState.Working -> {
                            val cookPct = dust.attention.elapsedMs.toFloat() /
                                dust.attention.greenEndMs.toFloat().coerceAtLeast(1f)
                            PropHit(
                                caption = "掸面锅 · 煮着…等满格",
                                pulse = true,
                                onClick = { onIntent(GameIntent.TapStation(StationId.DustCook)) },
                                modifier = Modifier.fillMaxWidth(),
                                art = {
                                    WokArt(
                                        working = true,
                                        hasNoodles = true,
                                        cookProgress = cookPct,
                                        quality = dust.quality,
                                    )
                                },
                            )
                        }
                        is StationState.Ready -> DraggableProp(
                            caption = "凉面·${GrayboxConfig.qualityLabel(dust.quality)}·点/拖去烫",
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
                            art = {
                                WokArt(
                                    working = false,
                                    hasNoodles = true,
                                    cookProgress = 1f,
                                    quality = dust.quality,
                                )
                            },
                        )
                    }
                    AttentionProgressBar(
                        station = dust,
                        readyMs = state.timings.dustCookMs,
                        goodUntilMs = state.timings.dustGoodUntilMs(),
                        redEnd = state.timings.dustRedMs,
                        burntEnd = state.timings.dustBurntMs,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    when (blanch) {
                        is StationState.Idle -> PropHit(
                            caption = "烫面锅 · 等凉面",
                            modifier = Modifier
                                .fillMaxWidth()
                                .dropTarget(dropRegistry, DropTargetId.Blanch, enabled = true),
                            art = { BlanchPotArt(working = false, hasNoodles = false) },
                        )
                        is StationState.Working -> {
                            val cookPct = blanch.attention.elapsedMs.toFloat() /
                                blanch.attention.greenEndMs.toFloat().coerceAtLeast(1f)
                            PropHit(
                                caption = "烫面锅 · 烫着…等满格",
                                pulse = true,
                                onClick = { onIntent(GameIntent.TapStation(StationId.Blanch)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .dropTarget(dropRegistry, DropTargetId.Blanch, enabled = false),
                                art = {
                                    BlanchPotArt(
                                        working = true,
                                        hasNoodles = true,
                                        cookProgress = cookPct,
                                        quality = blanch.quality,
                                    )
                                },
                            )
                        }
                        is StationState.Ready -> DraggableProp(
                            caption = "热面·${GrayboxConfig.qualityLabel(blanch.quality)}·点/拖入碗",
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
                            art = {
                                BlanchPotArt(
                                    working = false,
                                    hasNoodles = true,
                                    cookProgress = 1f,
                                    quality = blanch.quality,
                                )
                            },
                        )
                    }
                    AttentionProgressBar(
                        station = blanch,
                        readyMs = state.timings.blanchMs,
                        goodUntilMs = state.timings.blanchGoodUntilMs(),
                        redEnd = state.timings.blanchRedMs,
                        burntEnd = state.timings.blanchBurntMs,
                    )
                }
                } // stations pair row
                PropHit(
                    caption = "垃圾桶",
                    modifier = Modifier
                        .fillMaxWidth()
                        .dropTarget(dropRegistry, DropTargetId.Trash),
                    art = { TrashBinArt() },
                )
                } // left column

                // 中：碗
                Column(
                    modifier = Modifier
                        .weight(0.34f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState(), enabled = !dragging && !paused),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                    val filled = bowl !is BowlState.Empty
                    val quality = when (bowl) {
                        is BowlState.Assembling -> bowl.quality
                        is BowlState.ReadyToServe -> bowl.quality
                        is BowlState.Empty -> ItemQuality.Good
                    }
                    val toppingKey = toppings.sortedBy { it.name }.joinToString("+") { it.name }
                        .ifEmpty { if (filled) "noodles" else "empty" }
                    val borderMod = Modifier.border(
                        width = if (focused) 3.dp else 0.dp,
                        color = if (focused) WarmYellow else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                    )
                    val baseMod = Modifier
                        .weight(1f)
                        .then(borderMod)
                        .dropTarget(dropRegistry, DropTargetId.Bowl(bowlId))
                    val bowlVisual: @Composable () -> Unit = {
                        Box(contentAlignment = Alignment.Center) {
                            BowlArt(
                                filled = filled,
                                toppings = toppings,
                                quality = quality,
                            )
                            SauceDripBurst(
                                triggerKey = toppingKey,
                                color = if (IngredientId.SesameSauce in toppings) {
                                    SauceBrown
                                } else {
                                    WarmYellow
                                },
                            )
                        }
                    }

                    val liveCaption = bowlCaption(bowl, toppings, matched)
                    if (toppings.isNotEmpty()) {
                        DraggableProp(
                            caption = liveCaption,
                            payload = DragPayload.ReadyBowl(bowlId),
                            enabled = true,
                            onActiveDrag = { activeDrag = it },
                            onDrop = { finger, payload ->
                                handleDrop(finger, payload, dropRegistry, onIntent)
                            },
                            onClick = { onIntent(GameIntent.TapBowl(bowlId)) },
                            modifier = baseMod,
                            art = bowlVisual,
                        )
                    } else {
                        PropHit(
                            caption = liveCaption,
                            onClick = { onIntent(GameIntent.TapBowl(bowlId)) },
                            modifier = baseMod.sizeIn(minHeight = 72.dp),
                            art = bowlVisual,
                        )
                    }
                }
            }
            if (state.coinsOnCounter.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.coinsOnCounter.forEach { pile ->
                        PropHit(
                            caption = "+${pile.amount}金",
                            onClick = { onIntent(GameIntent.TapCoin(pile.id)) },
                            art = { CoinArt() },
                        )
                    }
                }
            }
            Button(
                onClick = onEndDay,
                enabled = !paused,
                modifier = Modifier.sizeIn(minWidth = 140.dp, minHeight = 40.dp),
            ) {
                Text("结束一天")
            }
                } // mid bowls

                // 右：配料槽
                Column(
                    modifier = Modifier
                        .weight(0.36f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState(), enabled = !dragging && !paused),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
            if (state.sideStations.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.sideStations.forEach { (sideId, side) ->
                        val name = GrayboxConfig.displayName(UpgradeEffects.sideIngredient(sideId))
                        when (side) {
                            is SideStationState.Idle -> PropHit(
                                caption = "$name · 点开工",
                                onClick = { onIntent(GameIntent.TapSide(sideId)) },
                                modifier = Modifier.weight(1f),
                                art = { SidePanArt(working = false) },
                            )
                            is SideStationState.Working -> PropHit(
                                caption = "$name · 点出锅",
                                pulse = true,
                                onClick = { onIntent(GameIntent.TapSide(sideId)) },
                                modifier = Modifier.weight(1f),
                                art = { SidePanArt(working = true) },
                            )
                            is SideStationState.Ready -> DraggableProp(
                                caption = "$name·${GrayboxConfig.qualityLabel(side.quality)}",
                                payload = DragPayload.SideReady(sideId),
                                enabled = true,
                                onActiveDrag = { activeDrag = it },
                                onDrop = { finger, payload ->
                                    handleDrop(finger, payload, dropRegistry, onIntent)
                                    activeDrag = null
                                },
                                onClick = { onIntent(GameIntent.TapSide(sideId)) },
                                modifier = Modifier.weight(1f),
                                art = { SidePanArt(working = false) },
                            )
                        }
                    }
                }
            }
            if (UpgradeId.D3 in state.ownedUpgrades || UpgradeId.B2 in state.ownedUpgrades) {
                Button(
                    onClick = { onIntent(GameIntent.ApplyClassicCombo) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .sizeIn(minHeight = 40.dp),
                ) {
                    Text(
                        if (UpgradeId.D3 in state.ownedUpgrades) "经典套餐键"
                        else "标准酱一键",
                    )
                }
            }
            state.toppingBar.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    rowItems.forEach { ingredient ->
                        DraggableProp(
                            caption = GrayboxConfig.displayName(ingredient),
                            payload = DragPayload.Ingredient(ingredient),
                            enabled = true,
                            onActiveDrag = { activeDrag = it },
                            onDrop = { finger, payload ->
                                handleDrop(finger, payload, dropRegistry, onIntent)
                            },
                            modifier = Modifier.weight(1f),
                            art = {
                                JarArt(
                                    fill = toppingColor(ingredient),
                                    ingredient = ingredient,
                                )
                            },
                        )
                    }
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
                } // right toppings
            } // counter row
        }
        }

        if (paused) {
            PauseOverlay(
                onResume = { onIntent(GameIntent.Resume) },
                onEndDay = onEndDay,
                onReplayTutorial = {
                    onReplayTutorial()
                    onIntent(GameIntent.Resume)
                },
            )
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
                    .sizeIn(minWidth = 72.dp, minHeight = 56.dp)
                    .padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (val p = drag.payload) {
                    DragPayload.CoolNoodles -> NoodleClumpArt(hot = false)
                    DragPayload.HotNoodles -> NoodleClumpArt(hot = true)
                    is DragPayload.Ingredient -> JarArt(
                        fill = toppingColor(p.id),
                        ingredient = p.id,
                    )
                    is DragPayload.SideReady -> SidePanArt(working = false)
                    is DragPayload.ReadyBowl -> {
                        val bowl = state.bowls[p.id]
                        val tops = when (bowl) {
                            is BowlState.Assembling -> bowl.toppings
                            is BowlState.ReadyToServe -> bowl.toppings
                            else -> emptySet()
                        }
                        val q = when (bowl) {
                            is BowlState.Assembling -> bowl.quality
                            is BowlState.ReadyToServe -> bowl.quality
                            else -> ItemQuality.Good
                        }
                        BowlArt(filled = true, toppings = tops, quality = q)
                    }
                }
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
    readyMs: Long,
    goodUntilMs: Long,
    redEnd: Long,
    burntEnd: Long,
) {
    val (elapsed, active, cooking) = when (station) {
        is StationState.Working -> Triple(station.attention.elapsedMs, true, true)
        is StationState.Ready -> Triple(station.attention.elapsedMs, true, false)
        is StationState.Idle -> Triple(0L, false, false)
    }
    if (!active) {
        Spacer(modifier = Modifier.height(10.dp))
        return
    }
    // 煮制阶段：进度相对「满格出锅」；出锅后：相对整段时间轴
    val progress = if (cooking) {
        (elapsed.toFloat() / readyMs.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
    } else {
        (elapsed.toFloat() / burntEnd.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
    }
    val barColor = when {
        cooking -> Color(0xFF3D8BDB) // 蓝=还在煮，别当成该捞
        elapsed <= goodUntilMs -> BambooGreen
        elapsed <= redEnd -> WarmYellow
        else -> ChiliRed
    }
    val label = when {
        cooking -> "煮着 ${(progress * 100).toInt()}% · 满格才出锅"
        elapsed <= goodUntilMs -> "好面宽限 · 尽快拖走"
        elapsed <= redEnd -> "软了 · 快捞"
        else -> "糊了！"
    }
    Column(modifier = Modifier.padding(top = 6.dp)) {
        // 分段底轨：煮 / 好面 / 软 / 糊
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(InkText.copy(alpha = 0.10f)),
        ) {
            val cookW = (readyMs.toFloat() / burntEnd.toFloat()).coerceIn(0.08f, 0.55f)
            val goodW = ((goodUntilMs - readyMs).toFloat() / burntEnd.toFloat()).coerceAtLeast(0.08f)
            val softW = ((redEnd - goodUntilMs).toFloat() / burntEnd.toFloat()).coerceAtLeast(0.08f)
            Row(modifier = Modifier.fillMaxSize()) {
                Box(Modifier.weight(cookW).fillMaxSize().background(Color(0xFF3D8BDB).copy(alpha = 0.22f)))
                Box(Modifier.weight(goodW).fillMaxSize().background(BambooGreen.copy(alpha = 0.22f)))
                Box(Modifier.weight(softW).fillMaxSize().background(WarmYellow.copy(alpha = 0.28f)))
                Box(Modifier.weight(1f).fillMaxSize().background(ChiliRed.copy(alpha = 0.22f)))
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = barColor,
                trackColor = Color.Transparent,
            )
        }
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
    paused: Boolean,
    onToggleMute: () -> Unit,
    onPause: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(InkText.copy(alpha = 0.42f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (dayId > 0) "Day $dayId · ${remainSec}s" else "剩余 ${remainSec}s",
            style = MaterialTheme.typography.titleMedium,
            color = CreamBg,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "金币 $coins",
            style = MaterialTheme.typography.titleMedium,
            color = WarmYellow,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        OutlinedButton(
            onClick = onPause,
            enabled = !paused,
            modifier = Modifier.sizeIn(minHeight = 44.dp),
        ) {
            Text("暂停")
        }
        Spacer(modifier = Modifier.width(6.dp))
        Button(onClick = onToggleMute, modifier = Modifier.sizeIn(minHeight = 44.dp)) {
            Text(if (muted) "音效:关" else "音效:开")
        }
    }
}

@Composable
private fun PauseOverlay(
    onResume: () -> Unit,
    onEndDay: () -> Unit,
    onReplayTutorial: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InkText.copy(alpha = 0.45f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(RoundedCornerShape(18.dp))
                .background(WarmYellow.copy(alpha = 0.95f))
                .border(2.dp, SauceBrown.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("小憩一下", style = MaterialTheme.typography.headlineSmall, color = InkText)
            Text(
                text = "时间已暂停，锅里的面也先歇着",
                style = MaterialTheme.typography.bodyMedium,
                color = InkText.copy(alpha = 0.75f),
            )
            Button(
                onClick = onResume,
                modifier = Modifier.sizeIn(minWidth = 200.dp, minHeight = 48.dp),
            ) {
                Text("继续过早")
            }
            OutlinedButton(
                onClick = onReplayTutorial,
                modifier = Modifier.sizeIn(minWidth = 200.dp, minHeight = 48.dp),
            ) {
                Text("重看引导")
            }
            OutlinedButton(
                onClick = onEndDay,
                modifier = Modifier.sizeIn(minWidth = 200.dp, minHeight = 48.dp),
            ) {
                Text("结束今天")
            }
        }
    }
}

/** 无论是否可出餐，都实时展示配料表。 */
private fun bowlCaption(
    bowl: BowlState,
    toppings: Set<IngredientId>,
    matched: Boolean,
): String {
    val head = when (bowl) {
        is BowlState.Empty -> "空·拖热面"
        is BowlState.Assembling -> GrayboxConfig.qualityLabel(bowl.quality)
        is BowlState.ReadyToServe -> GrayboxConfig.qualityLabel(bowl.quality)
    }
    val tops = GrayboxConfig.toppingsLine(toppings)
    val tail = when {
        bowl is BowlState.Empty -> ""
        matched -> " · 可出餐"
        toppings.isEmpty() && bowl !is BowlState.Empty -> " · 待加料"
        else -> ""
    }
    return if (bowl is BowlState.Empty) head else "$head · $tops$tail"
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
