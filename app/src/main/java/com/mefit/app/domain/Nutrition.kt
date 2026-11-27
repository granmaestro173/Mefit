package com.mefit.app.domain

import com.mefit.app.data.BodyGoal
import com.mefit.app.data.Settings
import com.mefit.app.data.Sex
import kotlin.math.roundToInt

data class Targets(
    val bmr: Int,
    val tdee: Int,
    val calories: Int,
    val proteinG: Int,
    val fatG: Int,
    val carbsG: Int,
    val waterL: Double,
)

object Nutrition {
    /**
     * Mifflin-St Jeor BMR × activity factor, then a goal adjustment.
     * Protein 1.6–2.2 g/kg and fat ~25% of energy follow common sports-nutrition guidance
     * (e.g. ISSN position stand on protein); carbs fill the rest.
     */
    fun targets(s: Settings, plan: DietPlan? = null): Targets {
        val base = 10 * s.weightKg + 6.25 * s.heightCm - 5 * s.age
        val bmr = if (s.sexEnum == Sex.MALE) base + 5 else base - 161
        val tdee = bmr * s.activityEnum.factor
        val calories = when (s.bodyGoalEnum) {
            BodyGoal.LOSE -> tdee - minOf(500.0, tdee * 0.2)
            BodyGoal.MAINTAIN -> tdee
            BodyGoal.GAIN -> tdee + minOf(400.0, tdee * 0.1)
        }.coerceAtLeast(if (s.sexEnum == Sex.MALE) 1500.0 else 1200.0)

        val proteinPerKg = when (s.bodyGoalEnum) {
            BodyGoal.LOSE -> 2.0
            BodyGoal.MAINTAIN -> 1.6
            BodyGoal.GAIN -> 1.8
        }
        val fatShare = plan?.fatShare ?: 0.25
        val protein = s.weightKg * proteinPerKg
        val fat = calories * fatShare / 9.0
        val carbs = ((calories - protein * 4 - fat * 9) / 4.0).coerceAtLeast(50.0)
        return Targets(
            bmr = bmr.roundToInt(),
            tdee = tdee.roundToInt(),
            calories = (calories / 10).roundToInt() * 10,
            proteinG = protein.roundToInt(),
            fatG = fat.roundToInt(),
            carbsG = carbs.roundToInt(),
            waterL = ((s.weightKg * 0.035) * 10).roundToInt() / 10.0,
        )
    }
}

data class Meal(val name: String, val share: Double, val foods: List<String>)

data class DietPlan(
    val id: String,
    val name: String,
    val tagline: String,
    val bestFor: String,
    val fatShare: Double,
    val principles: List<String>,
    val eatMore: List<String>,
    val limit: List<String>,
    val meals: List<Meal>,
)

