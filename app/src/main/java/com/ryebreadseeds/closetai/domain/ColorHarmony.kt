package com.ryebreadseeds.closetai.domain

object ColorHarmony {
    private val neutrals = setOf(
        "black", "white", "gray", "grey", "beige", "cream", "ivory", "navy",
        "brown", "tan", "khaki", "charcoal", "silver", "gold", "nude", "off-white"
    )

    private val families = mapOf(
        "red" to setOf("red", "burgundy", "maroon", "wine", "crimson", "scarlet"),
        "orange" to setOf("orange", "coral", "peach", "rust", "terracotta"),
        "yellow" to setOf("yellow", "mustard", "gold", "lemon"),
        "green" to setOf("green", "olive", "mint", "sage", "emerald", "forest"),
        "blue" to setOf("blue", "navy", "teal", "cyan", "denim", "indigo", "sky"),
        "purple" to setOf("purple", "lavender", "violet", "plum", "lilac"),
        "pink" to setOf("pink", "rose", "blush", "magenta", "fuchsia"),
        "brown" to setOf("brown", "tan", "camel", "chocolate", "espresso"),
        "neutral" to neutrals
    )

    fun normalize(color: String): String {
        val c = color.lowercase().trim()
        for ((family, members) in families) {
            if (members.any { c.contains(it) }) return family
        }
        return c.ifBlank { "neutral" }
    }

    fun isNeutral(color: String): Boolean = normalize(color) == "neutral" ||
        neutrals.any { color.lowercase().contains(it) }

    /** Higher is better (0..1). */
    fun scorePair(a: String, b: String): Double {
        val fa = normalize(a)
        val fb = normalize(b)
        if (fa == "neutral" || fb == "neutral") return 0.95
        if (fa == fb) return 0.85
        val complements = mapOf(
            "red" to setOf("green", "blue"),
            "blue" to setOf("orange", "yellow", "red"),
            "yellow" to setOf("purple", "blue"),
            "green" to setOf("red", "pink"),
            "orange" to setOf("blue"),
            "purple" to setOf("yellow", "green"),
            "pink" to setOf("green", "blue")
        )
        if (complements[fa]?.contains(fb) == true) return 0.9
        // Adjacent warm/cool mixes still ok
        val warm = setOf("red", "orange", "yellow", "pink", "brown")
        val cool = setOf("blue", "green", "purple")
        if ((fa in warm && fb in warm) || (fa in cool && fb in cool)) return 0.75
        return 0.55
    }

    fun scorePalette(colors: List<String>): Double {
        if (colors.size < 2) return 1.0
        var total = 0.0
        var n = 0
        for (i in colors.indices) {
            for (j in i + 1 until colors.size) {
                total += scorePair(colors[i], colors[j])
                n++
            }
        }
        return if (n == 0) 1.0 else total / n
    }
}
