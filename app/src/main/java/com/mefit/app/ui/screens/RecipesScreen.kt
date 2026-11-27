package com.mefit.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BreakfastDining
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DinnerDining
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LunchDining
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Icecream
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mefit.app.data.Recipe
import com.mefit.app.domain.Nutrition
import com.mefit.app.domain.RecipeMatcher
import com.mefit.app.domain.dietPlan
import com.mefit.app.ui.AppViewModel
import com.mefit.app.ui.components.ChoiceChip
import com.mefit.app.ui.components.Panel
import com.mefit.app.ui.theme.Coral
import com.mefit.app.ui.theme.Lime
import com.mefit.app.ui.theme.Sky

private val FatYellow = Color(0xFFFFD54F)

fun mealColor(meal: String): Color = when (meal) {
    "breakfast" -> Color(0xFFFFD54F)
    "pre-workout" -> Sky
    "post-workout" -> Lime
    "lunch" -> Coral
    "dinner" -> Color(0xFFB388FF)
    else -> Color(0xFF64FFDA)
}

fun mealIcon(meal: String): ImageVector = when (meal) {
    "breakfast" -> Icons.Rounded.BreakfastDining
    "pre-workout" -> Icons.Rounded.Bolt
    "post-workout" -> Icons.Rounded.FitnessCenter
    "lunch" -> Icons.Rounded.LunchDining
    "dinner" -> Icons.Rounded.DinnerDining
    else -> Icons.Rounded.Icecream
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(vm: AppViewModel, onBack: () -> Unit, openRecipe: (String) -> Unit) {
    val all by vm.recipes.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val plan = dietPlan(settings.dietPlanId)
    val targets = Nutrition.targets(settings, plan)

    var query by rememberSaveable { mutableStateOf("") }
    var meal by rememberSaveable { mutableStateOf<String?>(null) }
    var tag by rememberSaveable { mutableStateOf<String?>(null) }
    var planOnly by rememberSaveable { mutableStateOf(true) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }

    val shown = remember(all, query, meal, tag, planOnly, favoritesOnly, settings) {
        val q = query.trim().lowercase()
        all.filter { r ->
            (q.isEmpty() || r.name.lowercase().contains(q) || r.ingredients.any { it.label.lowercase().contains(q) }) &&
                (meal == null || r.meal == meal) &&
                (tag == null || tag in r.tags) &&
                (!planOnly || RecipeMatcher.fitsPlan(r, settings.dietPlanId)) &&
                (!favoritesOnly || r.id in settings.favoriteRecipes)
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Gym recipes") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    placeholder = { Text("Search recipes or ingredients") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                )
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { ChoiceChip("Fits ${plan.name}", planOnly, { planOnly = !planOnly }) }
                    item { ChoiceChip("Favorites", favoritesOnly, { favoritesOnly = !favoritesOnly }, Icons.Rounded.Favorite) }
                }
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { ChoiceChip("All meals", meal == null, { meal = null }) }
                    items(RecipeMatcher.MEALS) { (key, label) ->
                        ChoiceChip(label, meal == key, { meal = if (meal == key) null else key }, mealIcon(key))
                    }
                }
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(RecipeMatcher.TAGS) { (key, label) ->
                        ChoiceChip(label, tag == key, { tag = if (tag == key) null else key })
                    }
                }
            }
            item {
                Text(
                    "${shown.size} recipes · nutrition per serving",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            if (shown.isEmpty()) {
                item {
                    Text(
                        "No recipes match these filters.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
            items(shown, key = { it.id }) { r ->
                Box(Modifier.padding(horizontal = 20.dp)) {
                    RecipeCard(r, targets.proteinG, r.id in settings.favoriteRecipes) { openRecipe(r.id) }
                }
            }
            item {
                Text(
                    "Nutrition calculated from USDA FoodData Central (SR Legacy, public domain). Values are estimates; " +
                        "brands and cooking methods vary.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
            }
        }
    }
}

@Composable
fun RecipeCard(r: Recipe, dailyProtein: Int, favorite: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val color = mealColor(r.meal)
    Panel(modifier, onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(mealIcon(r.meal), contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        RecipeMatcher.mealLabel(r.meal).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        modifier = Modifier.weight(1f),
                    )
                    if (favorite) Icon(Icons.Rounded.Favorite, contentDescription = "Favorite", tint = Coral, modifier = Modifier.size(14.dp))
                }
                Text(r.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                MacroLine(r)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${r.minutes} min" + (if (dailyProtein > 0) " · ${r.protein * 100 / dailyProtein}% of your daily protein" else ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun MacroLine(r: Recipe, factor: Double = 1.0) {
    fun v(x: Int) = Math.round(x * factor).toInt()
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${v(r.kcal)} kcal", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text("P ${v(r.protein)}g", style = MaterialTheme.typography.labelLarge, color = Coral)
        Text("C ${v(r.carbs)}g", style = MaterialTheme.typography.labelLarge, color = Sky)
        Text("F ${v(r.fat)}g", style = MaterialTheme.typography.labelLarge, color = FatYellow)
    }
}
