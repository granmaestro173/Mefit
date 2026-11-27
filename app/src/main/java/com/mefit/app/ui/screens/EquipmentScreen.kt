package com.mefit.app.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mefit.app.data.EquipmentCatalog
import com.mefit.app.data.EquipmentItem
import com.mefit.app.data.Location
import com.mefit.app.data.Settings
import com.mefit.app.ui.AppViewModel
import com.mefit.app.ui.components.ChoiceChip
import com.mefit.app.ui.components.Panel
import com.mefit.app.ui.components.SectionTitle
import com.mefit.app.ui.theme.Lime

/** Lets the user tick off exactly what they have at their gym and at home. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EquipmentScreen(vm: AppViewModel, initial: Location, onBack: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    var locName by rememberSaveable { mutableStateOf(initial.name) }
    val loc = Location.valueOf(locName)
    val owned = s.equipmentFor(loc)
    val available = remember(exercises, s, loc) { vm.countAvailable(loc, s) }

    LaunchedEffect(Unit) {
        if (!s.equipmentReviewed) vm.updateSettings { it.copy(equipmentReviewed = true) }
    }

    fun write(transform: (Set<String>) -> Set<String>) = vm.updateSettings {
        if (loc == Location.GYM) it.copy(gymEquipment = transform(it.gymEquipment))
        else it.copy(homeEquipment = transform(it.homeEquipment))
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("My equipment") },
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip("My gym", loc == Location.GYM, { locName = Location.GYM.name }, Icons.Rounded.LocationCity)
                ChoiceChip("My home", loc == Location.HOME, { locName = Location.HOME.name }, Icons.Rounded.Home)
            }
            Spacer(Modifier.height(12.dp))
            Panel {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "$available exercises available",
                        style = MaterialTheme.typography.titleLarge,
                        color = Lime,
                    )
                    Text(
                        "Every ${if (loc == Location.GYM) "gym" else "home"} workout in your plan only uses what's ticked here. " +
                            "Bodyweight exercises are always included. Changes apply to all days you haven't edited by hand.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            EquipmentGroup("Free weights & accessories", EquipmentCatalog.FREE_WEIGHTS, owned, ::write)
            EquipmentGroup(
                if (loc == Location.GYM) "Machines & stations" else "Machines (if you have any at home)",
                EquipmentCatalog.MACHINES, owned, ::write,
            )

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Done") }
                TextButton(onClick = {
                    write { if (loc == Location.GYM) EquipmentCatalog.ALL_KEYS else EquipmentCatalog.DEFAULT_HOME }
                }) { Text("Reset to default") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EquipmentGroup(
    title: String,
    items: List<EquipmentItem>,
    owned: Set<String>,
    write: ((Set<String>) -> Set<String>) -> Unit,
) {
    val keys = items.map { it.key }.toSet()
    SectionTitle(title) {
        Row {
            TextButton(onClick = { write { it + keys } }) { Text("All") }
            TextButton(onClick = { write { it - keys } }) { Text("None") }
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            val on = item.key in owned
            ChoiceChip(item.label, on, { write { if (on) it - item.key else it + item.key } })
        }
    }
}

/** Short summary like "Gym: 24 items · Home: 2 items". */
fun equipmentSummary(s: Settings): String =
    "Gym: ${s.gymEquipment.size} items · Home: ${s.homeEquipment.size} items"
