package com.example.mvt.availability.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.availability.viewmodel.AvailabilityScreenState
import com.example.mvt.availability.viewmodel.AvailabilityViewModel
import com.example.mvt.goals.model.TrainingAvailability
import com.example.mvt.ui.components.FormErrorNotification
import com.example.mvt.ui.components.FormSuccessNotification
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppPrimarySoft
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue

private val weekDays = listOf(
    "lunes" to "Lunes", "martes" to "Martes", "miercoles" to "Miércoles",
    "jueves" to "Jueves", "viernes" to "Viernes", "sabado" to "Sábado",
    "domingo" to "Domingo"
)

@Composable
fun AvailabilityScreen(
    athleteId: String,
    viewModel: AvailabilityViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val action by viewModel.action.collectAsState()
    var selected by remember { mutableStateOf(TrainingAvailability()) }
    var dirty by remember { mutableStateOf(false) }

    LaunchedEffect(athleteId) { viewModel.load(athleteId) }
    LaunchedEffect((state as? AvailabilityScreenState.Ready)?.availability) {
        val loaded = (state as? AvailabilityScreenState.Ready)?.availability ?: return@LaunchedEffect
        selected = loaded
        dirty = false
    }

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        when (val current = state) {
            AvailabilityScreenState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
            is AvailabilityScreenState.Error -> AvailabilityError(current.message, onBack, Modifier.padding(padding))
            is AvailabilityScreenState.Ready -> AvailabilityContent(
                availability = selected,
                planningDays = current.plan.planningDays,
                isBronze = current.plan.isBronze,
                dirty = dirty,
                busy = action.isWorking,
                onBack = onBack,
                onToggle = { key ->
                    val alreadySelected = selected.asMap()[key] == true
                    if (alreadySelected || current.plan.planningDays <= 0 || selected.selectedCount < current.plan.planningDays) {
                        selected = selected.toggle(key)
                        dirty = selected != current.availability
                    }
                },
                onSave = { viewModel.save(selected) },
                modifier = Modifier.padding(padding)
            )
        }
    }

    action.message?.let { message ->
        if (action.isError) {
            FormErrorNotification(message, viewModel::clearAction)
        } else {
            FormSuccessNotification(message, viewModel::clearAction)
        }
    }
}

@Composable
private fun AvailabilityContent(
    availability: TrainingAvailability,
    planningDays: Int,
    isBronze: Boolean,
    dirty: Boolean,
    busy: Boolean,
    onBack: () -> Unit,
    onToggle: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { AvailabilityHeader(onBack) }
        item {
            AvailabilityCard {
                if (isBronze) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(AppPrimarySoft), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Lock, null, tint = PrimaryBlue)
                        }
                        Text("Disponibilidad no incluida", color = AppTextPrimary, fontWeight = FontWeight.Bold)
                        Text("Tu plan Bronce no requiere configurar días de entrenamiento.", color = AppTextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(AppPrimarySoft).padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            Modifier.size(36.dp).clip(RoundedCornerShape(11.dp))
                                .background(PrimaryBlue.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CalendarMonth, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Organiza tu entrenamiento", color = AppTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Selecciona los días en los que tienes disponibilidad para entrenar. Tu entrenador utilizará esta información para programar tus rutinas de forma personalizada, respetando tu agenda y distribuyendo adecuadamente las cargas de entrenamiento.",
                                color = AppTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                    Text("Tu plan requiere seleccionar $planningDays días.", color = AppTextSecondary, fontSize = 13.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        weekDays.chunked(2).forEach { rowDays ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowDays.forEach { (key, label) ->
                                    val active = availability.asMap()[key] == true
                                    val enabled = active || availability.selectedCount < planningDays
                                    AvailabilityDayOption(
                                        name = label,
                                        selected = active,
                                        enabled = enabled,
                                        onClick = { onToggle(key) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowDays.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "${availability.selectedCount} de $planningDays días seleccionados",
                            color = if (availability.selectedCount == planningDays) PrimaryBlue else AppTextSecondary,
                            fontSize = 12.sp
                        )
                        if (availability.selectedCount == planningDays) Icon(Icons.Default.Check, null, tint = PrimaryBlue)
                    }
                    Button(
                        onClick = onSave,
                        enabled = dirty && availability.selectedCount == planningDays && !busy,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        else { Icon(Icons.Default.Save, null); Spacer(Modifier.size(8.dp)); Text("Guardar disponibilidad") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AvailabilityDayOption(
    name: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.height(58.dp).alpha(if (enabled) 1f else 0.48f)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) PrimaryBlue else AppSurfaceAlt)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(
            imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) Color.White else AppTextSecondary,
            modifier = Modifier.size(21.dp)
        )
        Column {
            Text(name, color = if (selected) Color.White else AppTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(if (selected) "Disponible" else "No disponible", color = if (selected) Color.White.copy(alpha = 0.78f) else AppTextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun AvailabilityHeader(onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = AppTextPrimary) }
        Column {
            Text("Disponibilidad", color = AppTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Días disponibles para entrenamiento", color = AppTextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun AvailabilityCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
    }
}

@Composable
private fun AvailabilityError(message: String, onBack: () -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(8.dp)) {
        AvailabilityHeader(onBack)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(message, color = AppTextSecondary, textAlign = TextAlign.Center)
        }
    }
}
