package com.example.mvt.goals.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.goals.model.GoalDraft
import com.example.mvt.goals.model.GoalsScreenState
import com.example.mvt.goals.model.SportGoal
import com.example.mvt.goals.viewmodel.GoalsViewModel
import com.example.mvt.ui.components.FormSuccessNotification
import com.example.mvt.ui.components.FormConfirmationDialog
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

private data class GoalForm(
    val name: String = "",
    val date: String = "",
    val sport: String = "",
    val general: String = "",
    val description: String = "",
    val specific: String = "",
    val hours: String = "",
    val minutes: String = "",
    val seconds: String = ""
)

private val GoalDangerRed = Color(0xFFFF5A67)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    athleteId: String,
    viewModel: GoalsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val generalGoals by viewModel.generalGoals.collectAsState()
    val action by viewModel.action.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var showForm by remember { mutableStateOf(false) }
    var form by remember { mutableStateOf(GoalForm()) }
    var deleteGoal by remember { mutableStateOf<SportGoal?>(null) }
    var editingGoal by remember { mutableStateOf<SportGoal?>(null) }
    var addingGoal by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(athleteId) { viewModel.load(athleteId) }
    LaunchedEffect(action.message) {
        val message = action.message ?: return@LaunchedEffect
        if (action.isError && showForm) return@LaunchedEffect
        if (!action.isError) {
            if (addingGoal) {
                showForm = false
                form = GoalForm()
                editingGoal = null
                addingGoal = false
            }
            successMessage = message
            viewModel.clearAction()
            return@LaunchedEffect
        }
        snackbar.showSnackbar(message)
        viewModel.clearAction()
    }

    Scaffold(
        containerColor = AppSurface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        when (val current = state) {
            GoalsScreenState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
            is GoalsScreenState.Error -> GoalsError(current.message, onBack, { viewModel.load(athleteId) }, Modifier.padding(padding))
            is GoalsScreenState.Ready -> GoalsDashboardContent(
                data = current.data,
                onNewGoal = {
                    if (editingGoal != null) form = GoalForm()
                    editingGoal = null
                    showForm = true
                },
                onEdit = { goal ->
                    editingGoal = goal
                    form = goal.toForm()
                    viewModel.selectSport(goal.sport)
                    showForm = true
                },
                onDelete = { deleteGoal = it },
                modifier = Modifier.padding(padding)
            )
        }
    }

    val ready = state as? GoalsScreenState.Ready
    if (showForm && ready != null) {
        GoalFormSheet(
            form = form,
            sports = ready.data.sports,
            generalGoals = generalGoals,
            isEditing = editingGoal != null,
            errorMessage = action.message.takeIf { action.isError },
            busy = action.isWorking,
            onFormChange = { updated ->
                if (action.isError) viewModel.clearAction()
                if (updated.sport != form.sport) viewModel.selectSport(updated.sport)
                form = updated
            },
            onDismiss = {
                if (!action.isWorking) {
                    showForm = false
                    if (action.isError) viewModel.clearAction()
                }
            },
            onSave = {
                addingGoal = true
                val goal = editingGoal
                if (goal == null) viewModel.addGoal(form.toDraft())
                else viewModel.updateGoal(goal, form.toDraft())
            }
        )
    }
    deleteGoal?.let { goal ->
        FormConfirmationDialog(
            title = "Eliminar objetivo",
            message = "¿Estás seguro? Esta acción eliminará el objetivo de forma permanente y no se puede revertir.",
            confirmLabel = "Eliminar",
            destructive = true,
            enabled = !action.isWorking,
            onDismiss = { deleteGoal = null },
            onConfirm = { viewModel.deleteGoal(goal); deleteGoal = null }
        )
    }
    successMessage?.let { message ->
        FormSuccessNotification(
            message = message,
            onDismiss = { successMessage = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalFormSheet(
    form: GoalForm,
    sports: List<String>,
    generalGoals: List<String>,
    isEditing: Boolean,
    errorMessage: String?,
    busy: Boolean,
    onFormChange: (GoalForm) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDatePicker by remember { mutableStateOf(false) }
    val duration = isDurationSport(form.sport)
    val dateIsValid = form.date.isNotBlank() && form.date >= todayDate()
    val valid = form.name.trim().length >= 3 && dateIsValid && form.sport.isNotBlank() &&
        form.general.isNotBlank() && if (duration) validDuration(form) else form.specific.trim().length >= 3

    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState, containerColor = AppBackground,
        dragHandle = { Box(Modifier.padding(vertical = 10.dp).size(44.dp, 4.dp).clip(CircleShape).background(AppBorder)) }
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.94f)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (isEditing) "Editar objetivo" else "Nuevo objetivo", color = AppTextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text(if (isEditing) "Actualiza los datos de tu meta" else "Define una meta clara y medible", color = AppTextSecondary, fontSize = 12.sp)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cerrar", tint = AppTextPrimary) }
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(8.dp, 10.dp, 8.dp, 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { GoalTextField(form.name, { onFormChange(form.copy(name = it)) }, "Nombre del objetivo", "Ej. Mi primera 10K") }
                item {
                    Box(Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                        OutlinedTextField(
                            value = displayDate(form.date), onValueChange = {}, readOnly = true,
                            label = { Text("Fecha objetivo") }, trailingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                            modifier = Modifier.fillMaxWidth(), enabled = false,
                            colors = disabledClickableFieldColors(), shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
                item { GoalDropdown("Deporte", form.sport, sports, { onFormChange(form.copy(sport = it, general = "", specific = "", hours = "", minutes = "", seconds = "")) }) }
                item { GoalDropdown("Objetivo general", form.general, generalGoals, { onFormChange(form.copy(general = it)) }, enabled = form.sport.isNotBlank()) }
                item { GoalTextField(form.description, { if (it.length <= 500) onFormChange(form.copy(description = it)) }, "Descripción opcional", "Cuéntanos más sobre tu objetivo", minLines = 3) }
                if (duration) item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Marca objetivo", color = AppTextPrimary, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DurationField(form.hours, { onFormChange(form.copy(hours = digits(it, 3))) }, "Horas", Modifier.weight(1f))
                            DurationField(form.minutes, { onFormChange(form.copy(minutes = digits(it, 2))) }, "Min", Modifier.weight(1f))
                            DurationField(form.seconds, { onFormChange(form.copy(seconds = digits(it, 2))) }, "Seg", Modifier.weight(1f))
                        }
                    }
                } else if (form.sport.isNotBlank()) item {
                    GoalTextField(form.specific, { if (it.length <= 300) onFormChange(form.copy(specific = it)) }, "Objetivo específico", "¿Nos cuentas un poco más sobre tu objetivo?", minLines = 3)
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (form.date.isNotBlank() && !dateIsValid) {
                    FormInlineError("La fecha objetivo no puede estar en el pasado.")
                }
                errorMessage?.let { FormInlineError(it) }
                Button(
                    onClick = onSave, enabled = valid && !busy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    else { Icon(Icons.Default.Save, null); Spacer(Modifier.size(8.dp)); Text(if (isEditing) "Actualizar objetivo" else "Guardar objetivo") }
                }
            }
        }
    }

    if (showDatePicker) GoalDatePicker(form.date, { onFormChange(form.copy(date = it)); showDatePicker = false }, { showDatePicker = false })
}

@Composable
private fun GoalTextField(value: String, onChange: (String) -> Unit, label: String, placeholder: String, minLines: Int = 1) {
    OutlinedTextField(value, onChange, label = { Text(label) }, placeholder = { Text(placeholder) }, modifier = Modifier.fillMaxWidth(), minLines = minLines, shape = RoundedCornerShape(14.dp))
}

@Composable
private fun DurationField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier) {
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = modifier, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(14.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalDropdown(label: String, value: String, options: List<String>, onSelect: (String) -> Unit, enabled: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (enabled) expanded = !expanded }) {
        OutlinedTextField(
            value = value, onValueChange = {}, readOnly = true, enabled = enabled,
            label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(14.dp)
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            options.forEach { option -> androidx.compose.material3.DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalDatePicker(current: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val todayUtc = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    val selected = runCatching { LocalDate.parse(current).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = selected,
        selectableDates = object : SelectableDates { override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayUtc }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onSelect(SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(Date(it))) }
            }, enabled = state.selectedDateMillis != null) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        colors = DatePickerDefaults.colors(containerColor = AppSurface)
    ) { DatePicker(state = state) }
}

@Composable
private fun GoalsError(message: String, onBack: () -> Unit, onRetry: () -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(8.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = AppTextPrimary) }
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(message, color = AppTextSecondary, textAlign = TextAlign.Center)
            Button(onClick = onRetry) { Text("Reintentar") }
        }
    }
}

