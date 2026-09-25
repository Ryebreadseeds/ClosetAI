package com.ryebreadseeds.closetai.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ryebreadseeds.closetai.ClosetAiApp
import com.ryebreadseeds.closetai.ai.LocalImageFallback
import com.ryebreadseeds.closetai.ai.VisionSuggestion
import com.ryebreadseeds.closetai.data.entity.ClosetItemEntity
import com.ryebreadseeds.closetai.data.entity.OutfitEntity
import com.ryebreadseeds.closetai.data.repo.SettingsRepository
import com.ryebreadseeds.closetai.domain.ClothingCategory
import com.ryebreadseeds.closetai.domain.GeneratedOutfit
import com.ryebreadseeds.closetai.domain.Occasion
import com.ryebreadseeds.closetai.domain.OutfitSource
import com.ryebreadseeds.closetai.domain.Season
import com.ryebreadseeds.closetai.domain.WeatherSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodayUiState(
    val occasion: Occasion = Occasion.CASUAL,
    val mood: String = "",
    val weather: WeatherSnapshot? = null,
    val weatherLoading: Boolean = false,
    val generating: Boolean = false,
    val current: GeneratedOutfit? = null,
    val currentItems: List<ClosetItemEntity> = emptyList(),
    val currentOutfitId: Long? = null,
    val liked: Boolean? = null,
    val message: String? = null,
    val hasApiKey: Boolean = false
)

data class AddEditDraft(
    val id: Long? = null,
    val photoPath: String = "",
    val name: String = "",
    val category: String = ClothingCategory.TOP.label,
    val color: String = "",
    val season: String = Season.ALL.label,
    val notes: String = "",
    val analyzing: Boolean = false,
    val error: String? = null
)

class ClosetViewModel(private val app: ClosetAiApp) : ViewModel() {
    private val closet = app.closetRepository
    private val settings = app.settingsRepository

    val items: StateFlow<List<ClosetItemEntity>> = closet.observeItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val outfits: StateFlow<List<OutfitEntity>> = closet.observeOutfits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _today = MutableStateFlow(TodayUiState())
    val today: StateFlow<TodayUiState> = _today.asStateFlow()

    private val _draft = MutableStateFlow<AddEditDraft?>(null)
    val draft: StateFlow<AddEditDraft?> = _draft.asStateFlow()

    private val _settingsCity = MutableStateFlow(SettingsRepository.DEFAULT_CITY)
    private val _settingsLat = MutableStateFlow(SettingsRepository.DEFAULT_LAT)
    private val _settingsLon = MutableStateFlow(SettingsRepository.DEFAULT_LON)
    private val _apiKeyDraft = MutableStateFlow("")
    private val _statusMessage = MutableStateFlow<String?>(null)

    val settingsCity: StateFlow<String> = _settingsCity.asStateFlow()
    val settingsLat: StateFlow<String> = _settingsLat.asStateFlow()
    val settingsLon: StateFlow<String> = _settingsLon.asStateFlow()
    val apiKeyDraft: StateFlow<String> = _apiKeyDraft.asStateFlow()
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _hasApiKey = MutableStateFlow(settings.getApiKey().isNotBlank())
    val apiKeyConfigured: StateFlow<Boolean> = _hasApiKey.asStateFlow()

    init {
        _apiKeyDraft.value = settings.getApiKey()
        viewModelScope.launch {
            settings.weatherCity.collect { _settingsCity.value = it }
        }
        viewModelScope.launch {
            settings.weatherLat.collect { _settingsLat.value = it }
        }
        viewModelScope.launch {
            settings.weatherLon.collect { _settingsLon.value = it }
        }
        viewModelScope.launch {
            settings.defaultMood.collect { mood ->
                _today.update { it.copy(mood = mood) }
            }
        }
        viewModelScope.launch {
            settings.defaultOccasion.collect { occ ->
                _today.update { it.copy(occasion = Occasion.fromLabel(occ)) }
            }
        }
        refreshWeather()
        _today.update { it.copy(hasApiKey = settings.getApiKey().isNotBlank()) }
    }

    fun setOccasion(occasion: Occasion) {
        _today.update { it.copy(occasion = occasion) }
        viewModelScope.launch { settings.setDefaultOccasion(occasion.label) }
    }

    fun setMood(mood: String) {
        _today.update { it.copy(mood = mood) }
    }

    fun persistMood() {
        viewModelScope.launch { settings.setDefaultMood(_today.value.mood) }
    }

