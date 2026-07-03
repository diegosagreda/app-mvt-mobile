package com.example.mvt.goals.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
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
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.goals.model.GoalDraft
import com.example.mvt.goals.model.GoalsData
import com.example.mvt.goals.model.GoalsScreenState
import com.example.mvt.goals.model.SportGoal
import com.example.mvt.goals.viewmodel.GoalsViewModel
import com.example.mvt.ui.components.FormSuccessNotification
import com.example.mvt.ui.components.FormConfirmationDialog
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppPrimarySoft
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.text.Normalizer
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
    var searchText by remember { mutableStateOf("") }

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
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        when (val current = state) {
            GoalsScreenState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
            is GoalsScreenState.Error -> GoalsError(current.message, onBack, Modifier.padding(padding))
            is GoalsScreenState.Ready -> GoalsContent(
                data = current.data,
                searchText = searchText,
                onBack = onBack,
                onSearchChange = { searchText = it },
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

@Composable
private fun GoalsContent(
    data: GoalsData,
    searchText: String,
    onBack: () -> Unit,
    onSearchChange: (String) -> Unit,
    onNewGoal: () -> Unit,
    onEdit: (SportGoal) -> Unit,
    onDelete: (SportGoal) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredGoals = remember(data.goals, searchText) {
        val term = normalizeGoalSearch(searchText)
        data.goals.filter { term.isBlank() || normalizeGoalSearch(it.name).contains(term) }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { GoalsHeader(onBack) }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f).height(50.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(AppBackground.copy(alpha = 0.35f))
                            .border(1.dp, AppBorder, RoundedCornerShape(13.dp))
                            .padding(horizontal = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Search, null, tint = AppTextSecondary, modifier = Modifier.size(17.dp))
                        BasicTextField(
                            value = searchText,
                            onValueChange = onSearchChange,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = AppTextPrimary,
                                fontSize = 12.sp
                            ),
                            cursorBrush = SolidColor(PrimaryBlue),
                            decorationBox = { innerField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (searchText.isBlank()) {
                                        Text("Buscar objetivo", color = AppTextSecondary, fontSize = 11.sp, maxLines = 1)
                                    }
                                    innerField()
                                }
                            }
                        )
                    }
                    Button(
                        onClick = onNewGoal,
                        modifier = Modifier.height(50.dp),
                        contentPadding = PaddingValues(horizontal = 13.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Box(
                            Modifier.size(24.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)) }
                        Spacer(Modifier.size(7.dp))
                        Text("Nuevo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (filteredGoals.isEmpty()) {
                    EmptyGoals(if (searchText.isBlank()) "No hay objetivos registrados." else "No se encontraron objetivos con ese nombre.")
                } else {
                    filteredGoals.forEach { GoalCard(it, onEdit, onDelete) }
                }
            }
        }
    }
}

@Composable
private fun GoalsHeader(onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = AppTextPrimary) }
        Column {
            Text("Objetivos deportivos", color = AppTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GoalCard(goal: SportGoal, onEdit: (SportGoal) -> Unit, onDelete: (SportGoal) -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    var descriptionExpanded by remember(goal.id) { mutableStateOf(false) }
    var descriptionOverflows by remember(goal.id) { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = AppSurfaceAlt),
        border = BorderStroke(1.dp, AppBorder),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GoalDateBadge(goal.targetDate)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        goal.name,
                        color = AppTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth().basicMarquee()
                    )
                    if (goal.generalGoal.isNotBlank()) {
                        Text(
                            goal.generalGoal,
                            color = AppTextSecondary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth().basicMarquee()
                        )
                    }
                }
                Column(Modifier.width(88.dp), horizontalAlignment = Alignment.End) {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, "Opciones", tint = AppTextSecondary)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            containerColor = AppSurface
                        ) {
                            DropdownMenuItem(
                                text = { Text("Editar", color = AppTextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Edit, null, tint = PrimaryBlue) },
                                onClick = {
                                    menuExpanded = false
                                    onEdit(goal)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar", color = GoalDangerRed) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = GoalDangerRed) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete(goal)
                                }
                            )
                        }
                    }
                    if (goal.sport.isNotBlank()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50))
                                .background(PrimaryBlue).padding(horizontal = 9.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                goal.sport,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth().basicMarquee(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = AppBorder)
            if (goal.specific.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(AppBackground.copy(alpha = 0.45f)).padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(30.dp).clip(CircleShape).background(AppPrimarySoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Flag, null, tint = PrimaryBlue, modifier = Modifier.size(17.dp))
                    }
                    Column {
                        Text("Marca / objetivo específico", color = AppTextSecondary, fontSize = 10.sp)
                        Text(goal.specific, color = AppTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (goal.description.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        goal.description,
                        color = AppTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        maxLines = if (descriptionExpanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { result ->
                            if (!descriptionExpanded) descriptionOverflows = result.hasVisualOverflow
                        }
                    )
                    if (descriptionOverflows || descriptionExpanded) {
                        Text(
                            if (descriptionExpanded) "Ver menos" else "Ver más",
                            color = PrimaryBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { descriptionExpanded = !descriptionExpanded }
                                .padding(vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalDateBadge(targetDate: String) {
    val date = runCatching { LocalDate.parse(targetDate) }.getOrNull()
    val day = date?.dayOfMonth?.toString()?.padStart(2, '0') ?: "--"
    val month = date?.month?.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
        ?.replaceFirstChar { it.uppercase() } ?: "---"
    Column(
        modifier = Modifier.width(58.dp).clip(RoundedCornerShape(14.dp))
            .background(PrimaryBlue.copy(alpha = 0.14f)).padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(day, color = AppTextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text(month.take(3), color = PrimaryBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyGoals(message: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Flag, null, tint = AppTextSecondary, modifier = Modifier.size(38.dp))
        Text(message, color = AppTextSecondary, textAlign = TextAlign.Center)
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
private fun GoalsError(message: String, onBack: () -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(8.dp)) {
        GoalsHeader(onBack)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(message, color = AppTextSecondary, textAlign = TextAlign.Center) }
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
private fun normalizeGoalSearch(value: String): String = Normalizer
    .normalize(value.lowercase().trim(), Normalizer.Form.NFD)
    .replace("\\p{M}+".toRegex(), "")
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
