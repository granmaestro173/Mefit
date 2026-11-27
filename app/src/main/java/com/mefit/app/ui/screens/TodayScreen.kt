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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mefit.app.data.DayPlan
import com.mefit.app.data.Location
import com.mefit.app.domain.TrainingType
import com.mefit.app.ui.AppViewModel
import com.mefit.app.ui.components.ChoiceChip
import com.mefit.app.ui.components.ExerciseRow
import com.mefit.app.ui.components.Panel
import com.mefit.app.ui.components.SectionTitle
import com.mefit.app.ui.components.StatBox
import com.mefit.app.ui.components.Tag
import com.mefit.app.ui.components.TypeIconBadge
import com.mefit.app.ui.components.icon
import com.mefit.app.ui.components.uiColor
import com.mefit.app.ui.theme.Coral
import com.mefit.app.ui.theme.Lime
import com.mefit.app.ui.theme.Sky
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.TextButton
import com.mefit.app.domain.WorkoutItem
import com.mefit.app.ui.components.ExercisePickerSheet
import com.mefit.app.ui.components.ExerciseThumb
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(
    vm: AppViewModel,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    openExercise: (String) -> Unit,
    openSettings: () -> Unit,
    openEquipment: (Location) -> Unit,
) {
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val plans by vm.plans.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val plan = plans[date.toString()]
    val workout = remember(exercises, plan, settings, date) {
        if (plan == null || exercises.isEmpty()) null else vm.workoutFor(date, plan)
    }
    // Per-session ticks for individual exercises (reset when the date changes).
    var ticked by rememberSaveable(date.toString()) { mutableStateOf(setOf<String>()) }
    var editing by rememberSaveable(date.toString()) { mutableStateOf(false) }
    var swapFor by rememberSaveable(date.toString()) { mutableStateOf<String?>(null) }
    var adding by rememberSaveable(date.toString()) { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Header(settings.name, date, openSettings) }
        item { WeekStrip(date, plans, onDateChange) }
        item { MonthStats(plans, date) }
        if (!settings.equipmentReviewed) {
            item {
                Panel(onClick = { openEquipment(Location.GYM) }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.FitnessCenter, contentDescription = null, tint = Lime)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Tell MeFit what equipment you have", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Tick the machines at your gym and what you own at home. Every workout will use only those.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                    }
                }
            }
        }

        if (exercises.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            return@LazyColumn
        }

        if (plan == null) {
            item {
                QuickPlan(homeOnly = settings.homeOnly, onPick = { type, loc -> vm.setPlan(date, DayPlan(type.name, loc.name)) })
            }
            return@LazyColumn
        }

        val type = TrainingType.of(plan.type)
        item {
            HeroCard(
                type = type,
                plan = plan,
                exerciseCount = workout?.items?.size ?: 0,
                minutes = workout?.estimatedMinutes ?: 0,
                onToggleDone = { vm.toggleDone(date) },
                onShuffle = { vm.shuffle(date) },
                onSwitchLocation = {
                    val next = if (plan.location == Location.GYM.name) Location.HOME else Location.GYM
                    vm.setPlan(date, plan.copy(location = next.name, exercises = null))
                },
                onEquipment = { openEquipment(vm.locationOf(plan)) },
                location = vm.locationOf(plan),
                homeOnly = settings.homeOnly,
            )
        }
        if (plan.note.isNotBlank()) {
            item {
                Panel { Text("📝 ${plan.note}", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium) }
            }
        }

        if (type == TrainingType.REST) {
            item {
                Panel {
                    Column(Modifier.padding(18.dp)) {
                        Text("Recovery is training too", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Aim for 7–9 hours of sleep, drink water and get an easy 20–30 minute walk. " +
                                "Want to move a little? Switch today to Mobility from the Plan tab.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            return@LazyColumn
        }

        val w = workout ?: return@LazyColumn
        if (w.stations.isNotEmpty()) {
            item {
                Column {
                    SectionTitle(if (w.location == Location.GYM) "Machines & stations you'll use" else "Equipment you'll need")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        w.stations.forEach { Tag(it, type.uiColor) }
                    }
                }
            }
        }
        item {
            SectionTitle(if (editing) "Edit workout" else "Workout · ${ticked.size}/${w.items.size} done") {
                TextButton(onClick = { editing = !editing }) {
                    Icon(if (editing) Icons.Rounded.Check else Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (editing) "Done" else "Edit")
                }
            }
        }
        if (plan.exercises != null) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Tag("Customized by you", Lime)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { vm.resetExercises(date) }) { Text("Reset to suggested") }
                }
            }
        }
        if (w.items.isEmpty()) {
            item {
                Panel {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            if (plan.exercises != null) "No exercises here yet. Add some below."
                            else if (type == TrainingType.PULL || type == TrainingType.UPPER)
                                "A ${type.label} session needs something to pull on: a pull-up bar, resistance bands " +
                                    "or dumbbells. Tick one in My equipment, or change this day to Full body or Core."
                            else "Your ticked equipment doesn't cover a ${type.label} session. " +
                                "Tick more equipment, change the session type, or add exercises by hand.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = { openEquipment(w.location) }) { Text("Edit my equipment") }
                    }
                }
            }
        }
        itemsIndexed(w.items, key = { _, wi -> wi.exercise.id }) { index, item ->
            val id = item.exercise.id
            if (editing) {
                EditableRow(
                    item = item,
                    canMoveUp = index > 0,
                    canMoveDown = index < w.items.lastIndex,
                    onMove = { vm.moveExercise(date, id, it) },
                    onSwap = { swapFor = id },
                    onRemove = { vm.removeExercise(date, id) },
                )
            } else {
                ExerciseRow(
                    exercise = item.exercise,
                    prescription = item.prescription,
                    checked = id in ticked,
                    onCheck = { ticked = if (id in ticked) ticked - id else ticked + id },
                    onClick = { openExercise(id) },
                )
            }
        }
        if (editing || w.items.isEmpty()) {
            item {
                OutlinedButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add exercise")
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }

    val where = if (plan == null || vm.locationOf(plan) == Location.HOME) "home" else "gym"
    swapFor?.let { oldId ->
        val options = remember(oldId, plan, settings, exercises) { vm.alternatives(date, oldId) }
        ExercisePickerSheet(
            title = "Swap ${vm.exercise(oldId)?.name ?: "exercise"}",
            subtitle = "Same muscles, using only your $where equipment",
            candidates = options,
            onPick = { vm.swapExercise(date, oldId, it.id); swapFor = null },
            onDismiss = { swapFor = null },
        )
    }
    if (adding) {
        val options = remember(plan, settings, exercises) { vm.addable(date) }
        ExercisePickerSheet(
            title = "Add an exercise",
            subtitle = "Best matches for this session first, using only your $where equipment",
            candidates = options,
            onPick = { vm.addExercise(date, it.id); adding = false },
            onDismiss = { adding = false },
        )
    }
}

