package com.mefit.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mefit.app.data.DayPlan
import com.mefit.app.data.Exercise
import com.mefit.app.data.ExerciseRepository
import com.mefit.app.data.LocalStore
import com.mefit.app.data.Recipe
import com.mefit.app.data.RecipeRepository
import com.mefit.app.data.Location
import com.mefit.app.data.Settings
import com.mefit.app.domain.TrainingType
import com.mefit.app.domain.WEEK_TEMPLATES
import com.mefit.app.domain.Workout
import com.mefit.app.domain.WorkoutGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ExerciseRepository(app)
    private val recipeRepo = RecipeRepository(app)
    private val store = LocalStore(app)

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()
    private var byId: Map<String, Exercise> = emptyMap()

    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    private val _plans = MutableStateFlow(store.loadPlans())
    val plans: StateFlow<Map<String, DayPlan>> = _plans.asStateFlow()

    private val _settings = MutableStateFlow(store.loadSettings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            val list = repo.load()
            byId = list.associateBy { it.id }
            _exercises.value = list
        }
        viewModelScope.launch { _recipes.value = recipeRepo.load() }
    }

    fun exercise(id: String): Exercise? = byId[id]

    fun recipe(id: String): Recipe? = _recipes.value.firstOrNull { it.id == id }

    fun toggleFavorite(id: String) = updateSettings {
        it.copy(favoriteRecipes = if (id in it.favoriteRecipes) it.favoriteRecipes - id else it.favoriteRecipes + id)
    }

    fun planFor(date: LocalDate): DayPlan? = _plans.value[date.toString()]

    fun setPlan(date: LocalDate, plan: DayPlan?) {
        _plans.update { current ->
            val next = current.toMutableMap()
            if (plan == null) next.remove(date.toString()) else next[date.toString()] = plan
            next
        }
        store.savePlans(_plans.value)
    }

    fun toggleDone(date: LocalDate) {
        val p = planFor(date) ?: return
        setPlan(date, p.copy(done = !p.done))
    }

    fun shuffle(date: LocalDate) {
        val p = planFor(date) ?: return
        setPlan(date, p.copy(shuffle = p.shuffle + 1, exercises = null))
    }

    /** Fills [weeks] weeks starting from the Monday of [from]'s week; keeps days already marked done. */
    fun applyTemplate(templateIndex: Int, from: LocalDate, weeks: Int) {
        val template = WEEK_TEMPLATES.getOrNull(templateIndex) ?: return
        val monday = from.minusDays((from.dayOfWeek.value - 1).toLong())
        _plans.update { current ->
            val next = current.toMutableMap()
            for (i in 0 until weeks * 7) {
                val d = monday.plusDays(i.toLong())
                val existing = next[d.toString()]
                if (existing?.done == true) continue
                val (type, loc) = template.days[i % 7]
                val where = if (_settings.value.homeOnly) Location.HOME else loc
                next[d.toString()] = DayPlan(type.name, where.name)
            }
            next
        }
        store.savePlans(_plans.value)
    }

    fun clearMonth(anyDayInMonth: LocalDate) {
        _plans.update { current ->
            current.filterKeys {
                val d = LocalDate.parse(it)
                !(d.year == anyDayInMonth.year && d.month == anyDayInMonth.month)
            }
        }
        store.savePlans(_plans.value)
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        _settings.update(transform)
        store.saveSettings(_settings.value)
    }

    fun workoutFor(date: LocalDate, plan: DayPlan): Workout {
        val type = TrainingType.of(plan.type)
        val loc = locationOf(plan)
        plan.exercises?.let { ids ->
            return WorkoutGenerator.fromIds(byId, ids, type, loc, _settings.value)
        }
        val seed = date.toEpochDay() * 31 + type.ordinal * 7 + plan.shuffle * 101
        return WorkoutGenerator.generate(_exercises.value, type, loc, _settings.value, seed)
    }

    /** Where this day actually happens: always home when the user has said they don't use a gym. */
    fun locationOf(plan: DayPlan): Location =
        if (_settings.value.homeOnly) Location.HOME
        else runCatching { Location.valueOf(plan.location) }.getOrDefault(Location.GYM)

    /** Edits a day's exercise list; the first edit freezes the auto-generated list so it can be changed. */
    private fun editExercises(date: LocalDate, transform: (List<String>) -> List<String>) {
        val plan = planFor(date) ?: return
        val current = plan.exercises ?: workoutFor(date, plan).items.map { it.exercise.id }
        setPlan(date, plan.copy(exercises = transform(current).distinct()))
    }

    fun removeExercise(date: LocalDate, id: String) = editExercises(date) { it - id }

    fun addExercise(date: LocalDate, id: String) = editExercises(date) { it + id }

    fun swapExercise(date: LocalDate, oldId: String, newId: String) =
        editExercises(date) { list -> list.map { if (it == oldId) newId else it } }

    fun moveExercise(date: LocalDate, id: String, delta: Int) = editExercises(date) { list ->
        val i = list.indexOf(id)
        val j = i + delta
        if (i < 0 || j !in list.indices) list
        else list.toMutableList().apply { add(j, removeAt(i)) }
    }

    fun resetExercises(date: LocalDate) {
        val plan = planFor(date) ?: return
        setPlan(date, plan.copy(exercises = null))
    }

    /** Alternatives for [id]: same main muscle and kind of exercise, doable with this day's equipment. */
    fun alternatives(date: LocalDate, id: String): List<Exercise> {
        val plan = planFor(date) ?: return emptyList()
        val ex = byId[id] ?: return emptyList()
        val loc = locationOf(plan)
        val inUse = workoutFor(date, plan).items.map { it.exercise.id }.toSet()
        val kind = kindOf(ex.category)
        return _exercises.value.filter { c ->
            c.id !in inUse && kindOf(c.category) == kind &&
                c.primaryMuscles.any { it in ex.primaryMuscles } &&
                WorkoutGenerator.fits(c, loc, _settings.value)
        }.sortedWith(
            compareByDescending<Exercise> { it.primaryMuscles.firstOrNull() == ex.primaryMuscles.firstOrNull() }
                .thenBy { it.name },
        )
    }

    /** Everything doable on this day's equipment, exercises for the session's muscles first. */
    fun addable(date: LocalDate): List<Exercise> {
        val plan = planFor(date) ?: return emptyList()
        val loc = locationOf(plan)
        val type = TrainingType.of(plan.type)
        val targets = WorkoutGenerator.targetMuscles(type)
        val cats = type.slots.flatMap { it.categories }.toSet()
        val inUse = workoutFor(date, plan).items.map { it.exercise.id }.toSet()
        return _exercises.value.filter { it.id !in inUse && WorkoutGenerator.fits(it, loc, _settings.value) }
            .sortedWith(
                compareByDescending<Exercise> {
                    (targets.isEmpty() || it.primaryMuscles.any { m -> m in targets }) && it.category in cats
                }.thenBy { it.name },
            )
    }

    /** How many exercises the user's equipment unlocks at [location]. */
    fun countAvailable(location: Location, settings: Settings): Int =
        _exercises.value.count { WorkoutGenerator.fits(it, location, settings) }

    private fun kindOf(category: String?): String = when (category) {
        "stretching" -> "stretch"
        "cardio", "plyometrics" -> "cardio"
        else -> "strength"
    }
}
