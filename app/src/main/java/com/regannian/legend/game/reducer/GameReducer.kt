package com.regannian.legend.game.reducer

import com.regannian.legend.game.GrayboxConfig
import com.regannian.legend.game.copy.CustomerLines
import com.regannian.legend.game.intent.GameIntent
import com.regannian.legend.game.model.AttentionWindow
import com.regannian.legend.game.model.BowlId
import com.regannian.legend.game.model.BowlState
import com.regannian.legend.game.model.CoinPile
import com.regannian.legend.game.model.CoinPileId
import com.regannian.legend.game.model.CustomerId
import com.regannian.legend.game.model.CustomerKind
import com.regannian.legend.game.model.CustomerState
import com.regannian.legend.game.model.CustomerStatus
import com.regannian.legend.game.model.DayPhase
import com.regannian.legend.game.model.IngredientId
import com.regannian.legend.game.model.ItemQuality
import com.regannian.legend.game.model.ServeVerdict
import com.regannian.legend.game.model.SideId
import com.regannian.legend.game.model.SideStationState
import com.regannian.legend.game.model.StationId
import com.regannian.legend.game.model.StationOutput
import com.regannian.legend.game.model.StationState
import com.regannian.legend.game.model.ThiefState
import com.regannian.legend.game.model.ThiefTarget
import com.regannian.legend.game.state.GameState
import com.regannian.legend.game.upgrade.UpgradeEffects
import com.regannian.legend.game.upgrade.UpgradeId

/**
 * C1：AttentionWindow 过煮/过烫；主料错拒收；次要缺料低价；扔弃 −2。
 */
object GameReducer {

    fun reduce(state: GameState, intent: GameIntent): Transition {
        val transition = when (intent) {
            is GameIntent.Pause -> {
                if (state.phase != DayPhase.Playing) Transition.none(state)
                else Transition(state.copy(phase = DayPhase.Paused))
            }
            is GameIntent.Resume -> {
                if (state.phase != DayPhase.Paused) Transition.none(state)
                else Transition(state.copy(phase = DayPhase.Playing))
            }
            is GameIntent.Tick -> onTick(state)
            is GameIntent.TapStation -> {
                if (state.phase != DayPhase.Playing) Transition.none(state)
                else onTapStation(state, intent.id)
            }
            is GameIntent.TapBowl -> {
                if (state.phase != DayPhase.Playing) Transition.none(state)
                else onTapBowl(state, intent.id)
            }
            is GameIntent.DragCoolToBlanch -> {
                if (state.phase != DayPhase.Playing) Transition.none(state)
                else dragCoolToBlanch(state)
            }
            is GameIntent.DragHotToBowl -> {
                if (state.phase != DayPhase.Playing) Transition.none(state)
                else dragHotToBowl(state, intent.bowlId)
            }
            is GameIntent.DragIngredient -> {
                if (state.phase != DayPhase.Playing) Transition.none(state)
                else addIngredientToBowl(state, intent.ingredientId, intent.bowlId)
            }
            is GameIntent.DragServe -> {
                if (state.phase != DayPhase.Playing) Transition.none(state)
                else serveBowlToCustomer(state, intent.bowlId, intent.customerId)
            }
            is GameIntent.DiscardBowl -> {
                if (state.phase != DayPhase.Playing) Transition.none(state)
                else discardBowl(state, intent.bowlId)
            }
            is GameIntent.ApplyClassicCombo -> {
                if (state.phase != DayPhase.Playing) Transition.none(state)
                else applyClassicCombo(state)
            }
            is GameIntent.TapCustomer -> {
                if (state.phase != DayPhase.Playing) Transition.none(state)
                else onTapCustomer(state, intent.id)
            }
            is GameIntent.TapThief -> {
                if (state.phase != DayPhase.Playing) Transition.none(state)
                else onTapThief(state)
            }
            is GameIntent.TapSide -> {
                if (state.phase != DayPhase.Playing) Transition.none(state)
                else onTapSide(state, intent.sideId)
            }
            is GameIntent.DragSideToBowl -> {
                if (state.phase != DayPhase.Playing) Transition.none(state)
                else dragSideToBowl(state, intent.sideId, intent.bowlId)
            }
            is GameIntent.TapCoin -> {
                if (state.phase != DayPhase.Playing && state.phase != DayPhase.Paused) {
                    Transition.none(state)
                } else {
                    onTapCoin(state, intent.pileId)
                }
            }
        }
        return attachClickSfx(state, intent, transition)
    }

