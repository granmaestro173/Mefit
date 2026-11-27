package com.mefit.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mefit.app.data.DayPlan
import com.mefit.app.data.Exercise
import com.mefit.app.data.ExerciseRepository
import com.mefit.app.data.LocalStore
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
    private val store = LocalStore(app)

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _plans = MutableStateFlow(store.loadPlans())
    val plans: StateFlow<Map<String, DayPlan>> = _plans.asStateFlow()

    private val _settings = MutableStateFlow(store.loadSettings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    init {
        viewModelScope.launch { _exercises.value = repo.load() }
    }

    fun exercise(id: String): Exercise? = _exercises.value.firstOrNull { it.id == id }

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
        setPlan(date, p.copy(shuffle = p.shuffle + 1))
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
                next[d.toString()] = DayPlan(type.name, loc.name)
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
        val loc = runCatching { Location.valueOf(plan.location) }.getOrDefault(Location.GYM)
        val seed = date.toEpochDay() * 31 + type.ordinal * 7 + plan.shuffle * 101
        return WorkoutGenerator.generate(_exercises.value, type, loc, _settings.value, seed)
    }
}
