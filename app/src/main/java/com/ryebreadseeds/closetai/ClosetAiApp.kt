package com.ryebreadseeds.closetai

import android.app.Application
import com.ryebreadseeds.closetai.ai.OpenRouterClient
import com.ryebreadseeds.closetai.data.repo.ClosetRepository
import com.ryebreadseeds.closetai.data.repo.SettingsRepository
import com.ryebreadseeds.closetai.domain.OutfitRulesEngine
import com.ryebreadseeds.closetai.weather.OpenMeteoClient

class ClosetAiApp : Application() {
    lateinit var closetRepository: ClosetRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    val openRouterClient = OpenRouterClient()
    val weatherClient = OpenMeteoClient()
    val rulesEngine = OutfitRulesEngine()

    override fun onCreate() {
        super.onCreate()
        closetRepository = ClosetRepository(this)
        settingsRepository = SettingsRepository(this)
    }
}
