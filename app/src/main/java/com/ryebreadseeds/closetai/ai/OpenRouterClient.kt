package com.ryebreadseeds.closetai.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.ryebreadseeds.closetai.data.entity.ClosetItemEntity
import com.ryebreadseeds.closetai.domain.ClothingCategory
import com.ryebreadseeds.closetai.domain.GeneratedOutfit
import com.ryebreadseeds.closetai.domain.Occasion
import com.ryebreadseeds.closetai.domain.OutfitCheckResult
import com.ryebreadseeds.closetai.domain.OutfitSource
import com.ryebreadseeds.closetai.domain.ShoppingSuggestion
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
    val notes: String = "",
    /** Normalized 0..1 bbox: left, top, right, bottom — null if whole image. */
    val bbox: FloatArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VisionSuggestion) return false
        return name == other.name && category == other.category && color == other.color &&
            season == other.season && notes == other.notes &&
            ((bbox == null && other.bbox == null) || (bbox != null && other.bbox != null && bbox.contentEquals(other.bbox)))
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + season.hashCode()
        result = 31 * result + notes.hashCode()
        result = 31 * result + (bbox?.contentHashCode() ?: 0)
        return result
    }
}

data class MultiVisionResult(
    val items: List<VisionSuggestion>,
    val rawNote: String = ""
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

    /** Magic upload: detect multiple garments in one photo. */
    suspend fun analyzeMultiItemPhoto(
        apiKey: String,
        baseUrl: String,
        model: String,
        photoPath: String
    ): MultiVisionResult? = withContext(Dispatchers.IO) {
        val dataUrl = encodeImageDataUrl(photoPath) ?: return@withContext null
        val system = """
            You extract EVERY distinct clothing item visible in a wardrobe photo (flat lay, rack, pile, or worn).
            Reply ONLY with JSON (no markdown):
            {"items":[{"name":"...","category":"Top|Bottom|Dress|Romper|Outerwear|Shoes|Accessory|Other","color":"...","season":"Spring|Summer|Fall|Winter|All seasons","notes":"optional","bbox":[left,top,right,bottom]}]}
            bbox values are normalized 0..1 relative to image width/height. Omit bbox if unsure.
            Return 1–12 items. Skip people/faces; focus on garments and accessories.
        """.trimIndent()

        val userContent = JSONArray()
            .put(JSONObject().put("type", "text").put("text", "List every clothing item in this photo for my closet."))
            .put(
                JSONObject()
                    .put("type", "image_url")
                    .put("image_url", JSONObject().put("url", dataUrl))
            )

        val body = chatBody(model, system, userContent)
        val text = postChat(apiKey, baseUrl, body) ?: return@withContext null
        parseMultiVision(text)
    }

    suspend fun suggestOutfitFromInventory(
        apiKey: String,
        baseUrl: String,
        model: String,
        items: List<ClosetItemEntity>,
        occasion: Occasion,
        mood: String?,
        weather: WeatherSnapshot?,
        dislikedKeys: Set<String>,
        preferUnusedIds: Set<Long> = emptySet(),
        likedColorHints: List<String> = emptyList()
    ): GeneratedOutfit? = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext null
        val inventory = items.joinToString("\n") {
            "- id=${it.id}; name=${it.name}; category=${it.category}; color=${it.color}; season=${it.season}"
        }
        val weatherLine = weather?.let {
            "Weather: ${it.temperatureF.toInt()}°F, ${it.description}, ${it.cityLabel}"
        } ?: "Weather: unknown"
        val dislikeLine = if (dislikedKeys.isEmpty()) "none" else dislikedKeys.take(40).joinToString(", ")
        val unusedLine = if (preferUnusedIds.isEmpty()) "none" else preferUnusedIds.take(30).joinToString(",")
        val likedLine = if (likedColorHints.isEmpty()) "none" else likedColorHints.take(8).joinToString(", ")

        val system = """
            You are a stylist for a wardrobe app. Pick a coherent outfit from the inventory IDs only.
            Rules:
            - Support layering (e.g. tank under shirt + outerwear) when weather/mood warrants.
            - NEVER pair pants/bottoms with Dress or Romper.
            - Prefer items matching occasion and season.
            - Prefer unused item IDs and liked colors when listed.
            - Avoid disliked combo keys (sorted ids joined by |).
            Reply ONLY JSON:
            {"itemIds":[1,2,3],"title":"...","rationale":"...","layered":true}
        """.trimIndent()

        val userText = buildString {
            appendLine("Occasion: ${occasion.label}")
            if (!mood.isNullOrBlank()) appendLine("Mood: $mood")
            appendLine(weatherLine)
            appendLine("Disliked combo keys: $dislikeLine")
            appendLine("Prefer unused ids: $unusedLine")
            appendLine("Liked color hints: $likedLine")
            appendLine("Inventory:")
            append(inventory)
        }

        val userContent = JSONArray().put(JSONObject().put("type", "text").put("text", userText))
        val body = chatBody(model, system, userContent)
        val text = postChat(apiKey, baseUrl, body) ?: return@withContext null
        parseOutfit(text, occasion, mood)
    }

    suspend fun completeMixMatch(
        apiKey: String,
        baseUrl: String,
        model: String,
        items: List<ClosetItemEntity>,
        selectedIds: List<Long>,
        occasion: Occasion,
        weather: WeatherSnapshot?
    ): GeneratedOutfit? = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext null
        val inventory = items.joinToString("\n") {
            "- id=${it.id}; name=${it.name}; category=${it.category}; color=${it.color}; season=${it.season}"
        }
        val system = """
            Complete a partial outfit using ONLY inventory IDs. Keep all selected IDs.
            NEVER pair Bottom with Dress/Romper. Fill empty slots (top/bottom or one-piece, shoes, optional outer/accessory).
            Reply ONLY JSON: {"itemIds":[...],"title":"...","rationale":"...","layered":false}
        """.trimIndent()
        val userText = buildString {
            appendLine("Occasion: ${occasion.label}")
            appendLine("Already selected IDs (must keep): ${selectedIds.joinToString(",")}")
            weather?.let { appendLine("Weather: ${it.temperatureF.toInt()}°F ${it.description}") }
            appendLine("Inventory:")
            append(inventory)
        }
        val userContent = JSONArray().put(JSONObject().put("type", "text").put("text", userText))
        val text = postChat(apiKey, baseUrl, chatBody(model, system, userContent)) ?: return@withContext null
        parseOutfit(text, occasion, null)?.let { outfit ->
            val merged = (selectedIds + outfit.itemIds).distinct()
            outfit.copy(itemIds = merged)
        }
    }

    suspend fun whatGoesWithItem(
        apiKey: String,
        baseUrl: String,
        model: String,
        items: List<ClosetItemEntity>,
        anchorId: Long,
        occasion: Occasion,
        weather: WeatherSnapshot?
    ): List<GeneratedOutfit> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext emptyList()
        val inventory = items.joinToString("\n") {
            "- id=${it.id}; name=${it.name}; category=${it.category}; color=${it.color}; season=${it.season}"
        }
        val system = """
            Suggest exactly 3 different outfits that ALL include item id=$anchorId.
            Use only inventory IDs. NEVER pair Bottom with Dress/Romper.
            Reply ONLY JSON:
            {"outfits":[{"itemIds":[...],"title":"...","rationale":"...","layered":false}, ...]}
        """.trimIndent()
        val userText = buildString {
            appendLine("Anchor id: $anchorId")
            appendLine("Occasion: ${occasion.label}")
            weather?.let { appendLine("Weather: ${it.temperatureF.toInt()}°F ${it.description}") }
            appendLine("Inventory:")
            append(inventory)
        }
        val userContent = JSONArray().put(JSONObject().put("type", "text").put("text", userText))
        val text = postChat(apiKey, baseUrl, chatBody(model, system, userContent)) ?: return@withContext emptyList()
        parseOutfitList(text, occasion)
    }

    suspend fun checkOutfitAi(
        apiKey: String,
        baseUrl: String,
        model: String,
        pieces: List<ClosetItemEntity>,
        occasion: Occasion
    ): OutfitCheckResult? = withContext(Dispatchers.IO) {
        if (pieces.isEmpty()) return@withContext null
        val list = pieces.joinToString("\n") {
            "- ${it.name} (${it.category}, ${it.color})"
        }
        val system = """
            Score this outfit 1–10 for color harmony, coherence (slots make sense), and occasion fit.
            NEVER praise pants+dress combos — score coherence very low if that happens.
            Reply ONLY JSON:
            {"colorScore":8,"coherenceScore":7,"occasionScore":9,"overall":8,"tips":["...","..."]}
        """.trimIndent()
        val userText = "Occasion: ${occasion.label}\nPieces:\n$list"
        val userContent = JSONArray().put(JSONObject().put("type", "text").put("text", userText))
        val text = postChat(apiKey, baseUrl, chatBody(model, system, userContent)) ?: return@withContext null
        parseCheck(text)
    }

    suspend fun shoppingBuddyAi(
        apiKey: String,
        baseUrl: String,
        model: String,
        items: List<ClosetItemEntity>
    ): List<ShoppingSuggestion>? = withContext(Dispatchers.IO) {
        val inventory = if (items.isEmpty()) "(empty closet)" else items.joinToString("\n") {
            "- ${it.name}; ${it.category}; ${it.color}; ${it.season}"
        }
        val system = """
            You are a shopping stylist. Suggest 3–7 concrete wardrobe pieces to BUY (not brands required).
            Focus on closet gaps: missing categories, color imbalance, occasion holes.
            No affiliate links, no prices, no store names required.
            Reply ONLY JSON:
            {"suggestions":[{"name":"navy chinos","category":"Bottom","why":"...","occasionHint":"Work"}]}
        """.trimIndent()
        val userContent = JSONArray().put(
            JSONObject().put("type", "text").put("text", "My closet:\n$inventory")
        )
        val text = postChat(apiKey, baseUrl, chatBody(model, system, userContent)) ?: return@withContext null
        parseShopping(text)
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
        return visionFromJson(json)
    }

    private fun parseMultiVision(raw: String): MultiVisionResult? {
        val json = extractJsonObject(raw) ?: return null
        val arr = json.optJSONArray("items") ?: return null
        val items = buildList {
            for (i in 0 until arr.length()) {
                visionFromJson(arr.getJSONObject(i))?.let { add(it) }
            }
        }
        if (items.isEmpty()) return null
        return MultiVisionResult(items = items)
    }

    private fun visionFromJson(json: JSONObject): VisionSuggestion? {
        val category = json.optString("category", "Other")
        val valid = ClothingCategory.entries.any { it.label.equals(category, true) }
        val bboxArr = json.optJSONArray("bbox")
        val bbox = if (bboxArr != null && bboxArr.length() >= 4) {
            floatArrayOf(
                bboxArr.getDouble(0).toFloat().coerceIn(0f, 1f),
                bboxArr.getDouble(1).toFloat().coerceIn(0f, 1f),
                bboxArr.getDouble(2).toFloat().coerceIn(0f, 1f),
                bboxArr.getDouble(3).toFloat().coerceIn(0f, 1f)
            )
        } else null
        return VisionSuggestion(
            name = json.optString("name", "New item").ifBlank { "New item" },
            category = if (valid) ClothingCategory.fromLabel(category).label else "Other",
            color = json.optString("color", "Unknown").ifBlank { "Unknown" },
            season = json.optString("season", "All seasons").ifBlank { "All seasons" },
            notes = json.optString("notes", ""),
            bbox = bbox
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

    private fun parseOutfitList(raw: String, occasion: Occasion): List<GeneratedOutfit> {
        val json = extractJsonObject(raw) ?: return emptyList()
        val arr = json.optJSONArray("outfits") ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val idsArr = o.optJSONArray("itemIds") ?: continue
                val ids = buildList {
                    for (j in 0 until idsArr.length()) add(idsArr.getLong(j))
                }
                if (ids.isEmpty()) continue
                add(
                    GeneratedOutfit(
                        itemIds = ids,
                        title = o.optString("title", "With this · ${occasion.label}"),
                        rationale = o.optString("rationale", "AI pairing"),
                        occasion = occasion,
                        mood = null,
                        layered = o.optBoolean("layered", false),
                        source = OutfitSource.LLM
                    )
                )
            }
        }
    }

    private fun parseCheck(raw: String): OutfitCheckResult? {
        val json = extractJsonObject(raw) ?: return null
        val tipsArr = json.optJSONArray("tips")
        val tips = buildList {
            if (tipsArr != null) {
                for (i in 0 until tipsArr.length()) add(tipsArr.getString(i))
            }
        }
        return OutfitCheckResult(
            colorScore = json.optInt("colorScore", 5).coerceIn(1, 10),
            coherenceScore = json.optInt("coherenceScore", 5).coerceIn(1, 10),
            occasionScore = json.optInt("occasionScore", 5).coerceIn(1, 10),
            overall = json.optInt("overall", 5).coerceIn(1, 10),
            tips = tips.ifEmpty { listOf("AI scored this look.") },
            source = OutfitSource.LLM
        )
    }

    private fun parseShopping(raw: String): List<ShoppingSuggestion>? {
        val json = extractJsonObject(raw) ?: return null
        val arr = json.optJSONArray("suggestions") ?: return null
        val list = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    ShoppingSuggestion(
                        name = o.optString("name", "New piece").ifBlank { "New piece" },
                        category = o.optString("category", "Other"),
                        why = o.optString("why", "Fills a gap in your closet"),
                        occasionHint = o.optString("occasionHint", "Everyday")
                    )
                )
            }
        }
        return list.takeIf { it.isNotEmpty() }
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