    private fun attachClickSfx(
        before: GameState,
        intent: GameIntent,
        transition: Transition,
    ): Transition {
        val clickable = intent is GameIntent.TapStation ||
            intent is GameIntent.TapBowl ||
            intent is GameIntent.TapCoin ||
            intent is GameIntent.DragCoolToBlanch ||
            intent is GameIntent.DragHotToBowl ||
            intent is GameIntent.DragIngredient ||
            intent is GameIntent.DiscardBowl ||
            intent is GameIntent.ApplyClassicCombo ||
            intent is GameIntent.TapCustomer ||
            intent is GameIntent.TapThief ||
            intent is GameIntent.TapSide ||
            intent is GameIntent.DragSideToBowl
        if (!clickable) return transition
        if (transition.state == before) return transition
        if (transition.effects.any { it is GameEffect.Toast }) return transition
        return transition.copy(effects = transition.effects + GameEffect.PlaySfx(SfxId.Click))
    }

    private fun onTick(state: GameState): Transition {
        if (state.phase != DayPhase.Playing) return Transition.none(state)
        val step = GrayboxConfig.TICK_STEP_MS
        var next = state.copy(clockMs = state.clockMs + step)

        next = next.copy(
            stations = next.stations.mapValues { (id, station) ->
                advanceStation(id, station, step, next.timings)
            },
        )

        var satSum = next.daySatSum
        var satCount = next.daySatCount
        var leftThisTick = 0
        val customers = next.customers.map { customer ->
            if (customer.status != CustomerStatus.WaitingServe &&
                customer.status != CustomerStatus.WaitingInteract
            ) {
                customer
            } else {
                val patience = (customer.patienceMs - step).coerceAtLeast(0L)
                if (patience == 0L) {
                    satSum += 0f
                    satCount += 1
                    leftThisTick += 1
                    customer.copy(
                        status = CustomerStatus.LeftAngry,
                        patienceMs = 0L,
                        bark = CustomerLines.FED_UP,
                    )
                } else {
                    customer.copy(patienceMs = patience)
                }
            }
        }
        next = next.copy(customers = customers, daySatSum = satSum, daySatCount = satCount)
        next = maybeSpawn(next, step)

        val effects = mutableListOf<GameEffect>()
        if (leftThisTick > 0) effects += GameEffect.PlaySfx(SfxId.Leave)
        effects += overcookBellEffects(state, next)

        next = processPendingCool(next)
        next = maybeAutoDust(next, step)
        next = maybeAutoBowl(next)
        next = maybeAutoCollect(next)

        val (afterThief, thiefEffects) = advanceThief(next, step)
        next = afterThief
        effects += thiefEffects

        val (afterSides, sideEffects) = advanceSides(next, step)
        next = afterSides
        effects += sideEffects

        if (next.clockMs >= next.dayDurationMs) {
            next = next.copy(phase = DayPhase.Result, clockMs = next.dayDurationMs)
        }

        return Transition(next, effects)
    }

    private fun overcookBellEffects(before: GameState, after: GameState): List<GameEffect> {
        if (UpgradeId.C3 !in after.ownedUpgrades) return emptyList()
        val out = mutableListOf<GameEffect>()
        fun check(id: StationId, label: String) {
            val b = before.stations[id]
            val a = after.stations[id]
            if (b is StationState.Working && a is StationState.Working) {
                val green = a.attention.greenEndMs
                if (b.attention.elapsedMs < green && a.attention.elapsedMs >= green) {
                    out += GameEffect.Toast("$label 过绿区了！")
                    out += GameEffect.PlaySfx(SfxId.Click)
                }
            }
        }
        check(StationId.DustCook, "掸面")
        check(StationId.Blanch, "烫面")
        return out
    }

    private fun processPendingCool(state: GameState): GameState {
        val pending = state.pendingCoolQuality ?: return state
        val blanch = state.stations[StationId.Blanch]
        if (blanch !is StationState.Idle) return state
        val attention = AttentionWindow(
            greenEndMs = state.timings.blanchMs,
            redEndMs = state.timings.blanchRedMs,
            elapsedMs = 0L,
        )
        val working = StationState.Working(
            remainingMs = state.timings.blanchMs,
            totalMs = state.timings.blanchMs,
            output = StationOutput.HotNoodles,
            attention = attention,
            quality = pending,
        )
        return state.copy(
            pendingCoolQuality = null,
            stations = state.stations + (StationId.Blanch to working),
        )
    }

    private fun maybeAutoDust(state: GameState, step: Long): GameState {
        val interval = UpgradeEffects.autoDustIntervalMs(state.ownedUpgrades) ?: return state
        val dust = state.stations[StationId.DustCook]
        if (dust !is StationState.Idle) {
            return state.copy(autoDustAccMs = 0L)
        }
        val acc = state.autoDustAccMs + step
        if (acc < interval) return state.copy(autoDustAccMs = acc)
        val attention = AttentionWindow(
            greenEndMs = state.timings.dustCookMs,
            redEndMs = state.timings.dustRedMs,
            elapsedMs = 0L,
        )
        val working = StationState.Working(
            remainingMs = state.timings.dustCookMs,
            totalMs = state.timings.dustCookMs,
            output = StationOutput.CoolNoodles,
            attention = attention,
        )
        return state.copy(
            autoDustAccMs = 0L,
            stations = state.stations + (StationId.DustCook to working),
        )
    }

