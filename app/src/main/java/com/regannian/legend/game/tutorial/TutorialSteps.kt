package com.regannian.legend.game.tutorial

/**
 * 新手引导步骤（纯逻辑，UI 只展示）。
 */
data class TutorialStep(
    val title: String,
    val body: String,
)

object TutorialSteps {
    val ALL: List<TutorialStep> = listOf(
        TutorialStep(
            "① 掸面",
            "点「掸面锅」开工。蓝条在涨=还在煮，满格后自动出锅；煮着时点锅不会加速。",
        ),
        TutorialStep(
            "② 烫面",
            "出锅后点/拖凉面去烫锅。烫满格后点或拖热面。",
        ),
        TutorialStep(
            "③ 入碗加料",
            "热面入空碗。碗上会实时列出已加配料；拖酱料进碗，配齐再出餐。",
        ),
        TutorialStep(
            "④ 好面评价",
            "出锅后尽快捞走才是「好面」。黄条=变软、红条=更差。宽限内拖走即可。",
        ),
        TutorialStep(
            "⑤ 出餐收钱",
            "碗拖给对的客人，再点柜台金币。设置里可随时重看本引导。",
        ),
    )
}
