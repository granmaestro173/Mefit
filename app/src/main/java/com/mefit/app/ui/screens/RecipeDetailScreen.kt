package com.mefit.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mefit.app.domain.Nutrition
import com.mefit.app.domain.RecipeMatcher
import com.mefit.app.domain.dietPlan
import com.mefit.app.ui.AppViewModel
import com.mefit.app.ui.components.Panel
import com.mefit.app.ui.components.SectionTitle
import com.mefit.app.ui.components.Tag
import com.mefit.app.ui.theme.Coral
import com.mefit.app.ui.theme.Lime
import com.mefit.app.ui.theme.Sky
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipeDetailScreen(vm: AppViewModel, id: String, onBack: () -> Unit) {
    val recipes by vm.recipes.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val r = recipes.firstOrNull { it.id == id }
    val favorite = id in settings.favoriteRecipes

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            },
            actions = {
                IconButton(onClick = { vm.toggleFavorite(id) }) {
                    Icon(
                        if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (favorite) "Remove from favorites" else "Add to favorites",
                        tint = if (favorite) Coral else MaterialTheme.colorScheme.onSurface,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        if (r == null) return@Column
        val targets = Nutrition.targets(settings, dietPlan(settings.dietPlanId))
        // How many portions the user is cooking; nutrition below stays per portion.
        var portions by rememberSaveable(id) { mutableIntStateOf(r.servings) }
        val scale = portions.toDouble() / r.servings
        val color = mealColor(r.meal)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(Brush.linearGradient(listOf(color.copy(alpha = 0.30f), MaterialTheme.colorScheme.surfaceContainer)))
                    .padding(20.dp),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(mealIcon(r.meal), contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(RecipeMatcher.mealLabel(r.meal).uppercase(), style = MaterialTheme.typography.labelSmall, color = color)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${r.minutes} min", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(r.name, style = MaterialTheme.typography.headlineSmall)
                    if (r.blurb.isNotBlank()) {
                        Text(r.blurb, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        r.tags.forEach { Tag(RecipeMatcher.tagLabel(it)) }
                    }
                }
            }

            SectionTitle("Per serving")
            Panel {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${r.kcal}", style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "kcal · ${percent(r.kcal, targets.calories)}% of your day",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    MacroTarget("Protein", r.protein, targets.proteinG, Coral)
                    MacroTarget("Carbs", r.carbs, targets.carbsG, Sky)
                    MacroTarget("Fat", r.fat, targets.fatG, Color(0xFFFFD54F))
                }
            }

            SectionTitle("Ingredients") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = { if (portions > 1) portions-- }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Fewer portions", modifier = Modifier.size(16.dp))
                    }
                    Text(
                        "$portions ${if (portions == 1) "portion" else "portions"}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 10.dp),
                    )
                    FilledTonalIconButton(onClick = { if (portions < 12) portions++ }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Add, contentDescription = "More portions", modifier = Modifier.size(16.dp))
                    }
                }
            }
            Panel {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (portions != r.servings) {
                        Text(
                            "Amounts in grams are scaled to $portions ${if (portions == 1) "portion" else "portions"} " +
                                "(the recipe is written for ${r.servings}).",
                            style = MaterialTheme.typography.bodySmall,
                            color = Lime,
                        )
                    }
                    r.ingredients.forEach { ing ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(color))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                if (portions == r.servings) ing.label else "${ing.label} → ${(ing.grams * scale).roundToInt()} g",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            if (portions == r.servings) {
                                Text("${ing.grams} g", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            SectionTitle("Steps")
            r.steps.forEachIndexed { i, step ->
                Row(Modifier.padding(bottom = 12.dp)) {
                    Box(
                        Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${i + 1}", color = Lime, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(step, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
            }

            Text(
                "Nutrition per serving is calculated from the gram amounts using USDA FoodData Central " +
                    "(SR Legacy, public domain). Brands and cooking methods vary, so treat it as an estimate.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
}

private fun percent(part: Int, whole: Int): Int = if (whole <= 0) 0 else (part * 100.0 / whole).roundToInt()

@Composable
private fun MacroTarget(label: String, grams: Int, target: Int, color: Color) {
    val frac = if (target <= 0) 0f else (grams.toFloat() / target).coerceIn(0f, 1f)
    Column(Modifier.padding(bottom = 10.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("$grams g", style = MaterialTheme.typography.titleMedium, color = color)
            Text(
                "  ·  ${percent(grams, target)}% of $target g",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { frac },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = color,
            trackColor = Color.Black.copy(alpha = 0.3f),
            strokeCap = StrokeCap.Round,
            drawStopIndicator = {},
        )
    }
}
