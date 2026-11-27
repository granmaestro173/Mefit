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

/** Equipment someone might have at home (dataset equipment keys). */
enum class HomeEquipment(val key: String, val label: String) {
    DUMBBELL("dumbbell", "Dumbbells"),
    KETTLEBELL("kettlebells", "Kettlebells"),
    BANDS("bands", "Resistance bands"),
    EXERCISE_BALL("exercise ball", "Exercise ball"),
    MEDICINE_BALL("medicine ball", "Medicine ball"),
    FOAM_ROLL("foam roll", "Foam roller"),
}

@Serializable
data class DayPlan(
    val type: String,             // TrainingType.name
    val location: String = Location.GYM.name,
    val done: Boolean = false,
    val note: String = "",
    val shuffle: Int = 0,         // bumps the generator seed when the user asks for a new workout
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
    val homeEquipment: Set<String> = setOf(HomeEquipment.DUMBBELL.key, HomeEquipment.BANDS.key),
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
}