    private fun maybeAutoBowl(state: GameState): GameState {
        if (UpgradeId.C4 !in state.ownedUpgrades) return state
        val blanch = state.stations[StationId.Blanch]
        if (blanch !is StationState.Ready) return state
        val bowlId = state.firstEmptyBowlId() ?: return state
        return dragHotToBowl(state, bowlId).state
    }

    private fun maybeAutoCollect(state: GameState): GameState {
        if (UpgradeId.F6 !in state.ownedUpgrades) return state
        if (state.coinsOnCounter.isEmpty()) return state
        val sum = state.coinsOnCounter.sumOf { it.amount }
        // 95% 效率
        val gained = (sum * 95) / 100
        return state.copy(
            coins = state.coins + gained.coerceAtLeast(0),
            coinsOnCounter = emptyList(),
        )
    }

    private fun advanceThief(state: GameState, step: Long): Pair<GameState, List<GameEffect>> {
        when (val thief = state.thief) {
            is ThiefState.Absent -> {
                if (state.dayId < GrayboxConfig.THIEF_MIN_DAY) return state to emptyList()
                val cd = (state.thiefCooldownMs - step).coerceAtLeast(0L)
                if (cd > 0L) return state.copy(thiefCooldownMs = cd) to emptyList()
                val target = pickThiefTarget(state) ?: return state.copy(thiefCooldownMs = step) to emptyList()
                val tapsNeeded = if (UpgradeId.F5 in state.ownedUpgrades) {
                    1
                } else {
                    GrayboxConfig.THIEF_TAPS_DEFAULT
                }
                return state.copy(
                    thief = ThiefState.Present(
                        tapsDone = 0,
                        tapsNeeded = tapsNeeded,
                        escapeMs = GrayboxConfig.THIEF_ESCAPE_MS,
                        target = target,
                    ),
                    thiefCooldownMs = 0L,
                ) to listOf(GameEffect.Toast("偷碗贼来了！快点他"))
            }
            is ThiefState.Present -> {
                val left = thief.escapeMs - step
                if (left > 0L) {
                    return state.copy(thief = thief.copy(escapeMs = left)) to emptyList()
                }
                return applyThiefSteal(state, thief) to listOf(GameEffect.Toast("被偷了…"))
            }
        }
    }

    private fun pickThiefTarget(state: GameState): ThiefTarget? {
        val readyBowl = state.bowls.entries.firstOrNull { it.value is BowlState.ReadyToServe }?.key
        if (readyBowl != null) return ThiefTarget.Bowl(readyBowl)
        val assembling = state.bowls.entries.firstOrNull { it.value is BowlState.Assembling }?.key
        if (assembling != null) return ThiefTarget.Bowl(assembling)
        if (state.coinsOnCounter.isNotEmpty() || state.coins > 0) return ThiefTarget.CounterCoins
        return null
    }

    private fun applyThiefSteal(state: GameState, thief: ThiefState.Present): GameState {
        val cleared = when (val t = thief.target) {
            is ThiefTarget.Bowl -> state.copy(
                bowls = state.bowls + (t.bowlId to BowlState.Empty),
            )
            is ThiefTarget.CounterCoins -> state.copy(
                coinsOnCounter = emptyList(),
                coins = (state.coins - 8).coerceAtLeast(0),
            )
        }
        return cleared.copy(
            thief = ThiefState.Absent,
            thiefCooldownMs = GrayboxConfig.THIEF_COOLDOWN_MS,
        )
    }

    private fun onTapThief(state: GameState): Transition {
        val thief = state.thief
        if (thief !is ThiefState.Present) {
            return Transition(state, listOf(GameEffect.Toast("没有贼")))
        }
        val taps = thief.tapsDone + 1
        if (taps >= thief.tapsNeeded) {
            return Transition(
                state.copy(
                    thief = ThiefState.Absent,
                    thiefCooldownMs = GrayboxConfig.THIEF_COOLDOWN_MS,
                    dayStats = state.dayStats.copy(chasedThief = true),
                ),
                listOf(
                    GameEffect.Toast(
                        if (UpgradeId.F5 in state.ownedUpgrades) "防偷铃！贼跑了"
                        else "赶走了偷碗贼",
                    ),
                    GameEffect.PlaySfx(SfxId.Click),
                ),
            )
        }
        return Transition(
            state.copy(thief = thief.copy(tapsDone = taps)),
            listOf(GameEffect.PlaySfx(SfxId.Click)),
        )
    }

