package com.ryebreadseeds.closetai.domain

import com.ryebreadseeds.closetai.data.entity.ClosetItemEntity

object ShoppingBuddyEngine {

    fun suggest(items: List<ClosetItemEntity>, max: Int = 7): List<ShoppingSuggestion> {
        val byCat = items.groupBy { ClothingCategory.fromLabel(it.category) }
        val colors = items.map { ColorHarmony.normalize(it.color) }
        val colorCounts = colors.groupingBy { it }.eachCount()
        val out = mutableListOf<ShoppingSuggestion>()

        fun add(name: String, category: String, why: String, occasion: String = "Everyday") {
            if (out.none { it.name.equals(name, true) }) {
                out += ShoppingSuggestion(name, category, why, occasion)
            }
        }

        if (byCat[ClothingCategory.SHOES].orEmpty().isEmpty()) {
            add("White sneakers", "Shoes", "You have no shoes logged — sneakers unlock casual + gym looks.", "Casual")
            add("Black loafers or boots", "Shoes", "A dark pair covers work and evening without clashing.", "Work")
        } else if (byCat[ClothingCategory.SHOES].orEmpty().size == 1) {
            add("Second pair of shoes (contrast color)", "Shoes", "One shoe style limits occasions — add a dressier or sportier option.", "Everyday")
        }

        if (byCat[ClothingCategory.OUTERWEAR].orEmpty().isEmpty()) {
            add("Navy or black jacket", "Outerwear", "Missing outerwear — a neutral jacket layers over almost everything.", "Work")
        }

        if (byCat[ClothingCategory.TOP].orEmpty().isEmpty()) {
            add("White or cream tee / blouse", "Top", "No tops yet — a light base layer is the closet workhorse.", "Casual")
        } else if (byCat[ClothingCategory.TOP].orEmpty().size < 3) {
            add("Solid neutral top", "Top", "Thin top variety — another solid makes mix-and-match easier.", "Casual")
        }

        if (byCat[ClothingCategory.BOTTOM].orEmpty().isEmpty() &&
            byCat[ClothingCategory.DRESS].orEmpty().isEmpty() &&
            byCat[ClothingCategory.ROMPER].orEmpty().isEmpty()
        ) {
            add("Navy chinos or dark jeans", "Bottom", "No bottoms or one-pieces — start with a versatile dark pair.", "Casual")
        }

        val neutrals = colors.count { it == "neutral" || it == "brown" || it == "blue" }
        if (items.size >= 3 && neutrals < items.size / 3) {
            add("Beige or gray knit", "Top", "Color-heavy closet — a soft neutral balances louder pieces.", "Casual")
        }

        val dominant = colorCounts.maxByOrNull { it.value }?.key
        if (dominant != null && dominant != "neutral" && (colorCounts[dominant] ?: 0) >= 3) {
            val complement = when (dominant) {
                "blue" -> "warm camel or rust piece"
                "red", "pink" -> "olive or soft green piece"
                "green" -> "cream or blush piece"
                "black" -> "white contrast piece"
                else -> "neutral base piece"
            }
            add(complement.replaceFirstChar { it.uppercase() }, "Top", "Your wardrobe leans $dominant — a $complement opens new combos.", "Everyday")
        }

        if (byCat[ClothingCategory.ACCESSORY].orEmpty().isEmpty() && items.size >= 4) {
            add("Simple belt or everyday bag", "Accessory", "Accessories polish finished looks without buying a new outfit.", "Everyday")
        }

        if (byCat[ClothingCategory.DRESS].orEmpty().isEmpty() &&
            byCat[ClothingCategory.ROMPER].orEmpty().isEmpty() &&
            items.size >= 5
        ) {
            add("Simple midi dress or romper", "Dress", "A one-piece is the fastest date/formal path when bottoms run out.", "Date")
        }

        if (out.isEmpty()) {
            add("Versatile white sneakers", "Shoes", "Even a full closet benefits from a clean everyday sneaker.", "Casual")
            add("Lightweight layering piece", "Outerwear", "A cardigan or overshirt extends seasons without new outfits.", "Casual")
            add("Statement accessory", "Accessory", "One bold accessory refreshes outfits you already own.", "Date")
        }

        return out.take(max.coerceIn(3, 7))
    }
}
