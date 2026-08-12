package com.regannian.legend.game.config

import com.regannian.legend.game.GrayboxConfig
import com.regannian.legend.game.model.IngredientId
import com.regannian.legend.game.model.Order
import kotlinx.serialization.json.Json

object DayConfigParser {
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(text: String): DayConfig = json.decodeFromString(DayConfig.serializer(), text)
}

object RecipeCatalog {
    fun order(recipeId: String): Order = when (recipeId) {
        "classic" -> GrayboxConfig.CLASSIC
        "chili" -> GrayboxConfig.CHILI
        "bean" -> GrayboxConfig.BEAN
        "plain_veg" -> GrayboxConfig.PLAIN_VEG
        else -> GrayboxConfig.CLASSIC
    }

    fun ingredient(id: String): IngredientId? = runCatching {
        IngredientId.valueOf(id)
    }.getOrNull()

    fun toppings(ids: List<String>): List<IngredientId> =
        ids.mapNotNull { ingredient(it) }.distinct()
}
