package com.mefit.app.domain

import com.mefit.app.data.Exercise
import com.mefit.app.data.Goal
import com.mefit.app.data.Level
import com.mefit.app.data.Location
import com.mefit.app.data.Settings
import kotlin.random.Random

private val STRENGTH_CATS = setOf("strength", "powerlifting")

/** A slot in a workout: "pick [count] exercises training [muscle] from [categories]". */
data class Slot(val muscles: Set<String>?, val count: Int, val categories: Set<String> = STRENGTH_CATS) {
    fun matchesMuscle(ex: Exercise): Boolean {
        val m = muscles ?: return true
        return ex.primaryMuscles.any { it in m }
    }
}

enum class TrainingType(
    val label: String,
    val short: String,
    val color: Long,
    val description: String,
    val slots: List<Slot>,
) {
    PUSH(
        "Push", "PSH", 0xFFFF7A59,
        "Chest, shoulders and triceps",
        listOf(Slot(setOf("chest"), 2), Slot(setOf("shoulders"), 2), Slot(setOf("triceps"), 2)),
    ),
    PULL(
        "Pull", "PUL", 0xFF4FC3F7,
        "Back, rear shoulders and biceps",
        listOf(
            Slot(setOf("lats"), 2), Slot(setOf("middle back"), 1), Slot(setOf("traps"), 1),
            Slot(setOf("biceps"), 2),
        ),
    ),
    LEGS(
        "Legs", "LEG", 0xFFB388FF,
        "Quads, hamstrings, glutes and calves",
        listOf(
            Slot(setOf("quadriceps"), 2), Slot(setOf("hamstrings"), 1), Slot(setOf("glutes"), 1),
            Slot(setOf("calves"), 1), Slot(setOf("adductors", "abductors"), 1),
        ),
    ),
    UPPER(
        "Upper body", "UPP", 0xFFFFD54F,
        "Everything above the waist in one session",
        listOf(
            Slot(setOf("chest"), 1), Slot(setOf("lats"), 1), Slot(setOf("shoulders"), 1),
            Slot(setOf("middle back"), 1), Slot(setOf("biceps"), 1), Slot(setOf("triceps"), 1),
        ),
    ),
    LOWER(
        "Lower body", "LOW", 0xFF81C784,
        "Legs plus core",
        listOf(
            Slot(setOf("quadriceps"), 2), Slot(setOf("hamstrings"), 1), Slot(setOf("glutes"), 1),
            Slot(setOf("calves"), 1), Slot(setOf("abdominals"), 1),
        ),
    ),
    FULL_BODY(
        "Full body", "FUL", 0xFFC6FF3D,
        "One move per major muscle group",
        listOf(
            Slot(setOf("quadriceps"), 1), Slot(setOf("chest"), 1), Slot(setOf("lats", "middle back"), 1),
            Slot(setOf("shoulders"), 1), Slot(setOf("hamstrings", "glutes"), 1), Slot(setOf("abdominals"), 1),
        ),
    ),
    CORE(
        "Core", "COR", 0xFFFF8A80,
        "Abs, obliques and lower back",
        listOf(Slot(setOf("abdominals"), 4), Slot(setOf("lower back"), 1)),
    ),
    CARDIO(
        "Cardio & HIIT", "HIT", 0xFFFF5252,
        "Conditioning and explosive work",
        listOf(Slot(null, 2, setOf("cardio")), Slot(null, 4, setOf("plyometrics"))),
    ),
    MOBILITY(
        "Mobility", "MOB", 0xFF64FFDA,
        "Stretching and recovery",
        listOf(
            Slot(setOf("hamstrings", "calves", "quadriceps"), 2, setOf("stretching")),
            Slot(setOf("lower back", "glutes", "abductors", "adductors"), 2, setOf("stretching")),
            Slot(setOf("chest", "shoulders", "lats", "neck", "triceps", "biceps"), 2, setOf("stretching")),
        ),
    ),
    REST(
        "Rest day", "RST", 0xFF607D8B,
        "Recover. A walk and good sleep count.",
        emptyList(),
    );

    companion object {
        fun of(name: String?): TrainingType = entries.firstOrNull { it.name == name } ?: REST
    }
}

