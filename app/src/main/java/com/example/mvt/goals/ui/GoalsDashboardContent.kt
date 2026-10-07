package com.example.mvt.goals.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.goals.model.*
import com.example.mvt.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
internal fun GoalsDashboardContent(
    data: GoalsData,
    onNewGoal: () -> Unit,
    onEdit: (SportGoal) -> Unit,
    onDelete: (SportGoal) -> Unit,
    modifier: Modifier = Modifier
) {
    var period by rememberSaveable { mutableStateOf(GoalsPeriod.MONTH) }
    var completedTab by rememberSaveable { mutableStateOf(false) }
    var selectedGoalId by rememberSaveable { mutableStateOf<String?>(null) }
    val dashboard = remember(data.goals, period) { buildGoalsDashboard(data.goals, period) }
    val goals = if (completedTab) dashboard.completed else dashboard.active

    Column(modifier.fillMaxSize().background(AppSurface)) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GoalsPeriodPicker(period) { period = it }
                GoalsStatusTabs(completedTab) { completedTab = it }
            }
            item { GoalsComplianceCard(dashboard) }
            if (goals.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.Flag, null, tint = AthleteNavigationBlue, modifier = Modifier.size(32.dp))
                        Text(
                            if (completedTab) "Sin objetivos completados" else "Sin objetivos activos",
                            color = AppTextPrimary, fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (data.goals.isEmpty()) "Crea tu primera meta deportiva."
                            else "Prueba otro periodo o consulta todos tus objetivos.",
                            color = AppTextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center
                        )
                    }
                }
            }
            items(goals, key = { it.id }) { goal ->
                GoalProgressCard(goal, onClick = { selectedGoalId = goal.id })
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = { period = if (period == GoalsPeriod.ALL) GoalsPeriod.MONTH else GoalsPeriod.ALL }) {
                        Text(if (period == GoalsPeriod.ALL) "Volver a este mes" else "Ver todos los objetivos", color = AthleteNavigationBlue)
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
            Button(
                onClick = onNewGoal, shape = RoundedCornerShape(6.dp),
                modifier = Modifier.heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) { Text("Nuevo objetivo") }
        }
    }

    data.goals.firstOrNull { it.id == selectedGoalId }?.let { goal ->
        AlertDialog(
            onDismissRequest = { selectedGoalId = null },
            containerColor = AppSurface,
            title = { Text(goal.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (goal.generalGoal.isNotBlank()) Text(goal.generalGoal)
                    if (goal.description.isNotBlank()) Text(goal.description)
                    if (goal.specific.isNotBlank()) Text("Meta: ${goal.specific}")
                    Text("Fecha objetivo: ${goalDateLabel(goal)}")
                    Text(goal.displayProgress?.let { "Avance registrado: $it%" } ?: "Sin seguimiento registrado")
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedGoalId = null; onEdit(goal) }) {
                    Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Editar")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { selectedGoalId = null; onDelete(goal) }) {
                        Icon(Icons.Outlined.Delete, null, Modifier.size(18.dp), tint = AppError)
                        Text("Eliminar", color = AppError)
                    }
                    TextButton(onClick = { selectedGoalId = null }) { Text("Cerrar") }
                }
            }
        )
    }
}