val DIET_PLANS: List<DietPlan> = listOf(
    DietPlan(
        id = "balanced",
        name = "Balanced plate",
        tagline = "Half veg, a quarter protein, a quarter carbs",
        bestFor = "Everyday health and maintaining weight",
        fatShare = 0.28,
        principles = listOf(
            "Fill half the plate with vegetables and fruit",
            "Protein at every meal: eggs, fish, chicken, beans or dairy",
            "Choose whole grains over refined ones",
            "Cook with olive or other plant oils; keep sugary drinks rare",
        ),
        eatMore = listOf("Vegetables", "Fruit", "Whole grains", "Legumes", "Nuts", "Fish"),
        limit = listOf("Sugary drinks", "Ultra-processed snacks", "Fried food", "Excess salt"),
        meals = listOf(
            Meal("Breakfast", 0.25, listOf("Oatmeal with milk or yogurt", "Banana or berries", "Handful of nuts")),
            Meal("Lunch", 0.35, listOf("Grilled chicken or beans", "Brown rice or potatoes", "Big mixed salad with olive oil")),
            Meal("Snack", 0.10, listOf("Fruit + cheese, or yogurt")),
            Meal("Dinner", 0.30, listOf("Fish or lean meat", "Roasted vegetables", "Whole-grain bread or quinoa")),
        ),
    ),
    DietPlan(
        id = "cut",
        name = "High-protein cut",
        tagline = "Lose fat, keep your muscle",
        bestFor = "Fat loss while lifting",
        fatShare = 0.25,
        principles = listOf(
            "Stay in a moderate calorie deficit (about 15–20% under maintenance)",
            "Hit your protein target daily; it keeps you full and protects muscle",
            "Build meals around lean protein and high-volume vegetables",
            "Put most carbs around your workouts",
        ),
        eatMore = listOf("Chicken breast", "Egg whites & eggs", "Greek yogurt", "White fish", "Leafy greens", "Berries"),
        limit = listOf("Alcohol", "Liquid calories", "Pastries & sweets", "Large portions of oil and butter"),
        meals = listOf(
            Meal("Breakfast", 0.25, listOf("3-egg omelette with spinach", "1 slice whole-grain toast", "Coffee or tea")),
            Meal("Lunch", 0.30, listOf("Chicken or tuna salad bowl", "½ cup rice or a potato", "Lots of vegetables")),
            Meal("Pre/post-workout", 0.15, listOf("Greek yogurt or protein shake", "Fruit")),
            Meal("Dinner", 0.30, listOf("Lean beef, fish or tofu", "Steamed or roasted vegetables", "Small portion of whole grains")),
        ),
    ),
    DietPlan(
        id = "bulk",
        name = "Lean bulk",
        tagline = "Small surplus, steady muscle gain",
        bestFor = "Building muscle without much fat",
        fatShare = 0.25,
        principles = listOf(
            "Eat about 5–10% above maintenance; aim to gain ~0.25–0.5% bodyweight a week",
            "4–5 meals a day makes the calories easier",
            "Plenty of carbs to fuel training",
            "Keep protein high and spread across meals",
        ),
        eatMore = listOf("Rice, pasta, oats", "Potatoes", "Whole milk & yogurt", "Peanut butter", "Eggs", "Red meat & chicken"),
        limit = listOf("Junk-food 'dirty bulking'", "Skipping vegetables", "Late-night binge snacks"),
        meals = listOf(
            Meal("Breakfast", 0.25, listOf("Oats cooked in milk", "Peanut butter + banana", "2 eggs")),
            Meal("Lunch", 0.25, listOf("Chicken or beef", "Large portion of rice or pasta", "Vegetables")),
            Meal("Snack", 0.15, listOf("Sandwich with cheese and ham, or a protein shake + fruit")),
            Meal("Dinner", 0.25, listOf("Salmon or lean meat", "Potatoes or sweet potatoes", "Salad with olive oil")),
            Meal("Evening", 0.10, listOf("Greek yogurt or cottage cheese with nuts")),
        ),
    ),
    DietPlan(
        id = "mediterranean",
        name = "Mediterranean",
        tagline = "Olive oil, fish, legumes, vegetables",
        bestFor = "Long-term heart health",
        fatShare = 0.33,
        principles = listOf(
            "Olive oil as the main fat",
            "Fish or seafood at least twice a week",
            "Legumes, vegetables and whole grains every day",
            "Red meat occasionally; sweets rarely",
        ),
        eatMore = listOf("Olive oil", "Fish & seafood", "Lentils & chickpeas", "Tomatoes & peppers", "Nuts & seeds", "Herbs"),
        limit = listOf("Processed meats", "Refined sugar", "Butter & margarine", "White bread"),
        meals = listOf(
            Meal("Breakfast", 0.25, listOf("Greek yogurt with honey, walnuts and fruit", "Whole-grain toast with tomato and olive oil")),
            Meal("Lunch", 0.35, listOf("Lentil or chickpea salad", "Feta, cucumber, olives", "Whole-grain pita")),
            Meal("Snack", 0.10, listOf("Hummus with vegetable sticks")),
            Meal("Dinner", 0.30, listOf("Baked fish with lemon", "Roasted vegetables", "Farro or brown rice")),
        ),
    ),
    DietPlan(
        id = "vegetarian",
        name = "Vegetarian performance",
        tagline = "Plant-forward with eggs & dairy",
        bestFor = "Vegetarians who train",
        fatShare = 0.28,
        principles = listOf(
            "Combine legumes and grains across the day",
            "Use eggs, dairy, tofu and tempeh to reach protein",
            "Watch iron, B12 and omega-3; ask a professional about supplements",
            "Vitamin C with iron-rich meals helps absorption",
        ),
        eatMore = listOf("Lentils & beans", "Tofu & tempeh", "Eggs", "Greek yogurt & cottage cheese", "Quinoa", "Seeds"),
        limit = listOf("Relying on cheese-heavy fast food", "Refined carbs without protein"),
        meals = listOf(
            Meal("Breakfast", 0.25, listOf("Scrambled tofu or eggs", "Whole-grain toast", "Orange")),
            Meal("Lunch", 0.30, listOf("Bean and quinoa bowl", "Avocado, corn, salsa")),
            Meal("Snack", 0.15, listOf("Cottage cheese or yogurt with seeds")),
            Meal("Dinner", 0.30, listOf("Lentil curry or tempeh stir-fry", "Rice", "Vegetables")),
        ),
    ),
    DietPlan(
        id = "budget",
        name = "Budget & home-cook",
        tagline = "Cheap staples, big batches",
        bestFor = "Eating well on a budget",
        fatShare = 0.27,
        principles = listOf(
            "Cook in batches 2–3 times a week",
            "Cheap proteins: eggs, beans, lentils, canned fish, chicken thighs",
            "Frozen vegetables are as good as fresh",
            "Plan the week's meals before shopping",
        ),
        eatMore = listOf("Eggs", "Rice & oats", "Beans & lentils", "Canned tuna/sardines", "Frozen vegetables", "Seasonal fruit"),
        limit = listOf("Takeaway", "Pre-packaged snacks", "Soft drinks"),
        meals = listOf(
            Meal("Breakfast", 0.25, listOf("Oats with milk and seasonal fruit", "Boiled eggs")),
            Meal("Lunch", 0.35, listOf("Rice and beans", "Chicken thighs or eggs", "Frozen mixed vegetables")),
            Meal("Snack", 0.10, listOf("Banana + handful of peanuts")),
            Meal("Dinner", 0.30, listOf("Lentil stew or tuna pasta", "Side salad or vegetables")),
        ),
    ),
)

fun dietPlan(id: String): DietPlan = DIET_PLANS.firstOrNull { it.id == id } ?: DIET_PLANS.first()
