package com.mefit.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mefit.app.data.DayPlan
import com.mefit.app.data.Location
import com.mefit.app.domain.TrainingType
import com.mefit.app.domain.WEEK_TEMPLATES
import com.mefit.app.ui.AppViewModel
import com.mefit.app.ui.components.ChoiceChip
import com.mefit.app.ui.components.Panel
import com.mefit.app.ui.components.SectionTitle
import com.mefit.app.ui.components.icon
import com.mefit.app.ui.components.uiColor
import com.mefit.app.ui.theme.Lime
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(vm: AppViewModel, openEquipment: (Location) -> Unit, openDay: (LocalDate) -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val plans by vm.plans.collectAsStateWithLifecycle()
    var monthStr by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = YearMonth.parse(monthStr)
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var showTemplates by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Training plan", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            IconButton(onClick = { monthStr = month.minusMonths(1).toString() }) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous month")
            }
            IconButton(onClick = { monthStr = month.plusMonths(1).toString() }) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Next month")
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { showTemplates = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Auto-plan")
            }
            OutlinedButton(onClick = { vm.clearMonth(month.atDay(1)) }) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Clear month")
            }
        }

        Spacer(Modifier.height(16.dp))
        MonthGrid(month, plans, onDayClick = { editing = it.toString() })

        SectionTitle("Session types")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TrainingType.entries.forEach { t ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(t.uiColor))
                    Spacer(Modifier.width(6.dp))
                    Text(t.label, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap any day to choose the session type, gym or home, and add a note. Lime check = completed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }

    editing?.let { dateStr ->
        val date = LocalDate.parse(dateStr)
        ModalBottomSheet(
            onDismissRequest = { editing = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            DayEditor(
                date = date,
                existing = plans[dateStr],
                equipmentCount = { loc -> settings.equipmentFor(loc).size },
                homeOnly = settings.homeOnly,
                onEquipment = { loc -> editing = null; openEquipment(loc) },
                onSave = { vm.setPlan(date, it); editing = null },
                onClear = { vm.setPlan(date, null); editing = null },
                onOpen = { plan ->
                    vm.setPlan(date, plan)
                    editing = null
                    openDay(date)
                },
            )
        }
    }

    if (showTemplates) {
        ModalBottomSheet(
            onDismissRequest = { showTemplates = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            TemplatePicker(
                month = month,
                homeOnly = settings.homeOnly,
                onApply = { index, weeks ->
                    val now = LocalDate.now()
                    val start = if (YearMonth.from(now) == month) now else month.atDay(1)
                    vm.applyTemplate(index, start, weeks)
                    showTemplates = false
                },
            )
        }
    }
}

@Composable
private fun MonthGrid(month: YearMonth, plans: Map<String, DayPlan>, onDayClick: (LocalDate) -> Unit) {
    val first = month.atDay(1)
    val leading = first.dayOfWeek.value - 1 // Monday-first
    val days = month.lengthOfMonth()
    val rows = (leading + days + 6) / 7
    val today = LocalDate.now()

    Panel {
        Column(Modifier.padding(10.dp)) {
            Row {
                DayOfWeek.values().forEach {
                    Text(
                        it.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            for (r in 0 until rows) {
                Row {
                    for (c in 0..6) {
                        val index = r * 7 + c - leading
                        Box(Modifier.weight(1f).aspectRatio(0.78f).padding(2.dp)) {
                            if (index in 0 until days) {
                                val date = first.plusDays(index.toLong())
                                DayCell(date, plans[date.toString()], date == today) { onDayClick(date) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, plan: DayPlan?, isToday: Boolean, onClick: () -> Unit) {
    val type = plan?.let { TrainingType.of(it.type) }
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxSize()
            .clip(shape)
            .background(type?.uiColor?.copy(alpha = 0.16f) ?: MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
            .then(if (isToday) Modifier.border(1.5.dp, Lime, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "${date.dayOfMonth}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isToday) FontWeight.Black else FontWeight.Medium,
            color = if (isToday) Lime else MaterialTheme.colorScheme.onSurface,
        )
        if (type != null) {
            if (plan?.done == true) {
                Box(
                    Modifier.size(18.dp).clip(CircleShape).background(Lime),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = "Done", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                }
            } else {
                Icon(type.icon, contentDescription = type.label, tint = type.uiColor, modifier = Modifier.size(18.dp))
            }
            Text(
                type.short,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = type.uiColor,
                maxLines = 1,
            )
        } else {
            Spacer(Modifier.height(1.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayEditor(
    date: LocalDate,
    existing: DayPlan?,
    equipmentCount: (Location) -> Int,
    homeOnly: Boolean,
    onEquipment: (Location) -> Unit,
    onSave: (DayPlan) -> Unit,
    onClear: () -> Unit,
    onOpen: (DayPlan) -> Unit,
) {
    var type by remember(date) { mutableStateOf(existing?.let { TrainingType.of(it.type) } ?: TrainingType.FULL_BODY) }
    var loc by remember(date, homeOnly) {
        mutableStateOf(
            if (homeOnly) Location.HOME
            else existing?.location?.let { runCatching { Location.valueOf(it) }.getOrNull() } ?: Location.GYM,
        )
    }
    var note by remember(date) { mutableStateOf(existing?.note.orEmpty()) }
    var done by remember(date) { mutableStateOf(existing?.done ?: false) }

    fun current(): DayPlan {
        val base = existing ?: DayPlan(type.name)
        // A different session type or place means the hand-picked exercises no longer apply.
        val keepCustom = existing != null && existing.type == type.name && (homeOnly || existing.location == loc.name)
        return base.copy(
            type = type.name, location = loc.name, note = note, done = done,
            exercises = if (keepCustom) existing?.exercises else null,
        )
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding(),
    ) {
        Text(date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), style = MaterialTheme.typography.headlineSmall)
        SectionTitle("Session")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TrainingType.entries.forEach { t ->
                val sel = t == type
                Row(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (sel) t.uiColor else t.uiColor.copy(alpha = 0.12f))
                        .clickable { type = t }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(t.icon, contentDescription = null, tint = if (sel) MaterialTheme.colorScheme.onPrimary else t.uiColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        t.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        Text(
            type.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (type != TrainingType.REST) {
            SectionTitle("Where")
            if (!homeOnly) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("Gym", loc == Location.GYM, { loc = Location.GYM }, Icons.Rounded.LocationCity)
                    ChoiceChip("Home", loc == Location.HOME, { loc = Location.HOME }, Icons.Rounded.Home)
                }
            }
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Uses your ${if (loc == Location.GYM) "gym" else "home"} equipment (${equipmentCount(loc)} items)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onEquipment(loc) }) { Text("Edit equipment") }
            }
            if (existing?.exercises != null && existing.type == type.name && (homeOnly || existing.location == loc.name)) {
                Text(
                    "You've customized this day's exercises. Changing the session or place resets them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Lime,
                )
            }
        }
        SectionTitle("Note")
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            placeholder = { Text("e.g. Try 2.5 kg more on bench") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        )
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Completed", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Switch(checked = done, onCheckedChange = { done = it })
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { onSave(current()) }, modifier = Modifier.weight(1f)) { Text("Save") }
            OutlinedButton(onClick = { onOpen(current()) }, modifier = Modifier.weight(1f)) { Text("Open workout") }
        }
        if (existing != null) {
            TextButton(onClick = onClear, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Remove from plan", color = MaterialTheme.colorScheme.secondary)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TemplatePicker(month: YearMonth, homeOnly: Boolean, onApply: (Int, Int) -> Unit) {
    var selected by remember {
        mutableIntStateOf(if (homeOnly) WEEK_TEMPLATES.indexOfFirst { it.name == "Home only" }.coerceAtLeast(0) else 0)
    }
    var weeks by remember { mutableIntStateOf(4) }
    val isCurrent = YearMonth.from(LocalDate.now()) == month
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding(),
    ) {
        Text("Auto-plan your training", style = MaterialTheme.typography.headlineSmall)
        Text(
            (if (homeOnly) "Home-only mode: every day becomes a home workout. " else "") + "Fills days starting from ${if (isCurrent) "this week" else "the first week of " + month.format(DateTimeFormatter.ofPattern("MMMM"))}. " +
                "Days you've already completed are kept.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SectionTitle("Split")
        WEEK_TEMPLATES.forEachIndexed { i, t ->
            val sel = i == selected
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .then(if (sel) Modifier.border(1.dp, Lime, MaterialTheme.shapes.medium) else Modifier)
                    .clickable { selected = i }
                    .padding(14.dp),
            ) {
                Text(t.name, style = MaterialTheme.typography.titleMedium)
                Text(t.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    t.days.forEachIndexed { d, (type, loc) ->
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(type.uiColor.copy(alpha = 0.18f))
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                DayOfWeek.of(d + 1).getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(type.short, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = type.uiColor)
                            Text(if (type == TrainingType.REST) " " else if (loc == Location.GYM && !homeOnly) "gym" else "home", fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        SectionTitle("How many weeks")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 2, 4, 8).forEach { w ->
                ChoiceChip("$w wk", weeks == w, { weeks = w })
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onApply(selected, weeks) }, modifier = Modifier.fillMaxWidth()) {
            Text("Apply ${WEEK_TEMPLATES[selected].name}")
        }
        Spacer(Modifier.height(16.dp))
    }
}
