package com.ryebreadseeds.closetai.domain

enum class ClothingCategory(val label: String, val slot: OutfitSlot) {
    TOP("Top", OutfitSlot.TOP),
    BOTTOM("Bottom", OutfitSlot.BOTTOM),
    DRESS("Dress", OutfitSlot.ONE_PIECE),
    ROMPER("Romper", OutfitSlot.ONE_PIECE),
    OUTERWEAR("Outerwear", OutfitSlot.OUTERWEAR),
    SHOES("Shoes", OutfitSlot.SHOES),
    ACCESSORY("Accessory", OutfitSlot.ACCESSORY),
    OTHER("Other", OutfitSlot.ACCESSORY);

    companion object {
        fun fromLabel(label: String): ClothingCategory =
            entries.find { it.label.equals(label, ignoreCase = true) || it.name.equals(label, ignoreCase = true) }
                ?: OTHER
    }
}

enum class OutfitSlot { TOP, BOTTOM, ONE_PIECE, OUTERWEAR, SHOES, ACCESSORY }

enum class Season(val label: String) {
    SPRING("Spring"),
    SUMMER("Summer"),
    FALL("Fall"),
    WINTER("Winter"),
    ALL("All seasons");

    companion object {
        fun fromLabel(label: String): Season =
            entries.find { it.label.equals(label, ignoreCase = true) || it.name.equals(label, ignoreCase = true) }
                ?: ALL
    }
}

enum class Occasion(val label: String) {
    CASUAL("Casual"),
    WORK("Work"),
    DATE("Date"),
    GYM("Gym"),
    FORMAL("Formal");

    companion object {
        fun fromLabel(label: String): Occasion =
            entries.find { it.label.equals(label, ignoreCase = true) || it.name.equals(label, ignoreCase = true) }
                ?: CASUAL
    }
}

data class WeatherSnapshot(
    val temperatureC: Double,
    val weatherCode: Int,
    val cityLabel: String,
    val description: String
) {
    val temperatureF: Double get() = temperatureC * 9.0 / 5.0 + 32.0
    val isCold: Boolean get() = temperatureC < 12.0
    val isHot: Boolean get() = temperatureC > 26.0
    val isRainy: Boolean get() = weatherCode in 51..67 || weatherCode in 80..82 || weatherCode in 95..99
}

data class GeneratedOutfit(
    val itemIds: List<Long>,
    val title: String,
    val rationale: String,
    val occasion: Occasion,
    val mood: String?,
    val layered: Boolean,
    val source: OutfitSource
)

enum class OutfitSource { RULES, LLM }
