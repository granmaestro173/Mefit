package com.mefit.app.domain

import com.mefit.app.data.Recipe
import java.time.LocalDate

object RecipeMatcher {
    val MEALS = listOf(
        "breakfast" to "Breakfast",
        "pre-workout" to "Pre-workout",
        "post-workout" to "Post-workout",
        "lunch" to "Lunch",
        "dinner" to "Dinner",
        "snack" to "Snacks",
    )

    val TAGS = listOf(
        "high-protein" to "High protein", "cut" to "Cutting", "bulk" to "Bulking", "vegetarian" to "Vegetarian",
        "budget" to "Budget", "no-cook" to "No cooking", "meal-prep" to "Meal prep", "gluten-free" to "Gluten-free",
        "mediterranean" to "Mediterranean",
    )

    fun mealLabel(meal: String): String = MEALS.firstOrNull { it.first == meal }?.second ?: meal
    fun tagLabel(tag: String): String = TAGS.firstOrNull { it.first == tag }?.second ?: tag

    /** Whether a recipe suits a diet style (plan ids from DIET_PLANS). */
    fun fitsPlan(r: Recipe, planId: String): Boolean = when (planId) {
        "cut" -> "cut" in r.tags || ("high-protein" in r.tags && r.kcal <= 500)
        "bulk" -> "bulk" in r.tags || r.kcal >= 500
        "vegetarian" -> "vegetarian" in r.tags
        "mediterranean" -> "mediterranean" in r.tags || "vegetarian" in r.tags || r.name.contains("salmon", true) ||
            r.name.contains("tuna", true) || r.name.contains("sardine", true)
        "budget" -> "budget" in r.tags
        else -> true
    }

    /** Which recipe meal types fit a meal slot of a diet plan's sample day (e.g. "Pre/post-workout"). */
    fun mealsForSlot(slotName: String): List<String> {
        val n = slotName.lowercase()
        return when {
            "breakfast" in n -> listOf("breakfast")
            "workout" in n -> listOf("post-workout", "pre-workout")
            "lunch" in n -> listOf("lunch", "post-workout")
            "dinner" in n -> listOf("dinner")
            else -> listOf("snack")
        }
    }

    /** A stable, rotating pick so suggestions change day to day. */
    fun suggest(all: List<Recipe>, meals: List<String>, planId: String, date: LocalDate, salt: Int = 0): Recipe? {
        val pool = all.filter { it.meal in meals && fitsPlan(it, planId) }.ifEmpty { all.filter { it.meal in meals } }
        if (pool.isEmpty()) return null
        val i = Math.floorMod(date.toEpochDay() + salt, pool.size.toLong()).toInt()
        return pool[i]
    }
}
