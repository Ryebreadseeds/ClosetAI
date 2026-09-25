package com.ryebreadseeds.closetai.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import kotlin.math.max
import kotlin.math.min

/**
 * Offline fallback when no API key: sample dominant colors and invent a simple name.
 * Optional mild contrast boost for the stored photo is left to the caller.
 */
object LocalImageFallback {

    fun suggestFromPhoto(path: String): VisionSuggestion {
        val bmp = BitmapFactory.decodeFile(path)
            ?: return VisionSuggestion("Closet item", "Other", "Unknown", "All seasons")
        val sample = Bitmap.createScaledBitmap(bmp, 48, 48, true)
        if (sample !== bmp) bmp.recycle()

        var rSum = 0L; var gSum = 0L; var bSum = 0L; var n = 0
        for (x in 0 until sample.width) {
            for (y in 0 until sample.height) {
                val c = sample.getPixel(x, y)
                if (Color.alpha(c) < 32) continue
                rSum += Color.red(c); gSum += Color.green(c); bSum += Color.blue(c); n++
            }
        }
        sample.recycle()
        if (n == 0) return VisionSuggestion("Closet item", "Other", "Unknown", "All seasons")
        val r = (rSum / n).toInt()
        val g = (gSum / n).toInt()
        val b = (bSum / n).toInt()
        val colorName = nearestColorName(r, g, b)
        return VisionSuggestion(
            name = "$colorName item",
            category = "Other",
            color = colorName,
            season = "All seasons",
            notes = "Local color estimate (set an OpenRouter key for smarter AI tagging)"
        )
    }

    /** Mild local contrast stretch — returns a new bitmap or null. */
    fun contrastBoost(path: String): Bitmap? {
        val src = BitmapFactory.decodeFile(path) ?: return null
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        var minL = 255; var maxL = 0
        val w = out.width; val h = out.height
        val pixels = IntArray(w * h)
        out.getPixels(pixels, 0, w, 0, 0, w, h)
        for (p in pixels) {
            val l = (Color.red(p) + Color.green(p) + Color.blue(p)) / 3
            minL = min(minL, l); maxL = max(maxL, l)
        }
        val range = (maxL - minL).coerceAtLeast(1)
        for (i in pixels.indices) {
            val p = pixels[i]
            val a = Color.alpha(p)
            fun stretch(v: Int) = ((v - minL) * 255 / range).coerceIn(0, 255)
            pixels[i] = Color.argb(a, stretch(Color.red(p)), stretch(Color.green(p)), stretch(Color.blue(p)))
        }
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        if (out !== src) src.recycle()
        return out
    }

    private fun nearestColorName(r: Int, g: Int, b: Int): String {
        val named = listOf(
            Triple("Black", Triple(20, 20, 20)),
            Triple("White", Triple(240, 240, 240)),
            Triple("Gray", Triple(128, 128, 128)),
            Triple("Beige", Triple(210, 190, 160)),
            Triple("Brown", Triple(120, 75, 45)),
            Triple("Navy", Triple(20, 35, 80)),
            Triple("Blue", Triple(40, 90, 200)),
            Triple("Teal", Triple(30, 140, 140)),
            Triple("Green", Triple(50, 140, 70)),
            Triple("Olive", Triple(110, 120, 60)),
            Triple("Yellow", Triple(230, 200, 50)),
            Triple("Orange", Triple(230, 120, 40)),
            Triple("Red", Triple(200, 40, 40)),
            Triple("Pink", Triple(230, 140, 170)),
            Triple("Purple", Triple(120, 60, 160)),
            Triple("Burgundy", Triple(100, 20, 40))
        )
        return named.minBy { (_, c) ->
            val dr = r - c.first; val dg = g - c.second; val db = b - c.third
            dr * dr + dg * dg + db * db
        }.first
    }
}