@Composable
private fun GoalsPeriodPicker(selected: GoalsPeriod, onSelect: (GoalsPeriod) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(shape = RoundedCornerShape(8.dp), color = AppSurface, border = BorderStroke(1.dp, AppBorder)) {
            Row(Modifier.selectableGroup()) {
                GoalsPeriod.entries.filter { it != GoalsPeriod.ALL }.forEach { period ->
                    Box(
                        Modifier.selectable(selected == period, role = Role.Tab, onClick = { onSelect(period) })
                            .background(if (selected == period) AthleteNavigationBlue.copy(alpha = 0.12f) else Color.Transparent)
                            .heightIn(min = 48.dp).padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(period.label, color = AthleteNavigationBlue, fontSize = 10.sp,
                            fontWeight = if (selected == period) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
    if (selected == GoalsPeriod.ALL) {
        Text("Todos los objetivos", color = AppTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun GoalsStatusTabs(completed: Boolean, onSelect: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().selectableGroup()) {
        listOf(false to "Objetivos activos", true to "Completados").forEach { (value, title) ->
            Column(
                Modifier.weight(1f).selectable(completed == value, role = Role.Tab, onClick = { onSelect(value) }),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.heightIn(min = 48.dp).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                    Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppTextPrimary)
                }
                Box(Modifier.fillMaxWidth(0.86f).height(2.dp)
                    .background(if (completed == value) AthleteNavigationBlue else Color.Transparent))
            }
        }
    }
}

@Composable
private fun GoalsComplianceCard(dashboard: GoalsDashboard) {
    val progress by animateFloatAsState((dashboard.averagePercent ?: 0) / 100f, tween(350), label = "goalsCompliance")
    Surface(color = GoalsSummaryBackground, shape = RoundedCornerShape(6.dp), shadowElevation = 2.dp) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Cumplimiento", color = Color.White, fontSize = 11.sp)
                    Text(dashboard.averagePercent?.let { "$it%" } ?: "—", color = Color.White,
                        fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { progress }, modifier = Modifier.width(100.dp).height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFFAFDFFF), trackColor = Color.White.copy(alpha = 0.25f)
                    )
                }
                Row(
                    Modifier.weight(1.3f).height(66.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    dashboard.months.forEach { month ->
                        Column(Modifier.weight(1f).semantics {
                            contentDescription = "${month.label}: ${month.percent?.let { "$it por ciento" } ?: "sin seguimiento"}"
                        }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.height(44.dp), contentAlignment = Alignment.BottomCenter) {
                                if (month.percent != null) {
                                    Box(Modifier.width(8.dp).height((month.percent * 0.44f).coerceAtLeast(2f).dp)
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(Color(0xFFAFDFFF)))
                                } else Text("—", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                            Text(month.label, color = Color.White, fontSize = 9.sp)
                        }
                    }
                }
            }
            Text(
                if (dashboard.trackedCount == 0) "Sin seguimiento registrado"
                else "Promedio de ${dashboard.trackedCount}/${dashboard.totalCount} objetivos · por fecha objetivo",
                color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun GoalProgressCard(goal: SportGoal, onClick: () -> Unit) {
    val percent = goal.displayProgress
    val progress by animateFloatAsState((percent ?: 0) / 100f, tween(350), label = "goalProgress")
    val accent = when {
        percent == null -> AppTextSecondary
        percent >= 100 -> AthleteNavigationBlue
        percent >= 80 -> Color(0xFF91BF24)
        percent >= 50 -> AthleteNavigationBlue
        else -> Color(0xFFD568DC)
    }
    val icon = when {
        goal.isDuration -> Icons.Outlined.Timer
        goal.sport.equals("Atletismo", true) -> Icons.AutoMirrored.Outlined.DirectionsRun
        goal.sport.contains("fuerza", true) -> Icons.Outlined.FitnessCenter
        else -> Icons.Outlined.Flag
    }
    Surface(onClick = onClick, shape = RoundedCornerShape(8.dp), color = AppSurface,
        border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.7f)), shadowElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = AthleteNavigationBlue, modifier = Modifier.size(22.dp).align(Alignment.Top))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(goal.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(when (goal.assignedByTrainer) {
                    true -> "Asignado por el entrenador"
                    false -> "Objetivo personal"
                    null -> goal.generalGoal.ifBlank { "Objetivo deportivo" }
                }, color = AppTextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    Icon(Icons.Outlined.CalendarMonth, null, tint = AppTextSecondary, modifier = Modifier.size(16.dp))
                    Text(goalDateLabel(goal), color = AppTextSecondary, fontSize = 11.sp)
                }
            }
            Column(Modifier.width(68.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(44.dp).semantics {
                    contentDescription = percent?.let { "Avance: $it por ciento" } ?: "Sin seguimiento"
                }, contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(),
                        color = accent, trackColor = accent.copy(alpha = 0.16f), strokeWidth = 2.dp)
                    Text(percent?.let { "$it%" } ?: "—", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text(if (percent == null) "Sin seguimiento" else goal.specific.ifBlank { "Avance" },
                    color = AppTextSecondary, fontSize = 10.sp, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun goalDateLabel(goal: SportGoal): String = goalDueDate(goal)
    ?.let { SimpleDateFormat("dd MMM yyyy", Locale("es", "CO")).format(it) } ?: "Sin fecha"
