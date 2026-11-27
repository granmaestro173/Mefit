package com.mefit.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mefit.app.data.Activity
import com.mefit.app.data.BodyGoal
import com.mefit.app.data.Goal
import com.mefit.app.data.Level
import com.mefit.app.data.Sex
import com.mefit.app.ui.AppViewModel
import com.mefit.app.ui.components.ChoiceChip
import com.mefit.app.ui.components.Panel
import com.mefit.app.ui.components.SectionTitle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit, openEquipment: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Profile & settings") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            OutlinedTextField(
                value = s.name,
                onValueChange = { v -> vm.updateSettings { it.copy(name = v.take(24)) } },
                label = { Text("Your name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            )

            SectionTitle("Training level")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Level.entries.forEach { l ->
                    ChoiceChip(l.label, s.levelEnum == l, { vm.updateSettings { it.copy(level = l.name) } })
                }
            }

            SectionTitle("Training goal")
            Goal.entries.forEach { g ->
                RadioLine(g.label, g.blurb, s.goalEnum == g) { vm.updateSettings { it.copy(goal = g.name) } }
            }

            SectionTitle("My equipment")
            Panel(onClick = openEquipment) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Gym & home equipment", style = MaterialTheme.typography.titleMedium)
                        Text(
                            equipmentSummary(s),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                }
            }

            SectionTitle("Body (for diet targets)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Sex.entries.forEach { x ->
                    ChoiceChip(x.label, s.sexEnum == x, { vm.updateSettings { it.copy(sex = x.name) } })
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField("Age", s.age.toString(), Modifier.weight(1f)) { v ->
                    v.toIntOrNull()?.takeIf { it in 13..100 }?.let { n -> vm.updateSettings { it.copy(age = n) } }
                }
                NumberField("Height cm", s.heightCm.toString(), Modifier.weight(1f)) { v ->
                    v.toIntOrNull()?.takeIf { it in 120..230 }?.let { n -> vm.updateSettings { it.copy(heightCm = n) } }
                }
                NumberField("Weight kg", trimDouble(s.weightKg), Modifier.weight(1f), decimal = true) { v ->
                    v.replace(',', '.').toDoubleOrNull()?.takeIf { it in 30.0..300.0 }
                        ?.let { n -> vm.updateSettings { it.copy(weightKg = n) } }
                }
            }

            SectionTitle("Activity level")
            Activity.entries.forEach { a ->
                RadioLine(a.label, null, s.activityEnum == a) { vm.updateSettings { it.copy(activity = a.name) } }
            }

            SectionTitle("Body goal")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BodyGoal.entries.forEach { b ->
                    ChoiceChip(b.label, s.bodyGoalEnum == b, { vm.updateSettings { it.copy(bodyGoal = b.name) } })
                }
            }

            SectionTitle("About & credits")
            Panel {
                Text(
                    "Exercises, instructions and images come from free-exercise-db " +
                        "(github.com/yuhonas/free-exercise-db), an open, public-domain exercise dataset. " +
                        "Images load from GitHub, so an internet connection is needed to see them. " +
                        "Calorie targets use the Mifflin-St Jeor equation. MeFit gives general guidance, not medical advice.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun trimDouble(d: Double): String = if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()

@Composable
private fun NumberField(
    label: String,
    initial: String,
    modifier: Modifier = Modifier,
    decimal: Boolean = false,
    onValid: (String) -> Unit,
) {
    // Keep the raw text locally so partially typed values ("1", "17") don't get rejected mid-typing.
    var text by rememberSaveable(label) { mutableStateOf(initial) }
    OutlinedTextField(
        value = text,
        onValueChange = { v ->
            val clean = v.filter { it.isDigit() || (decimal && (it == '.' || it == ',')) }.take(5)
            text = clean
            onValid(clean)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
    )
}

@Composable
private fun RadioLine(title: String, subtitle: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
