package com.ryebreadseeds.closetai.weather

import com.ryebreadseeds.closetai.domain.WeatherSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenMeteoClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    suspend fun fetch(lat: Double, lon: Double, cityLabel: String): WeatherSnapshot? =
        withContext(Dispatchers.IO) {
            val url =
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,weather_code&timezone=auto"
            val req = Request.Builder().url(url).get().build()
            runCatching {
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val body = resp.body?.string() ?: return@use null
                    val json = JSONObject(body)
                    val current = json.getJSONObject("current")
                    val temp = current.getDouble("temperature_2m")
                    val code = current.getInt("weather_code")
                    WeatherSnapshot(
                        temperatureC = temp,
                        weatherCode = code,
                        cityLabel = cityLabel,
                        description = describeCode(code)
                    )
                }
            }.getOrNull()
        }

    suspend fun geocodeCity(city: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        val q = java.net.URLEncoder.encode(city, Charsets.UTF_8.name())
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$q&count=1&language=en&format=json"
        val req = Request.Builder().url(url).get().build()
        runCatching {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string() ?: return@use null
                val results = JSONObject(body).optJSONArray("results") ?: return@use null
                if (results.length() == 0) return@use null
                val first = results.getJSONObject(0)
                first.getDouble("latitude") to first.getDouble("longitude")
            }
        }.getOrNull()
    }

    private fun describeCode(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2, 3 -> "Partly cloudy"
        45, 48 -> "Foggy"
        51, 53, 55, 56, 57 -> "Drizzle"
        61, 63, 65, 66, 67 -> "Rain"
        71, 73, 75, 77 -> "Snow"
        80, 81, 82 -> "Showers"
        85, 86 -> "Snow showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "Mixed"
    }
}