@Composable
private fun EditableRow(
    item: WorkoutItem,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int) -> Unit,
    onSwap: () -> Unit,
    onRemove: () -> Unit,
) {
    Panel {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExerciseThumb(item.exercise, 48)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.exercise.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                    Text(
                        item.exercise.equipmentLabel + "  •  ${item.prescription.sets} × ${item.prescription.reps}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Move up")
                }
                IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Move down")
                }
                TextButton(onClick = onSwap) {
                    Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Swap")
                }
                TextButton(onClick = onRemove) {
                    Icon(
                        Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Remove", color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
private fun Header(name: String, date: LocalDate, openSettings: () -> Unit) {
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 19 -> "Good afternoon"
        else -> "Good evening"
    }
    val isToday = date == LocalDate.now()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(
                if (isToday) "$greeting${if (name.isNotBlank()) ", $name" else ""}"
                else date.format(DateTimeFormatter.ofPattern("EEEE")),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Me", style = MaterialTheme.typography.headlineMedium)
                Text("Fit", style = MaterialTheme.typography.headlineMedium, color = Lime)
                Spacer(Modifier.width(10.dp))
                Text(
                    date.format(DateTimeFormatter.ofPattern("d MMM")),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
        IconButton(onClick = openSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WeekStrip(date: LocalDate, plans: Map<String, DayPlan>, onDateChange: (LocalDate) -> Unit) {
    val monday = date.minusDays((date.dayOfWeek.value - 1).toLong())
    val today = LocalDate.now()
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 0..6) {
            val d = monday.plusDays(i.toLong())
            val p = plans[d.toString()]
            val type = p?.let { TrainingType.of(it.type) }
            val selected = d == date
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer)
                    .then(
                        if (d == today && !selected) Modifier.border(1.dp, Lime.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        else Modifier,
                    )
                    .clickable { onDateChange(d) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                Text(
                    d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = fg.copy(alpha = 0.7f),
                )
                Text("${d.dayOfMonth}", style = MaterialTheme.typography.titleMedium, color = fg, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.size(8.dp).clip(CircleShape).background(
                        when {
                            p?.done == true -> if (selected) MaterialTheme.colorScheme.onPrimary else Lime
                            type != null -> type.uiColor
                            else -> Color.Transparent
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun MonthStats(plans: Map<String, DayPlan>, date: LocalDate) {
    val inMonth = plans.filter {
        val d = LocalDate.parse(it.key)
        d.year == date.year && d.month == date.month && TrainingType.of(it.value.type) != TrainingType.REST
    }
    val done = inMonth.count { it.value.done }
    // Streak: consecutive past training days completed (rest days don't break it).
    var streak = 0
    var d = LocalDate.now()
    var guard = 0
    while (guard++ < 365) {
        val p = plans[d.toString()]
        if (p == null) {
            if (d != LocalDate.now()) break
        } else if (TrainingType.of(p.type) != TrainingType.REST) {
            if (p.done) streak++ else if (d != LocalDate.now()) break
        }
        d = d.minusDays(1)
    }
    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatBox("$done/${inMonth.size}", "Workouts this month", Lime, Modifier.weight(1f))
        StatBox("$streak", "Workout streak", Coral, Modifier.weight(1f))
    }
}

@Composable
private fun HeroCard(
    type: TrainingType,
    plan: DayPlan,
    exerciseCount: Int,
    minutes: Int,
    onToggleDone: () -> Unit,
    onShuffle: () -> Unit,
    onSwitchLocation: () -> Unit,
    onEquipment: () -> Unit,
    location: Location,
    homeOnly: Boolean,
) {
    val isGym = location == Location.GYM
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.linearGradient(listOf(type.uiColor.copy(alpha = 0.35f), MaterialTheme.colorScheme.surfaceContainer)),
            )
            .padding(20.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypeIconBadge(type, 52)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(type.label, style = MaterialTheme.typography.headlineSmall)
                    Text(type.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (type != TrainingType.REST) {
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.25f))
                            .then(if (homeOnly) Modifier else Modifier.clickable(onClick = onSwitchLocation))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (isGym) Icons.Rounded.LocationCity else Icons.Rounded.Home,
                            contentDescription = null, modifier = Modifier.size(16.dp), tint = Sky,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (isGym) "Gym" else "Home", style = MaterialTheme.typography.labelLarge)
                    }
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.25f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(16.dp), tint = Coral)
                        Spacer(Modifier.width(6.dp))
                        Text("~$minutes min · $exerciseCount moves", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .clickable(onClick = onEquipment)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.FitnessCenter, contentDescription = null, modifier = Modifier.size(16.dp), tint = Lime)
                    Spacer(Modifier.width(6.dp))
                    Text("My ${if (isGym) "gym" else "home"} equipment", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onToggleDone,
                    modifier = Modifier.weight(1f),
                    colors = if (plan.done) ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) else ButtonDefaults.buttonColors(),
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (plan.done) "Completed" else "Mark complete")
                }
                if (type != TrainingType.REST) {
                    OutlinedButton(onClick = onShuffle) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("New mix")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickPlan(homeOnly: Boolean, onPick: (TrainingType, Location) -> Unit) {
    var picked by rememberSaveable { mutableStateOf(Location.GYM) }
    val loc = if (homeOnly) Location.HOME else picked
    Panel(Modifier.padding(top = 8.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("Nothing planned for this day", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Pick a session and MeFit builds the workout from the exercise library. " +
                    "Or plan your whole month in the Plan tab.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            if (!homeOnly) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("Gym", loc == Location.GYM, { picked = Location.GYM }, Icons.Rounded.LocationCity)
                    ChoiceChip("Home", loc == Location.HOME, { picked = Location.HOME }, Icons.Rounded.Home)
                }
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TrainingType.entries.forEach { t ->
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(t.uiColor.copy(alpha = 0.14f))
                            .clickable { onPick(t, loc) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(t.icon, contentDescription = null, tint = t.uiColor, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(t.label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
