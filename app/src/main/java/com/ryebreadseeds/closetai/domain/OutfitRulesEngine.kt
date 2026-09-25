package com.ryebreadseeds.closetai.domain

import com.ryebreadseeds.closetai.data.entity.ClosetItemEntity
import com.ryebreadseeds.closetai.data.entity.DislikedComboEntity
import kotlin.random.Random

/**
 * Offline outfit generator: category slots, layering, color harmony,
 * weather-aware outerwear, liked-style weighting, and disliked-combo avoidance.
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
        stylePrefs: StylePrefs = StylePrefs(),
        glowUp: Boolean = false,
        anchorItemId: Long? = null,
        attempts: Int = 48
    ): GeneratedOutfit? {
        if (items.isEmpty()) return null

        val byCat = items.groupBy { ClothingCategory.fromLabel(it.category) }
        val candidates = mutableListOf<GeneratedOutfit>()

        repeat(attempts) {
            val build = buildOne(byCat, occasion, mood, weather, stylePrefs, glowUp, anchorItemId) ?: return@repeat
            val key = DislikedComboEntity.keyFor(build.itemIds)
            if (key in dislikedKeys) return@repeat
            if (anchorItemId != null && anchorItemId !in build.itemIds) return@repeat
            candidates += build
        }

        return candidates.maxByOrNull { score(it, items, weather, stylePrefs, glowUp) }
            ?: buildOne(byCat, occasion, mood, weather, stylePrefs, glowUp, anchorItemId)?.takeIf {
                DislikedComboEntity.keyFor(it.itemIds) !in dislikedKeys &&
                    (anchorItemId == null || anchorItemId in it.itemIds)
            }
    }

    /** Generate up to [count] complementary outfits that include [anchor]. */
    fun whatGoesWith(
        items: List<ClosetItemEntity>,
        anchor: ClosetItemEntity,
        occasion: Occasion,
        weather: WeatherSnapshot?,
        dislikedKeys: Set<String>,
        stylePrefs: StylePrefs = StylePrefs(),
        count: Int = 3
    ): List<GeneratedOutfit> {
        val results = mutableListOf<GeneratedOutfit>()
        val seen = mutableSetOf<String>()
        repeat(count * 12) {
            if (results.size >= count) return@repeat
            val g = generate(
                items = items,
                occasion = occasion,
                mood = null,
                weather = weather,
                dislikedKeys = dislikedKeys,
                stylePrefs = stylePrefs,
                glowUp = false,
                anchorItemId = anchor.id,
                attempts = 16
            ) ?: return@repeat
            val key = DislikedComboEntity.keyFor(g.itemIds)
            if (key in seen) return@repeat
            seen += key
            results += g.copy(
                title = "With ${anchor.name}",
                rationale = "Built around your ${anchor.color} ${anchor.category.lowercase()} · ${g.rationale}"
            )
        }
        return results
    }

    private fun buildOne(
        byCat: Map<ClothingCategory, List<ClosetItemEntity>>,
        occasion: Occasion,
        mood: String?,
        weather: WeatherSnapshot?,
        stylePrefs: StylePrefs,
        glowUp: Boolean,
        anchorItemId: Long?
    ): GeneratedOutfit? {
        val tops = byCat[ClothingCategory.TOP].orEmpty()
        val bottoms = byCat[ClothingCategory.BOTTOM].orEmpty()
        val onePieces = (byCat[ClothingCategory.DRESS].orEmpty() + byCat[ClothingCategory.ROMPER].orEmpty())
        val outerwear = byCat[ClothingCategory.OUTERWEAR].orEmpty()
        val shoes = byCat[ClothingCategory.SHOES].orEmpty()
        val accessories = byCat[ClothingCategory.ACCESSORY].orEmpty() + byCat[ClothingCategory.OTHER].orEmpty()

        val anchor = listOf(tops, bottoms, onePieces, outerwear, shoes, accessories)
            .flatten()
            .find { it.id == anchorItemId }
        val anchorCat = anchor?.let { ClothingCategory.fromLabel(it.category) }

        val preferOnePiece = when {
            anchorCat == ClothingCategory.DRESS || anchorCat == ClothingCategory.ROMPER -> true
            anchorCat == ClothingCategory.BOTTOM || anchorCat == ClothingCategory.TOP -> false
            occasion == Occasion.FORMAL || occasion == Occasion.DATE ->
                onePieces.isNotEmpty() && random.nextFloat() < 0.45f
            occasion == Occasion.GYM -> false
            else -> onePieces.isNotEmpty() && random.nextFloat() < 0.25f
        }

        val picked = mutableListOf<ClosetItemEntity>()
        var layered = false

        if (anchor != null && (anchorCat == ClothingCategory.DRESS || anchorCat == ClothingCategory.ROMPER)) {
            picked += anchor
        } else if (preferOnePiece && onePieces.isNotEmpty()) {
            picked += weightedPick(onePieces, weather, stylePrefs, glowUp) ?: return null
        } else {
            val wantLayer = (weather?.isCold == true || mood?.contains("cozy", true) == true ||
                mood?.contains("layer", true) == true) && tops.size >= 2

            if (anchor != null && anchorCat == ClothingCategory.TOP) {
                picked += anchor
                if (wantLayer) {
                    val over = tops.filter { it.id != anchor.id }
                        .let { weightedPick(it, weather, stylePrefs, glowUp) }
                    if (over != null) {
                        picked += over
                        layered = true
                    }
                }
            } else if (wantLayer) {
                val base = weightedPick(tops, weather, stylePrefs, glowUp) ?: return null
                val over = tops.filter { it.id != base.id }.let { weightedPick(it, weather, stylePrefs, glowUp) }
                if (over != null) {
                    picked += base
                    picked += over
                    layered = true
                } else {
                    picked += base
                }
            } else {
                val top = if (anchorCat == ClothingCategory.TOP && anchor != null) anchor
                else weightedPick(tops, weather, stylePrefs, glowUp)
                if (top != null) picked += top
                else if (onePieces.isNotEmpty()) {
                    picked += weightedPick(onePieces, weather, stylePrefs, glowUp) ?: return null
                } else return null
            }

            val hasOnePiece = picked.any {
                val c = ClothingCategory.fromLabel(it.category)
                c == ClothingCategory.DRESS || c == ClothingCategory.ROMPER
            }
            if (!hasOnePiece) {
                if (bottoms.isEmpty()) return null
                val bottom = if (anchorCat == ClothingCategory.BOTTOM && anchor != null) anchor
                else weightedPick(bottoms, weather, stylePrefs, glowUp) ?: return null
                if (picked.none { it.id == bottom.id }) picked += bottom
            }
        }

        // Ensure anchor included for non-core slots
        if (anchor != null && picked.none { it.id == anchor.id }) {
            when (anchorCat) {
                ClothingCategory.OUTERWEAR, ClothingCategory.SHOES,
                ClothingCategory.ACCESSORY, ClothingCategory.OTHER -> picked += anchor
                else -> {}
            }
        }

        val needsOuter = weather?.isCold == true || weather?.isRainy == true ||
            occasion == Occasion.FORMAL || mood?.contains("layer", true) == true
        if (needsOuter && outerwear.isNotEmpty()) {
            val outer = if (anchorCat == ClothingCategory.OUTERWEAR && anchor != null) anchor
            else weightedPick(outerwear, weather, stylePrefs, glowUp)
            outer?.let {
                if (picked.none { p -> p.id == it.id }) {
                    picked += it
                    layered = true
                }
            }
        } else if (outerwear.isNotEmpty() && random.nextFloat() < 0.2f) {
            weightedPick(outerwear, weather, stylePrefs, glowUp)?.let {
                if (picked.none { p -> p.id == it.id }) picked += it
            }
        }

        if (shoes.isNotEmpty()) {
            val shoe = if (anchorCat == ClothingCategory.SHOES && anchor != null) anchor
            else pickForOccasion(shoes, occasion, weather, stylePrefs, glowUp)
            shoe?.let { if (picked.none { p -> p.id == it.id }) picked += it }
        }

        if (accessories.isNotEmpty() && (random.nextFloat() < 0.55f || anchorCat == ClothingCategory.ACCESSORY)) {
            val acc = if (anchorCat == ClothingCategory.ACCESSORY || anchorCat == ClothingCategory.OTHER) {
                anchor
            } else weightedPick(accessories, weather, stylePrefs, glowUp)
            acc?.let { if (picked.none { p -> p.id == it.id }) picked += it }
        }

        if (picked.isEmpty()) return null
        if (anchorItemId != null && picked.none { it.id == anchorItemId }) return null

        val title = buildTitle(picked, occasion, layered, glowUp)
        val rationale = buildRationale(picked, occasion, mood, weather, layered, stylePrefs, glowUp)

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

    private fun weightedPick(
        pool: List<ClosetItemEntity>,
        weather: WeatherSnapshot?,
        stylePrefs: StylePrefs,
        glowUp: Boolean
    ): ClosetItemEntity? {
        if (pool.isEmpty()) return null
        val season = seasonFromWeather(weather)
        val preferred = pool.filter {
            val s = Season.fromLabel(it.season)
            s == Season.ALL || s == season
        }
        val use = if (preferred.isNotEmpty()) preferred else pool
        val weights = use.map { item ->
            var w = 1.0
            val colorKey = ColorHarmony.normalize(item.color)
            w += (stylePrefs.likedColors[colorKey] ?: 0) * 0.35
            w += (stylePrefs.likedCategories[item.category] ?: 0) * 0.25
            if (glowUp) {
                if (item.id !in stylePrefs.usedItemIds) w += 1.2
                w += (stylePrefs.likedColors[colorKey] ?: 0) * 0.2
            }
            w.coerceAtLeast(0.1)
        }
        val total = weights.sum()
        var r = random.nextDouble() * total
        for (i in use.indices) {
            r -= weights[i]
            if (r <= 0) return use[i]
        }
        return use.last()
    }

    private fun pickForOccasion(
        pool: List<ClosetItemEntity>,
        occasion: Occasion,
        weather: WeatherSnapshot?,
        stylePrefs: StylePrefs,
        glowUp: Boolean
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
        return weightedPick(filtered, weather, stylePrefs, glowUp)
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
        weather: WeatherSnapshot?,
        stylePrefs: StylePrefs,
        glowUp: Boolean
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
        var prefBonus = 0.0
        pieces.forEach { p ->
            prefBonus += (stylePrefs.likedColors[ColorHarmony.normalize(p.color)] ?: 0) * 0.04
            prefBonus += (stylePrefs.likedCategories[p.category] ?: 0) * 0.03
            if (glowUp && p.id !in stylePrefs.usedItemIds) prefBonus += 0.08
        }
        return colorScore + weatherBonus + layerBonus + prefBonus + random.nextDouble() * 0.05
    }

    private fun buildTitle(
        items: List<ClosetItemEntity>,
        occasion: Occasion,
        layered: Boolean,
        glowUp: Boolean
    ): String {
        val cats = items.map { ClothingCategory.fromLabel(it.category) }
        val base = when {
            glowUp -> "Glow-up look"
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
        layered: Boolean,
        stylePrefs: StylePrefs,
        glowUp: Boolean
    ): String {
        val parts = mutableListOf<String>()
        parts += "Picked for ${occasion.label.lowercase()}"
        mood?.takeIf { it.isNotBlank() }?.let { parts += "mood: $it" }
        if (layered) parts += "with layering"
        if (glowUp) parts += "glow-up: fresher & liked-style pieces"
        weather?.let {
            parts += "${it.temperatureF.toInt()}°F in ${it.cityLabel}"
            if (it.isRainy) parts += "rain-ready"
        }
        val colors = items.map { it.color }.distinct().joinToString(", ")
        if (colors.isNotBlank()) parts += "palette: $colors"
        if (stylePrefs.likedColors.isNotEmpty()) {
            val top = stylePrefs.likedColors.maxByOrNull { it.value }?.key
            if (top != null) parts += "leans into your liked $top tones"
        }
        return parts.joinToString(" · ")
    }

    /** Complete a partial Mix & Match selection using inventory. */
    fun completeMix(
        items: List<ClosetItemEntity>,
        selectedIds: List<Long>,
        occasion: Occasion,
        weather: WeatherSnapshot?,
        stylePrefs: StylePrefs = StylePrefs()
    ): GeneratedOutfit? {
        if (selectedIds.isEmpty()) {
            return generate(items, occasion, null, weather, emptySet(), stylePrefs)
        }
        val selected = items.filter { it.id in selectedIds }
        val cats = selected.map { ClothingCategory.fromLabel(it.category) }
        val hasOne = ClothingCategory.DRESS in cats || ClothingCategory.ROMPER in cats
        val hasBottom = ClothingCategory.BOTTOM in cats
        if (hasOne && hasBottom) return null

        val remaining = items.filter { it.id !in selectedIds }
        val byCat = remaining.groupBy { ClothingCategory.fromLabel(it.category) }
        val picked = selected.toMutableList()
        var layered = picked.count { ClothingCategory.fromLabel(it.category) == ClothingCategory.TOP } >= 2

        val hasTop = ClothingCategory.TOP in cats
        if (!hasOne && !hasBottom) {
            if (!hasTop) {
                weightedPick(byCat[ClothingCategory.TOP].orEmpty(), weather, stylePrefs, false)?.let { picked += it }
            }
            // Prefer bottoms unless we somehow got a one-piece
            val stillNoOne = picked.none {
                val c = ClothingCategory.fromLabel(it.category)
                c == ClothingCategory.DRESS || c == ClothingCategory.ROMPER
            }
            if (stillNoOne && picked.none { ClothingCategory.fromLabel(it.category) == ClothingCategory.BOTTOM }) {
                weightedPick(byCat[ClothingCategory.BOTTOM].orEmpty(), weather, stylePrefs, false)?.let { picked += it }
                    ?: weightedPick(
                        byCat[ClothingCategory.DRESS].orEmpty() + byCat[ClothingCategory.ROMPER].orEmpty(),
                        weather, stylePrefs, false
                    )?.let { picked += it }
            }
        } else if (!hasOne && hasBottom && !hasTop) {
            weightedPick(byCat[ClothingCategory.TOP].orEmpty(), weather, stylePrefs, false)?.let { picked += it }
        }

        if (picked.none { ClothingCategory.fromLabel(it.category) == ClothingCategory.SHOES }) {
            weightedPick(byCat[ClothingCategory.SHOES].orEmpty(), weather, stylePrefs, false)?.let { picked += it }
        }
        if (weather?.isCold == true &&
            picked.none { ClothingCategory.fromLabel(it.category) == ClothingCategory.OUTERWEAR }
        ) {
            weightedPick(byCat[ClothingCategory.OUTERWEAR].orEmpty(), weather, stylePrefs, false)?.let {
                picked += it
                layered = true
            }
        }

        return GeneratedOutfit(
            itemIds = picked.map { it.id },
            title = "Mix complete · ${occasion.label}",
            rationale = "Filled empty slots around your picks · palette: " +
                picked.map { it.color }.distinct().joinToString(", "),
            occasion = occasion,
            mood = null,
            layered = layered,
            source = OutfitSource.RULES
        )
    }
}