    private fun advanceSides(state: GameState, step: Long): Pair<GameState, List<GameEffect>> {
        if (state.sideStations.isEmpty()) return state to emptyList()
        val effects = mutableListOf<GameEffect>()
        val next = state.sideStations.mapValues { (id, side) ->
            when (side) {
                is SideStationState.Idle -> side
                is SideStationState.Working -> {
                    val remain = side.remainingMs - step
                    if (remain <= 0L) SideStationState.Ready() else side.copy(remainingMs = remain)
                }
                is SideStationState.Ready -> {
                    if (id != SideId.MianWo || UpgradeId.E4 in state.ownedUpgrades) {
                        side
                    } else {
                        val elapsed = side.elapsedMs + step
                        when {
                            elapsed >= 4_000L && side.quality != ItemQuality.Burnt -> {
                                effects += GameEffect.Toast("面窝糊了！")
                                SideStationState.Ready(ItemQuality.Burnt, elapsed)
                            }
                            elapsed >= 2_000L && side.quality == ItemQuality.Good ->
                                SideStationState.Ready(ItemQuality.Soft, elapsed)
                            else -> side.copy(elapsedMs = elapsed)
                        }
                    }
                }
            }
        }
        return state.copy(sideStations = next) to effects
    }

    private fun onTapSide(state: GameState, sideId: SideId): Transition {
        val station = state.sideStations[sideId]
            ?: return Transition(state, listOf(GameEffect.Toast("未解锁该辅食")))
        return when (station) {
            is SideStationState.Idle -> {
                val total = GrayboxConfig.sideCookMs(sideId, state.ownedUpgrades)
                Transition(
                    state.copy(
                        sideStations = state.sideStations + (
                            sideId to SideStationState.Working(total, total)
                            ),
                    ),
                )
            }
            is SideStationState.Working -> {
                Transition(
                    state.copy(
                        sideStations = state.sideStations + (
                            sideId to SideStationState.Ready()
                            ),
                    ),
                    listOf(GameEffect.Toast("辅食好了，拖进碗")),
                )
            }
            is SideStationState.Ready -> Transition(state, listOf(GameEffect.Toast("拖到碗里")))
        }
    }

    private fun dragSideToBowl(state: GameState, sideId: SideId, bowlId: BowlId): Transition {
        val side = state.sideStations[sideId]
        if (side !is SideStationState.Ready) {
            return Transition(state, listOf(GameEffect.Toast("辅食还没好")))
        }
        if (side.quality == ItemQuality.Burnt) {
            return Transition(
                state.copy(sideStations = state.sideStations + (sideId to SideStationState.Idle)),
                listOf(GameEffect.Toast("糊了，扔掉重做")),
            )
        }
        val ingredient = UpgradeEffects.sideIngredient(sideId)
        var next = addIngredientToBowl(state, ingredient, bowlId).state
        // 糊软面窝：降低碗品质
        if (side.quality != ItemQuality.Good) {
            val bowl = next.bowls[bowlId]
            next = when (bowl) {
                is BowlState.Assembling -> next.copy(
                    bowls = next.bowls + (
                        bowlId to bowl.copy(
                            quality = worseQuality(bowl.quality, side.quality),
                        )
                        ),
                )
                is BowlState.ReadyToServe -> next.copy(
                    bowls = next.bowls + (
                        bowlId to bowl.copy(
                            quality = worseQuality(bowl.quality, side.quality),
                        )
                        ),
                )
                else -> next
            }
        }
        return Transition(
            next.copy(sideStations = next.sideStations + (sideId to SideStationState.Idle)),
            listOf(GameEffect.Toast("${GrayboxConfig.displayName(ingredient)}已入碗")),
        )
    }

    /** 座位非等餐时可刷入新客；满座则攒满一间隔后等待空位。 */
    private fun maybeSpawn(state: GameState, step: Long): GameState {
        if (state.spawnIntervalMs <= 0L || state.spawnIntervalMs >= Long.MAX_VALUE / 8) {
            return state
        }
        if (state.recipePool.isEmpty()) return state
        var acc = state.spawnAccMs + step
        var customers = state.customers
        var seq = state.nextCustomerSeq
        while (acc >= state.spawnIntervalMs) {
            val slot = customers.indexOfFirst { it.status != CustomerStatus.WaitingServe }
            if (slot < 0) {
                acc = state.spawnIntervalMs
                break
            }
            acc -= state.spawnIntervalMs
            val order = state.recipePool[seq % state.recipePool.size]
            seq += 1
            val id = customers[slot].id
            val newbie = CustomerState(
                id = id,
                status = CustomerStatus.WaitingServe,
                order = order,
                patienceMs = state.patienceMaxMs,
                patienceMaxMs = state.patienceMaxMs,
                kind = CustomerKind.Normal,
            )
            customers = customers.toMutableList().also { it[slot] = newbie }
        }
        return state.copy(
            customers = customers,
            spawnAccMs = acc,
            nextCustomerSeq = seq,
        )
    }