data class Prescription(val sets: String, val reps: String, val rest: String)

data class WorkoutItem(val exercise: Exercise, val prescription: Prescription)

data class Workout(val type: TrainingType, val location: Location, val items: List<WorkoutItem>) {
    /** Distinct machines/stations needed, for the "Machines you'll use" section. */
    val stations: List<String>
        get() = items.mapNotNull { stationFor(it.exercise) }.distinct()

    val estimatedMinutes: Int
        get() = items.sumOf {
            when (it.exercise.category) {
                "cardio" -> 12
                "stretching" -> 3
                else -> 8
            }
        }.coerceAtLeast(if (items.isEmpty()) 0 else 15)
}

object WorkoutGenerator {
    private val GYM_PREFERRED = setOf("machine", "cable", "barbell", "dumbbell", "e-z curl bar")

    private val GYM_ONLY = setOf("machine", "cable", "barbell", "e-z curl bar")

    /** Whether an exercise can be done at [location] with the user's equipment. */
    fun fits(ex: Exercise, location: Location, settings: Settings): Boolean = when (location) {
        Location.GYM -> true
        Location.HOME -> when {
            ex.equipment == "body only" -> true
            ex.equipment != null && ex.equipment in settings.homeEquipment -> true
            // Stretches and simple cardio (jump rope, running outside) tagged "other"/none need nothing special.
            (ex.category == "stretching" || ex.category == "cardio") &&
                (ex.equipment == null || ex.equipment == "other") &&
                !ex.name.contains("Prowler", ignoreCase = true) -> true
            else -> false
        }
    }

    fun isGymOnly(ex: Exercise): Boolean = ex.equipment in GYM_ONLY

    fun generate(
        all: List<Exercise>,
        type: TrainingType,
        location: Location,
        settings: Settings,
        seed: Long,
    ): Workout {
        val rnd = Random(seed)
        val maxLevel = settings.levelEnum.rank
        val used = mutableSetOf<String>()
        val items = mutableListOf<WorkoutItem>()

        fun add(ex: Exercise) {
            used += ex.id
            items += WorkoutItem(ex, prescriptionFor(ex, settings.goalEnum))
        }

        for (slot in type.slots) {
            val base = all.filter { ex ->
                ex.category in slot.categories && slot.matchesMuscle(ex) && fits(ex, location, settings) && ex.id !in used
            }
            // Respect the user's level when possible; relax it if that leaves too few options.
            var pool = base.filter { Level.rankOf(it.level) <= maxLevel }
            if (pool.size < slot.count) pool = base
            if (location == Location.GYM) {
                val preferred = pool.filter { it.equipment in GYM_PREFERRED }
                if (preferred.size >= slot.count) pool = preferred
            }
            // Compound lifts first, then isolation, both shuffled.
            val (compound, iso) = pool.shuffled(rnd).partition { it.mechanic == "compound" }
            (compound + iso).take(slot.count).forEach(::add)
        }

        // With little home equipment some slots can't be filled: top up from any muscle in this session.
        val target = type.slots.sumOf { it.count }
        if (items.size < target && type.slots.isNotEmpty()) {
            val extra = all.filter { ex ->
                ex.id !in used && fits(ex, location, settings) &&
                    type.slots.any { slot -> ex.category in slot.categories && slot.matchesMuscle(ex) }
            }.shuffled(rnd)
            extra.take(target - items.size).forEach(::add)
        }
        return Workout(type, location, items)
    }

    fun prescriptionFor(ex: Exercise, goal: Goal): Prescription = when (ex.category) {
        "stretching" -> Prescription("2", "30–45 s hold", "15 s")
        "cardio" -> Prescription("1", "10–15 min steady", "—")
        "plyometrics" -> Prescription("3", "8–10", "60 s")
        else -> when (goal) {
            Goal.STRENGTH -> if (ex.mechanic == "compound") Prescription("5", "3–5", "2–3 min")
            else Prescription("3", "6–8", "90 s")
            Goal.MUSCLE -> Prescription(if (ex.mechanic == "compound") "4" else "3", "8–12", "60–90 s")
            Goal.ENDURANCE -> Prescription("3", "15–20", "30–45 s")
        }
    }
}

