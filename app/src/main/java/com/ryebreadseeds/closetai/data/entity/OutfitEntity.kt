package com.ryebreadseeds.closetai.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outfits")
data class OutfitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val itemIdsCsv: String,
    val occasion: String,
    val mood: String? = null,
    val rationale: String = "",
    val source: String = "RULES",
    val liked: Boolean? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun itemIds(): List<Long> =
        if (itemIdsCsv.isBlank()) emptyList()
        else itemIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }

    companion object {
        fun idsToCsv(ids: List<Long>): String = ids.joinToString(",")
    }
}