    private fun advanceStation(
        id: StationId,
        station: StationState,
        step: Long,
        timings: com.regannian.legend.game.upgrade.StationTimings,
    ): StationState {
        val (readyMs, goodUntil, red, burnt) = when (id) {
            StationId.DustCook -> listOf(
                timings.dustCookMs,
                timings.dustGoodUntilMs(),
                timings.dustRedMs,
                timings.dustBurntMs,
            )
            StationId.Blanch -> listOf(
                timings.blanchMs,
                timings.blanchGoodUntilMs(),
                timings.blanchRedMs,
                timings.blanchBurntMs,
            )
        }
        return when (station) {
            is StationState.Idle -> station
            is StationState.Working -> {
                val elapsed = station.attention.elapsedMs + step
                val fromElapsed = GrayboxConfig.qualityFromElapsed(
                    readyMs, goodUntil, red, burnt, elapsed,
                )
                val quality = worseQuality(station.quality, fromElapsed)
                val attention = station.attention.copy(elapsedMs = elapsed)
                val remain = station.remainingMs - step
                if (remain <= 0L || elapsed >= readyMs) {
                    StationState.Ready(
                        output = station.output,
                        quality = quality,
                        attention = attention,
                    )
                } else {
                    station.copy(
                        remainingMs = remain,
                        attention = attention,
                        quality = quality,
                    )
                }
            }
            is StationState.Ready -> {
                val elapsed = station.attention.elapsedMs + step
                val fromElapsed = GrayboxConfig.qualityFromElapsed(
                    readyMs, goodUntil, red, burnt, elapsed,
                )
                val quality = worseQuality(station.quality, fromElapsed)
                station.copy(
                    quality = quality,
                    attention = station.attention.copy(elapsedMs = elapsed),
                )
            }
        }
    }

    private fun worseQuality(a: ItemQuality, b: ItemQuality): ItemQuality =
        if (a.ordinal >= b.ordinal) a else b

    private fun onTapStation(state: GameState, id: StationId): Transition {
        return when (id) {
            StationId.DustCook -> tapDustCook(state)
            StationId.Blanch -> tapBlanch(state)
        }
    }

    private fun tapDustCook(state: GameState): Transition {
        val t = state.timings
        val station = state.stations.getValue(StationId.DustCook)
        return when (station) {
            is StationState.Idle -> {
                val attention = AttentionWindow(
                    greenEndMs = t.dustCookMs,
                    redEndMs = t.dustRedMs,
                    elapsedMs = 0L,
                )
                val working = StationState.Working(
                    remainingMs = t.dustCookMs,
                    totalMs = t.dustCookMs,
                    output = StationOutput.CoolNoodles,
                    attention = attention,
                )
                Transition(state.copy(stations = state.stations + (StationId.DustCook to working)))
            }
            is StationState.Working -> {
                // 煮面中不可点加速/提前出锅，满格后自动 Ready
                Transition(state, listOf(GameEffect.Toast("还在煮，等进度满格再捞")))
            }
            is StationState.Ready -> {
                if (state.stations[StationId.Blanch] is StationState.Idle) {
                    dragCoolToBlanch(state)
                } else {
                    Transition(state, listOf(GameEffect.Toast("烫锅忙，或拖凉面过去")))
                }
            }
        }
    }

    private fun tapBlanch(state: GameState): Transition {
        val t = state.timings
        val station = state.stations.getValue(StationId.Blanch)
        return when (station) {
            is StationState.Idle -> Transition(state, listOf(GameEffect.Toast("先拖/点凉面过来")))
            is StationState.Working -> {
                Transition(state, listOf(GameEffect.Toast("还在烫，等进度满格再停")))
            }
            is StationState.Ready -> {
                val bowlId = state.focusedBowlId
                    ?.takeIf { state.bowls[it] is BowlState.Empty }
                    ?: state.firstEmptyBowlId()
                    ?: return Transition(state, listOf(GameEffect.Toast("没有空碗")))
                dragHotToBowl(state, bowlId)
            }
        }
    }