    fun refreshWeather() {
        viewModelScope.launch {
            _today.update { it.copy(weatherLoading = true) }
            val city = _settingsCity.value
            var lat = _settingsLat.value.toDoubleOrNull()
            var lon = _settingsLon.value.toDoubleOrNull()
            if (lat == null || lon == null) {
                val geo = app.weatherClient.geocodeCity(city)
                if (geo != null) {
                    lat = geo.first
                    lon = geo.second
                    settings.setWeatherCoords(lat.toString(), lon.toString())
                }
            }
            val snap = if (lat != null && lon != null) {
                app.weatherClient.fetch(lat, lon, city)
            } else null
            _today.update { it.copy(weather = snap, weatherLoading = false) }
        }
    }

    fun generateOutfit(preferLlm: Boolean = true) {
        viewModelScope.launch {
            _today.update { it.copy(generating = true, message = null) }
            val inventory = closet.getItems()
            if (inventory.isEmpty()) {
                _today.update {
                    it.copy(generating = false, message = "Add some clothes in Closet first.")
                }
                return@launch
            }
            val disliked = closet.getDislikedKeys()
            val occasion = _today.value.occasion
            val mood = _today.value.mood.ifBlank { null }
            val weather = _today.value.weather
            val apiKey = settings.getApiKey()

            var result: GeneratedOutfit? = null
            if (preferLlm && apiKey.isNotBlank()) {
                result = runCatching {
                    app.openRouterClient.suggestOutfitFromInventory(
                        apiKey = apiKey,
                        baseUrl = settings.getOpenRouterBaseUrl(),
                        model = settings.getChatModel(),
                        items = inventory,
                        occasion = occasion,
                        mood = mood,
                        weather = weather,
                        dislikedKeys = disliked
                    )
                }.getOrNull()?.let { llm ->
                    // Validate IDs exist & dress/romper rule
                    val map = inventory.associateBy { it.id }
                    val pieces = llm.itemIds.mapNotNull { map[it] }
                    if (pieces.isEmpty()) null
                    else if (violatesOnePieceRule(pieces)) null
                    else llm.copy(itemIds = pieces.map { it.id })
                }
            }
            if (result == null) {
                result = app.rulesEngine.generate(
                    items = inventory,
                    occasion = occasion,
                    mood = mood,
                    weather = weather,
                    dislikedKeys = disliked
                )
            }
            if (result == null) {
                _today.update {
                    it.copy(
                        generating = false,
                        message = "Could not build an outfit. Try adding more variety (top + bottom or a dress)."
                    )
                }
                return@launch
            }

            val entity = OutfitEntity(
                title = result.title,
                itemIdsCsv = OutfitEntity.idsToCsv(result.itemIds),
                occasion = result.occasion.label,
                mood = result.mood,
                rationale = result.rationale,
                source = result.source.name,
                liked = null
            )
            val id = closet.saveOutfit(entity)
            val pieces = result.itemIds.mapNotNull { iid -> inventory.find { it.id == iid } }
            _today.update {
                it.copy(
                    generating = false,
                    current = result,
                    currentItems = pieces,
                    currentOutfitId = id,
                    liked = null,
                    message = if (result.source == OutfitSource.LLM) "AI-enhanced suggestion"
                    else "Offline rules suggestion",
                    hasApiKey = apiKey.isNotBlank()
                )
            }
        }
    }

    private fun violatesOnePieceRule(pieces: List<ClosetItemEntity>): Boolean {
        val cats = pieces.map { ClothingCategory.fromLabel(it.category) }
        val hasOne = ClothingCategory.DRESS in cats || ClothingCategory.ROMPER in cats
        val hasBottom = ClothingCategory.BOTTOM in cats
        return hasOne && hasBottom
    }

    fun likeCurrent() {
        val id = _today.value.currentOutfitId ?: return
        viewModelScope.launch {
            closet.setLiked(id, true)
            _today.update { it.copy(liked = true, message = "Liked — we'll lean into looks like this.") }
        }
    }

    fun dislikeCurrent() {
        val current = _today.value.current ?: return
        val id = _today.value.currentOutfitId
        viewModelScope.launch {
            closet.dislikeCombo(current.itemIds)
            if (id != null) closet.setLiked(id, false)
            _today.update { it.copy(liked = false, message = "Noted — that combo won't repeat.") }
            generateOutfit()
        }
    }

    fun deleteOutfit(id: Long) {
        viewModelScope.launch {
            closet.deleteOutfit(id)
            if (_today.value.currentOutfitId == id) {
                _today.update {
                    it.copy(current = null, currentItems = emptyList(), currentOutfitId = null, liked = null)
                }
            }
        }
    }

    fun startAddFromUri(uri: Uri) {
        viewModelScope.launch {
            _draft.value = AddEditDraft(analyzing = true)
            runCatching {
                val path = closet.persistPhotoFromUri(uri)
                analyzeAndFillDraft(path, id = null)
            }.onFailure { e ->
                _draft.value = AddEditDraft(error = e.message ?: "Failed to import photo", analyzing = false)
            }
        }
    }

