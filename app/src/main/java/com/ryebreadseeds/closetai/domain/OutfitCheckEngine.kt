package com.ryebreadseeds.closetai.domain

import com.ryebreadseeds.closetai.data.entity.ClosetItemEntity

object OutfitCheckEngine {

    fun score(
        pieces: List<ClosetItemEntity>,
        occasion: Occasion,
        weather: WeatherSnapshot? = null
    ): OutfitCheckResult {
        if (pieces.isEmpty()) {
            return OutfitCheckResult(1, 1, 1, 1, listOf("Pick at least one item to check."), OutfitSource.RULES)
        }

        val cats = pieces.map { ClothingCategory.fromLabel(it.category) }
        val hasOne = ClothingCategory.DRESS in cats || ClothingCategory.ROMPER in cats
        val hasBottom = ClothingCategory.BOTTOM in cats
        val hasTop = ClothingCategory.TOP in cats
        val hasShoes = ClothingCategory.SHOES in cats
        val hasOuter = ClothingCategory.OUTERWEAR in cats

        val colorRaw = ColorHarmony.scorePalette(pieces.map { it.color })
        val colorScore = (colorRaw * 10).toInt().coerceIn(1, 10)

        var coherence = 7
        val tips = mutableListOf<String>()

        if (hasOne && hasBottom) {
            coherence = 2
            tips += "Dress/romper should not be paired with pants — drop one."
        }
        if (!hasOne && !hasTop && hasBottom) {
            coherence -= 2
            tips += "A top would complete this look."
        }
        if (!hasOne && hasTop && !hasBottom) {
            coherence -= 2
            tips += "Add bottoms or switch to a dress/romper."
        }
        if (!hasShoes) {
            coherence -= 1
            tips += "Shoes finish the outfit — pick a pair that matches the vibe."
        }
        if (pieces.size == 1) {
            coherence -= 2
            tips += "One piece alone is a start; add complementary items."
        }
        if (weather?.isCold == true && !hasOuter) {
            coherence -= 1
            tips += "It's chilly — an outer layer would help."
        }
        if (weather?.isHot == true && hasOuter) {
            tips += "Warm day — consider skipping heavy outerwear."
        }
        coherence = coherence.coerceIn(1, 10)

        var occasionScore = 7
        when (occasion) {
            Occasion.GYM -> {
                val gymmy = pieces.any {
                    it.name.contains("sneaker", true) || it.name.contains("short", true) ||
                        it.name.contains("legging", true) || it.name.contains("tank", true) ||
                        it.name.contains("hoodie", true)
                }
                occasionScore = if (gymmy) 9 else 5
                if (!gymmy) tips += "For gym, lean into sneakers, tanks, or flex fabrics."
            }
            Occasion.FORMAL -> {
                val casualCue = pieces.any {
                    it.name.contains("sneaker", true) || it.name.contains("hoodie", true) ||
                        it.name.contains("jean", true) || it.name.contains("tee", true)
                }
                occasionScore = if (casualCue) 4 else 8
                if (casualCue) tips += "Swap casual cues (sneakers/tees) for polish."
                if (hasOne) occasionScore = (occasionScore + 1).coerceAtMost(10)
            }
            Occasion.DATE -> {
                occasionScore = if (hasOne || colorRaw >= 0.75) 8 else 6
                if (colorRaw < 0.7) tips += "Tighten the color story for a date-night glow."
            }
            Occasion.WORK -> {
                val tooLoud = pieces.count { !ColorHarmony.isNeutral(it.color) } > 2
                occasionScore = if (tooLoud) 5 else 8
                if (tooLoud) tips += "Tone down bold colors slightly for work."
            }
            Occasion.CASUAL -> {
                occasionScore = 8
            }
        }
        occasionScore = occasionScore.coerceIn(1, 10)

        if (tips.isEmpty()) {
            tips += "Balanced palette and slots — wear with confidence."
            if (colorScore >= 8) tips += "Colors play well together."
        }

        val overall = ((colorScore + coherence + occasionScore) / 3.0).toInt().coerceIn(1, 10)
        return OutfitCheckResult(
            colorScore = colorScore,
            coherenceScore = coherence,
            occasionScore = occasionScore,
            overall = overall,
            tips = tips.take(4),
            source = OutfitSource.RULES
        )
    }
}
