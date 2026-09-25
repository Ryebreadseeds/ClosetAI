package com.ryebreadseeds.closetai.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "disliked_combos")
data class DislikedComboEntity(
    @PrimaryKey val comboKey: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun keyFor(ids: List<Long>): String =
            ids.sorted().joinToString("|")
    }
}
