package com.mefit.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mefit.app.data.Location
import com.mefit.app.domain.WorkoutGenerator
import com.mefit.app.ui.AppViewModel
import com.mefit.app.ui.components.ChoiceChip
import com.mefit.app.ui.components.ExerciseRow
import com.mefit.app.ui.components.titleCase

private val MUSCLES = listOf(
    "chest", "shoulders", "triceps", "biceps", "forearms", "lats", "middle back", "lower back", "traps",
    "abdominals", "quadriceps", "hamstrings", "glutes", "calves", "adductors", "abductors", "neck",
)
private val CATEGORIES = listOf("strength", "stretching", "plyometrics", "cardio", "powerlifting", "olympic weightlifting", "strongman")
private val EQUIPMENT = listOf(
    "machine", "cable", "barbell", "dumbbell", "kettlebells", "body only", "bands", "e-z curl bar",
    "medicine ball", "exercise ball", "foam roll", "other",
)

@Composable
fun LibraryScreen(vm: AppViewModel, openExercise: (String) -> Unit) {
    val all by vm.exercises.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var where by rememberSaveable { mutableStateOf<String?>(null) }   // Location name or null = anywhere
    var muscle by rememberSaveable { mutableStateOf<String?>(null) }
    var equipment by rememberSaveable { mutableStateOf<String?>(null) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }

    val filtered = remember(all, query, where, muscle, equipment, category, settings) {
        val q = query.trim().lowercase()
        all.filter { ex ->
            (q.isEmpty() || ex.name.lowercase().contains(q) || ex.primaryMuscles.any { it.contains(q) }) &&
                (muscle == null || muscle in ex.primaryMuscles || muscle in ex.secondaryMuscles) &&
                (equipment == null || ex.equipment == equipment) &&
                (category == null || ex.category == category) &&
                when (where) {
                    Location.HOME.name -> WorkoutGenerator.fits(ex, Location.HOME, settings)
                    Location.GYM.name -> WorkoutGenerator.fits(ex, Location.GYM, settings)
                    else -> true
                }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Exercise library", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp))
                Text("${all.size} exercises", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear")
                        }
                    },
                    placeholder = { Text("Search squat, curl, leg press…") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { ChoiceChip("Anywhere", where == null, { where = null }) }
                item { ChoiceChip("My gym", where == Location.GYM.name, { where = Location.GYM.name }, Icons.Rounded.LocationCity) }
                item { ChoiceChip("My home", where == Location.HOME.name, { where = Location.HOME.name }, Icons.Rounded.Home) }
            }
        }
        item { FilterRow("Muscle", MUSCLES, muscle) { muscle = it } }
        item { FilterRow("Equipment", EQUIPMENT, equipment) { equipment = it } }
        item { FilterRow("Type", CATEGORIES, category) { category = it } }
        item {
            Text(
                "${filtered.size} results",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        items(filtered, key = { it.id }) { ex ->
            Column(Modifier.padding(horizontal = 20.dp)) {
                ExerciseRow(exercise = ex, onClick = { openExercise(ex.id) })
            }
        }
        item {
            Text(
                "Exercise data & images: free-exercise-db (public domain).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}

@Composable
private fun FilterRow(label: String, options: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    Column {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, bottom = 6.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { ChoiceChip("All", selected == null, { onSelect(null) }) }
            items(options) { opt ->
                ChoiceChip(opt.titleCase(), selected == opt, { onSelect(if (selected == opt) null else opt) })
            }
        }
    }
}
