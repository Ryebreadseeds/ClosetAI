package com.ryebreadseeds.closetai.data.repo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.ryebreadseeds.closetai.data.ClosetDatabase
import com.ryebreadseeds.closetai.data.entity.ClosetItemEntity
import com.ryebreadseeds.closetai.data.entity.DislikedComboEntity
import com.ryebreadseeds.closetai.data.entity.OutfitEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ClosetRepository(private val context: Context) {
    private val dao = ClosetDatabase.get(context).closetDao()

    fun observeItems(): Flow<List<ClosetItemEntity>> = dao.observeItems()
    fun observeOutfits(): Flow<List<OutfitEntity>> = dao.observeOutfits()

    suspend fun getItems() = dao.getItems()
    suspend fun getItem(id: Long) = dao.getItem(id)
    suspend fun getOutfits() = dao.getOutfits()
    suspend fun getDislikedKeys() = dao.getDislikedKeys().toSet()

    suspend fun saveItem(item: ClosetItemEntity): Long = dao.upsertItem(item)
    suspend fun updateItem(item: ClosetItemEntity) = dao.updateItem(item.copy(updatedAt = System.currentTimeMillis()))
    suspend fun deleteItem(item: ClosetItemEntity) {
        deletePhotoFile(item.photoPath)
        dao.deleteItem(item)
    }

    suspend fun saveOutfit(outfit: OutfitEntity): Long = dao.upsertOutfit(outfit)
    suspend fun setLiked(id: Long, liked: Boolean?) = dao.setOutfitLiked(id, liked)
    suspend fun deleteOutfit(id: Long) = dao.deleteOutfit(id)

    suspend fun dislikeCombo(ids: List<Long>) {
        dao.insertDisliked(DislikedComboEntity(DislikedComboEntity.keyFor(ids)))
    }

    suspend fun clearAllData() {
        val items = dao.getItems()
        items.forEach { deletePhotoFile(it.photoPath) }
        dao.clearItems()
        dao.clearOutfits()
        dao.clearDisliked()
    }

    suspend fun persistPhotoFromUri(uri: Uri): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "closet").apply { mkdirs() }
        val outFile = File(dir, "item_${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            val original = BitmapFactory.decodeStream(input)
                ?: error("Could not decode image")
            val maxSide = 1600
            val scaled = if (original.width > maxSide || original.height > maxSide) {
                val ratio = minOf(maxSide.toFloat() / original.width, maxSide.toFloat() / original.height)
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * ratio).toInt().coerceAtLeast(1),
                    (original.height * ratio).toInt().coerceAtLeast(1),
                    true
                ).also { if (it !== original) original.recycle() }
            } else original
            FileOutputStream(outFile).use { fos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 88, fos)
            }
            if (scaled !== original) scaled.recycle() else original.recycle()
        } ?: error("Could not open image")
        outFile.absolutePath
    }

    suspend fun copyFromAbsolutePath(absolutePath: String): String = withContext(Dispatchers.IO) {
        val src = File(absolutePath)
        require(src.exists()) { "Photo not found" }
        val dir = File(context.filesDir, "closet").apply { mkdirs() }
        val outFile = File(dir, "item_${UUID.randomUUID()}.jpg")
        val original = BitmapFactory.decodeFile(absolutePath)
            ?: run {
                src.copyTo(outFile, overwrite = true)
                return@withContext outFile.absolutePath
            }
        val maxSide = 1600
        val scaled = if (original.width > maxSide || original.height > maxSide) {
            val ratio = minOf(maxSide.toFloat() / original.width, maxSide.toFloat() / original.height)
            Bitmap.createScaledBitmap(
                original,
                (original.width * ratio).toInt().coerceAtLeast(1),
                (original.height * ratio).toInt().coerceAtLeast(1),
                true
            ).also { if (it !== original) original.recycle() }
        } else original
        FileOutputStream(outFile).use { fos ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 88, fos)
        }
        if (scaled !== original) scaled.recycle() else original.recycle()
        outFile.absolutePath
    }

    /**
     * Crop [sourcePath] using normalized bbox [left, top, right, bottom] in 0..1.
     * Returns a new file path, or copies the whole image if crop fails.
     */
    suspend fun cropPhoto(sourcePath: String, bbox: FloatArray): String = withContext(Dispatchers.IO) {
        val original = BitmapFactory.decodeFile(sourcePath)
            ?: return@withContext copyFromAbsolutePath(sourcePath)
        val left = (bbox[0] * original.width).toInt().coerceIn(0, original.width - 1)
        val top = (bbox[1] * original.height).toInt().coerceIn(0, original.height - 1)
        val right = (bbox[2] * original.width).toInt().coerceIn(left + 1, original.width)
        val bottom = (bbox[3] * original.height).toInt().coerceIn(top + 1, original.height)
        val w = right - left
        val h = bottom - top
        val cropped = try {
            Bitmap.createBitmap(original, left, top, w, h)
        } catch (_: Exception) {
            original.recycle()
            return@withContext copyFromAbsolutePath(sourcePath)
        }
        if (cropped !== original) original.recycle()
        val dir = File(context.filesDir, "closet").apply { mkdirs() }
        val outFile = File(dir, "item_${UUID.randomUUID()}.jpg")
        FileOutputStream(outFile).use { fos ->
            cropped.compress(Bitmap.CompressFormat.JPEG, 88, fos)
        }
        cropped.recycle()
        outFile.absolutePath
    }

    suspend fun createCameraCacheFile(): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        File(dir, "capture_${System.currentTimeMillis()}.jpg")
    }

    private fun deletePhotoFile(path: String) {
        if (path.isBlank()) return
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }
}
