package com.regannian.legend.ui.art

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.regannian.legend.game.model.IngredientId

/**
 * 美术替换槽：把 PNG/WebP 放到 `res/drawable/`，文件名与 [slot] 一致即自动盖住矢量占位。
 *
 * 道具图默认会抠掉奶白/纯绿底，避免玩法区出现白框。
 */
object ArtSlots {
    const val SEAT_EMPTY = "art_seat_empty"
    const val CUSTOMER_HOPEFUL = "art_customer_hopeful"
    const val CUSTOMER_IMPATIENT = "art_customer_impatient"
    const val CUSTOMER_FEDUP = "art_customer_fedup"
    const val CUSTOMER_HAPPY = "art_customer_happy"
    const val CUSTOMER_RESIGNED = "art_customer_resigned"
    const val CUSTOMER_ANGRY = "art_customer_angry"
    const val CUSTOMER_UNCLE = "art_customer_uncle"
    const val WOK = "art_wok"
    const val WOK_NOODLES = "art_wok_noodles"
    const val BLANCH = "art_blanch"
    const val BLANCH_NOODLES = "art_blanch_noodles"
    const val BOWL_EMPTY = "art_bowl_empty"
    const val BOWL_NOODLES = "art_bowl_noodles"
    const val BOWL_SAUCED = "art_bowl_sauced"
    const val BOWL = "art_bowl"
    const val JAR = "art_jar"
    const val TRASH = "art_trash"
    const val COIN = "art_coin"
    const val THIEF = "art_thief"
    const val SIDE = "art_side"
    const val KITCHEN_BG = "art_kitchen_bg"

    const val TOP_SESAME_SAUCE = "art_top_sesame_sauce"
    const val TOP_SESAME_OIL = "art_top_sesame_oil"
    const val TOP_SALAD_OIL = "art_top_salad_oil"
    const val TOP_CHILI_OIL = "art_top_chili_oil"
    const val TOP_GARLIC = "art_top_garlic"
    const val TOP_SCALLION = "art_top_scallion"
    const val TOP_RADISH = "art_top_radish"
    const val TOP_PICKLED_BEAN = "art_top_pickled_bean"
    const val TOP_BRINE = "art_top_brine"
    const val TOP_SOY_MILK = "art_top_soy_milk"
    const val TOP_MIAN_WO = "art_top_mian_wo"
    const val TOP_EGG_WINE = "art_top_egg_wine"

    fun toppingSlot(id: IngredientId): String = when (id) {
        IngredientId.SesameSauce -> TOP_SESAME_SAUCE
        IngredientId.SesameOil -> TOP_SESAME_OIL
        IngredientId.SaladOil -> TOP_SALAD_OIL
        IngredientId.ChiliOil -> TOP_CHILI_OIL
        IngredientId.Garlic -> TOP_GARLIC
        IngredientId.Scallion -> TOP_SCALLION
        IngredientId.Radish -> TOP_RADISH
        IngredientId.PickledBean -> TOP_PICKLED_BEAN
        IngredientId.Brine -> TOP_BRINE
        IngredientId.SoyMilk -> TOP_SOY_MILK
        IngredientId.MianWo -> TOP_MIAN_WO
        IngredientId.EggWine -> TOP_EGG_WINE
    }
}

@Composable
fun ArtOrFallback(
    slot: String,
    modifier: Modifier = Modifier,
    /** 全屏背景等不要抠底。 */
    stripPaperBackdrop: Boolean = true,
    fallback: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val resId = remember(slot) {
        context.resources.getIdentifier(slot, "drawable", context.packageName)
    }
    if (resId != 0) {
        if (stripPaperBackdrop) {
            val bmp = remember(resId) {
                val src = BitmapFactory.decodeResource(context.resources, resId)
                stripPaperBackdrop(src).asImageBitmap()
            }
            Image(
                bitmap = bmp,
                contentDescription = null,
                modifier = modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        } else {
            Image(
                painter = androidx.compose.ui.res.painterResource(resId),
                contentDescription = null,
                modifier = modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    } else {
        fallback()
    }
}

/**
 * 抠掉乳白纸底 / 纯绿幕 / 纯黑实底，让道具融入厨房背景。
 */
internal fun stripPaperBackdrop(src: Bitmap): Bitmap {
    val out = src.copy(Bitmap.Config.ARGB_8888, true) ?: return src
    val w = out.width
    val h = out.height
    val pixels = IntArray(w * h)
    out.getPixels(pixels, 0, w, 0, 0, w, h)
    for (i in pixels.indices) {
        val c = pixels[i]
        val a = AndroidColor.alpha(c)
        if (a < 8) {
            pixels[i] = AndroidColor.TRANSPARENT
            continue
        }
        val r = AndroidColor.red(c)
        val g = AndroidColor.green(c)
        val b = AndroidColor.blue(c)
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val bright = (r + g + b) / 3
        val sat = if (max == 0) 0f else (max - min).toFloat() / max.toFloat()
        val paper = bright >= 232 && sat < 0.14f
        val cream = r >= 238 && g >= 228 && b >= 200 && sat < 0.22f && bright >= 220
        // 生成图可用纯绿幕；不抠黑色，避免吃掉锅边/描线
        val greenScreen = g >= 200 && r < 100 && b < 100 && g > r + 60 && g > b + 60
        if (paper || cream || greenScreen) {
            pixels[i] = AndroidColor.TRANSPARENT
        }
    }
    out.setPixels(pixels, 0, w, 0, 0, w, h)
    return out
}
