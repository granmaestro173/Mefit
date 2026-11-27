package com.mefit.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mefit.app.domain.DIET_PLANS
import com.mefit.app.domain.DietPlan
import com.mefit.app.domain.Nutrition
import com.mefit.app.domain.Targets
import com.mefit.app.domain.dietPlan
import com.mefit.app.ui.AppViewModel
import com.mefit.app.ui.components.Panel
import com.mefit.app.ui.components.SectionTitle
import com.mefit.app.ui.components.Tag
import com.mefit.app.ui.theme.Coral
import com.mefit.app.ui.theme.Lime
import com.mefit.app.ui.theme.Sky
import com.mefit.app.domain.RecipeMatcher
import java.time.LocalDate
import kotlin.math.roundToInt

private val ProteinColor = Coral
private val CarbColor = Sky
private val FatColor = Color(0xFFFFD54F)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DietScreen(
    vm: AppViewModel,
    openSettings: () -> Unit,
    openRecipes: () -> Unit,
    openRecipe: (String) -> Unit,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val recipes by vm.recipes.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val plan = dietPlan(settings.dietPlanId)
    val t = Nutrition.targets(settings, plan)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Nutrition", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Your daily targets", style = MaterialTheme.typography.headlineMedium)
                }
                TextButton(onClick = openSettings) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Profile")
                }
            }
        }
        item { TargetsCard(t, settings.bodyGoalEnum.label, Modifier.padding(horizontal = 20.dp)) }

        item {
            SectionTitle("Gym recipes for ${plan.name}", Modifier.padding(horizontal = 20.dp)) {
                TextButton(onClick = openRecipes) { Text("See all") }
            }
        }
        item {
            val picks = recipes.filter { RecipeMatcher.fitsPlan(it, plan.id) }.sortedByDescending { it.protein }.take(8)
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(picks, key = { it.id }) { r ->
                    RecipeCard(r, t.proteinG, r.id in settings.favoriteRecipes, Modifier.width(300.dp)) { openRecipe(r.id) }
                }
            }
        }

        item { SectionTitle("Choose a diet style", Modifier.padding(horizontal = 20.dp)) }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(DIET_PLANS, key = { it.id }) { p ->
                    PlanCard(p, p.id == plan.id) { vm.updateSettings { s -> s.copy(dietPlanId = p.id) } }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                SectionTitle("${plan.name} · sample day")
                plan.meals.forEach { meal ->
                    val kcal = ((t.calories * meal.share) / 10).roundToInt() * 10
                    Panel(Modifier.padding(bottom = 10.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(meal.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                Tag("≈ $kcal kcal", Lime)
                            }
                            Spacer(Modifier.height(8.dp))
                            meal.foods.forEach { food ->
                                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant))
                                    Spacer(Modifier.width(10.dp))
                                    Text(food, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            val idea = RecipeMatcher.suggest(
                                recipes, RecipeMatcher.mealsForSlot(meal.name), plan.id, today, salt = meal.name.length,
                            )
                            if (idea != null) {
                                Row(
                                    Modifier
                                        .padding(top = 10.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Lime.copy(alpha = 0.08f))
                                        .clickable { openRecipe(idea.id) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(mealIcon(idea.meal), contentDescription = null, tint = Lime, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Recipe idea", style = MaterialTheme.typography.labelSmall, color = Lime)
                                        Text(idea.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                    Text("${idea.kcal} kcal · P ${idea.protein}g", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                SectionTitle("Key principles")
                Panel {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        plan.principles.forEachIndexed { i, p ->
                            Row {
                                Text("${i + 1}", color = Lime, fontWeight = FontWeight.Black, modifier = Modifier.width(22.dp))
                                Text(p, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                SectionTitle("Eat more")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    plan.eatMore.forEach { Tag(it, Lime) }
                }
                SectionTitle("Limit")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    plan.limit.forEach { Tag(it, Coral) }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "Targets use the Mifflin-St Jeor equation with your activity level, and protein of 1.6–2.0 g per kg. " +
                        "These are general estimates, not medical advice. If you have a health condition, are pregnant, " +
                        "or have a history of disordered eating, talk to a doctor or registered dietitian first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TargetsCard(t: Targets, goalLabel: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(Lime.copy(alpha = 0.22f), MaterialTheme.colorScheme.surfaceContainer)))
            .padding(20.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = Coral, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("${t.calories}", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.width(6.dp))
                Text("kcal / day", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp))
            }
            Text(
                "Goal: $goalLabel · maintenance ≈ ${t.tdee} kcal · BMR ${t.bmr}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            MacroBar("Protein", t.proteinG, t.proteinG * 4, t.calories, ProteinColor)
            MacroBar("Carbs", t.carbsG, t.carbsG * 4, t.calories, CarbColor)
            MacroBar("Fat", t.fatG, t.fatG * 9, t.calories, FatColor)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = Sky, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Water: about ${t.waterL} L a day (more on training days)", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun MacroBar(label: String, grams: Int, kcal: Int, total: Int, color: Color) {
    val frac = (kcal.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)
    Column(Modifier.padding(bottom = 12.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${grams} g", style = MaterialTheme.typography.titleMedium, color = color)
            Text("  ·  ${(frac * 100).roundToInt()}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { frac },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
            color = color,
            trackColor = Color.Black.copy(alpha = 0.3f),
            strokeCap = StrokeCap.Round,
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun PlanCard(plan: DietPlan, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .width(200.dp)
            .height(150.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
            .then(if (selected) Modifier.border(1.5.dp, Lime, MaterialTheme.shapes.medium) else Modifier)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Text(plan.name, style = MaterialTheme.typography.titleMedium, color = if (selected) Lime else MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(plan.tagline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        Spacer(Modifier.weight(1f))
        Text("Best for", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(plan.bestFor, style = MaterialTheme.typography.bodySmall, maxLines = 2)
    }
}
