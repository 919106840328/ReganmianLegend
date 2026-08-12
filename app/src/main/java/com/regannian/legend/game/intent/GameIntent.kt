package com.regannian.legend.game.intent

import com.regannian.legend.game.model.BowlId
import com.regannian.legend.game.model.CoinPileId
import com.regannian.legend.game.model.CustomerId
import com.regannian.legend.game.model.IngredientId
import com.regannian.legend.game.model.SideId
import com.regannian.legend.game.model.StationId

/**
 * UI 只发 Intent。少点击多拖：凉面拖烫锅、热面拖入碗、配料拖碗、碗拖顾客。
 */
sealed interface GameIntent {
    /** 仅用于开掸面锅计时。 */
    data class TapStation(val id: StationId) : GameIntent

    data class TapBowl(val id: BowlId) : GameIntent
    data class TapCoin(val pileId: CoinPileId) : GameIntent

    /** 掸面 Ready(凉面) → 拖到烫面锅。 */
    data class DragCoolToBlanch(val from: StationId = StationId.DustCook) : GameIntent

    /** 烫面 Ready(热面) → 拖到空碗。 */
    data class DragHotToBowl(val bowlId: BowlId) : GameIntent

    data class DragIngredient(
        val ingredientId: IngredientId,
        val bowlId: BowlId,
    ) : GameIntent

    data class DragServe(
        val bowlId: BowlId,
        val customerId: CustomerId,
    ) : GameIntent

    /** 扔掉错碗/不想做的碗，扣 2 金。 */
    data class DiscardBowl(val bowlId: BowlId) : GameIntent

    /** 已购「经典套餐键」：向焦点碗一键加经典配料。 */
    data object ApplyClassicCombo : GameIntent

    /** 点顾客：大爷 WaitingInteract 时收款 / C5 出餐。 */
    data class TapCustomer(val id: CustomerId) : GameIntent

    /** D2：连点 / 防偷铃驱赶偷碗贼。 */
    data object TapThief : GameIntent

    /** D3：点辅食工位开工 / 再点出锅。 */
    data class TapSide(val sideId: SideId) : GameIntent

    /** D3：辅食成品拖入碗。 */
    data class DragSideToBowl(val sideId: SideId, val bowlId: BowlId) : GameIntent

    data object Tick : GameIntent
    data object Pause : GameIntent
    data object Resume : GameIntent
}
