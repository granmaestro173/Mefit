package com.mefit.app.data

import kotlinx.serialization.Serializable

/** One exercise from the open-source free-exercise-db dataset (public domain, Unlicense). */
@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val level: String? = null,
    val mechanic: String? = null,
    val force: String? = null,
    val equipment: String? = null,
    val category: String? = null,
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val images: List<String> = emptyList(),
) {
    val imageUrls: List<String>
        get() = images.map { IMAGE_BASE + it }

    val equipmentLabel: String
        get() = equipment?.replaceFirstChar { it.uppercase() } ?: "Other / none"

    val isMachine: Boolean
        get() = equipment == "machine" || equipment == "cable"

    companion object {
        const val IMAGE_BASE =
            "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"
    }
}

enum class Location(val label: String) { GYM("Gym"), HOME("Home") }

enum class Goal(val label: String, val blurb: String) {
    STRENGTH("Strength", "Heavy, low reps, long rests"),
    MUSCLE("Build muscle", "Moderate weight, 8–12 reps"),
    ENDURANCE("Endurance & tone", "Lighter weight, high reps, short rests"),
}

enum class Level(val label: String, val rank: Int) {
    BEGINNER("Beginner", 0), INTERMEDIATE("Intermediate", 1), EXPERT("Expert", 2);

    companion object {
        fun rankOf(datasetLevel: String?): Int = when (datasetLevel) {
            "intermediate" -> 1
            "expert" -> 2
            else -> 0
        }
    }
}

/**
 * Everything MeFit can ask "do you have this?" about. Free-weight keys match the dataset's
 * `equipment` field; machine keys are station names (see domain.stationFor).
 */
data class EquipmentItem(val key: String, val label: String)

object EquipmentCatalog {
    val FREE_WEIGHTS = listOf(
        EquipmentItem("dumbbell", "Dumbbells"),
        EquipmentItem("barbell", "Barbell & plates"),
        EquipmentItem("e-z curl bar", "EZ curl bar"),
        EquipmentItem("kettlebells", "Kettlebells"),
        EquipmentItem("bands", "Resistance bands"),
        EquipmentItem("medicine ball", "Medicine ball"),
        EquipmentItem("exercise ball", "Exercise ball"),
        EquipmentItem("foam roll", "Foam roller"),
        EquipmentItem(PULL_UP_BAR, "Pull-up bar"),
    )
    const val OTHER_MACHINES = "Other machines"
    const val PULL_UP_BAR = "pull-up bar"
    val MACHINES = listOf(
        "Cable station", "Lat pulldown", "Seated cable row", "Smith machine", "Leg press",
        "Hack squat machine", "Leg curl machine", "Leg extension machine", "Calf raise machine",
        "Pec deck", "Plate-loaded lever machine", "Dip machine", "Hip abductor / adductor",
        "Treadmill", "Stationary bike", "Rowing machine", "Elliptical", "Stair climber", OTHER_MACHINES,
    ).map { EquipmentItem(it, it) }

    val ALL_KEYS: Set<String> = (FREE_WEIGHTS + MACHINES).map { it.key }.toSet()
    val MACHINE_KEYS: Set<String> = MACHINES.map { it.key }.toSet()
    val DEFAULT_HOME: Set<String> = setOf("dumbbell", "bands")

    /** One-tap setups for people training at home. */
    val HOME_PRESETS: List<Pair<String, Set<String>>> = listOf(
        "Bodyweight only" to emptySet(),
        "Basic kit" to setOf("dumbbell", "bands"),
        "Full home gym" to setOf(
            "dumbbell", "barbell", "kettlebells", "bands", PULL_UP_BAR, "exercise ball", "medicine ball", "foam roll",
        ),
    )

    /** Bump when new equipment is added so existing gym setups gain it (see LocalStore). */
    const val CATALOG_VERSION = 2

    fun label(key: String): String = (FREE_WEIGHTS + MACHINES).firstOrNull { it.key == key }?.label ?: key
}

@Serializable
data class DayPlan(
    val type: String,             // TrainingType.name
    val location: String = Location.GYM.name,
    val done: Boolean = false,
    val note: String = "",
    val shuffle: Int = 0,         // bumps the generator seed when the user asks for a new workout
    val exercises: List<String>? = null, // user-edited exercise ids for this day; null = auto-generated
)

enum class Sex(val label: String) { MALE("Male"), FEMALE("Female") }

enum class Activity(val label: String, val factor: Double) {
    SEDENTARY("Sedentary (desk job, little exercise)", 1.2),
    LIGHT("Light (1–3 workouts/week)", 1.375),
    MODERATE("Moderate (3–5 workouts/week)", 1.55),
    HIGH("High (6–7 workouts/week)", 1.725),
    ATHLETE("Very high (physical job + training)", 1.9),
}

enum class BodyGoal(val label: String) { LOSE("Lose fat"), MAINTAIN("Maintain"), GAIN("Gain muscle") }

@Serializable
data class Settings(
    val name: String = "",
    val level: String = Level.BEGINNER.name,
    val goal: String = Goal.MUSCLE.name,
    val homeEquipment: Set<String> = EquipmentCatalog.DEFAULT_HOME,
    val gymEquipment: Set<String> = EquipmentCatalog.ALL_KEYS,
    val equipmentReviewed: Boolean = false,
    val homeOnly: Boolean = false,
    val catalogVersion: Int = 1,
    val sex: String = Sex.MALE.name,
    val age: Int = 28,
    val heightCm: Int = 175,
    val weightKg: Double = 75.0,
    val activity: String = Activity.MODERATE.name,
    val bodyGoal: String = BodyGoal.MAINTAIN.name,
    val dietPlanId: String = "balanced",
) {
    val levelEnum get() = runCatching { Level.valueOf(level) }.getOrDefault(Level.BEGINNER)
    val goalEnum get() = runCatching { Goal.valueOf(goal) }.getOrDefault(Goal.MUSCLE)
    val sexEnum get() = runCatching { Sex.valueOf(sex) }.getOrDefault(Sex.MALE)
    val activityEnum get() = runCatching { Activity.valueOf(activity) }.getOrDefault(Activity.MODERATE)
    val bodyGoalEnum get() = runCatching { BodyGoal.valueOf(bodyGoal) }.getOrDefault(BodyGoal.MAINTAIN)

    fun equipmentFor(location: Location): Set<String> =
        if (location == Location.GYM && !homeOnly) gymEquipment else homeEquipment
}
