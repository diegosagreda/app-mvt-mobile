package com.example.mvt.health.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonalInjury
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.health.model.HealthInjury
import com.example.mvt.health.model.HealthProfile
import com.example.mvt.health.viewmodel.HealthScreenState
import com.example.mvt.health.viewmodel.HealthViewModel
import com.example.mvt.ui.components.FormConfirmationDialog
import com.example.mvt.ui.components.FormErrorNotification
import com.example.mvt.ui.components.FormFieldShape
import com.example.mvt.ui.components.FormSuccessNotification
import com.example.mvt.ui.components.formFieldColors
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppPrimarySoft
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue

private val HealthDangerRed = Color(0xFFFF5A67)

@Composable
fun HealthScreen(
    athleteId: String,
    viewModel: HealthViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val action by viewModel.action.collectAsState()
    var showInjuryForm by remember { mutableStateOf(false) }
    var editingInjury by remember { mutableStateOf<HealthInjury?>(null) }
    var injuryToDelete by remember { mutableStateOf<HealthInjury?>(null) }
    var showDiscard by remember { mutableStateOf(false) }
    val ready = state as? HealthScreenState.Ready

    LaunchedEffect(athleteId) { viewModel.load(athleteId) }
    BackHandler(enabled = ready?.hasUnsavedChanges == true) { showDiscard = true }

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (ready?.hasUnsavedChanges == true) {
                HealthSaveBar(action.isSaving, viewModel::save)
            }
        }
    ) { padding ->
        when (val current = state) {
            HealthScreenState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
            is HealthScreenState.Error -> HealthError(current.message, onBack, Modifier.padding(padding))
            is HealthScreenState.Ready -> HealthContent(
                state = current,
                onBack = { if (current.hasUnsavedChanges) showDiscard = true else onBack() },
                onUpdate = viewModel::updateHealth,
                onAddInjury = {
                    editingInjury = null
                    showInjuryForm = true
                },
                onEditInjury = { injury ->
                    editingInjury = injury
                    showInjuryForm = true
                },
                onDeleteInjury = { injuryToDelete = it },
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (showInjuryForm) {
        InjuryFormSheet(
            injury = editingInjury,
            onDismiss = {
                showInjuryForm = false
                editingInjury = null
            },
            onSave = { location, treatment, active ->
                val injury = editingInjury
                if (injury == null) viewModel.addInjury(location, treatment, active)
                else viewModel.updateInjury(injury.id, location, treatment, active)
                showInjuryForm = false
                editingInjury = null
            }
        )
    }
    injuryToDelete?.let { injury ->
        FormConfirmationDialog(
            title = "Eliminar lesión",
            message = "La lesión se eliminará inmediatamente de tu información de salud. Esta acción no se puede revertir.",
            confirmLabel = "Eliminar",
            destructive = true,
            onDismiss = { injuryToDelete = null },
            onConfirm = {
                viewModel.removeInjury(injury.id)
                injuryToDelete = null
            }
        )
    }
    if (showDiscard) {
        FormConfirmationDialog(
            title = "Cambios sin guardar",
            message = "Si sales ahora, perderás las modificaciones realizadas en tu información de salud.",
            confirmLabel = "Salir",
            onDismiss = { showDiscard = false },
            onConfirm = {
                viewModel.discardChanges()
                showDiscard = false
                onBack()
            }
        )
    }
    action.message?.let { message ->
        if (action.isError) FormErrorNotification(message, viewModel::clearAction)
        else FormSuccessNotification(message, viewModel::clearAction)
    }
}

@Composable
private fun HealthContent(
    state: HealthScreenState.Ready,
    onBack: () -> Unit,
    onUpdate: ((HealthProfile) -> HealthProfile) -> Unit,
    onAddInjury: () -> Unit,
    onEditInjury: (HealthInjury) -> Unit,
    onDeleteInjury: (HealthInjury) -> Unit,
    modifier: Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { HealthHeader(onBack) }
        state.catalogError?.let { item { HealthInfoBanner(it, onDismiss = null, warning = true) } }
        item {
            HealthSection("Información general", Icons.Default.HealthAndSafety) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HealthDropdown(
                        label = "Esfuerzo laboral",
                        value = state.health.workPhysicalEffort?.toString().orEmpty(),
                        options = (1..10).map(Int::toString),
                        onSelect = { value -> onUpdate { it.copy(workPhysicalEffort = value.toIntOrNull()) } },
                        modifier = Modifier.weight(1f)
                    )
                    HealthDropdown(
                        label = "Horas de sueño",
                        value = state.health.sleepHours?.toString().orEmpty(),
                        options = (1..10).map(Int::toString),
                        onSelect = { value -> onUpdate { it.copy(sleepHours = value.toIntOrNull()) } },
                        modifier = Modifier.weight(1f)
                    )
                }
                HealthDropdown(
                    label = "Estado nutricional",
                    value = state.health.nutritionalStatus,
                    options = state.nutritionalOptions,
                    onSelect = { value -> onUpdate { it.copy(nutritionalStatus = value) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Escalas de 1 a 10 para esfuerzo físico diario y horas habituales de sueño.", color = AppTextSecondary, fontSize = 11.sp)
            }
        }
        item {
            HealthSection("Condiciones de salud", Icons.Default.Healing) {
                Text(
                    "Describe cualquier condición médica relevante para que tu entrenador pueda adaptar las rutinas de forma segura.",
                    color = AppTextSecondary, fontSize = 12.sp, lineHeight = 18.sp
                )
                OutlinedTextField(
                    value = state.health.healthCondition,
                    onValueChange = { value -> if (value.length <= 1000) onUpdate { it.copy(healthCondition = value) } },
                    modifier = Modifier.fillMaxWidth(), minLines = 4,
                    label = { Text("Descripción") },
                    placeholder = { Text("Ej. Asma leve controlada, uso de inhalador...") },
                    supportingText = { Text("${state.health.healthCondition.length}/1000") },
                    shape = FormFieldShape,
                    colors = formFieldColors()
                )
            }
        }
        item {
            HealthSection(
                title = "Lesiones",
                icon = Icons.Default.PersonalInjury,
                headerAction = {
                    Button(
                        onClick = onAddInjury,
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Agregar", maxLines = 1, fontSize = 11.sp)
                    }
                }
            ) {
                Text(
                    text = "Registrar tus lesiones activas o anteriores permite que tu entrenador adapte las rutinas, evite ejercicios que puedan agravarlas y planifique tu entrenamiento de forma más segura.",
                    color = AppTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Justify
                )
                if (state.health.injuries.isEmpty()) {
                    Text("No hay lesiones registradas.", color = AppTextSecondary, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), textAlign = TextAlign.Center)
                } else {
                    state.health.injuries.forEach { injury ->
                        InjuryCard(injury, onEditInjury, onDeleteInjury)
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthSection(
    title: String,
    icon: ImageVector,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    Card(colors = CardDefaults.cardColors(containerColor = AppSurface), border = BorderStroke(1.dp, AppBorder), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(AppPrimarySoft), contentAlignment = Alignment.Center) { Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(19.dp)) }
                Text(title, color = AppTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ExpandMore, null, tint = AppTextSecondary, modifier = Modifier.rotate(if (expanded) 180f else 0f))
                headerAction?.invoke()
            }
            AnimatedVisibility(expanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(color = AppBorder); content()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HealthDropdown(label: String, value: String, options: List<String>, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = !expanded }, modifier) {
        OutlinedTextField(
            value = value, onValueChange = {}, readOnly = true,
            label = { Text(label, maxLines = 1) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = FormFieldShape,
            colors = formFieldColors()
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false }) }
        }
    }
}

@Composable
private fun InjuryCard(
    injury: HealthInjury,
    onEdit: (HealthInjury) -> Unit,
    onDelete: (HealthInjury) -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    var treatmentExpanded by remember(injury.id) { mutableStateOf(false) }
    var treatmentOverflows by remember(injury.id) { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = AppSurfaceAlt), border = BorderStroke(1.dp, AppBorder), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(injury.location, color = AppTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.MoreVert, "Opciones", tint = AppTextSecondary) }
                    DropdownMenu(menu, { menu = false }, containerColor = AppSurface) {
                        DropdownMenuItem(
                            text = { Text("Editar", color = AppTextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Edit, null, tint = PrimaryBlue) },
                            onClick = { menu = false; onEdit(injury) }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar", color = HealthDangerRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = HealthDangerRed) },
                            onClick = { menu = false; onDelete(injury) }
                        )
                    }
                }
            }
            HorizontalDivider(color = AppBorder)
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                    .background(if (injury.currentlyActive) Color(0x22FFA24C) else AppBackground.copy(alpha = 0.4f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (injury.currentlyActive) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (injury.currentlyActive) Color(0xFFFFA24C) else AppTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    "Actualmente la tiene: ${if (injury.currentlyActive) "Sí" else "No"}",
                    color = AppTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text("Tratamiento", color = AppTextSecondary, fontSize = 10.sp)
            Text(
                injury.treatment,
                color = AppTextPrimary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                maxLines = if (treatmentExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { result ->
                    if (!treatmentExpanded) treatmentOverflows = result.hasVisualOverflow
                }
            )
            if (treatmentOverflows || treatmentExpanded) {
                Text(
                    if (treatmentExpanded) "Ver menos" else "Ver más",
                    color = PrimaryBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { treatmentExpanded = !treatmentExpanded }
                        .padding(vertical = 2.dp)
                )
            }
            if (injury.startDate.isNotBlank() || injury.duration.isNotBlank()) {
                Text(listOf(injury.startDate, injury.duration).filter(String::isNotBlank).joinToString(" · "), color = AppTextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InjuryFormSheet(
    injury: HealthInjury?,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var location by remember(injury?.id) { mutableStateOf(injury?.location.orEmpty()) }
    var treatment by remember(injury?.id) { mutableStateOf(injury?.treatment.orEmpty()) }
    var active by remember(injury?.id) { mutableStateOf(injury?.currentlyActive) }
    val valid = location.trim().length >= 2 && treatment.trim().length >= 2 && active != null
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = AppBackground) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.78f)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (injury == null) "Agregar lesión" else "Editar lesión", color = AppTextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text(if (injury == null) "Completa la información básica" else "Actualiza la información registrada", color = AppTextSecondary, fontSize = 12.sp)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cerrar", tint = AppTextPrimary) }
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(8.dp, 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    OutlinedTextField(location, { if (it.length <= 120) location = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Localización") }, placeholder = { Text("Rodilla derecha, tobillo, hombro...") }, shape = FormFieldShape, colors = formFieldColors())
                }
                item {
                    OutlinedTextField(treatment, { if (it.length <= 300) treatment = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Tratamiento") }, placeholder = { Text("Fisioterapia, reposo, medicación...") }, minLines = 3, shape = FormFieldShape, colors = formFieldColors())
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("¿Actualmente tienes esta lesión?", color = AppTextPrimary, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(true to "Sí", false to "No").forEach { (value, label) ->
                                OutlinedButton(
                                    onClick = { active = value }, modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (active == value) AppPrimarySoft else Color.Transparent),
                                    border = BorderStroke(1.dp, if (active == value) PrimaryBlue else AppBorder)
                                ) { Text(label, color = if (active == value) PrimaryBlue else AppTextSecondary) }
                            }
                        }
                    }
                }
            }
            Button(
                onClick = { onSave(location, treatment, active == true) }, enabled = valid,
                modifier = Modifier.fillMaxWidth().padding(12.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Icon(if (injury == null) Icons.Default.Add else Icons.Default.Save, null)
                Spacer(Modifier.size(8.dp))
                Text(if (injury == null) "Agregar lesión" else "Actualizar lesión")
            }
        }
    }
}

@Composable
private fun HealthSaveBar(saving: Boolean, onSave: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = AppSurface), shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)) {
        Button(
            onClick = onSave, enabled = !saving, modifier = Modifier.fillMaxWidth().padding(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            if (saving) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            else { Icon(Icons.Default.Save, null); Spacer(Modifier.size(8.dp)); Text("Actualizar información") }
        }
    }
}

@Composable
private fun HealthInfoBanner(message: String, onDismiss: (() -> Unit)?, warning: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(if (warning) Color(0x22FFA24C) else AppPrimarySoft).padding(11.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Info, null, tint = if (warning) Color(0xFFFFA24C) else PrimaryBlue, modifier = Modifier.size(19.dp))
        Text(message, color = AppTextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
        onDismiss?.let { IconButton(onClick = it, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Close, "Cerrar", tint = AppTextSecondary, modifier = Modifier.size(16.dp)) } }
    }
}

@Composable
private fun HealthHeader(onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = AppTextPrimary) }
        Text("Información de salud", color = AppTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HealthError(message: String, onBack: () -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(8.dp)) {
        HealthHeader(onBack)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(message, color = AppTextSecondary, textAlign = TextAlign.Center) }
    }
}
