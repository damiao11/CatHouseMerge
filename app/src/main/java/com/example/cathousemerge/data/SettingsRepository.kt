package com.example.cathousemerge.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.cathousemerge.game.GameSettings
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val sound = floatPreferencesKey("sound_volume")
    private val haptics = booleanPreferencesKey("haptics")
    private val motion = booleanPreferencesKey("reduced_motion")

    val settings = context.settingsStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { values ->
        GameSettings(
            soundVolume = (values[sound] ?: 0.7f).coerceIn(0f, 1f),
            haptics = values[haptics] ?: true,
            reducedMotion = values[motion] ?: false,
        )
    }

    suspend fun save(settings: GameSettings) {
        context.settingsStore.edit { values ->
            values[sound] = settings.soundVolume.coerceIn(0f, 1f)
            values[haptics] = settings.haptics
            values[motion] = settings.reducedMotion
        }
    }
}
