package com.mefit.app.domain

import com.mefit.app.data.EquipmentCatalog.ADJUSTABLE_BENCH
import com.mefit.app.data.EquipmentCatalog.BENCH
import com.mefit.app.data.EquipmentCatalog.BICYCLE
import com.mefit.app.data.EquipmentCatalog.DIP_BARS
import com.mefit.app.data.EquipmentCatalog.JUMP_ROPE
import com.mefit.app.data.EquipmentCatalog.OTHER_MACHINES
import com.mefit.app.data.EquipmentCatalog.PARTNER
import com.mefit.app.data.EquipmentCatalog.PULL_UP_BAR
import com.mefit.app.data.EquipmentCatalog.SKATES
import com.mefit.app.data.EquipmentCatalog.SQUAT_RACK
import com.mefit.app.data.Exercise

/**
 * Extra things an exercise needs beyond its dataset `equipment` field.
 *
 * free-exercise-db only records one piece of equipment per exercise, so many moves tagged
 * "body only" or with no equipment really need a bench, a bar, a machine or a partner, and
 * free-weight moves don't mention the bench or rack they're done on.
 */
object Requirements {
    /** Exercises reviewed by hand (ids from free-exercise-db). */
    private val CURATED: Map<String, Set<String>> = HashMap<String, Set<String>>().apply {
        fun need(keys: Set<String>, vararg ids: String) = ids.forEach { id -> this[id] = keys }

        need(
            setOf(BENCH),
            "Bench_Dips", "Bench_Jump", "Flat_Bench_Leg_Pull-In", "Flat_Bench_Lying_Leg_Raise",
            "Seated_Flat_Bench_Leg_Pull-In", "Seated_Leg_Tucks", "Flutter_Kicks", "Push-Ups_With_Feet_Elevated",
            "Step-up_with_Knee_Raise", "Incline_Push-Up", "On-Your-Back_Quad_Stretch", "Oblique_Crunches",
        )
        need(setOf(BENCH, PARTNER), "Hyperextensions_With_No_Hyperextension_Bench")
        need(setOf(ADJUSTABLE_BENCH), "Decline_Crunch", "Decline_Oblique_Crunch", "Decline_Reverse_Crunch")
        need(
            setOf(PULL_UP_BAR),
            "Chin-Up", "Pullups", "V-Bar_Pullup", "Wide-Grip_Rear_Pull-Up", "Gorilla_Chin_Crunch",
            "Hanging_Leg_Raise", "Hanging_Pike", "Wind_Sprints", "One_Handed_Hang",
        )
        need(setOf(DIP_BARS), "Dips_-_Triceps_Version")
        // Shown on a Smith machine bar.
        need(
            setOf("Smith machine"),
            "Incline_Push-Up_Close-Grip", "Incline_Push-Up_Medium", "Incline_Push-Up_Reverse_Grip", "Incline_Push-Up_Wide",
        )
        need(setOf("barbell", SQUAT_RACK), "Body_Tricep_Press")
        need(setOf("dumbbell"), "Close-Grip_Push-Up_off_of_a_Dumbbell")
        need(setOf("exercise ball"), "Crunch_-_Legs_On_Exercise_Ball")
        need(setOf(OTHER_MACHINES), "Natural_Glute_Ham_Raise", "Prowler_Sprint")
        need(
            setOf(PARTNER),
            "Lying_Crossover", "Lying_Glute", "Lying_Prone_Quadriceps", "Overhead_Triceps", "Seated_Biceps",
            "Seated_Front_Deltoid", "Seated_Glute", "Standing_Towel_Triceps_Extension", "Adductor_Groin",
            "Behind_Head_Chest_Stretch", "Lying_Bent_Leg_Groin", "Lying_Hamstring", "Overhead_Lat", "Seated_Hamstring",
        )
        need(setOf("foam roll"), "Anterior_Tibialis-SMR", "Foot-SMR", "Neck-SMR")
        need(setOf(JUMP_ROPE), "Rope_Jumping")
        need(setOf(BICYCLE), "Bicycling")
        need(setOf(SKATES), "Skating")
    }

    private val FREE_WEIGHT_EQUIPMENT =
        setOf("dumbbell", "barbell", "e-z curl bar", "kettlebells", "bands", "medicine ball", "exercise ball")

    private val cache = HashMap<String, Set<String>>()

    fun of(ex: Exercise): Set<String> = synchronized(cache) { cache.getOrPut(ex.id) { compute(ex) } }

    private fun compute(ex: Exercise): Set<String> {
        CURATED[ex.id]?.let { return it }
        val needs = mutableSetOf<String>()
        if (ex.equipment == "body only" && WorkoutGenerator.needsPullUpBar(ex)) needs += PULL_UP_BAR
        if (ex.equipment in FREE_WEIGHT_EQUIPMENT) {
            val name = ex.name.lowercase()
            val setup = ex.instructions.take(2).joinToString(" ").lowercase()
            when {
                Regex("incline|decline").containsMatchIn(name) ||
                    Regex("(incline|decline) bench").containsMatchIn(setup) -> needs += ADJUSTABLE_BENCH
                "bench" in name || "preacher" in name || Regex("\\bbench\\b").containsMatchIn(setup) -> needs += BENCH
            }
            if (ex.equipment == "barbell" && Regex("\\b(squat |power )?rack\\b").containsMatchIn(setup)) {
                needs += SQUAT_RACK
            }
        }
        return needs
    }
}