    private fun dragCoolToBlanch(state: GameState): Transition {
        val dust = state.stations.getValue(StationId.DustCook)
        if (dust !is StationState.Ready || dust.output != StationOutput.CoolNoodles) {
            return Transition(state, listOf(GameEffect.Toast("没有凉面可烫")))
        }
        val blanch = state.stations.getValue(StationId.Blanch)
        if (blanch !is StationState.Idle) {
            if (UpgradeId.C1 in state.ownedUpgrades && state.pendingCoolQuality == null) {
                return Transition(
                    state.copy(
                        pendingCoolQuality = dust.quality,
                        stations = state.stations + (StationId.DustCook to StationState.Idle),
                    ),
                    listOf(GameEffect.Toast("凉面已排队等烫")),
                )
            }
            return Transition(state, listOf(GameEffect.Toast("烫锅忙碌")))
        }
        val attention = AttentionWindow(
            greenEndMs = state.timings.blanchMs,
            redEndMs = state.timings.blanchRedMs,
            elapsedMs = 0L,
        )
        val startQuality = dust.quality
        val working = StationState.Working(
            remainingMs = state.timings.blanchMs,
            totalMs = state.timings.blanchMs,
            output = StationOutput.HotNoodles,
            attention = attention,
            quality = startQuality,
        )
        return Transition(
            state.copy(
                stations = state.stations +
                    (StationId.DustCook to StationState.Idle) +
                    (StationId.Blanch to working),
            ),
        )
    }

    private fun dragHotToBowl(state: GameState, bowlId: BowlId): Transition {
        val blanch = state.stations.getValue(StationId.Blanch)
        if (blanch !is StationState.Ready || blanch.output != StationOutput.HotNoodles) {
            return Transition(state, listOf(GameEffect.Toast("没有热面")))
        }
        val bowl = state.bowls[bowlId]
        if (bowl !is BowlState.Empty) {
            return Transition(state, listOf(GameEffect.Toast("碗被占用")))
        }
        var toppings = emptySet<IngredientId>()
        if (UpgradeId.A4 in state.ownedUpgrades && IngredientId.SesameOil in state.toppingBar) {
            toppings = toppings + IngredientId.SesameOil
        }
        if (UpgradeId.B5 in state.ownedUpgrades && IngredientId.SesameSauce in state.toppingBar) {
            toppings = toppings + IngredientId.SesameSauce
        }
        if (UpgradeId.D1 in state.ownedUpgrades && IngredientId.Brine in state.toppingBar) {
            toppings = toppings + IngredientId.Brine
        }
        val assembled = if (toppings.isEmpty()) {
            BowlState.Assembling(quality = blanch.quality)
        } else if (canMarkReady(state, toppings)) {
            BowlState.ReadyToServe(quality = blanch.quality, toppings = toppings)
        } else {
            BowlState.Assembling(quality = blanch.quality, toppings = toppings)
        }
        return Transition(
            state.copy(
                bowls = state.bowls + (bowlId to assembled),
                focusedBowlId = bowlId,
                stations = state.stations + (StationId.Blanch to StationState.Idle),
            ),
        )
    }

    private fun onTapBowl(state: GameState, id: BowlId): Transition {
        if (!state.bowls.containsKey(id)) return Transition.none(state)
        return Transition(state.copy(focusedBowlId = id))
    }

    private fun applyClassicCombo(state: GameState): Transition {
        val hasD3 = UpgradeId.D3 in state.ownedUpgrades
        val hasB2 = UpgradeId.B2 in state.ownedUpgrades
        if (!hasD3 && !hasB2) {
            return Transition(state, listOf(GameEffect.Toast("未购买套餐/标准酱")))
        }
        val bowlId = state.focusedBowlId
            ?: return Transition(state, listOf(GameEffect.Toast("先选中碗")))
        val bowl = state.bowls[bowlId]
        if (bowl !is BowlState.Assembling && bowl !is BowlState.ReadyToServe) {
            return Transition(state, listOf(GameEffect.Toast("先把热面拖进碗")))
        }
        val wanted = buildList {
            add(IngredientId.SesameSauce)
            if (hasD3) {
                add(IngredientId.Scallion)
                if (IngredientId.Radish in state.toppingBar) add(IngredientId.Radish)
            }
        }.filter { it in state.toppingBar }
        if (wanted.isEmpty()) {
            return Transition(state, listOf(GameEffect.Toast("今日无可用套餐料")))
        }
        var next = state
        wanted.forEach { ing ->
            next = addIngredientToBowl(next, ing, bowlId).state
        }
        val tip = if (hasD3) "套餐已加" else "标准酱已加"
        return Transition(next, listOf(GameEffect.Toast(tip)))
    }

