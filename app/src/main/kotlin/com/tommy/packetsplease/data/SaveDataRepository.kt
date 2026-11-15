package com.tommy.packetsplease.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tommy.packetsplease.domain.AppSettings
import com.tommy.packetsplease.domain.CollectionProgress
import com.tommy.packetsplease.domain.M1TuningConfig
import com.tommy.packetsplease.domain.SavedProgress
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

private val Context.packetsDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "packets_please_save",
)

class SaveDataRepository(private val context: Context) {

  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  private val progressKey = stringPreferencesKey("saved_progress_json")
  private val tuningKey = stringPreferencesKey("m1_tuning_json")
  private val settingsKey = stringPreferencesKey("app_settings_json")
  private val collectionKey = stringPreferencesKey("collection_progress_json")

  suspend fun loadProgress(): SavedProgress? {
    val prefs = context.packetsDataStore.data.first()
    val raw = prefs[progressKey] ?: return null
    return runCatching { json.decodeFromString<SavedProgress>(raw) }.getOrNull()
  }

  suspend fun saveProgress(progress: SavedProgress) {
    val encoded = json.encodeToString(SavedProgress.serializer(), progress)
    context.packetsDataStore.edit { prefs ->
      prefs[progressKey] = encoded
    }
  }

  suspend fun clearProgress() {
    context.packetsDataStore.edit { prefs ->
      prefs.remove(progressKey)
    }
  }

  suspend fun loadTuningConfig(): M1TuningConfig {
    val prefs = context.packetsDataStore.data.first()
    val raw = prefs[tuningKey] ?: return M1TuningConfig()
    return runCatching { json.decodeFromString<M1TuningConfig>(raw) }.getOrDefault(M1TuningConfig())
  }

  suspend fun saveTuningConfig(config: M1TuningConfig) {
    val encoded = json.encodeToString(M1TuningConfig.serializer(), config)
    context.packetsDataStore.edit { prefs ->
      prefs[tuningKey] = encoded
    }
  }

  suspend fun loadSettings(): AppSettings {
    val prefs = context.packetsDataStore.data.first()
    val raw = prefs[settingsKey] ?: return AppSettings()
    return runCatching { json.decodeFromString<AppSettings>(raw) }.getOrDefault(AppSettings())
  }

  suspend fun saveSettings(settings: AppSettings) {
    val encoded = json.encodeToString(AppSettings.serializer(), settings)
    context.packetsDataStore.edit { prefs ->
      prefs[settingsKey] = encoded
    }
  }

  suspend fun loadCollection(): CollectionProgress {
    val prefs = context.packetsDataStore.data.first()
    val raw = prefs[collectionKey] ?: return CollectionProgress()
    return runCatching { json.decodeFromString<CollectionProgress>(raw) }.getOrDefault(CollectionProgress())
  }

  suspend fun saveCollection(collection: CollectionProgress) {
    val encoded = json.encodeToString(CollectionProgress.serializer(), collection)
    context.packetsDataStore.edit { prefs ->
      prefs[collectionKey] = encoded
    }
  }
}
