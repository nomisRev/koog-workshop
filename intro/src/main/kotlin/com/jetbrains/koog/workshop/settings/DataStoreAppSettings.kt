package com.jetbrains.koog.workshop.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class DataStoreAppSettings(prefPathProvider: PrefPathProvider) : AppSettings {

    private val _appearanceModeFlow = MutableStateFlow(AppearanceMode.Auto)
    override val appearanceModeFlow: StateFlow<AppearanceMode> = _appearanceModeFlow.asStateFlow()

    override suspend fun updateAppearanceMode(mode: AppearanceMode) {
        _appearanceModeFlow.value = mode
        dataStore.edit { preferences ->
            preferences[APPEARANCE_MODE_KEY] = mode.label
        }
    }

    companion object {
        val APPEARANCE_MODE_KEY = stringPreferencesKey("appearance_mode")
    }

    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { prefPathProvider.get() }
        )
    }

    override suspend fun getCurrentSettings(): AppSettingsData {
        val data = dataStore.data.map { preferences ->
            AppSettingsData(
                appearanceMode = when (preferences[APPEARANCE_MODE_KEY]) {
                    AppearanceMode.Light.label -> AppearanceMode.Light
                    AppearanceMode.Dark.label -> AppearanceMode.Dark
                    else -> AppearanceMode.Auto
                }
            )
        }.first()
        _appearanceModeFlow.value = data.appearanceMode
        return data
    }

    override suspend fun setCurrentSettings(settings: AppSettingsData) {
        _appearanceModeFlow.value = settings.appearanceMode
        dataStore.edit { preferences ->
            preferences[APPEARANCE_MODE_KEY] = settings.appearanceMode.label
        }
    }
}