    fun addIngredientToBowl(
        state: GameState,
        ingredientId: IngredientId,
        bowlId: BowlId,
    ): Transition {
        val bowl = state.bowls[bowlId]
            ?: return Transition(state, listOf(GameEffect.Toast("碗不存在")))
        val (quality, existing) = when (bowl) {
            is BowlState.Assembling -> bowl.quality to bowl.toppings
            is BowlState.ReadyToServe -> bowl.quality to bowl.toppings
            is BowlState.Empty -> {
                return Transition(state, listOf(GameEffect.Toast("先把热面拖进碗")))
            }
        }
        if (ingredientId in existing) return Transition.none(state)

        val toppings = existing + ingredientId
        // 有主料且至少一样次要/或已匹配任一单 → 可拖出餐；否则继续组装
        val nextBowl: BowlState =
            if (canMarkReady(state, toppings)) {
                BowlState.ReadyToServe(quality = quality, toppings = toppings)
            } else {
                BowlState.Assembling(quality = quality, toppings = toppings)
            }

        return Transition(
            state.copy(
                bowls = state.bowls + (bowlId to nextBowl),
                focusedBowlId = bowlId,
            ),
        )
    }

    private fun canMarkReady(state: GameState, toppings: Set<IngredientId>): Boolean {
        // 至少含主料，且满足某位客人的主料要求（可拖去尝试出餐；次要缺料在 serve 时降价）
        if (toppings.none { it in GrayboxConfig.MAIN_INGREDIENTS }) return false
        return state.customers.any { c ->
            c.status == CustomerStatus.WaitingServe &&
                c.order.required.intersect(GrayboxConfig.MAIN_INGREDIENTS).all { it in toppings }
        }
    }

    fun serveBowlToCustomer(
        state: GameState,
        bowlId: BowlId,
        customerId: CustomerId,
    ): Transition {
        val customer = state.customers.find { it.id == customerId }
            ?: return Transition.none(state)
        if (customer.status != CustomerStatus.WaitingServe) {
            return Transition(state, listOf(GameEffect.RejectServe))
        }

        val (quality, rawToppings) = when (val bowl = state.bowls[bowlId]) {
            is BowlState.ReadyToServe -> bowl.quality to bowl.toppings
            is BowlState.Assembling -> bowl.quality to bowl.toppings
            else -> return Transition(state, listOf(GameEffect.RejectServe))
        }
        val toppings = maybeHelperGarnish(state, customer.order, rawToppings)

        when (val verdict = GrayboxConfig.judgeServe(customer.order, toppings, quality)) {
            is ServeVerdict.Reject -> {
                return Transition(
                    state.copy(
                        customers = state.customers.map {
                            if (it.id == customer.id) it.copy(bark = CustomerLines.RESIGNED) else it
                        },
                    ),
                    listOf(GameEffect.RejectServe),
                )
            }
            is ServeVerdict.Accept -> {
                val patienceSat = if (customer.patienceMaxMs <= 0L) 1f
                else customer.patienceMs.toFloat() / customer.patienceMaxMs.toFloat()
                val sat = (patienceSat * verdict.satFactor).coerceIn(0f, 1f)
                val reward = UpgradeEffects.scaledReward(verdict.reward, state.ownedUpgrades)
                val bark = CustomerLines.serveLine(quality == ItemQuality.Good)
                val stats = if (quality == ItemQuality.Good) {
                    state.dayStats.copy(servedGood = true)
                } else {
                    state.dayStats
                }

                val nextFocus = when {
                    state.focusedBowlId != bowlId -> state.focusedBowlId
                    else -> state.bowls.keys.firstOrNull { it != bowlId }
                }

                // 大爷：给完面先不落币，点一下互动再收款
                if (customer.kind == CustomerKind.EarlyMarketUncle) {
                    return Transition(
                        state.copy(
                            bowls = state.bowls + (bowlId to BowlState.Empty),
                            focusedBowlId = nextFocus,
                            customers = state.customers.map {
                                if (it.id == customer.id) {
                                    it.copy(status = CustomerStatus.WaitingInteract, bark = bark)
                                } else {
                                    it
                                }
                            },
                            daySatSum = state.daySatSum + sat,
                            daySatCount = state.daySatCount + 1,
                            dayStats = stats,
                        ),
                        listOf(
                            GameEffect.ServedOk,
                            GameEffect.PlaySfx(SfxId.Serve),
                            GameEffect.Toast("大爷点头了，点他收钱"),
                        ),
                    )
                }

                val pileId = CoinPileId("coin-${state.nextCoinPileSeq}")
                val pile = CoinPile(id = pileId, amount = reward)
                return Transition(
                    state.copy(
                        bowls = state.bowls + (bowlId to BowlState.Empty),
                        focusedBowlId = nextFocus,
                        customers = state.customers.map {
                            if (it.id == customer.id) it.copy(status = CustomerStatus.Served, bark = bark) else it
                        },
                        coinsOnCounter = state.coinsOnCounter + pile,
                        nextCoinPileSeq = state.nextCoinPileSeq + 1,
                        daySatSum = state.daySatSum + sat,
                        daySatCount = state.daySatCount + 1,
                        dayStats = stats,
                    ),
                    listOf(GameEffect.ServedOk, GameEffect.PlaySfx(SfxId.Serve)),
                )
            }
        }
    }