    fun startAddFromPath(path: String) {
        viewModelScope.launch {
            _draft.value = AddEditDraft(analyzing = true)
            runCatching {
                val persisted = closet.copyFromAbsolutePath(path)
                analyzeAndFillDraft(persisted, id = null)
            }.onFailure { e ->
                _draft.value = AddEditDraft(error = e.message ?: "Failed to import photo", analyzing = false)
            }
        }
    }

    fun startEdit(item: ClosetItemEntity) {
        _draft.value = AddEditDraft(
            id = item.id,
            photoPath = item.photoPath,
            name = item.name,
            category = item.category,
            color = item.color,
            season = item.season,
            notes = item.notes
        )
    }

    private suspend fun analyzeAndFillDraft(path: String, id: Long?) {
        val apiKey = settings.getApiKey()
        val suggestion: VisionSuggestion = if (apiKey.isNotBlank()) {
            app.openRouterClient.analyzeItemPhoto(
                apiKey = apiKey,
                baseUrl = settings.getOpenRouterBaseUrl(),
                model = settings.getVisionModel(),
                photoPath = path
            ) ?: LocalImageFallback.suggestFromPhoto(path)
        } else {
            LocalImageFallback.suggestFromPhoto(path)
        }
        _draft.value = AddEditDraft(
            id = id,
            photoPath = path,
            name = suggestion.name,
            category = suggestion.category,
            color = suggestion.color,
            season = suggestion.season,
            notes = suggestion.notes,
            analyzing = false
        )
    }

    fun updateDraft(transform: (AddEditDraft) -> AddEditDraft) {
        _draft.update { cur -> cur?.let(transform) }
    }

    fun dismissDraft() {
        _draft.value = null
    }

    fun saveDraft() {
        val d = _draft.value ?: return
        if (d.photoPath.isBlank()) {
            _draft.update { it?.copy(error = "Photo required") }
            return
        }
        viewModelScope.launch {
            val entity = ClosetItemEntity(
                id = d.id ?: 0,
                name = d.name.ifBlank { "Untitled" },
                category = ClothingCategory.fromLabel(d.category).label,
                color = d.color.ifBlank { "Unknown" },
                season = Season.fromLabel(d.season).label,
                photoPath = d.photoPath,
                notes = d.notes
            )
            if (d.id == null) closet.saveItem(entity) else closet.updateItem(entity)
            _draft.value = null
            _statusMessage.value = "Saved to closet"
        }
    }

    fun deleteItem(item: ClosetItemEntity) {
        viewModelScope.launch { closet.deleteItem(item) }
    }

    fun setApiKeyDraft(value: String) {
        _apiKeyDraft.value = value
    }

    fun saveApiKey() {
        settings.setApiKey(_apiKeyDraft.value)
        val on = settings.getApiKey().isNotBlank()
        _hasApiKey.value = on
        _today.update { it.copy(hasApiKey = on) }
        _statusMessage.value = if (on) "API key saved (encrypted on device)" else "API key cleared"
    }

    fun clearApiKey() {
        settings.clearApiKey()
        _apiKeyDraft.value = ""
        _hasApiKey.value = false
        _today.update { it.copy(hasApiKey = false) }
        _statusMessage.value = "API key cleared"
    }

    fun setCity(city: String) {
        _settingsCity.value = city
    }

    fun setLatLon(lat: String, lon: String) {
        _settingsLat.value = lat
        _settingsLon.value = lon
    }

    fun saveLocation() {
        viewModelScope.launch {
            settings.setWeatherCity(_settingsCity.value)
            val lat = _settingsLat.value.toDoubleOrNull()
            val lon = _settingsLon.value.toDoubleOrNull()
            if (lat != null && lon != null) {
                settings.setWeatherCoords(lat.toString(), lon.toString())
            } else {
                val geo = app.weatherClient.geocodeCity(_settingsCity.value)
                if (geo != null) {
                    settings.setWeatherCoords(geo.first.toString(), geo.second.toString())
                    _settingsLat.value = geo.first.toString()
                    _settingsLon.value = geo.second.toString()
                }
            }
            _statusMessage.value = "Location saved"
            refreshWeather()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            closet.clearAllData()
            _today.update {
                it.copy(current = null, currentItems = emptyList(), currentOutfitId = null, liked = null)
            }
            _statusMessage.value = "All closet data cleared"
        }
    }

    fun clearStatus() {
        _statusMessage.value = null
        _today.update { it.copy(message = null) }
    }

    suspend fun createCameraFile(): java.io.File = closet.createCameraCacheFile()
}

class ClosetViewModelFactory(private val app: ClosetAiApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClosetViewModel::class.java)) {
            return ClosetViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}
