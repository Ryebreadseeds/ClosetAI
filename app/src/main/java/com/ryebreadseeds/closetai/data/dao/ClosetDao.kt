package com.ryebreadseeds.closetai.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ryebreadseeds.closetai.data.entity.ClosetItemEntity
import com.ryebreadseeds.closetai.data.entity.DislikedComboEntity
import com.ryebreadseeds.closetai.data.entity.OutfitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClosetDao {
    @Query("SELECT * FROM closet_items ORDER BY updatedAt DESC")
    fun observeItems(): Flow<List<ClosetItemEntity>>

    @Query("SELECT * FROM closet_items ORDER BY updatedAt DESC")
    suspend fun getItems(): List<ClosetItemEntity>

    @Query("SELECT * FROM closet_items WHERE id = :id")
    suspend fun getItem(id: Long): ClosetItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: ClosetItemEntity): Long

    @Update
    suspend fun updateItem(item: ClosetItemEntity)

    @Delete
    suspend fun deleteItem(item: ClosetItemEntity)

    @Query("DELETE FROM closet_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("SELECT * FROM outfits ORDER BY createdAt DESC")
    fun observeOutfits(): Flow<List<OutfitEntity>>

    @Query("SELECT * FROM outfits ORDER BY createdAt DESC")
    suspend fun getOutfits(): List<OutfitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOutfit(outfit: OutfitEntity): Long

    @Query("UPDATE outfits SET liked = :liked WHERE id = :id")
    suspend fun setOutfitLiked(id: Long, liked: Boolean?)

    @Query("DELETE FROM outfits WHERE id = :id")
    suspend fun deleteOutfit(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDisliked(combo: DislikedComboEntity)

    @Query("SELECT comboKey FROM disliked_combos")
    suspend fun getDislikedKeys(): List<String>

    @Query("DELETE FROM closet_items")
    suspend fun clearItems()

    @Query("DELETE FROM outfits")
    suspend fun clearOutfits()

    @Query("DELETE FROM disliked_combos")
    suspend fun clearDisliked()
}
