package com.ryebreadseeds.closetai.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "closet_items")
data class ClosetItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val color: String,
    val season: String,
    val photoPath: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
