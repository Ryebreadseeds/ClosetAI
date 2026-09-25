package com.ryebreadseeds.closetai.domain

import com.ryebreadseeds.closetai.data.entity.ClosetItemEntity
import com.ryebreadseeds.closetai.data.entity.DislikedComboEntity
import kotlin.random.Random

/**
 * Offline outfit generator: category slots, layering, color harmony,
 * weather-aware outerwear, and disliked-combo avoidance.
 * Never pairs Bottom with Dress/Romper.
 */
class OutfitRulesEngine(
    private val random: Random = Random.Default
) {

    fun generate(
        items: List<ClosetItemEntity>,
        occasion: Occasion,
        mood: String?,
        weather: WeatherSnapshot?,
        dislikedKeys: Set<String>,
        attempts: Int = 48
    ): GeneratedOutfit? {
        if (items.isEmpty()) return null

        val byCat = items.groupBy { ClothingCategory.fromLabel(it.category) }
        val candidates = mutableListOf<GeneratedOutfit>()

        repeat(attempts) {
            val build = buildOne(byCat, occasion, mood, weather) ?: return@repeat
            val key = DislikedComboEntity.keyFor(build.itemIds)
            if (key in dislikedKeys) return@repeat
            candidates += build
        }

        return candidates.maxByOrNull { score(it, items, weather) }
            ?: buildOne(byCat, occasion, mood, weather)?.takeIf {
                DislikedComboEntity.keyFor(it.itemIds) !in dislikedKeys
            }
    }

    private fun buildOne(
        byCat: Map<ClothingCategory, List<ClosetItemEntity>>,
        occasion: Occasion,
        mood: String?,
        weather: WeatherSnapshot?
    ): GeneratedOutfit? {
        val tops = byCat[ClothingCategory.TOP].orEmpty()
        val bottoms = byCat[ClothingCategory.BOTTOM].orEmpty()
        val onePieces = (byCat[ClothingCategory.DRESS].orEmpty() + byCat[ClothingCategory.ROMPER].orEmpty())
        val outerwear = byCat[ClothingCategory.OUTERWEAR].orEmpty()
        val shoes = byCat[ClothingCategory.SHOES].orEmpty()
        val accessories = byCat[ClothingCategory.ACCESSORY].orEmpty() + byCat[ClothingCategory.OTHER].orEmpty()

        val preferOnePiece = when (occasion) {
            Occasion.FORMAL, Occasion.DATE -> onePieces.isNotEmpty() && random.nextFloat() < 0.45f
            Occasion.GYM -> false
            else -> onePieces.isNotEmpty() && random.nextFloat() < 0.25f
        }

        val picked = mutableListOf<ClosetItemEntity>()
        var layered = false

        if (preferOnePiece && onePieces.isNotEmpty()) {
            picked += pickSeasonAware(onePieces, weather) ?: return null
            // Never add bottoms with dress/romper
        } else {
            // Layering: optional base top + outer top when cold or mood says cozy
            val wantLayer = (weather?.isCold == true || mood?.contains("cozy", true) == true ||
                mood?.contains("layer", true) == true) && tops.size >= 2

            if (wantLayer) {
                val base = pickSeasonAware(tops, weather) ?: return null
                val over = tops.filter { it.id != base.id }.let { pickSeasonAware(it, weather) }
                if (over != null) {
                    picked += base
                    picked += over
                    layered = true
                } else {
                    picked += base
                }
            } else {
                val top = pickSeasonAware(tops, weather)
                if (top != null) picked += top
                else if (onePieces.isNotEmpty()) {
                    // Fallback to one-piece if no tops
                    picked += pickSeasonAware(onePieces, weather) ?: return null
                } else return null
            }

            // Bottoms only if we didn't pick a one-piece
            val hasOnePiece = picked.any {
                val c = ClothingCategory.fromLabel(it.category)
                c == ClothingCategory.DRESS || c == ClothingCategory.ROMPER
            }
            if (!hasOnePiece) {
                if (bottoms.isEmpty()) return null
                picked += pickSeasonAware(bottoms, weather) ?: return null
            }
        }

        // Outerwear for cold / rain / formal
        val needsOuter = weather?.isCold == true || weather?.isRainy == true ||
            occasion == Occasion.FORMAL || mood?.contains("layer", true) == true
        if (needsOuter && outerwear.isNotEmpty()) {
            pickSeasonAware(outerwear, weather)?.let {
                if (picked.none { p -> p.id == it.id }) {
                    picked += it
                    layered = true
                }
            }
        } else if (outerwear.isNotEmpty() && random.nextFloat() < 0.2f) {
            pickSeasonAware(outerwear, weather)?.let {
                if (picked.none { p -> p.id == it.id }) picked += it
            }
        }

        if (shoes.isNotEmpty()) {
            pickForOccasion(shoes, occasion, weather)?.let { picked += it }
        }

        if (accessories.isNotEmpty() && random.nextFloat() < 0.55f) {
            pickSeasonAware(accessories, weather)?.let { picked += it }
        }

        // Gym: prefer lighter pieces — filter already partially by season/occasion heuristics in title
        if (picked.isEmpty()) return null

        val title = buildTitle(picked, occasion, layered)
        val rationale = buildRationale(picked, occasion, mood, weather, layered)

        return GeneratedOutfit(
            itemIds = picked.map { it.id },
            title = title,
            rationale = rationale,
            occasion = occasion,
            mood = mood?.takeIf { it.isNotBlank() },
            layered = layered,
            source = OutfitSource.RULES
        )
    }

    private fun pickSeasonAware(
        pool: List<ClosetItemEntity>,
        weather: WeatherSnapshot?
    ): ClosetItemEntity? {
        if (pool.isEmpty()) return null
        val season = seasonFromWeather(weather)
        val preferred = pool.filter {
            val s = Season.fromLabel(it.season)
            s == Season.ALL || s == season
        }
        val use = if (preferred.isNotEmpty()) preferred else pool
        return use.random(random)
    }

    private fun pickForOccasion(
        pool: List<ClosetItemEntity>,
        occasion: Occasion,
        weather: WeatherSnapshot?
    ): ClosetItemEntity? {
        val filtered = when (occasion) {
            Occasion.GYM -> pool.filter {
                it.name.contains("sneaker", true) || it.name.contains("trainer", true) ||
                    it.color.contains("black", true) || it.category.contains("Shoes", true)
            }.ifEmpty { pool }
            Occasion.FORMAL -> pool.filter {
                !it.name.contains("sneaker", true) && !it.name.contains("flip", true)
            }.ifEmpty { pool }
            else -> pool
        }
        return pickSeasonAware(filtered, weather)
    }

    private fun seasonFromWeather(weather: WeatherSnapshot?): Season {
        if (weather == null) return Season.ALL
        return when {
            weather.temperatureC >= 22 -> Season.SUMMER
            weather.temperatureC >= 15 -> Season.SPRING
            weather.temperatureC >= 8 -> Season.FALL
            else -> Season.WINTER
        }
    }

    private fun score(
        outfit: GeneratedOutfit,
        allItems: List<ClosetItemEntity>,
        weather: WeatherSnapshot?
    ): Double {
        val map = allItems.associateBy { it.id }
        val pieces = outfit.itemIds.mapNotNull { map[it] }
        val colorScore = ColorHarmony.scorePalette(pieces.map { it.color })
        var weatherBonus = 0.0
        if (weather != null) {
            val hasOuter = pieces.any { ClothingCategory.fromLabel(it.category) == ClothingCategory.OUTERWEAR }
            if (weather.isCold && hasOuter) weatherBonus += 0.15
            if (weather.isHot && !hasOuter) weatherBonus += 0.1
            if (weather.isRainy && hasOuter) weatherBonus += 0.1
        }
        val layerBonus = if (outfit.layered) 0.05 else 0.0
        return colorScore + weatherBonus + layerBonus + random.nextDouble() * 0.05
    }

    private fun buildTitle(items: List<ClosetItemEntity>, occasion: Occasion, layered: Boolean): String {
        val cats = items.map { ClothingCategory.fromLabel(it.category) }
        val base = when {
            ClothingCategory.DRESS in cats -> "Dress look"
            ClothingCategory.ROMPER in cats -> "Romper look"
            layered -> "Layered look"
            else -> "Everyday look"
        }
        return "$base · ${occasion.label}"
    }

    private fun buildRationale(
        items: List<ClosetItemEntity>,
        occasion: Occasion,
        mood: String?,
        weather: WeatherSnapshot?,
        layered: Boolean
    ): String {
        val parts = mutableListOf<String>()
        parts += "Picked for ${occasion.label.lowercase()}"
        mood?.takeIf { it.isNotBlank() }?.let { parts += "mood: $it" }
        if (layered) parts += "with layering"
        weather?.let {
            parts += "${it.temperatureF.toInt()}°F in ${it.cityLabel}"
            if (it.isRainy) parts += "rain-ready"
        }
        val colors = items.map { it.color }.distinct().joinToString(", ")
        if (colors.isNotBlank()) parts += "palette: $colors"
        return parts.joinToString(" · ")
    }
}
