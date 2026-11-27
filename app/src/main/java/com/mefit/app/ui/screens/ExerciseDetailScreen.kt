package com.mefit.app.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mefit.app.domain.WorkoutGenerator
import com.mefit.app.domain.stationFor
import com.mefit.app.ui.AppViewModel
import com.mefit.app.ui.components.Panel
import com.mefit.app.ui.components.SectionTitle
import com.mefit.app.ui.components.Tag
import com.mefit.app.ui.components.titleCase
import com.mefit.app.ui.theme.Coral
import com.mefit.app.ui.theme.Lime
import com.mefit.app.ui.theme.Sky
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailScreen(vm: AppViewModel, id: String, onBack: () -> Unit) {
    val all by vm.exercises.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val ex = remember(all, id) { all.firstOrNull { it.id == id } }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(ex?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        if (ex == null) return@Column

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            // Two-frame "animation": the dataset ships start and end positions for each move.
            var frame by remember { mutableIntStateOf(0) }
            LaunchedEffect(ex.id) {
                while (ex.imageUrls.size > 1) {
                    delay(1100)
                    frame = (frame + 1) % ex.imageUrls.size
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.25f)
                    .clip(MaterialTheme.shapes.large)
                    .background(Color.White),
            ) {
                Crossfade(targetState = frame, animationSpec = tween(350), label = "frame") { f ->
                    AsyncImage(
                        model = ex.imageUrls.getOrNull(f),
                        contentDescription = ex.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Row(
                    Modifier.align(Alignment.BottomCenter).padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ex.imageUrls.indices.forEach { i ->
                        Box(
                            Modifier.size(if (i == frame) 18.dp else 8.dp, 8.dp).clip(CircleShape)
                                .background(if (i == frame) Color(0xFF0E1116) else Color(0x550E1116)),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(ex.name, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ex.level?.let { Tag(it.titleCase(), Lime) }
                ex.category?.let { Tag(it.titleCase(), Sky) }
                Tag(ex.equipmentLabel, Coral)
                ex.mechanic?.let { Tag(it.titleCase()) }
                ex.force?.let { Tag(it.titleCase()) }
            }

            stationFor(ex)?.let { station ->
                SectionTitle(if (ex.isMachine) "Machine" else "Equipment")
                Panel {
                    Text(station, Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                }
            }

            val rx = WorkoutGenerator.prescriptionFor(ex, settings.goalEnum)
            SectionTitle("Suggested for your goal · ${settings.goalEnum.label}")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("Sets", rx.sets, Modifier.weight(1f))
                Metric("Reps", rx.reps, Modifier.weight(1f))
                Metric("Rest", rx.rest, Modifier.weight(1f))
            }

            SectionTitle("Muscles")
            Panel {
                Column(Modifier.padding(16.dp)) {
                    Text("Primary", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(ex.primaryMuscles.joinToString(", ") { it.titleCase() }, style = MaterialTheme.typography.titleMedium, color = Lime)
                    if (ex.secondaryMuscles.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text("Secondary", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(ex.secondaryMuscles.joinToString(", ") { it.titleCase() }, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            SectionTitle("How to do it")
            ex.instructions.forEachIndexed { i, step ->
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
                "Source: free-exercise-db (public domain). Stop if you feel pain; consider a coach for new lifts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 2)
    }
}