@Composable
private fun FormInlineError(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
        Text(message, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

private fun GoalForm.toDraft() = GoalDraft(name, sport, general, date, description, specific, hours.toIntOrNull(), minutes.toIntOrNull(), seconds.toIntOrNull())
private fun SportGoal.toForm(): GoalForm {
    val duration = isDurationSport(sport)
    val parts = specific.split(":")
    return GoalForm(
        name = name,
        date = targetDate,
        sport = sport,
        general = generalGoal,
        description = description,
        specific = if (duration) "" else specific,
        hours = (specificHours ?: parts.getOrNull(0)?.toIntOrNull())?.toString().orEmpty(),
        minutes = (specificMinutes ?: parts.getOrNull(1)?.toIntOrNull())?.toString().orEmpty(),
        seconds = (specificSeconds ?: parts.getOrNull(2)?.toIntOrNull())?.toString().orEmpty()
    )
}
private fun validDuration(form: GoalForm): Boolean {
    val h = form.hours.toIntOrNull() ?: return false; val m = form.minutes.toIntOrNull() ?: return false; val s = form.seconds.toIntOrNull() ?: return false
    return h >= 0 && m in 0..59 && s in 0..59 && h + m + s > 0
}
private fun isDurationSport(sport: String) = sport in setOf("Atletismo", "Ciclismo", "Natacion", "Natación", "Triatlon", "Triatlón")
private fun digits(value: String, max: Int) = value.filter(Char::isDigit).take(max)
private fun todayDate() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
private fun displayDate(value: String): String = runCatching {
    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value) ?: return@runCatching value
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(parsed)
}.getOrDefault(value)

@Composable
private fun disabledClickableFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    disabledTextColor = AppTextPrimary,
    disabledBorderColor = AppBorder,
    disabledLabelColor = AppTextSecondary,
    disabledTrailingIconColor = AppTextSecondary,
    disabledContainerColor = Color.Transparent
)
