package com.mefit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Hiking
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Rowing
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SportsGymnastics
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mefit.app.data.Exercise
import com.mefit.app.domain.Prescription
import com.mefit.app.domain.TrainingType

val TrainingType.uiColor: Color get() = Color(color)

val TrainingType.icon: ImageVector
    get() = when (this) {
        TrainingType.PUSH -> Icons.Rounded.FitnessCenter
        TrainingType.PULL -> Icons.Rounded.Rowing
        TrainingType.LEGS -> Icons.Rounded.Hiking
        TrainingType.UPPER -> Icons.Rounded.SportsGymnastics
        TrainingType.LOWER -> Icons.Rounded.DirectionsBike
        TrainingType.FULL_BODY -> Icons.Rounded.AccessibilityNew
        TrainingType.CORE -> Icons.Rounded.Whatshot
        TrainingType.CARDIO -> Icons.Rounded.Favorite
        TrainingType.MOBILITY -> Icons.Rounded.SelfImprovement
        TrainingType.REST -> Icons.Rounded.Bedtime
    }

fun String.titleCase(): String =
    split(" ").joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth().let { if (onClick != null) it.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick) else it },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        content = content,
    )
}

@Composable
fun TypeIconBadge(type: TrainingType, size: Int = 44) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape((size / 3).dp)).background(type.uiColor.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(type.icon, contentDescription = null, tint = type.uiColor, modifier = Modifier.size((size * 0.55).dp))
    }
}

@Composable
fun Tag(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit, leading: ImageVector? = null) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (leading != null) {
            { Icon(leading, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else null,
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@Composable
fun ExerciseThumb(exercise: Exercise, size: Int = 64) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(14.dp)).background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = exercise.imageUrls.firstOrNull(),
            contentDescription = exercise.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size.dp),
        )
    }
}

@Composable
fun ExerciseRow(
    exercise: Exercise,
    prescription: Prescription? = null,
    checked: Boolean? = null,
    onCheck: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Panel(onClick = onClick) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ExerciseThumb(exercise)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    exercise.primaryMuscles.joinToString(" · ") { it.titleCase() } + "  •  " + exercise.equipmentLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (prescription != null) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Tag("${prescription.sets} × ${prescription.reps}", MaterialTheme.colorScheme.primary)
                        Tag("Rest ${prescription.rest}")
                    }
                }
            }
            if (checked != null && onCheck != null) {
                Icon(
                    if (checked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = if (checked) "Done" else "Not done",
                    tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onCheck),
                )
            }
        }
    }
}

@Composable
fun StatBox(value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, accent.copy(alpha = 0.15f), MaterialTheme.shapes.medium)
            .padding(14.dp),
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = accent, fontWeight = FontWeight.Black)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
