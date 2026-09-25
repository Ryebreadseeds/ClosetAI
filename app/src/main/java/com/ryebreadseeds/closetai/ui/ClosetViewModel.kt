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
import com.ryebreadseeds.closetai.domain.ColorHarmony
import com.ryebreadseeds.closetai.domain.MixSlot
import com.ryebreadseeds.closetai.domain.OutfitCheckEngine
import com.ryebreadseeds.closetai.domain.OutfitCheckResult
import com.ryebreadseeds.closetai.domain.ShoppingBuddyEngine
import com.ryebreadseeds.closetai.domain.ShoppingSuggestion
import com.ryebreadseeds.closetai.domain.StylePrefs
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

data class MagicUploadState(
    val active: Boolean = false,
    val analyzing: Boolean = false,
    val message: String? = null,
    val savedCount: Int = 0
)

data class MixMatchState(
    val slots: Map<MixSlot, Long?> = MixSlot.entries.associateWith { null },
    val occasion: Occasion = Occasion.CASUAL,
    val working: Boolean = false,
    val message: String? = null
)

data class WhatGoesWithState(
    val anchorId: Long? = null,
    val loading: Boolean = false,
    val suggestions: List<GeneratedOutfit> = emptyList(),
    val suggestionItems: Map<Int, List<ClosetItemEntity>> = emptyMap(),
    val message: String? = null
)

data class OutfitCheckUiState(
    val selectedIds: Set<Long> = emptySet(),
    val fromOutfitId: Long? = null,
    val occasion: Occasion = Occasion.CASUAL,
    val loading: Boolean = false,
    val result: OutfitCheckResult? = null,
    val message: String? = null
)