    /** D6：出餐前 50% 补一件次要缺料。 */
    private fun maybeHelperGarnish(
        state: GameState,
        order: com.regannian.legend.game.model.Order,
        toppings: Set<IngredientId>,
    ): Set<IngredientId> {
        if (UpgradeId.D6 !in state.ownedUpgrades) return toppings
        val missing = order.required
            .filterNot { it in GrayboxConfig.MAIN_INGREDIENTS }
            .filterNot { it in toppings }
            .filter { it in state.toppingBar }
        if (missing.isEmpty()) return toppings
        val roll = (state.clockMs / GrayboxConfig.TICK_STEP_MS + missing.size).toInt()
        if (roll % 2 != 0) return toppings
        return toppings + missing.first()
    }

    private fun onTapCustomer(state: GameState, customerId: CustomerId): Transition {
        val customer = state.customers.find { it.id == customerId }
            ?: return Transition.none(state)
        if (customer.kind == CustomerKind.EarlyMarketUncle) {
            return when (customer.status) {
                CustomerStatus.WaitingServe ->
                    Transition(state, listOf(GameEffect.Toast("先给大爷一碗素面（只要芝麻酱）")))
                CustomerStatus.WaitingInteract -> {
                    val tip = UpgradeEffects.scaledReward(
                        GrayboxConfig.UNCLE_TIP_REWARD,
                        state.ownedUpgrades,
                    )
                    val pileId = CoinPileId("coin-${state.nextCoinPileSeq}")
                    val pile = CoinPile(id = pileId, amount = tip)
                    Transition(
                        state.copy(
                            customers = state.customers.map {
                                if (it.id == customerId) it.copy(status = CustomerStatus.Served) else it
                            },
                            coinsOnCounter = state.coinsOnCounter + pile,
                            nextCoinPileSeq = state.nextCoinPileSeq + 1,
                            dayStats = state.dayStats.copy(servedUncle = true),
                        ),
                        listOf(
                            GameEffect.Toast("大爷拍桌付钱 +$tip"),
                            GameEffect.PlaySfx(SfxId.Serve),
                        ),
                    )
                }
                else -> Transition(state, listOf(GameEffect.Toast("大爷已经走了")))
            }
        }
        // C5 递碗滑轨：点顾客出餐
        if (UpgradeId.C5 in state.ownedUpgrades && customer.status == CustomerStatus.WaitingServe) {
            val bowlId = state.focusedBowlId
                ?.takeIf { id ->
                    val b = state.bowls[id]
                    b is BowlState.ReadyToServe || b is BowlState.Assembling
                }
                ?: state.bowls.entries.firstOrNull { (_, b) ->
                    b is BowlState.ReadyToServe || b is BowlState.Assembling
                }?.key
            if (bowlId != null) {
                return serveBowlToCustomer(state, bowlId, customerId)
            }
            return Transition(state, listOf(GameEffect.Toast("先做好一碗面")))
        }
        return Transition(state, listOf(GameEffect.Toast("把碗拖给客人")))
    }

    private fun discardBowl(state: GameState, bowlId: BowlId): Transition {
        val bowl = state.bowls[bowlId]
        if (bowl is BowlState.Empty) {
            return Transition(state, listOf(GameEffect.Toast("碗是空的")))
        }
        val nextFocus = when {
            state.focusedBowlId != bowlId -> state.focusedBowlId
            else -> state.bowls.keys.firstOrNull { it != bowlId }
        }
        return Transition(
            state.copy(
                bowls = state.bowls + (bowlId to BowlState.Empty),
                focusedBowlId = nextFocus,
                coins = (state.coins - GrayboxConfig.DISCARD_PENALTY).coerceAtLeast(0),
                dayStats = state.dayStats.copy(discarded = true),
            ),
            listOf(GameEffect.Toast("扔掉，−${GrayboxConfig.DISCARD_PENALTY}金")),
        )
    }

    private fun onTapCoin(state: GameState, pileId: CoinPileId): Transition {
        val pile = state.coinsOnCounter.find { it.id == pileId } ?: return Transition.none(state)
        return Transition(
            state.copy(
                coins = state.coins + pile.amount,
                coinsOnCounter = state.coinsOnCounter.filterNot { it.id == pileId },
            ),
        )
    }
}
