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

    fun loadSettings(): Settings =
        prefs.getString(KEY_SETTINGS, null)
            ?.let { runCatching { json.decodeFromString<Settings>(it) }.getOrNull() }
            ?: Settings()

    fun saveSettings(settings: Settings) {
        prefs.edit().putString(KEY_SETTINGS, json.encodeToString(settings)).apply()
    }

    companion object {
        private const val KEY_PLANS = "plans"
        private const val KEY_SETTINGS = "settings"
    }
}