data class ShoppingUiState(
    val loading: Boolean = false,
    val suggestions: List<ShoppingSuggestion> = emptyList(),
    val message: String? = null,
    val source: OutfitSource? = null
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

    private val _magic = MutableStateFlow(MagicUploadState())
    val magic: StateFlow<MagicUploadState> = _magic.asStateFlow()

    private val _mix = MutableStateFlow(MixMatchState())
    val mix: StateFlow<MixMatchState> = _mix.asStateFlow()

    private val _whatGoes = MutableStateFlow(WhatGoesWithState())
    val whatGoes: StateFlow<WhatGoesWithState> = _whatGoes.asStateFlow()

    private val _check = MutableStateFlow(OutfitCheckUiState())
    val check: StateFlow<OutfitCheckUiState> = _check.asStateFlow()

    private val _shop = MutableStateFlow(ShoppingUiState())
    val shop: StateFlow<ShoppingUiState> = _shop.asStateFlow()

    private val _closetSearch = MutableStateFlow("")
    val closetSearch: StateFlow<String> = _closetSearch.asStateFlow()

    private val _closetCategoryFilter = MutableStateFlow("All")
    val closetCategoryFilter: StateFlow<String> = _closetCategoryFilter.asStateFlow()


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

    fun generateOutfit(preferLlm: Boolean = true, glowUp: Boolean = false) {
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
            val prefs = buildStylePrefs()
            val unusedIds = inventory.map { it.id }.filter { it !in prefs.usedItemIds }.toSet()

            var result: GeneratedOutfit? = null
            if (preferLlm && apiKey.isNotBlank()) {
                result = runCatching {
                    app.openRouterClient.suggestOutfitFromInventory(
                        apiKey = apiKey,
                        baseUrl = settings.getOpenRouterBaseUrl(),
                        model = settings.getChatModel(),
                        items = inventory,
                        occasion = occasion,
                        mood = if (glowUp) listOfNotNull(mood, "glow up", "fresh unused pieces").joinToString(", ") else mood,
                        weather = weather,
                        dislikedKeys = disliked,
                        preferUnusedIds = if (glowUp) unusedIds else emptySet(),
                        likedColorHints = prefs.likedColors.keys.toList()
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
                    dislikedKeys = disliked,
                    stylePrefs = prefs,
                    glowUp = glowUp
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
                    message = when {
                        glowUp && result.source == OutfitSource.LLM -> "Glow-up with AI"
                        glowUp -> "Glow-up · fresher & liked-style picks"
                        result.source == OutfitSource.LLM -> "AI-enhanced suggestion"
                        else -> "Offline rules suggestion"
                    },
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
            clearMix()
            clearCheck()
            _shop.value = ShoppingUiState()
            _whatGoes.value = WhatGoesWithState()
            _statusMessage.value = "All closet data cleared"
        }
    }

    fun clearStatus() {
        _statusMessage.value = null
        _today.update { it.copy(message = null) }
    }


    fun glowUp() {
        generateOutfit(preferLlm = true, glowUp = true)
    }

    fun setClosetSearch(q: String) {
        _closetSearch.value = q
    }

    fun setClosetCategoryFilter(label: String) {
        _closetCategoryFilter.value = label
    }

    fun filteredItems(): List<ClosetItemEntity> {
        val q = _closetSearch.value.trim()
        val cat = _closetCategoryFilter.value
        return items.value.filter { item ->
            val catOk = cat == "All" || item.category.equals(cat, ignoreCase = true)
            val qOk = q.isBlank() ||
                item.name.contains(q, true) ||
                item.color.contains(q, true) ||
                item.category.contains(q, true) ||
                item.notes.contains(q, true)
            catOk && qOk
        }
    }

    private suspend fun buildStylePrefs(): StylePrefs {
        val outfits = closet.getOutfits()
        val allItems = closet.getItems().associateBy { it.id }
        val colorCounts = mutableMapOf<String, Int>()
        val catCounts = mutableMapOf<String, Int>()
        val used = mutableSetOf<Long>()
        outfits.forEach { outfit ->
            val ids = outfit.itemIds()
            used.addAll(ids)
            if (outfit.liked == true) {
                ids.mapNotNull { allItems[it] }.forEach { item ->
                    val key = ColorHarmony.normalize(item.color)
                    colorCounts[key] = (colorCounts[key] ?: 0) + 1
                    catCounts[item.category] = (catCounts[item.category] ?: 0) + 1
                }
            }
        }
        return StylePrefs(colorCounts, catCounts, used)
    }

    // —— Magic upload ——

    fun startMagicFromUri(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val path = closet.persistPhotoFromUri(uri)
                runMagicUpload(path)
            }.onFailure { e ->
                _magic.value = MagicUploadState(active = true, message = e.message ?: "Import failed")
            }
        }
    }

    fun startMagicFromPath(path: String) {
        viewModelScope.launch {
            runCatching {
                val persisted = closet.copyFromAbsolutePath(path)
                runMagicUpload(persisted)
            }.onFailure { e ->
                _magic.value = MagicUploadState(active = true, message = e.message ?: "Import failed")
            }
        }
    }

    private suspend fun runMagicUpload(path: String) {
        val apiKey = settings.getApiKey()
        if (apiKey.isBlank()) {
            _magic.value = MagicUploadState(
                active = true,
                message = "Magic upload needs an OpenRouter API key (Settings). Falling back to single-item local suggest."
            )
            _draft.value = AddEditDraft(analyzing = true)
            analyzeAndFillDraft(path, id = null)
            return
        }
        _magic.value = MagicUploadState(active = true, analyzing = true, message = "Extracting items…")
        val multi = runCatching {
            app.openRouterClient.analyzeMultiItemPhoto(
                apiKey = apiKey,
                baseUrl = settings.getOpenRouterBaseUrl(),
                model = settings.getVisionModel(),
                photoPath = path
            )
        }.getOrNull()

        if (multi == null || multi.items.isEmpty()) {
            _magic.value = MagicUploadState(
                active = true,
                analyzing = false,
                message = "Could not split items — opening single-item editor."
            )
            _draft.value = AddEditDraft(analyzing = true)
            analyzeAndFillDraft(path, id = null)
            return
        }

        var saved = 0
        multi.items.forEachIndexed { index, suggestion ->
            val photo = if (suggestion.bbox != null) {
                runCatching { closet.cropPhoto(path, suggestion.bbox!!) }.getOrDefault(path)
            } else {
                path
            }
            val name = if (multi.items.size > 1 && suggestion.bbox == null) {
                "${suggestion.name} (${index + 1})"
            } else {
                suggestion.name
            }
            closet.saveItem(
                ClosetItemEntity(
                    name = name,
                    category = ClothingCategory.fromLabel(suggestion.category).label,
                    color = suggestion.color.ifBlank { "Unknown" },
                    season = Season.fromLabel(suggestion.season).label,
                    photoPath = photo,
                    notes = suggestion.notes.ifBlank { "Magic upload" }
                )
            )
            saved++
        }
        _magic.value = MagicUploadState(
            active = true,
            analyzing = false,
            savedCount = saved,
            message = "Saved $saved items from one photo."
        )
        _statusMessage.value = "Magic upload: $saved items added"
    }

    fun dismissMagic() {
        _magic.value = MagicUploadState()
    }

    // —— Mix & Match ——

    fun setMixOccasion(occasion: Occasion) {
        _mix.update { it.copy(occasion = occasion) }
    }

    fun setMixSlot(slot: MixSlot, itemId: Long?) {
        _mix.update { state ->
            val next = state.slots.toMutableMap()
            next[slot] = itemId
            if (itemId != null) {
                when (slot) {
                    MixSlot.BOTTOM -> next[MixSlot.ONE_PIECE] = null
                    MixSlot.ONE_PIECE -> next[MixSlot.BOTTOM] = null
                    else -> Unit
                }
            }
            state.copy(slots = next, message = null)
        }
    }

    fun clearMix() {
        _mix.value = MixMatchState(occasion = _mix.value.occasion)
    }

    fun mixSelectedIds(): List<Long> =
        _mix.value.slots.values.filterNotNull().distinct()

    fun saveMixOutfit() {
        viewModelScope.launch {
            val ids = mixSelectedIds()
            if (ids.isEmpty()) {
                _mix.update { it.copy(message = "Pick at least one item.") }
                return@launch
            }
            val inventory = closet.getItems()
            val pieces = ids.mapNotNull { id -> inventory.find { it.id == id } }
            if (violatesOnePieceRule(pieces)) {
                _mix.update { it.copy(message = "Can't combine bottoms with dress/romper.") }
                return@launch
            }
            closet.saveOutfit(
                OutfitEntity(
                    title = "Mix & Match · ${_mix.value.occasion.label}",
                    itemIdsCsv = OutfitEntity.idsToCsv(ids),
                    occasion = _mix.value.occasion.label,
                    rationale = "Hand-picked · " + pieces.joinToString(", ") { it.name },
                    source = "MIX"
                )
            )
            _mix.update { it.copy(message = "Outfit saved.") }
            _statusMessage.value = "Mix outfit saved"
        }
    }

    fun askAiCompleteMix() {
        viewModelScope.launch {
            _mix.update { it.copy(working = true, message = null) }
            val inventory = closet.getItems()
            val selected = mixSelectedIds()
            val apiKey = settings.getApiKey()
            val weather = _today.value.weather
            val occasion = _mix.value.occasion
            val prefs = buildStylePrefs()

            var result: GeneratedOutfit? = null
            if (apiKey.isNotBlank()) {
                result = runCatching {
                    app.openRouterClient.completeMixMatch(
                        apiKey = apiKey,
                        baseUrl = settings.getOpenRouterBaseUrl(),
                        model = settings.getChatModel(),
                        items = inventory,
                        selectedIds = selected,
                        occasion = occasion,
                        weather = weather
                    )
                }.getOrNull()?.let { llm ->
                    val map = inventory.associateBy { it.id }
                    val pieces = llm.itemIds.mapNotNull { map[it] }
                    if (pieces.isEmpty() || violatesOnePieceRule(pieces)) null
                    else llm.copy(itemIds = pieces.map { it.id })
                }
            }
            if (result == null) {
                result = app.rulesEngine.completeMix(
                    items = inventory,
                    selectedIds = selected,
                    occasion = occasion,
                    weather = weather,
                    stylePrefs = prefs
                )
            }
            if (result == null) {
                _mix.update { it.copy(working = false, message = "Could not complete — add more items.") }
                return@launch
            }
            val byId = inventory.associateBy { it.id }
            val slotMap = _mix.value.slots.toMutableMap()
            result.itemIds.mapNotNull { byId[it] }.forEach { item ->
                when (ClothingCategory.fromLabel(item.category)) {
                    ClothingCategory.TOP -> {
                        if (slotMap[MixSlot.BASE_TOP] == null) slotMap[MixSlot.BASE_TOP] = item.id
                        else if (slotMap[MixSlot.LAYER_TOP] == null && slotMap[MixSlot.BASE_TOP] != item.id) {
                            slotMap[MixSlot.LAYER_TOP] = item.id
                        }
                    }
                    ClothingCategory.BOTTOM -> {
                        slotMap[MixSlot.ONE_PIECE] = null
                        slotMap[MixSlot.BOTTOM] = item.id
                    }
                    ClothingCategory.DRESS, ClothingCategory.ROMPER -> {
                        slotMap[MixSlot.BOTTOM] = null
                        slotMap[MixSlot.ONE_PIECE] = item.id
                    }
                    ClothingCategory.OUTERWEAR -> slotMap[MixSlot.OUTERWEAR] = item.id
                    ClothingCategory.SHOES -> slotMap[MixSlot.SHOES] = item.id
                    ClothingCategory.ACCESSORY, ClothingCategory.OTHER ->
                        slotMap[MixSlot.ACCESSORY] = item.id
                }
            }
            _mix.update {
                it.copy(
                    slots = slotMap,
                    working = false,
                    message = if (result.source == OutfitSource.LLM) "AI filled empty slots"
                    else "Offline complete filled empty slots"
                )
            }
        }
    }

    // —— What goes with this ——

    fun requestWhatGoesWith(item: ClosetItemEntity) {
        viewModelScope.launch {
            _whatGoes.value = WhatGoesWithState(anchorId = item.id, loading = true)
            val inventory = closet.getItems()
            val disliked = closet.getDislikedKeys()
            val prefs = buildStylePrefs()
            val occasion = _today.value.occasion
            val weather = _today.value.weather
            val apiKey = settings.getApiKey()

            var suggestions: List<GeneratedOutfit> = emptyList()
            if (apiKey.isNotBlank()) {
                suggestions = runCatching {
                    app.openRouterClient.whatGoesWithItem(
                        apiKey = apiKey,
                        baseUrl = settings.getOpenRouterBaseUrl(),
                        model = settings.getChatModel(),
                        items = inventory,
                        anchorId = item.id,
                        occasion = occasion,
                        weather = weather
                    )
                }.getOrDefault(emptyList()).mapNotNull { llm ->
                    val map = inventory.associateBy { it.id }
                    val pieces = llm.itemIds.mapNotNull { map[it] }
                    if (pieces.isEmpty() || item.id !in llm.itemIds || violatesOnePieceRule(pieces)) null
                    else llm.copy(itemIds = pieces.map { it.id })
                }
            }
            if (suggestions.size < 3) {
                val more = app.rulesEngine.whatGoesWith(
                    items = inventory,
                    anchor = item,
                    occasion = occasion,
                    weather = weather,
                    dislikedKeys = disliked,
                    stylePrefs = prefs,
                    count = 3
                )
                val seen = suggestions.map { it.itemIds.sorted() }.toMutableSet()
                for (r in more) {
                    if (suggestions.size >= 3) break
                    val key = r.itemIds.sorted()
                    if (key in seen) continue
                    seen += key
                    suggestions = suggestions + r
                }
            }
            suggestions = suggestions.take(3)
            val itemMap = suggestions.mapIndexed { idx, g ->
                idx to g.itemIds.mapNotNull { id -> inventory.find { it.id == id } }
            }.toMap()
            _whatGoes.value = WhatGoesWithState(
                anchorId = item.id,
                loading = false,
                suggestions = suggestions,
                suggestionItems = itemMap,
                message = if (suggestions.isEmpty()) "Need more closet variety to pair with this." else null
            )
        }
    }

    fun dismissWhatGoes() {
        _whatGoes.value = WhatGoesWithState()
    }

    fun likeWhatGoesSuggestion(index: Int) {
        viewModelScope.launch {
            val g = _whatGoes.value.suggestions.getOrNull(index) ?: return@launch
            val id = closet.saveOutfit(
                OutfitEntity(
                    title = g.title,
                    itemIdsCsv = OutfitEntity.idsToCsv(g.itemIds),
                    occasion = g.occasion.label,
                    rationale = g.rationale,
                    source = g.source.name,
                    liked = true
                )
            )
            closet.setLiked(id, true)
            _whatGoes.update { it.copy(message = "Saved & liked suggestion ${index + 1}") }
        }
    }

    fun dislikeWhatGoesSuggestion(index: Int) {
        viewModelScope.launch {
            val g = _whatGoes.value.suggestions.getOrNull(index) ?: return@launch
            closet.dislikeCombo(g.itemIds)
            closet.saveOutfit(
                OutfitEntity(
                    title = g.title,
                    itemIdsCsv = OutfitEntity.idsToCsv(g.itemIds),
                    occasion = g.occasion.label,
                    rationale = g.rationale,
                    source = g.source.name,
                    liked = false
                )
            )
            _whatGoes.update { it.copy(message = "Noted — won't repeat that pairing.") }
        }
    }

    fun saveWhatGoesSuggestion(index: Int) {
        viewModelScope.launch {
            val g = _whatGoes.value.suggestions.getOrNull(index) ?: return@launch
            closet.saveOutfit(
                OutfitEntity(
                    title = g.title,
                    itemIdsCsv = OutfitEntity.idsToCsv(g.itemIds),
                    occasion = g.occasion.label,
                    rationale = g.rationale,
                    source = g.source.name
                )
            )
            _whatGoes.update { it.copy(message = "Outfit saved") }
        }
    }

    // —— Outfit Check ——

    fun toggleCheckItem(id: Long) {
        _check.update { state ->
            val next = state.selectedIds.toMutableSet()
            if (id in next) next.remove(id) else next.add(id)
            state.copy(selectedIds = next, result = null, fromOutfitId = null)
        }
    }

    fun loadOutfitIntoCheck(outfit: OutfitEntity) {
        _check.update {
            it.copy(
                selectedIds = outfit.itemIds().toSet(),
                fromOutfitId = outfit.id,
                occasion = Occasion.fromLabel(outfit.occasion),
                result = null
            )
        }
    }

    fun setCheckOccasion(occasion: Occasion) {
        _check.update { it.copy(occasion = occasion) }
    }

    fun clearCheck() {
        _check.value = OutfitCheckUiState(occasion = _today.value.occasion)
    }

    fun runOutfitCheck() {
        viewModelScope.launch {
            val ids = _check.value.selectedIds
            if (ids.isEmpty()) {
                _check.update { it.copy(message = "Select items or an outfit first.") }
                return@launch
            }
            _check.update { it.copy(loading = true, message = null, result = null) }
            val inventory = closet.getItems()
            val pieces = ids.mapNotNull { id -> inventory.find { it.id == id } }
            val occasion = _check.value.occasion
            val apiKey = settings.getApiKey()
            var result: OutfitCheckResult? = null
            if (apiKey.isNotBlank()) {
                result = runCatching {
                    app.openRouterClient.checkOutfitAi(
                        apiKey = apiKey,
                        baseUrl = settings.getOpenRouterBaseUrl(),
                        model = settings.getChatModel(),
                        pieces = pieces,
                        occasion = occasion
                    )
                }.getOrNull()
            }
            if (result == null) {
                result = OutfitCheckEngine.score(pieces, occasion, _today.value.weather)
            }
            _check.update {
                it.copy(
                    loading = false,
                    result = result,
                    message = if (result.source == OutfitSource.LLM) "AI style check"
                    else "Offline heuristic check"
                )
            }
        }
    }

    // —— Shopping Buddy ——

    fun runShoppingBuddy() {
        viewModelScope.launch {
            _shop.update { it.copy(loading = true, message = null) }
            val inventory = closet.getItems()
            val apiKey = settings.getApiKey()
            var suggestions: List<ShoppingSuggestion>? = null
            var source = OutfitSource.RULES
            if (apiKey.isNotBlank()) {
                suggestions = runCatching {
                    app.openRouterClient.shoppingBuddyAi(
                        apiKey = apiKey,
                        baseUrl = settings.getOpenRouterBaseUrl(),
                        model = settings.getChatModel(),
                        items = inventory
                    )
                }.getOrNull()
                if (suggestions != null) source = OutfitSource.LLM
            }
            if (suggestions.isNullOrEmpty()) {
                suggestions = ShoppingBuddyEngine.suggest(inventory)
                source = OutfitSource.RULES
            }
            _shop.update {
                it.copy(
                    loading = false,
                    suggestions = suggestions.take(7),
                    source = source,
                    message = if (source == OutfitSource.LLM) "AI gap analysis"
                    else "Offline gap analysis · add API key for richer tips"
                )
            }
        }
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
