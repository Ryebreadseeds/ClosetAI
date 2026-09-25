package com.ryebreadseeds.closetai.data.repo

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("closetai_settings")

class SettingsRepository(private val context: Context) {

    private val cityKey = stringPreferencesKey("weather_city")
    private val latKey = stringPreferencesKey("weather_lat")
    private val lonKey = stringPreferencesKey("weather_lon")
    private val moodKey = stringPreferencesKey("default_mood")
    private val occasionKey = stringPreferencesKey("default_occasion")

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "closetai_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getApiKey(): String =
        encryptedPrefs.getString(KEY_API, "")?.trim().orEmpty()

    fun setApiKey(value: String) {
        encryptedPrefs.edit().putString(KEY_API, value.trim()).apply()
    }

    fun getOpenRouterBaseUrl(): String =
        encryptedPrefs.getString(KEY_BASE, DEFAULT_BASE)?.trim().orEmpty().ifBlank { DEFAULT_BASE }

    fun setOpenRouterBaseUrl(value: String) {
        encryptedPrefs.edit().putString(KEY_BASE, value.trim().ifBlank { DEFAULT_BASE }).apply()
    }

    fun getVisionModel(): String =
        encryptedPrefs.getString(KEY_VISION_MODEL, DEFAULT_VISION)?.trim().orEmpty()
            .ifBlank { DEFAULT_VISION }

    fun setVisionModel(value: String) {
        encryptedPrefs.edit().putString(KEY_VISION_MODEL, value.trim().ifBlank { DEFAULT_VISION }).apply()
    }

    fun getChatModel(): String =
        encryptedPrefs.getString(KEY_CHAT_MODEL, DEFAULT_CHAT)?.trim().orEmpty()
            .ifBlank { DEFAULT_CHAT }

    fun setChatModel(value: String) {
        encryptedPrefs.edit().putString(KEY_CHAT_MODEL, value.trim().ifBlank { DEFAULT_CHAT }).apply()
    }

    val weatherCity: Flow<String> = context.dataStore.data.map { it[cityKey] ?: DEFAULT_CITY }
    val weatherLat: Flow<String> = context.dataStore.data.map { it[latKey] ?: DEFAULT_LAT }
    val weatherLon: Flow<String> = context.dataStore.data.map { it[lonKey] ?: DEFAULT_LON }
    val defaultMood: Flow<String> = context.dataStore.data.map { it[moodKey] ?: "" }
    val defaultOccasion: Flow<String> = context.dataStore.data.map { it[occasionKey] ?: "Casual" }

    suspend fun setWeatherCity(city: String) {
        context.dataStore.edit { it[cityKey] = city.trim() }
    }

    suspend fun setWeatherCoords(lat: String, lon: String) {
        context.dataStore.edit {
            it[latKey] = lat.trim()
            it[lonKey] = lon.trim()
        }
    }

    suspend fun setDefaultMood(mood: String) {
        context.dataStore.edit { it[moodKey] = mood.trim() }
    }

    suspend fun setDefaultOccasion(occasion: String) {
        context.dataStore.edit { it[occasionKey] = occasion.trim() }
    }

    suspend fun clearNonSecure() {
        context.dataStore.edit { it.clear() }
    }

    fun clearApiKey() {
        encryptedPrefs.edit().remove(KEY_API).apply()
    }

    companion object {
        private const val KEY_API = "openrouter_api_key"
        private const val KEY_BASE = "openrouter_base_url"
        private const val KEY_VISION_MODEL = "vision_model"
        private const val KEY_CHAT_MODEL = "chat_model"
        const val DEFAULT_BASE = "https://openrouter.ai/api/v1"
        // Free / low-cost OpenRouter models (user can change)
        const val DEFAULT_VISION = "google/gemini-2.0-flash-001"
        const val DEFAULT_CHAT = "google/gemini-2.0-flash-001"
        // Little Falls, NJ area defaults
        const val DEFAULT_CITY = "Little Falls, NJ"
        const val DEFAULT_LAT = "40.8754"
        const val DEFAULT_LON = "-74.2107"
    }
}
