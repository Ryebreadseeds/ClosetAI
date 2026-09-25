package com.ryebreadseeds.closetai.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.ryebreadseeds.closetai.data.entity.ClosetItemEntity
import com.ryebreadseeds.closetai.domain.ClothingCategory
import com.ryebreadseeds.closetai.domain.GeneratedOutfit
import com.ryebreadseeds.closetai.domain.Occasion
import com.ryebreadseeds.closetai.domain.OutfitSource
import com.ryebreadseeds.closetai.domain.WeatherSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

data class VisionSuggestion(
    val name: String,
    val category: String,
    val color: String,
    val season: String,
    val notes: String = ""
)

class OpenRouterClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    suspend fun analyzeItemPhoto(
        apiKey: String,
        baseUrl: String,
        model: String,
        photoPath: String
    ): VisionSuggestion? = withContext(Dispatchers.IO) {
        val dataUrl = encodeImageDataUrl(photoPath) ?: return@withContext null
        val system = """
            You identify clothing items in photos for a personal wardrobe app.
            Reply with ONLY compact JSON (no markdown):
            {"name":"short name","category":"Top|Bottom|Dress|Romper|Outerwear|Shoes|Accessory|Other","color":"main color","season":"Spring|Summer|Fall|Winter|All seasons","notes":"optional"}
        """.trimIndent()

        val userContent = JSONArray()
            .put(JSONObject().put("type", "text").put("text", "Describe this clothing item for my closet."))
            .put(
                JSONObject()
                    .put("type", "image_url")
                    .put("image_url", JSONObject().put("url", dataUrl))
            )

        val body = chatBody(model, system, userContent)
        val text = postChat(apiKey, baseUrl, body) ?: return@withContext null
        parseVision(text)
    }

    suspend fun suggestOutfitFromInventory(
        apiKey: String,
        baseUrl: String,
        model: String,
        items: List<ClosetItemEntity>,
        occasion: Occasion,
        mood: String?,
        weather: WeatherSnapshot?,
        dislikedKeys: Set<String>
    ): GeneratedOutfit? = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext null
        val inventory = items.joinToString("\n") {
            "- id=${it.id}; name=${it.name}; category=${it.category}; color=${it.color}; season=${it.season}"
        }
        val weatherLine = weather?.let {
            "Weather: ${it.temperatureF.toInt()}°F, ${it.description}, ${it.cityLabel}"
        } ?: "Weather: unknown"
        val dislikeLine = if (dislikedKeys.isEmpty()) "none" else dislikedKeys.take(40).joinToString(", ")

        val system = """
            You are a stylist for a wardrobe app. Pick a coherent outfit from the inventory IDs only.
            Rules:
            - Support layering (e.g. tank under shirt + outerwear) when weather/mood warrants.
            - NEVER pair pants/bottoms with Dress or Romper.
            - Prefer items matching occasion and season.
            - Avoid disliked combo keys (sorted ids joined by |).
            Reply ONLY JSON:
            {"itemIds":[1,2,3],"title":"...","rationale":"...","layered":true}
        """.trimIndent()

        val userText = buildString {
            appendLine("Occasion: ${occasion.label}")
            if (!mood.isNullOrBlank()) appendLine("Mood: $mood")
            appendLine(weatherLine)
            appendLine("Disliked combo keys: $dislikeLine")
            appendLine("Inventory:")
            append(inventory)
        }

        val userContent = JSONArray().put(JSONObject().put("type", "text").put("text", userText))
        val body = chatBody(model, system, userContent)
        val text = postChat(apiKey, baseUrl, body) ?: return@withContext null
        parseOutfit(text, occasion, mood)
    }

    private fun chatBody(model: String, system: String, userContent: JSONArray): JSONObject {
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", system))
            .put(JSONObject().put("role", "user").put("content", userContent))
        return JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", 0.4)
    }

    private fun postChat(apiKey: String, baseUrl: String, body: JSONObject): String? {
        val url = baseUrl.trimEnd('/') + "/chat/completions"
        val req = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("HTTP-Referer", "https://github.com/Ryebreadseeds/ClosetAI")
            .addHeader("X-Title", "ClosetAI")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return runCatching {
            client.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@use null
                val json = JSONObject(raw)
                val content = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .get("content")
                when (content) {
                    is String -> content
                    is JSONArray -> buildString {
                        for (i in 0 until content.length()) {
                            val part = content.getJSONObject(i)
                            if (part.optString("type") == "text") append(part.optString("text"))
                        }
                    }
                    else -> content.toString()
                }
            }
        }.getOrNull()
    }

    private fun encodeImageDataUrl(path: String): String? {
        val file = File(path)
        if (!file.exists()) return null
        val original = BitmapFactory.decodeFile(path) ?: return null
        val max = 1024
        val bmp = if (original.width > max || original.height > max) {
            val r = minOf(max.toFloat() / original.width, max.toFloat() / original.height)
            Bitmap.createScaledBitmap(
                original,
                (original.width * r).toInt().coerceAtLeast(1),
                (original.height * r).toInt().coerceAtLeast(1),
                true
            ).also { if (it !== original) original.recycle() }
        } else original
        val baos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        if (bmp !== original) bmp.recycle() else original.recycle()
        val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        return "data:image/jpeg;base64,$b64"
    }

    private fun parseVision(raw: String): VisionSuggestion? {
        val json = extractJsonObject(raw) ?: return null
        val category = json.optString("category", "Other")
        val valid = ClothingCategory.entries.any { it.label.equals(category, true) }
        return VisionSuggestion(
            name = json.optString("name", "New item").ifBlank { "New item" },
            category = if (valid) ClothingCategory.fromLabel(category).label else "Other",
            color = json.optString("color", "Unknown").ifBlank { "Unknown" },
            season = json.optString("season", "All seasons").ifBlank { "All seasons" },
            notes = json.optString("notes", "")
        )
    }

    private fun parseOutfit(raw: String, occasion: Occasion, mood: String?): GeneratedOutfit? {
        val json = extractJsonObject(raw) ?: return null
        val arr = json.optJSONArray("itemIds") ?: return null
        val ids = buildList {
            for (i in 0 until arr.length()) add(arr.getLong(i))
        }
        if (ids.isEmpty()) return null
        return GeneratedOutfit(
            itemIds = ids,
            title = json.optString("title", "AI outfit · ${occasion.label}"),
            rationale = json.optString("rationale", "Suggested by AI from your closet"),
            occasion = occasion,
            mood = mood?.takeIf { it.isNotBlank() },
            layered = json.optBoolean("layered", false),
            source = OutfitSource.LLM
        )
    }

    private fun extractJsonObject(raw: String): JSONObject? {
        val trimmed = raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        return runCatching { JSONObject(trimmed) }.getOrElse {
            val start = trimmed.indexOf('{')
            val end = trimmed.lastIndexOf('}')
            if (start >= 0 && end > start) {
                runCatching { JSONObject(trimmed.substring(start, end + 1)) }.getOrNull()
            } else null
        }
    }
}