/** Turns an exercise into a human name for the gym station / machine it needs. */
fun stationFor(ex: Exercise): String? {
    val n = ex.name.lowercase()
    return when {
        "smith" in n -> "Smith machine"
        "leg press" in n || "calf press" in n -> "Leg press"
        "hack squat" in n -> "Hack squat machine"
        "leg curl" in n -> "Leg curl machine"
        "leg extension" in n -> "Leg extension machine"
        "treadmill" in n -> "Treadmill"
        "bicycl" in n || "bike" in n -> "Stationary bike"
        "elliptical" in n -> "Elliptical"
        "rowing, stationary" in n -> "Rowing machine"
        "stairmaster" in n || "step mill" in n -> "Stair climber"
        "abductor" in n || "adductor" in n -> "Hip abductor / adductor"
        "butterfly" in n || "machine fly" in n || "reverse machine flyes" in n -> "Pec deck"
        "pulldown" in n -> "Lat pulldown"
        "seated cable row" in n || "seated row" in n -> "Seated cable row"
        "calf raise" in n && ex.equipment == "machine" -> "Calf raise machine"
        "leverage" in n -> "Plate-loaded lever machine"
        "dip machine" in n -> "Dip machine"
        "preacher" in n -> "Preacher bench"
        ex.equipment == "cable" -> "Cable station"
        ex.equipment == "machine" -> ex.name
        ex.equipment == "barbell" && ("squat" in n || "press" in n || "bench" in n) -> "Barbell rack & bench"
        ex.equipment == "barbell" || ex.equipment == "e-z curl bar" -> "Barbell & plates"
        ex.equipment == "dumbbell" -> "Dumbbells"
        ex.equipment == "kettlebells" -> "Kettlebells"
        ex.equipment == "bands" -> "Resistance bands"
        ex.equipment == "exercise ball" -> "Exercise ball"
        ex.equipment == "medicine ball" -> "Medicine ball"
        ex.equipment == "foam roll" -> "Foam roller"
        else -> null
    }
}

/** Ready-made weekly splits. Index 0 = Monday. */
data class WeekTemplate(val name: String, val description: String, val days: List<Pair<TrainingType, Location>>)

val WEEK_TEMPLATES: List<WeekTemplate> = run {
    val g = Location.GYM
    val h = Location.HOME
    listOf(
        WeekTemplate(
            "Push / Pull / Legs", "6 gym days, classic hypertrophy split",
            listOf(TrainingType.PUSH to g, TrainingType.PULL to g, TrainingType.LEGS to g,
                TrainingType.PUSH to g, TrainingType.PULL to g, TrainingType.LEGS to g, TrainingType.REST to h),
        ),
        WeekTemplate(
            "Upper / Lower", "4 gym days + mobility",
            listOf(TrainingType.UPPER to g, TrainingType.LOWER to g, TrainingType.REST to h,
                TrainingType.UPPER to g, TrainingType.LOWER to g, TrainingType.MOBILITY to h, TrainingType.REST to h),
        ),
        WeekTemplate(
            "Full body 3x", "Great for beginners",
            listOf(TrainingType.FULL_BODY to g, TrainingType.REST to h, TrainingType.FULL_BODY to g,
                TrainingType.REST to h, TrainingType.FULL_BODY to g, TrainingType.CARDIO to h, TrainingType.REST to h),
        ),
        WeekTemplate(
            "Home only", "No gym needed",
            listOf(TrainingType.FULL_BODY to h, TrainingType.CARDIO to h, TrainingType.CORE to h,
                TrainingType.FULL_BODY to h, TrainingType.MOBILITY to h, TrainingType.CARDIO to h, TrainingType.REST to h),
        ),
        WeekTemplate(
            "Hybrid gym + home", "Gym 3x, home 2x",
            listOf(TrainingType.UPPER to g, TrainingType.CARDIO to h, TrainingType.LOWER to g,
                TrainingType.CORE to h, TrainingType.FULL_BODY to g, TrainingType.MOBILITY to h, TrainingType.REST to h),
        ),
    )
}
