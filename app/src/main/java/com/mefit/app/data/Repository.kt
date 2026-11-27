package com.mefit.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

/** Loads the bundled exercise library (assets/exercises.json, from free-exercise-db). */
class ExerciseRepository(private val context: Context) {
    suspend fun load(): List<Exercise> = withContext(Dispatchers.IO) {
        val text = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
        json.decodeFromString<List<Exercise>>(text).sortedBy { it.name }
    }
}

/** Persists the training calendar and user settings in SharedPreferences as JSON. */
class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("mefit", Context.MODE_PRIVATE)

    fun loadPlans(): Map<String, DayPlan> =
        prefs.getString(KEY_PLANS, null)
            ?.let { runCatching { json.decodeFromString<Map<String, DayPlan>>(it) }.getOrNull() }
            ?: emptyMap()

    fun savePlans(plans: Map<String, DayPlan>) {
        prefs.edit().putString(KEY_PLANS, json.encodeToString(plans)).apply()
    }

    fun loadSettings(): Settings {
        val saved = prefs.getString(KEY_SETTINGS, null)
            ?.let { runCatching { json.decodeFromString<Settings>(it) }.getOrNull() }
            ?: return Settings(catalogVersion = EquipmentCatalog.CATALOG_VERSION)
        // Newer catalog versions added equipment every gym has; give it to existing gym setups.
        if (saved.catalogVersion >= EquipmentCatalog.CATALOG_VERSION) return saved
        val added = buildSet {
            if (saved.catalogVersion < 2) add(EquipmentCatalog.PULL_UP_BAR)
            if (saved.catalogVersion < 3) addAll(
                listOf(
                    EquipmentCatalog.BENCH, EquipmentCatalog.ADJUSTABLE_BENCH, EquipmentCatalog.SQUAT_RACK,
                    EquipmentCatalog.DIP_BARS, EquipmentCatalog.JUMP_ROPE,
                ),
            )
        }
        val migrated = saved.copy(gymEquipment = saved.gymEquipment + added, catalogVersion = EquipmentCatalog.CATALOG_VERSION)
        saveSettings(migrated)
        return migrated
    }

    fun saveSettings(settings: Settings) {
        prefs.edit().putString(KEY_SETTINGS, json.encodeToString(settings)).apply()
    }

    companion object {
        private const val KEY_PLANS = "plans"
        private const val KEY_SETTINGS = "settings"
    }
}
