@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.mvt.welcome.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.mvt.R
import com.example.mvt.data.firebase.models.Routine
import com.example.mvt.goals.model.SportGoal
import com.example.mvt.trainer.ui.CompactDetailGrid
import com.example.mvt.trainer.ui.Detail
import com.example.mvt.trainer.ui.ExpandableDetail
import com.example.mvt.trainer.ui.InlineDetail
import com.example.mvt.trainer.ui.SectionCard
import com.example.mvt.ui.components.EmptyRoutineDayCard
import com.example.mvt.ui.components.FormErrorNotification
import com.example.mvt.ui.components.FormFieldShape
import com.example.mvt.ui.components.FormLegendLabel
import com.example.mvt.ui.components.FormTooltip
import com.example.mvt.ui.components.RoutineWeekCalendar
import com.example.mvt.ui.components.RoutineWeekDay
import com.example.mvt.ui.components.SharedRoutineCard
import com.example.mvt.ui.components.formFieldColors
import com.example.mvt.ui.screens.RoutineDetailScreen
import com.example.mvt.ui.theme.*
import com.example.mvt.welcome.model.*
import com.example.mvt.welcome.viewmodel.WelcomeViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.text.NumberFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

private val days = linkedMapOf("lunes" to "L", "martes" to "M", "miercoles" to "X", "jueves" to "J", "viernes" to "V", "sabado" to "S", "domingo" to "D")
private val subjectiveLevels = listOf("Básico" to 1, "Intermedio" to 5, "Avanzado" to 8)

@Composable
fun WelcomeScreen(uid: String, viewModel: WelcomeViewModel, onCompleted: () -> Unit) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(uid) { viewModel.start(uid) }
    LaunchedEffect(state) {
        val ready = state as? WelcomeUiState.Ready
        if (ready?.snapshot?.step == WelcomeStep.COMPLETED) onCompleted()
    }
    Surface(Modifier.fillMaxSize(), color = AppBackground) {
        when (val current = state) {
            WelcomeUiState.Loading -> LoadingState()
            is WelcomeUiState.Error -> ErrorState(current.message, viewModel::retry)
            is WelcomeUiState.Ready -> WelcomeContent(uid, current, viewModel)
        }
    }
}

@Composable
private fun WelcomeContent(uid: String, state: WelcomeUiState.Ready, viewModel: WelcomeViewModel) {
    var showStartScreen by remember(uid) {
        mutableStateOf(
            state.snapshot.step == WelcomeStep.PERSONAL_DATA && !viewModel.hasStartedWelcomeSession
        )
    }
    if (showStartScreen) {
        WelcomeStartScreen(
            listOf(state.personal.firstName, state.personal.lastName)
                .filter(String::isNotBlank)
                .joinToString(" ")
        ) {
            viewModel.markWelcomeSessionStarted()
            showStartScreen = false
        }
        return
    }

    val listState = rememberLazyListState()
    var profileStage by rememberSaveable { mutableIntStateOf(0) }
    var showGoalForm by rememberSaveable { mutableStateOf(false) }
    var editingGoalId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDeleteGoal by remember { mutableStateOf<SportGoal?>(null) }
    val selectedPlan = state.snapshot.plans.firstOrNull { it.id == state.selectedPlanId }
    val paidPlanSelected = state.snapshot.step == WelcomeStep.PLAN && selectedPlan?.price?.let { it > 0 } == true
    val isBronzeFinalStep = state.snapshot.step == WelcomeStep.FINAL_SELECTION &&
        state.snapshot.planName.contains("bronce", ignoreCase = true)
    val detailFreePlan = if (isBronzeFinalStep) {
        state.snapshot.freePlans.firstOrNull { it.id == state.previewFreePlanId }
    } else {
        null
    }
    val isFreePlanDetailOpen = detailFreePlan != null
    var finalStepIntroDismissed by rememberSaveable { mutableStateOf(false) }
    val showFinalStepIntro = isBronzeFinalStep && !finalStepIntroDismissed

    LaunchedEffect(isBronzeFinalStep) {
        if (!isBronzeFinalStep) finalStepIntroDismissed = false
    }

    LaunchedEffect(state.validation.errors) {
        if (state.validation.isValid) return@LaunchedEffect
        val keys = state.validation.errors.keys
        val firstKey = keys.firstOrNull()
        val targetIndex = when {
            state.snapshot.step == WelcomeStep.OBJECTIVES -> {
                1
            }
            firstKey in setOf("height", "weight", "minHeartRate", "maxHeartRate") -> {
                profileStage = 1
                1
            }
            firstKey in setOf("subjectiveLevel", "heartRateMonitor") -> {
                profileStage = 2
                1
            }
            else -> {
                profileStage = 0
                1
            }
        }
        delay(120)
        listState.animateScrollToItem(targetIndex)
    }

    Box(Modifier.fillMaxSize()) {
        if (detailFreePlan != null) {
            WelcomeFreePlanDetailScreen(
                plan = detailFreePlan,
                selected = state.selectedFreePlanId == detailFreePlan.id,
                onBack = viewModel::closeFreeTrainingPlanPreview,
                onEnroll = { viewModel.enrollFreeTrainingPlan(detailFreePlan) }
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .blur(if (showFinalStepIntro) 10.dp else 0.dp)
            ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 136.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { WelcomeHeader(state.snapshot.step, profileStage) }
                when (state.snapshot.step) {
                    WelcomeStep.PERSONAL_DATA -> personalItems(state, viewModel, profileStage)
                    WelcomeStep.PLAN -> planItems(state, viewModel)
                    WelcomeStep.OBJECTIVES -> goalItems(
                        state = state,
                        vm = viewModel,
                        onNewGoal = {
                            editingGoalId = null
                            viewModel.updateGoal { WelcomeGoalForm() }
                            showGoalForm = true
                        },
                        onEditGoal = { goal ->
                            editingGoalId = goal.id
                            viewModel.updateGoal { goal.toWelcomeGoalForm() }
                            showGoalForm = true
                        },
                        onDeleteGoal = { pendingDeleteGoal = it }
                    )
                    WelcomeStep.FINAL_SELECTION -> finalItems(state, viewModel)
                    WelcomeStep.COMPLETED -> Unit
                }
            }
            if (!isFreePlanDetailOpen) {
                WelcomeBottomBar(
                    state = state,
                    onBack = {
                        if (state.snapshot.step == WelcomeStep.PERSONAL_DATA && profileStage > 0) profileStage--
                        else viewModel.goBack()
                    },
                    onContinue = when (state.snapshot.step) {
                        WelcomeStep.PERSONAL_DATA -> ({
                            if (profileStage < 2) {
                                if (viewModel.validatePersonalStage(profileStage)) profileStage++
                            } else {
                                viewModel.savePersonal()
                            }
                        })
                        WelcomeStep.PLAN -> if (paidPlanSelected) ({}) else viewModel::continueWithSelectedPlan
                        WelcomeStep.OBJECTIVES -> viewModel::continueFromObjectives
                        WelcomeStep.FINAL_SELECTION -> viewModel::completeFinalSelection
                        WelcomeStep.COMPLETED -> ({})
                    },
                    canGoBackOverride = state.snapshot.step == WelcomeStep.PERSONAL_DATA && profileStage > 0,
                    continueEnabled = !(
                        state.snapshot.step == WelcomeStep.FINAL_SELECTION &&
                            !state.snapshot.planName.contains("bronce", ignoreCase = true) &&
                            state.selectedTrainerId == null
                    ),
                    labelOverride = when {
                        state.snapshot.step == WelcomeStep.PERSONAL_DATA -> {
                            if (profileStage < 2) "Continuar" else "Guardar perfil"
                        }
                        paidPlanSelected -> "Continuar al pago"
                        else -> null
                    },
                    showContinue = !isBronzeFinalStep,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
            if (state.isSaving) {
                Surface(Modifier.fillMaxSize(), color = AppBackground.copy(alpha = .78f)) {
                    Box(contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryBlue) }
                }
            }
            if (!isBronzeFinalStep) {
                state.message?.let { FormErrorNotification(it, viewModel::clearMessage) }
            }
        }
        }
        if (showFinalStepIntro) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(AppBackground.copy(alpha = .64f))
            )
        }
    }
    if (showFinalStepIntro) {
        FinalStepIntroDialog(onContinue = { finalStepIntroDismissed = true })
    }
    if (showGoalForm && state.snapshot.step == WelcomeStep.OBJECTIVES) {
        WelcomeGoalFormSheet(
            state = state,
            vm = viewModel,
            editing = editingGoalId != null,
            onDismiss = { if (!state.isSaving) showGoalForm = false },
            onSave = {
                viewModel.saveGoal(editingGoalId) {
                    showGoalForm = false
                    editingGoalId = null
                }
            }
        )
    }
    pendingDeleteGoal?.let { goal ->
        AlertDialog(
            onDismissRequest = { if (!state.isSaving) pendingDeleteGoal = null },
            title = { Text("Eliminar objetivo") },
            text = { Text("¿Estás seguro? Esta acción eliminará el objetivo de forma permanente.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteGoal(goal.id); pendingDeleteGoal = null }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteGoal = null }) { Text("Cancelar") } }
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.personalItems(
    state: WelcomeUiState.Ready,
    vm: WelcomeViewModel,
    stage: Int
) {
    if (stage == 0) item {
        CollapsibleFormSection(
            title = "Datos personales",
            subtitle = "Tu identidad y la forma de mantenernos en contacto.",
            icon = Icons.Default.Person,
            expanded = true,
            onExpandedChange = {},
            collapsible = false
        ) {
            ProfilePhotoPicker(vm, state.personal.photoUrl)
            WelcomeTextField("Nombres", state.personal.firstName, { vm.updatePersonal { f -> f.copy(firstName = it) } }, error(state, "firstName"), focusOnError = isFirstError(state, "firstName"))
            WelcomeTextField("Apellidos", state.personal.lastName, { vm.updatePersonal { f -> f.copy(lastName = it) } }, error(state, "lastName"), focusOnError = isFirstError(state, "lastName"))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                BirthDatePickerField(
                    value = state.personal.birthDate,
                    error = error(state, "birthDate"),
                    modifier = Modifier.weight(.53f),
                    onDateSelected = { date -> vm.updatePersonal { it.copy(birthDate = date) } }
                )
                GenderDropdown(
                    value = state.personal.gender,
                    error = error(state, "gender"),
                    modifier = Modifier.weight(.47f),
                    onSelected = { gender -> vm.updatePersonal { it.copy(gender = gender) } }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                DialCountrySelector(
                    selectedCountryCode = state.personal.dialCountryCode,
                    phonePrefix = state.personal.phonePrefix,
                    error = error(state, "phonePrefix"),
                    modifier = Modifier.width(126.dp),
                    onCountryChange = { countryCode, prefix ->
                        vm.updatePersonal { it.copy(dialCountryCode = countryCode, phonePrefix = prefix) }
                    },
                    onCustomPrefixChange = { prefix -> vm.updatePersonal { it.copy(phonePrefix = prefix) } }
                )
                WelcomeTextField(
                    label = "Teléfono",
                    value = state.personal.phone,
                    onChange = { value -> vm.updatePersonal { it.copy(phone = value.filter(Char::isDigit)) } },
                    error = error(state, "phone"),
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Phone,
                    focusOnError = isFirstError(state, "phone"),
                    leadingIcon = Icons.Default.Phone
                )
            }
        }
    }
    if (stage == 1) item {
        CollapsibleFormSection(
            title = "Morfología y capacidad",
            subtitle = "Con estos datos calcularemos tus zonas de esfuerzo y ajustaremos las intensidades a tu punto de partida.",
            icon = Icons.Default.MonitorHeart,
            expanded = true,
            onExpandedChange = {},
            collapsible = false
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WelcomeTextField("Estatura (cm)", state.personal.height, { vm.updatePersonal { f -> f.copy(height = it) } }, error(state, "height"), Modifier.weight(1f), KeyboardType.Decimal, focusOnError = isFirstError(state, "height"))
                WelcomeTextField("Peso (kg)", state.personal.weight, { vm.updatePersonal { f -> f.copy(weight = it) } }, error(state, "weight"), Modifier.weight(1f), KeyboardType.Decimal, focusOnError = isFirstError(state, "weight"))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WelcomeTextField(
                    label = "FC mínima",
                    value = state.personal.minHeartRate,
                    onChange = { value -> vm.updatePersonal { it.copy(minHeartRate = value) } },
                    error = error(state, "minHeartRate"),
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number,
                    focusOnError = isFirstError(state, "minHeartRate"),
                    info = "Pulsaciones de tu corazón en reposo al despertar."
                )
                WelcomeTextField(
                    label = "FC máxima",
                    value = state.personal.maxHeartRate,
                    onChange = { value -> vm.updatePersonal { it.copy(maxHeartRate = value) } },
                    error = error(state, "maxHeartRate"),
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number,
                    focusOnError = isFirstError(state, "maxHeartRate"),
                    info = "Máximas pulsaciones de tu corazón. Si no las conoces, puedes estimarlas restando tu edad a 220."
                )
            }
        }
    }
    if (stage == 2) item {
        CollapsibleFormSection(
            title = "Tu nivel actual",
            subtitle = "Así definiremos un punto de partida realista para ti.",
            icon = Icons.Default.DirectionsRun,
            expanded = true,
            onExpandedChange = {},
            collapsible = false
        ) {
            SubjectiveLevelSelector(state.personal.subjectiveLevel) { level -> vm.updatePersonal { it.copy(subjectiveLevel = level) } }
            ErrorAnchor(error(state, "subjectiveLevel"), isFirstError(state, "subjectiveLevel"))
            HeartRateMonitorSelector(state.personal.heartRateMonitorAnswer) { answer -> vm.updatePersonal { it.copy(heartRateMonitorAnswer = answer) } }
            ErrorAnchor(error(state, "heartRateMonitor"), isFirstError(state, "heartRateMonitor"))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialCountrySelector(
    selectedCountryCode: String,
    phonePrefix: String,
    error: String?,
    modifier: Modifier = Modifier,
    onCountryChange: (String, String) -> Unit,
    onCustomPrefixChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCountry = dialCountryOptions.first { it.value == selectedCountryCode }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = if (selectedCountry.value == "OTHER") phonePrefix.ifBlank { "+" } else selectedCountry.dialCode,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                readOnly = true,
                label = {
                    Text(
                        text = countryCodeToFlagEmoji(selectedCountry.value),
                        fontSize = 18.sp
                    )
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                isError = error != null,
                shape = FormFieldShape,
                colors = formFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 320.dp)
            ) {
                dialCountryOptions.forEach { country ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (country.value == "OTHER") "🌐 Otro indicativo"
                                else "${countryCodeToFlagEmoji(country.value)} ${country.dialCode}"
                            )
                        },
                        onClick = {
                            expanded = false
                            onCountryChange(country.value, if (country.value == "OTHER") "" else country.dialCode)
                        }
                    )
                }
            }
        }

        if (selectedCountryCode == "OTHER") {
            WelcomeTextField(
                label = "Indicativo personalizado",
                value = phonePrefix,
                onChange = { onCustomPrefixChange(normalizeCustomPhonePrefix(it)) },
                error = error,
                keyboardType = KeyboardType.Phone,
                placeholder = "+999"
            )
        } else if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDatePickerField(
    value: String,
    error: String?,
    modifier: Modifier = Modifier,
    onDateSelected: (String) -> Unit
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val dateFormatter = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    val initialDateMillis = remember(value) {
        runCatching { dateFormatter.parse(value)?.time }.getOrNull()
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            label = { Text("Fecha de nacimiento") },
            placeholder = { Text("Selecciona una fecha") },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Seleccionar fecha")
                }
            },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            shape = FormFieldShape,
            colors = formFieldColors()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDatePicker = true }
        )
    }

    if (showDatePicker) {
        val todayUtc = remember {
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
                timeInMillis
            }
        }
        val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDateMillis,
            yearRange = 1900..currentYear,
            selectableDates = remember {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayUtc
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    enabled = datePickerState.selectedDateMillis != null,
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateSelected(dateFormatter.format(Date(millis)))
                        }
                        showDatePicker = false
                    }
                ) { Text("Seleccionar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenderDropdown(
    value: String,
    error: String?,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
            readOnly = true,
            label = { Text("Género") },
            placeholder = { Text("Elegir") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            shape = FormFieldShape,
            colors = formFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            listOf("Masculino", "Femenino", "Otro").forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                    leadingIcon = {
                        if (value == option) Icon(Icons.Default.Check, null, tint = PrimaryBlue)
                    }
                )
            }
        }
    }
}

@Composable private fun ProfilePhotoPicker(vm: WelcomeViewModel, currentPhotoUrl: String) {
    val selected by vm.selectedPhoto.collectAsState()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent(), vm::selectPhoto)
    val hasPhoto = selected != null || currentPhotoUrl.isNotBlank()

    Surface(
        color = AppSurfaceAlt,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    modifier = Modifier.size(92.dp).clickable { launcher.launch("image/*") },
                    shape = CircleShape,
                    color = AppSurfaceMuted,
                    border = BorderStroke(3.dp, PrimaryBlue.copy(alpha = .45f)),
                    shadowElevation = 5.dp
                ) {
                    when {
                        selected != null -> AsyncImage(
                            model = selected,
                            contentDescription = "Vista previa de la foto de perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        currentPhotoUrl.isNotBlank() -> AsyncImage(
                            model = currentPhotoUrl,
                            contentDescription = "Foto de perfil actual",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = AppTextSecondary, modifier = Modifier.size(38.dp))
                        }
                    }
                }
                Surface(
                    modifier = Modifier.size(32.dp).clickable { launcher.launch("image/*") },
                    shape = CircleShape,
                    color = PrimaryBlue,
                    border = BorderStroke(2.dp, AppSurfaceAlt)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PhotoCamera, "Seleccionar foto", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (selected != null) "Vista previa lista" else "Foto de perfil",
                    color = AppTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (selected != null) "Se guardará al continuar." else "Opcional · JPG o PNG",
                    color = AppTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                TextButton(
                    onClick = { launcher.launch("image/*") },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (hasPhoto) "Cambiar fotografía" else "Elegir fotografía")
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.planItems(state: WelcomeUiState.Ready, vm: WelcomeViewModel) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionTitle(
                Icons.Default.WorkspacePremium,
                "Elige tu acompañamiento",
                "Puedes comenzar gratis y mejorar tu plan después."
            )
            ErrorAnchor(error(state, "selectedPlan"), isFirstError(state, "selectedPlan"))
        }
    }
    if (state.snapshot.plans.isEmpty()) item { EmptyCard("No hay planes disponibles en este momento.") }
    items(state.snapshot.plans, key = { it.id }) { plan ->
        WelcomeCompactPlanCard(
            plan = plan,
            selected = state.selectedPlanId == plan.id,
            onSelect = { vm.selectPlan(plan) }
        )
    }
}

private data class WelcomePlanVisualStyle(
    val accent: Color,
    val accentSoft: Color,
    val surfaceStart: Color,
    val imageAlignment: Alignment,
    val badge: String,
    val description: String
)

@Composable
private fun WelcomeCompactPlanCard(plan: WelcomePlan, selected: Boolean, onSelect: () -> Unit) {
    val style = welcomePlanVisualStyle(plan)
    var expanded by rememberSaveable(plan.id) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) style.accent else style.accent.copy(alpha = .55f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().background(
                Brush.verticalGradient(listOf(style.surfaceStart, AppSurface))
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WelcomePlanMedal(style)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = style.accentSoft,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, style.accent.copy(alpha = .45f))
                    ) {
                        Text(
                            style.badge,
                            color = style.accent,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = .7.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Text(plan.name, color = AppTextPrimary, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        if (plan.price <= 0) "Gratis" else "$${NumberFormat.getIntegerInstance(Locale("es", "CO")).format(plan.price)} COP",
                        color = style.accent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.Info,
                            contentDescription = if (expanded) "Ocultar detalles" else "Ver detalles",
                            tint = style.accent
                        )
                    }
                    Text(if (expanded) "Ocultar" else "Detalles", color = AppTextSecondary, fontSize = 9.sp)
                }
            }
            if (selected) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = AppSuccess, modifier = Modifier.size(16.dp))
                    Text("Plan seleccionado", color = AppTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            AnimatedVisibility(visible = expanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    HorizontalDivider(color = style.accent.copy(alpha = .25f))
                    Text(style.description, color = AppTextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                    if (plan.features.isNotEmpty()) {
                        Text("LO QUE INCLUYE", color = style.accent, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = .8.sp)
                        plan.features.forEach { feature -> WelcomePlanFeatureRow(feature, style) }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomePlanMedal(style: WelcomePlanVisualStyle) {
    Box(Modifier.size(66.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(58.dp).clip(CircleShape).background(Brush.radialGradient(listOf(style.accent.copy(alpha = .24f), Color.Transparent))))
        Image(
            painter = painterResource(R.drawable.plan_medals),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            contentScale = ContentScale.Crop,
            alignment = style.imageAlignment
        )
    }
}

@Composable
private fun WelcomePlanFeatureRow(feature: WelcomePlanFeature, style: WelcomePlanVisualStyle) {
    val featureColor = when (feature.included) {
        true -> AppSuccess
        false -> Color(0xFFFF6572)
        null -> style.accent
    }
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(shape = RoundedCornerShape(7.dp), color = featureColor.copy(alpha = .11f), modifier = Modifier.size(22.dp)) {
            Box(contentAlignment = Alignment.Center) {
                when (feature.included) {
                    true -> Icon(Icons.Default.Check, null, tint = featureColor, modifier = Modifier.size(14.dp))
                    false -> Icon(Icons.Default.Close, null, tint = featureColor, modifier = Modifier.size(14.dp))
                    null -> Box(Modifier.size(5.dp).clip(CircleShape).background(featureColor))
                }
            }
        }
        Text(
            if (feature.value.isNullOrBlank()) feature.label else "${feature.label}: ${feature.value}",
            color = if (feature.included == false) AppTextSecondary else AppTextPrimary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

private fun welcomePlanVisualStyle(plan: WelcomePlan): WelcomePlanVisualStyle {
    val identity = "${plan.id} ${plan.name}".lowercase()
    return when {
        "oro" in identity || "gold" in identity -> WelcomePlanVisualStyle(
            Color(0xFFFFC857), Color(0x22FFC857), Color(0xFF3A3325), Alignment.CenterEnd,
            "EXPERIENCIA PREMIUM", "Máximo acompañamiento para llevar tu rendimiento al siguiente nivel."
        )
        "plata" in identity || "silver" in identity -> WelcomePlanVisualStyle(
            Color(0xFFD7E0EA), Color(0x22D7E0EA), Color(0xFF343D49), Alignment.Center,
            "MÁS EQUILIBRADO", "Seguimiento constante y herramientas completas para avanzar con estructura."
        )
        else -> WelcomePlanVisualStyle(
            Color(0xFFD98B55), Color(0x22D98B55), Color(0xFF3A2F2A), Alignment.CenterStart,
            "PUNTO DE PARTIDA", "Lo esencial para comenzar y descubrir una forma más clara de progresar."
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.goalItems(
    state: WelcomeUiState.Ready,
    vm: WelcomeViewModel,
    onNewGoal: () -> Unit,
    onEditGoal: (SportGoal) -> Unit,
    onDeleteGoal: (SportGoal) -> Unit
) {
    val paidPlan = state.snapshot.planName.isNotBlank() &&
        !state.snapshot.planName.contains("bronce", ignoreCase = true)
    item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(Icons.Default.Flag, "Define tus objetivos", "Registra las metas que orientarán tu entrenamiento.")
            WelcomeGoalsToolbar(onNewGoal)
            ErrorAnchor(error(state, "goals"), isFirstError(state, "goals"))
        }
    }
    if (state.snapshot.goals.isEmpty()) {
        item { EmptyCard("No hay objetivos registrados. Debes registrar al menos un objetivo para continuar.") }
    } else {
        items(state.snapshot.goals, key = { it.id }) { goal ->
            WelcomeGoalCard(goal, onEditGoal, onDeleteGoal)
        }
    }
    if (paidPlan) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = AppSurface),
                border = BorderStroke(1.dp, AppBorder),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Días disponibles", color = AppTextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Tu plan requiere una disponibilidad de 4 días. Por favor, selecciónalos.",
                        color = AppTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        days.forEach { (key, label) ->
                            val selected = key in state.selectedDays
                            Surface(
                                Modifier.size(38.dp).clickable { vm.updateAvailableDay(key) },
                                shape = CircleShape,
                                color = if (selected) PrimaryBlue else AppSurfaceAlt,
                                border = BorderStroke(1.dp, if (selected) PrimaryBlue else AppBorder)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(label, color = if (selected) Color.White else AppTextPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Text(
                        "${state.selectedDays.size} de 4 días seleccionados",
                        color = if (state.selectedDays.size == 4) PrimaryBlue else AppTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    ErrorAnchor(error(state, "days"), isFirstError(state, "days"))
                }
            }
        }
    }
}

@Composable
private fun WelcomeGoalsToolbar(onNewGoal: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Button(
            onClick = onNewGoal,
            modifier = Modifier.height(50.dp),
            contentPadding = PaddingValues(horizontal = 14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(13.dp)
        ) {
            Box(
                Modifier.size(24.dp).clip(CircleShape).background(Color.White.copy(alpha = .16f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)) }
            Spacer(Modifier.width(7.dp))
            Text("Nuevo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun WelcomeGoalCard(goal: SportGoal, onEdit: (SportGoal) -> Unit, onDelete: (SportGoal) -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = AppSurfaceAlt),
        border = BorderStroke(1.dp, AppBorder),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WelcomeGoalDateBadge(goal.targetDate)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(goal.name, color = AppTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.fillMaxWidth().basicMarquee())
                    if (goal.generalGoal.isNotBlank()) {
                        Text(goal.generalGoal, color = AppTextSecondary, fontSize = 13.sp, maxLines = 1, modifier = Modifier.fillMaxWidth().basicMarquee())
                    }
                }
                Column(Modifier.width(88.dp), horizontalAlignment = Alignment.End) {
                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.MoreVert, "Opciones", tint = AppTextSecondary)
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, containerColor = AppSurface) {
                            DropdownMenuItem(
                                text = { Text("Editar", color = AppTextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Edit, null, tint = PrimaryBlue) },
                                onClick = { menuExpanded = false; onEdit(goal) }
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menuExpanded = false; onDelete(goal) }
                            )
                        }
                    }
                    if (goal.sport.isNotBlank()) {
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(PrimaryBlue)
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(goal.sport, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.fillMaxWidth().basicMarquee(), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            HorizontalDivider(color = AppBorder)
            if (goal.specific.isNotBlank()) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(AppBackground.copy(alpha = .45f)).padding(10.dp),
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
                Text(goal.description, color = AppTextSecondary, fontSize = 13.sp, lineHeight = 19.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun WelcomeGoalDateBadge(targetDate: String) {
    val date = runCatching { java.time.LocalDate.parse(targetDate) }.getOrNull()
    val day = date?.dayOfMonth?.toString()?.padStart(2, '0') ?: "--"
    val month = date?.month?.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
        ?.replaceFirstChar { it.uppercase() } ?: "---"
    Column(
        Modifier.width(58.dp).clip(RoundedCornerShape(14.dp)).background(PrimaryBlue.copy(alpha = .14f)).padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(day, color = AppTextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text(month.take(3), color = PrimaryBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WelcomeGoalFormSheet(
    state: WelcomeUiState.Ready,
    vm: WelcomeViewModel,
    editing: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val form = state.goal
    val durationSport = isDurationGoalSport(form.sport)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppBackground,
        dragHandle = { Box(Modifier.padding(vertical = 10.dp).size(44.dp, 4.dp).clip(CircleShape).background(AppBorder)) }
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.94f)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (editing) "Editar objetivo" else "Nuevo objetivo", color = AppTextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text(if (editing) "Actualiza los datos de tu meta" else "Define una meta clara y medible", color = AppTextSecondary, fontSize = 12.sp)
                }
                IconButton(onClick = onDismiss, enabled = !state.isSaving) { Icon(Icons.Default.Close, "Cerrar", tint = AppTextPrimary) }
            }
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(8.dp, 10.dp, 8.dp, 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { WelcomeTextField("Nombre del objetivo", form.name, { vm.updateGoal { current -> current.copy(name = it) } }, error(state, "name"), placeholder = "Ej. Mi primera 10K", focusOnError = isFirstError(state, "name")) }
                item { WelcomeGoalDatePickerField(form.targetDate, { vm.updateGoal { current -> current.copy(targetDate = it) } }, error(state, "targetDate"), isFirstError(state, "targetDate")) }
                item {
                    WelcomeGoalDropdown("Deporte", form.sport, state.snapshot.goalOptions.keys.toList(), error = error(state, "sport"), focusOnError = isFirstError(state, "sport")) { sport ->
                        vm.updateGoal { current -> current.copy(sport = sport, generalGoal = "", specificText = "", hours = "", minutes = "", seconds = "") }
                    }
                }
                item {
                    WelcomeGoalDropdown("Objetivo general", form.generalGoal, state.snapshot.goalOptions[form.sport].orEmpty(), enabled = form.sport.isNotBlank(), error = error(state, "generalGoal"), focusOnError = isFirstError(state, "generalGoal")) {
                        vm.updateGoal { current -> current.copy(generalGoal = it) }
                    }
                }
                item { WelcomeTextField("Descripción opcional", form.description, { value -> if (value.length <= 500) vm.updateGoal { it.copy(description = value) } }, singleLine = false, placeholder = "Cuéntanos más sobre tu objetivo") }
                if (durationSport) item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Marca objetivo", color = AppTextPrimary, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GoalDurationField(form.hours, { value -> vm.updateGoal { it.copy(hours = goalDigits(value, 3)) } }, "Horas", Modifier.weight(1f))
                            GoalDurationField(form.minutes, { value -> vm.updateGoal { it.copy(minutes = goalDigits(value, 2)) } }, "Min", Modifier.weight(1f))
                            GoalDurationField(form.seconds, { value -> vm.updateGoal { it.copy(seconds = goalDigits(value, 2)) } }, "Seg", Modifier.weight(1f))
                        }
                        ErrorAnchor(error(state, "specific"), isFirstError(state, "specific"))
                    }
                } else if (form.sport.isNotBlank()) item {
                    WelcomeTextField("Objetivo específico", form.specificText, { value -> if (value.length <= 300) vm.updateGoal { it.copy(specificText = value) } }, error(state, "specific"), singleLine = false, placeholder = "¿Nos cuentas un poco más sobre tu objetivo?", focusOnError = isFirstError(state, "specific"))
                }
            }
            Button(
                onClick = onSave,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (state.isSaving) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else {
                    Icon(Icons.Default.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (editing) "Actualizar objetivo" else "Guardar objetivo")
                }
            }
        }
    }
}

private fun SportGoal.toWelcomeGoalForm(): WelcomeGoalForm = WelcomeGoalForm(
    name = name,
    sport = sport,
    description = description,
    targetDate = targetDate,
    generalGoal = generalGoal,
    specificText = if (isDurationGoalSport(sport)) "" else specific,
    hours = specificHours?.toString().orEmpty(),
    minutes = specificMinutes?.toString().orEmpty(),
    seconds = specificSeconds?.toString().orEmpty()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WelcomeGoalDatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    focusOnError: Boolean
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val requester = remember { BringIntoViewRequester() }
    val formatter = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    }
    LaunchedEffect(focusOnError, error) {
        if (focusOnError && error != null) {
            delay(260)
            requester.bringIntoView()
        }
    }
    Box(Modifier.fillMaxWidth().bringIntoViewRequester(requester)) {
        OutlinedTextField(
            value = welcomeGoalDisplayDate(value),
            onValueChange = {},
            readOnly = true,
            label = { Text("Fecha objetivo") },
            placeholder = { Text("Selecciona una fecha") },
            trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            shape = FormFieldShape,
            colors = formFieldColors()
        )
        Box(Modifier.matchParentSize().clickable { showDatePicker = true })
    }
    if (showDatePicker) {
        val todayUtc = remember {
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                timeInMillis
            }
        }
        val selectedMillis = remember(value) { runCatching { formatter.parse(value)?.time }.getOrNull() }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedMillis,
            selectableDates = remember(todayUtc) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayUtc
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { onValueChange(formatter.format(Date(it))) }
                        showDatePicker = false
                    },
                    enabled = pickerState.selectedDateMillis != null
                ) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = pickerState) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WelcomeGoalDropdown(
    label: String,
    value: String,
    options: List<String>,
    enabled: Boolean = true,
    error: String?,
    focusOnError: Boolean,
    onSelect: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(focusOnError, error) {
        if (focusOnError && error != null) {
            delay(260)
            requester.bringIntoView()
        }
    }
    Column(Modifier.fillMaxWidth().bringIntoViewRequester(requester)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (enabled) expanded = !expanded }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                isError = error != null,
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = FormFieldShape,
                colors = formFieldColors()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { onSelect(option); expanded = false }
                    )
                }
            }
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
    }
}

@Composable
private fun GoalDurationField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = FormFieldShape,
        colors = formFieldColors()
    )
}

private fun goalDigits(value: String, maxLength: Int) = value.filter(Char::isDigit).take(maxLength)

private fun welcomeGoalDisplayDate(value: String): String = runCatching {
    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value) ?: return@runCatching value
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(parsed)
}.getOrDefault(value)

private fun androidx.compose.foundation.lazy.LazyListScope.finalItems(state: WelcomeUiState.Ready, vm: WelcomeViewModel) {
    val bronze = state.snapshot.planName.contains("bronce", ignoreCase = true)
    if (bronze) {
        item { ErrorAnchor(state.message, bringIntoView = true) }
        item { WelcomeFreePlansDashboard(state, vm) }
    } else {
        item { SectionTitle(Icons.Default.Groups, "Elige a tu entrenador", "Envía una solicitud para comenzar el acompañamiento.") }
        if (state.snapshot.trainers.isEmpty()) item { EmptyCard("No hay entrenadores aprobados disponibles.") }
        items(state.snapshot.trainers, key = { it.id }) { trainer ->
            WelcomeTrainerCatalogCard(
                trainer = trainer,
                selected = state.selectedTrainerId == trainer.id,
                onSelect = { vm.selectTrainer(trainer) }
            )
        }
    }
}

@Composable
private fun FinalStepIntroDialog(onContinue: () -> Unit) {
    var showFireworks by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(6000)
        showFireworks = false
    }

    Dialog(
        onDismissRequest = onContinue,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(.92f)
                .navigationBarsPadding(),
            color = AppSurface,
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 8.dp
        ) {
            Box(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Estás a un paso de completar tu configuración",
                            color = AppTextPrimary,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 28.sp
                        )
                        Text(
                            "Elige un plan gratuito para finalizar el proceso. Prepararemos tus primeras sesiones y dejaremos tu calendario listo para comenzar.",
                            color = AppTextSecondary,
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp
                        )
                    }
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.EventAvailable, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Entendido, elegir plan", fontWeight = FontWeight.Bold)
                    }
                }
                if (showFireworks) {
                    FinalStepFireworks(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(18.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun FinalStepFireworks(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "finalStepFireworks")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fireworksProgress"
    )
    Canvas(modifier = modifier) {
        fun drawFirework(center: Offset, color: Color, delay: Float, radius: Float) {
            val raw = (progress - delay + 1f) % 1f
            val alpha = if (raw < .18f) raw / .18f else (1f - raw).coerceIn(0f, 1f)
            val currentRadius = radius * (.35f + raw * .85f)
            val particleCount = 12
            repeat(particleCount) { index ->
                val angle = (Math.PI.toFloat() * 2f / particleCount) * index
                val start = Offset(
                    center.x + cos(angle) * currentRadius * .25f,
                    center.y + sin(angle) * currentRadius * .25f
                )
                val end = Offset(
                    center.x + cos(angle) * currentRadius,
                    center.y + sin(angle) * currentRadius
                )
                drawLine(
                    color = color.copy(alpha = alpha * .62f),
                    start = start,
                    end = end,
                    strokeWidth = 2.2f,
                    cap = StrokeCap.Round
                )
            }
            drawCircle(color.copy(alpha = alpha * .38f), radius = 3.5f, center = center)
        }

        drawFirework(
            center = Offset(size.width * .78f, size.height * .30f),
            color = PrimaryBlue,
            delay = 0f,
            radius = size.minDimension * .32f
        )
        drawFirework(
            center = Offset(size.width * .55f, size.height * .48f),
            color = AppSuccess,
            delay = .34f,
            radius = size.minDimension * .24f
        )
        drawFirework(
            center = Offset(size.width * .90f, size.height * .68f),
            color = Color(0xFFFFC857),
            delay = .62f,
            radius = size.minDimension * .22f
        )
    }
}
@Composable
private fun WelcomeFreePlansDashboard(state: WelcomeUiState.Ready, vm: WelcomeViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var visiblePages by rememberSaveable { mutableStateOf(1) }
    var quickEnrollPlan by remember { mutableStateOf<FreeTrainingPlan?>(null) }
    val plans = state.snapshot.freePlans
    val filtered = remember(plans, query) {
        val normalized = query.trim().lowercase(Locale.getDefault())
        if (normalized.isBlank()) plans else plans.filter { normalized in it.searchableText }
    }
    val visiblePlans = filtered.take(visiblePages * 6)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                color = AppSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, AppBorder)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Elige tu ruta gratuita de inicio", color = AppTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("Comienza con un plan guía y activa tus primeras rutinas en calendario.", color = AppTextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            visiblePages = 1
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        label = { Text("Buscar por deporte, nivel u objetivo", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                        shape = FormFieldShape,
                        colors = formFieldColors()
                    )
                }
            }

            when {
                plans.isEmpty() -> EmptyCard("Aún no hay planes gratuitos disponibles.")
                filtered.isEmpty() -> EmptyCard("No encontramos planes con ese criterio.")
                else -> {
                    visiblePlans.forEach { plan ->
                        WelcomeFreePlanCard(
                            plan = plan,
                            selected = state.selectedFreePlanId == plan.id,
                            onDetail = { vm.previewFreeTrainingPlan(plan) },
                            onQuickEnroll = { quickEnrollPlan = plan }
                        )
                    }
                    if (visiblePlans.size < filtered.size) {
                        OutlinedButton(
                            onClick = { visiblePages += 1 },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.ExpandMore, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ver más planes")
                        }
                    }
                }
            }
    }

    quickEnrollPlan?.let { plan ->
        ConfirmFreePlanEnrollmentDialog(
            plan = plan,
            onDismiss = { quickEnrollPlan = null },
            onReviewDetail = {
                quickEnrollPlan = null
                vm.previewFreeTrainingPlan(plan)
            },
            onConfirm = {
                quickEnrollPlan = null
                vm.enrollFreeTrainingPlan(plan)
            }
        )
    }
}

@Composable
private fun FreePlanMetric(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.heightIn(min = 78.dp),
        color = AppSurfaceAlt,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
            Text(value, color = AppTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(label, color = AppTextSecondary, fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun WelcomeFreePlanCard(
    plan: FreeTrainingPlan,
    selected: Boolean,
    onDetail: () -> Unit,
    onQuickEnroll: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (selected) AppSurfaceAlt else AppSurface),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) PrimaryBlue else AppBorder),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(plan.name, color = AppTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (selected) Icon(Icons.Default.CheckCircle, null, tint = PrimaryBlue)
            }
            Text(plan.description.ifBlank { "Plan gratuito listo para comenzar." }, color = AppTextSecondary, fontSize = 12.sp, lineHeight = 17.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FreePlanInfoChip(
                    text = plan.level.ifBlank { "Nivel abierto" },
                    icon = Icons.Default.Speed,
                    accent = Color(0xFF0B5CAD),
                    background = Color(0xFFE7F1FF),
                    modifier = Modifier.weight(1f)
                )
                FreePlanInfoChip(
                    text = plan.sport.ifBlank { "Deporte" },
                    icon = Icons.Default.DirectionsRun,
                    accent = Color(0xFF00876C),
                    background = Color(0xFFE4F8F1),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FreePlanTinyStat(
                    value = "${plan.activeWeeks} ${if (plan.activeWeeks == 1) "semana" else "semanas"}",
                    label = "Duración",
                    icon = Icons.Default.CalendarMonth,
                    modifier = Modifier.weight(1f)
                )
                FreePlanTinyStat(
                    value = "${plan.sessionCount} ${if (plan.sessionCount == 1) "rutina" else "rutinas"}",
                    label = "Carga",
                    icon = Icons.Default.FitnessCenter,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDetail,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AppBorder)
                ) {
                    Icon(Icons.Default.Visibility, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Detalle", maxLines = 1)
                }
                Button(
                    onClick = onQuickEnroll,
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.EventAvailable, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Inscribirme", maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ConfirmFreePlanEnrollmentDialog(
    plan: FreeTrainingPlan,
    onDismiss: () -> Unit,
    onReviewDetail: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(.92f),
            color = AppBackground,
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 8.dp
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(color = AppPrimarySoft, shape = RoundedCornerShape(14.dp)) {
                        Icon(
                            Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.padding(10.dp).size(24.dp)
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Confirmar inscripción", color = AppTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("Plan gratuito", color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        plan.name,
                        color = AppTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 23.sp
                    )
                    Text(
                        "Puedes inscribirte ahora o revisar primero el detalle completo de semanas, sesiones y objetivos.",
                        color = AppTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FreePlanDialogMetric(
                        label = "Duración",
                        value = "${plan.activeWeeks} ${if (plan.activeWeeks == 1) "semana" else "semanas"}",
                        icon = Icons.Default.CalendarMonth,
                        modifier = Modifier.weight(1f)
                    )
                    FreePlanDialogMetric(
                        label = "Rutinas",
                        value = plan.sessionCount.toString(),
                        icon = Icons.Default.FitnessCenter,
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(
                    color = AppPrimarySoft.copy(alpha = .55f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                        Text(
                            "Al confirmar, se activará este plan y terminaremos tu configuración inicial.",
                            color = AppTextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onReviewDetail,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, AppBorder)
                    ) {
                        Text("Ver detalle", maxLines = 1)
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Inscribirme", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun FreePlanDialogMetric(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(76.dp),
        color = AppSurfaceAlt,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AppBorder.copy(alpha = .72f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(color = AppPrimarySoft, shape = RoundedCornerShape(10.dp)) {
                Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.padding(7.dp).size(18.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text(value, color = AppTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(label, color = AppTextSecondary, fontSize = 11.sp, lineHeight = 13.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun FreePlanInfoChip(
    text: String,
    icon: ImageVector,
    accent: Color,
    background: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(30.dp),
        color = background,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = .28f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                text,
                color = accent,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FreePlanTinyStat(value: String, label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(64.dp),
        color = AppSurfaceAlt,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AppBorder.copy(alpha = .7f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(color = AppPrimarySoft, shape = RoundedCornerShape(10.dp)) {
                Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.padding(7.dp).size(17.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text(value, color = AppTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(label, color = AppTextSecondary, fontSize = 10.5.sp, lineHeight = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun FreePlanLegendTextBlock(
    label: String,
    text: String,
    placeholder: String,
    textAlign: TextAlign = TextAlign.Start
) {
    val resolvedText = text.trim().ifBlank { placeholder }
    val isPlaceholder = text.isBlank()
    var expanded by remember(resolvedText) { mutableStateOf(false) }
    var canExpand by remember(resolvedText) { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            shape = RoundedCornerShape(16.dp),
            color = AppSurfaceAlt,
            border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Text(
                    text = resolvedText,
                    color = if (isPlaceholder) AppTextSecondary else AppTextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = textAlign,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = canExpand || expanded) { expanded = !expanded },
                    onTextLayout = { result: TextLayoutResult ->
                        if (!expanded) canExpand = result.hasVisualOverflow
                    }
                )
                if (canExpand || expanded) {
                    Text(
                        text = if (expanded) "Ver menos" else "Ver más",
                        color = PrimaryBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable { expanded = !expanded }
                    )
                }
            }
        }

        Box(modifier = Modifier.padding(start = 12.dp)) {
            FormLegendLabel(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FreePlanStartInfoBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppPrimarySoft.copy(alpha = .72f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = .18f))
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = Color.White.copy(alpha = .68f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.EventAvailable,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.padding(8.dp).size(18.dp)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "Inicio del plan",
                    color = AppTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "La semana 1 inicia el próximo lunes. Mientras tanto, recibirás sesiones comodín según tu nivel.",
                    color = AppTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun WelcomeFreePlanDetailScreen(
    plan: FreeTrainingPlan,
    selected: Boolean,
    onBack: () -> Unit,
    onEnroll: () -> Unit
) {
    var selectedRoutine by remember { mutableStateOf<Routine?>(null) }

    selectedRoutine?.let { routine ->
        RoutineDetailScreen(
            routine = routine,
            ritmos = null,
            zonas = null,
            onBackClick = { selectedRoutine = null }
        )
        return
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Surface(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            color = AppSurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, AppBorder)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Volver a los planes")
                }
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(
                        plan.name,
                        color = AppTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.fillMaxWidth().basicMarquee()
                    )
                    Text(listOf(plan.sport, plan.level, plan.trainerName).filter(String::isNotBlank).joinToString(" · "), color = AppTextSecondary, fontSize = 12.sp)
                }
            }
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { FreePlanInformationSection(plan = plan) }
            plan.weeks.forEachIndexed { weekIndex, week ->
                item {
                    WelcomeFreePlanWeek(
                        weekIndex = weekIndex,
                        week = week,
                        onRoutineSelected = { selectedRoutine = it }
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AppSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, AppBorder)
        ) {
            Button(
                onClick = onEnroll,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(10.dp).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(if (selected) Icons.Default.CheckCircle else Icons.Default.EventAvailable, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Inscribirse", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FreePlanInformationSection(plan: FreeTrainingPlan) {
    var expanded by rememberSaveable(plan.id) { mutableStateOf(true) }
    FreePlanInfoAccordion(
        title = "Información del plan",
        subtitle = "Descripción y objetivo principal",
        expanded = expanded,
        onExpandChange = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FreePlanLegendTextBlock(
                label = "Descripción",
                text = plan.description,
                placeholder = "Plan gratuito para activar tu primera guía de entrenamiento.",
                textAlign = TextAlign.Justify
            )
            FreePlanLegendTextBlock(
                label = "Objetivo principal",
                text = plan.goals,
                placeholder = "Adaptación inicial y continuidad.",
                textAlign = TextAlign.Justify
            )
        }
    }
}

@Composable
private fun FreePlanInfoAccordion(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onExpandChange: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (expanded) AppSurfaceAlt else AppSurface,
        border = BorderStroke(
            1.dp,
            if (expanded) Color(0xFF7EA8FF).copy(alpha = 0.32f) else AppBorder.copy(alpha = 0.85f)
        ),
        tonalElevation = if (expanded) 4.dp else 0.dp
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandChange() }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFF7EA8FF).copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFF7EA8FF).copy(alpha = 0.28f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Article, null, tint = Color(0xFF7EA8FF), modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, fontWeight = FontWeight.Bold, color = AppTextPrimary, fontSize = 16.sp)
                    Text(
                        subtitle,
                        color = AppTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth().basicMarquee()
                    )
                }
                IconButton(onClick = onExpandChange, modifier = Modifier.size(40.dp)) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Contraer" else "Expandir",
                        tint = if (expanded) Color(0xFF7EA8FF) else AppTextSecondary
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun WelcomeFreePlanWeek(
    weekIndex: Int,
    week: List<FreeTrainingRoutine>,
    onRoutineSelected: (Routine) -> Unit
) {
    val locked = weekIndex > 0
    val firstAssigned = week.indexOfFirst { it.isAssigned }.takeIf { it >= 0 } ?: 0
    var selectedDay by rememberSaveable(weekIndex) { mutableStateOf(firstAssigned) }
    val selectedRoutine = week.getOrNull(selectedDay)
    val days = week.mapIndexed { index, routine ->
        RoutineWeekDay(
            label = dayName(index),
            routine = routine.takeIf { it.isAssigned }?.toRoutinePreview(),
            locked = locked,
            enabled = true
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RoutineWeekCalendar(
            title = "Semana ${weekIndex + 1}",
            days = days,
            selectedIndex = selectedDay,
            onDaySelected = { selectedDay = it },
            locked = locked
        )
        val currentRoutine = selectedRoutine
        if (currentRoutine?.isAssigned == true) {
            val routinePreview = currentRoutine.toRoutinePreview()
            SharedRoutineCard(
                routine = routinePreview,
                enabled = true,
                locked = locked,
                onClick = { onRoutineSelected(routinePreview) }
            )
        } else {
            EmptyRoutineDayCard(
                title = "Día disponible",
                subtitle = "Sin sesión asignada",
                locked = locked
            )
        }
    }
}

private fun dayName(index: Int) = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo").getOrElse(index) { "Día" }

private fun FreeTrainingRoutine.toRoutinePreview(): Routine = Routine(
    id = text("id").ifBlank { listOf(title, text("tipo_esfuerzo"), text("tipo_medicion")).joinToString("|") },
    titulo = title,
    descripcion = text("descripcion"),
    objetivos = text("objetivos"),
    tipo_esfuerzo = text("tipo_esfuerzo"),
    tipo_medicion = text("tipo_medicion"),
    tipo_terreno = text("tipo_terreno"),
    estado = "Pendiente",
    completa = true,
    sesiones_calentamiento = listMap("sesiones_calentamiento", "calentamiento"),
    sesiones_central = map("sesiones_central", "central"),
    sesiones_calma = listMap("sesiones_calma", "vuelta_calma", "calma"),
    comentarios_fase_calentamiento = text("comentarios_fase_calentamiento"),
    comentarios_fase_central = text("comentarios_fase_central"),
    comentarios_fase_calma = text("comentarios_fase_calma"),
    videosCalentamiento = stringList("videosCalentamiento", "videos_calentamiento"),
    videosCentral = stringList("videosCentral", "videos_central"),
    videosCalma = stringList("videosCalma", "videos_calma")
)

private fun FreeTrainingRoutine.listMap(vararg keys: String): List<Map<String, Any>>? {
    val value = keys.firstNotNullOfOrNull { key -> data[key] } ?: return null
    return (value as? List<*>)?.mapNotNull { item ->
        (item as? Map<*, *>)?.mapKeys { it.key.toString() } as? Map<String, Any>
    }
}

private fun FreeTrainingRoutine.map(vararg keys: String): Map<String, Any>? {
    val value = keys.firstNotNullOfOrNull { key -> data[key] } ?: return null
    return (value as? Map<*, *>)?.mapKeys { it.key.toString() } as? Map<String, Any>
}

private fun FreeTrainingRoutine.stringList(vararg keys: String): List<String>? {
    val value = keys.firstNotNullOfOrNull { key -> data[key] } ?: return null
    return (value as? List<*>)?.mapNotNull { it?.toString() }
}

@Composable
private fun WelcomeTrainerCatalogCard(
    trainer: WelcomeTrainer,
    selected: Boolean,
    onSelect: () -> Unit
) {
    var showProfile by rememberSaveable(trainer.id) { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) PrimaryBlue else AppBorder),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncImage(
                    model = trainer.photoUrl.ifBlank { R.drawable.placeholder },
                    contentDescription = "Fotografía de ${trainer.name}",
                    modifier = Modifier.size(68.dp).clip(CircleShape).clickable { showProfile = true },
                    contentScale = ContentScale.Crop
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        trainer.name,
                        color = AppTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    WelcomeTrainerStars(trainer.rating)
                    when {
                        selected -> Text("Entrenador seleccionado", color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        trainer.specialty.isNotBlank() -> Text(trainer.specialty, color = AppTextSecondary, fontSize = 12.sp)
                        trainer.sport.isNotBlank() -> Text(trainer.sport, color = AppTextSecondary, fontSize = 12.sp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showProfile = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Visitar perfil", fontSize = 12.6.sp, maxLines = 1, softWrap = false)
                }
                Button(
                    onClick = onSelect,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(if (selected) Icons.Default.CheckCircle else Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (selected) "Seleccionado" else "Elegir",
                        fontSize = 12.6.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
    if (showProfile) WelcomeTrainerProfileSheet(trainer, onDismiss = { showProfile = false }, onSelect = {
        onSelect()
        showProfile = false
    })
}

@Composable
private fun WelcomeTrainerStars(value: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { index ->
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = if (index < value.roundToInt()) Color(0xFFFFC857) else AppBorder,
                modifier = Modifier.size(17.dp)
            )
        }
        Text(
            if (value > 0) " %.1f".format(value) else " Sin calificar",
            color = AppTextSecondary,
            fontSize = 11.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WelcomeTrainerProfileSheet(trainer: WelcomeTrainer, onDismiss: () -> Unit, onSelect: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppBackground,
        contentColor = AppTextPrimary,
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 6.dp).size(44.dp, 4.dp).clip(CircleShape).background(AppBorder))
        }
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.94f)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Text("Perfil del entrenador", color = AppTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Información profesional y deportiva", color = AppTextSecondary, fontSize = 12.sp)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cerrar") }
            }
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { WelcomeTrainerProfileHero(trainer) }
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppSurfaceAlt),
                        border = BorderStroke(1.dp, PrimaryBlue),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Calificación del entrenador", color = AppTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                WelcomeTrainerStars(trainer.rating)
                            }
                            Text("Disponible", color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                item {
                    SectionCard("Información personal", Icons.Default.LocationOn) {
                        CompactDetailGrid(listOf("Género" to trainer.gender, "Fecha de nacimiento" to trainer.birthDate))
                        InlineDetail("Origen", welcomeTrainerLocation(trainer.country, trainer.city))
                        InlineDetail("Ubicación actual", welcomeTrainerLocation(trainer.currentCountry, trainer.currentCity))
                    }
                }
                if (listOf(trainer.review, trainer.milestones, trainer.specialty, trainer.experience).any(String::isNotBlank)) item {
                    SectionCard("Trayectoria deportiva", Icons.Default.FitnessCenter) {
                        ExpandableDetail("Reseña profesional", trainer.review)
                        ExpandableDetail("Hitos deportivos", trainer.milestones)
                        CompactDetailGrid(listOf("Especialidad" to trainer.specialty, "Experiencia" to trainer.experience))
                    }
                }
                if (listOf(trainer.professionalProfile, trainer.academicBackground, trainer.certifications).any(String::isNotBlank)) item {
                    SectionCard("Perfil y formación", Icons.Default.School) {
                        Detail("Perfil profesional", trainer.professionalProfile)
                        Detail("Formación académica", trainer.academicBackground)
                        Detail("Certificaciones", trainer.certifications)
                    }
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppSurface),
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
            ) {
                Button(
                    onClick = onSelect,
                    modifier = Modifier.fillMaxWidth().padding(12.dp).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Elegir entrenador")
                }
            }
        }
    }
}

@Composable
private fun WelcomeTrainerProfileHero(trainer: WelcomeTrainer) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppBorder),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = trainer.photoUrl.ifBlank { R.drawable.placeholder },
                contentDescription = "Fotografía de ${trainer.name}",
                modifier = Modifier.size(96.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(trainer.name, color = AppTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                if (trainer.specialty.isNotBlank()) Text(trainer.specialty, color = AppTextSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                WelcomeTrainerStars(trainer.rating)
                Text(
                    if (trainer.rating > 0) "%.1f de 5".format(trainer.rating) else "Sin calificaciones",
                    color = AppTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun welcomeTrainerLocation(country: String, city: String) =
    listOf(city, country).filter(String::isNotBlank).joinToString(", ")

@Composable private fun WelcomeHeader(step: WelcomeStep, profileStage: Int) {
    val position = step.position.coerceAtMost(4)
    val title = when (step) {
        WelcomeStep.PERSONAL_DATA -> "Cuéntanos sobre ti"
        WelcomeStep.PLAN -> "Elige tu experiencia"
        WelcomeStep.OBJECTIVES -> "Define tu objetivo"
        WelcomeStep.FINAL_SELECTION -> "Tu acompañamiento"
        WelcomeStep.COMPLETED -> "Todo listo"
    }
    Column(modifier = Modifier.padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = AppTextPrimary, fontSize = 25.sp, fontWeight = FontWeight.Black)
                Text("Paso $position de 4", color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                RunningAthleteIndicator()
                Text("${position * 25}%", color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(3) { subStep ->
                    val completed = step != WelcomeStep.PERSONAL_DATA || subStep <= profileStage
                    Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(if (completed) PrimaryBlue else AppBorder))
                }
            }
            repeat(3) { index ->
                val stepPosition = index + 2
                Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(if (position >= stepPosition) PrimaryBlue else AppBorder))
            }
        }
    }
}

@Composable
private fun RunningAthleteIndicator() {
    val transition = rememberInfiniteTransition(label = "runningAthlete")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 620, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "runningCycle"
    )
    Canvas(modifier = Modifier.size(width = 34.dp, height = 28.dp)) {
        val unit = size.minDimension / 28f
        val bounce = abs(sin(phase * 2f)) * .45f * unit
        val hip = Offset(15f * unit, 14f * unit - bounce)
        val shoulder = Offset(17f * unit, 7.5f * unit - bounce)
        val stroke = 1.75f * unit

        fun joint(origin: Offset, angle: Float, length: Float) = Offset(
            origin.x + sin(angle) * length * unit,
            origin.y + cos(angle) * length * unit
        )

        fun drawLeg(cycle: Float, color: Color) {
            val thighAngle = sin(cycle) * .82f
            val knee = joint(hip, thighAngle, 5.2f)
            val kneeBend = max(0f, -sin(cycle)) * .95f
            val shinAngle = thighAngle * .30f - kneeBend
            val ankle = joint(knee, shinAngle, 5.1f)
            val footEnd = Offset(ankle.x + 2.2f * unit, ankle.y)
            drawLine(color, hip, knee, stroke, StrokeCap.Round)
            drawLine(color, knee, ankle, stroke, StrokeCap.Round)
            drawLine(color, ankle, footEnd, stroke, StrokeCap.Round)
        }

        fun drawArm(cycle: Float, color: Color) {
            val upperAngle = sin(cycle) * .88f
            val elbow = joint(shoulder, upperAngle, 3.7f)
            val forearmAngle = upperAngle + if (sin(cycle) >= 0f) -.72f else .72f
            val hand = joint(elbow, forearmAngle, 3.3f)
            drawLine(color, shoulder, elbow, stroke, StrokeCap.Round)
            drawLine(color, elbow, hand, stroke, StrokeCap.Round)
        }

        val backColor = PrimaryBlue.copy(alpha = .42f)
        drawLeg(phase + Math.PI.toFloat(), backColor)
        drawArm(phase, backColor)
        drawLine(PrimaryBlue, hip, shoulder, strokeWidth = 2.1f * unit, cap = StrokeCap.Round)
        drawCircle(PrimaryBlue, radius = 2.05f * unit, center = Offset(18.2f * unit, 3.2f * unit - bounce))
        drawLeg(phase, PrimaryBlue)
        drawArm(phase + Math.PI.toFloat(), PrimaryBlue)

        drawLine(
            PrimaryBlue.copy(alpha = .24f),
            Offset(2f * unit, 11f * unit),
            Offset(8f * unit, 11f * unit),
            strokeWidth = unit,
            cap = StrokeCap.Round
        )
        drawLine(
            PrimaryBlue.copy(alpha = .15f),
            Offset(4f * unit, 14f * unit),
            Offset(9f * unit, 14f * unit),
            strokeWidth = unit,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun WelcomeStartScreen(userName: String, onStart: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = AppBackground) {
        Box(Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(R.drawable.banner),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(.08f)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                text = buildAnnotatedString {
                    append("Vamos a configurar ")
                    withStyle(SpanStyle(color = PrimaryBlue)) { append("My Virtual Trainer") }
                    append(" a tu medida")
                    userName.trim().takeIf(String::isNotBlank)?.let { append(", $it") }
                },
                color = AppTextPrimary,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Cuéntanos un poco sobre ti para adaptar tus rutinas, métricas y acompañamiento.",
                color = AppTextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AppSurface,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, AppBorder)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    listOf(
                        Icons.Default.Person to "Tu perfil",
                        Icons.Default.WorkspacePremium to "Plan",
                        Icons.Default.Flag to "Tus objetivos",
                        Icons.Default.Groups to "Tu acompañamiento"
                    ).forEachIndexed { index, (icon, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(shape = CircleShape, color = AppPrimarySoft, modifier = Modifier.size(34.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp)) }
                            }
                            Text(label, color = AppTextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text("0${index + 1}", color = AppTextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(Icons.Default.Schedule, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                Text("Aproximadamente 3 minutos", color = AppTextSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Comenzar configuración", fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
            }
            }
        }
    }
}

@Composable private fun SectionTitle(icon: ImageVector, title: String, subtitle: String) { Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Surface(Modifier.size(42.dp), shape = RoundedCornerShape(13.dp), color = AppPrimarySoft) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = PrimaryBlue) } }; Column { Text(title, color = AppTextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = AppTextSecondary, fontSize = 12.sp, lineHeight = 17.sp) } } }
@Composable private fun ContextInsight(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(Icons.Default.AutoAwesome, null, tint = PrimaryBlue, modifier = Modifier.size(17.dp))
        Text(text, color = AppTextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
    }
}
@Composable private fun CollapsibleFormSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    collapsible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppBorder),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth().then(if (collapsible) Modifier.clickable { onExpandedChange(!expanded) } else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(AppPrimarySoft),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(19.dp)) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, color = AppTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, color = AppTextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                }
                if (collapsible) {
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Contraer $title" else "Expandir $title",
                        tint = AppTextSecondary,
                        modifier = Modifier.rotate(if (expanded) 180f else 0f)
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = AppBorder)
                    content()
                }
            }
        }
    }
}
@Composable private fun WelcomeTextField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    error: String? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    placeholder: String = "",
    focusOnError: Boolean = false,
    info: String? = null,
    leadingIcon: ImageVector? = null
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusOnError, error) {
        if (focusOnError && error != null) {
            delay(260)
            bringIntoViewRequester.bringIntoView()
            focusRequester.requestFocus()
        }
    }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.bringIntoViewRequester(bringIntoViewRequester).focusRequester(focusRequester),
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(label)
                info?.let { FormTooltip(it) }
            }
        },
        placeholder = { if (placeholder.isNotBlank()) Text(placeholder) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        leadingIcon = leadingIcon?.let { icon ->
            { Icon(icon, contentDescription = null, tint = if (error == null) AppTextSecondary else MaterialTheme.colorScheme.error) }
        },
        shape = FormFieldShape,
        colors = formFieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}
@Composable private fun SubjectiveLevelSelector(selectedLevel: Int?, onSelected: (Int?) -> Unit) {
    Text("Nivel deportivo subjetivo", color = AppTextPrimary, fontWeight = FontWeight.SemiBold)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        subjectiveLevels.forEach { (label, level) ->
            val selected = selectedLevel == level
            FilterChip(
                selected = selected,
                onClick = { onSelected(level.takeUnless { selected }) },
                label = { Text(label, fontSize = 11.sp) },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AppPrimarySoft,
                    selectedLabelColor = Color.White,
                    labelColor = AppTextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = AppBorder,
                    selectedBorderColor = PrimaryBlue
                )
            )
        }
    }
}

@Composable private fun HeartRateMonitorSelector(selectedAnswer: Boolean?, onSelected: (Boolean) -> Unit) {
    Text("¿Usas pulsómetro?", color = AppTextPrimary, fontWeight = FontWeight.SemiBold)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple(true, "Sí", "Entreno con datos"),
            Triple(false, "No", "Uso percepción")
        ).forEach { (answer, title, subtitle) ->
            val selected = selectedAnswer == answer
            Surface(
                modifier = Modifier.weight(1f).clickable { onSelected(answer) },
                shape = RoundedCornerShape(14.dp),
                color = if (selected) AppPrimarySoft else AppSurfaceAlt,
                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) PrimaryBlue else AppBorder)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = if (answer) Icons.Default.Watch else Icons.Default.DoNotDisturb,
                        contentDescription = null,
                        tint = if (selected) PrimaryBlue else AppTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(title, color = AppTextPrimary, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = AppTextSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
@Composable private fun ErrorAnchor(message: String?, bringIntoView: Boolean) {
    if (message == null) return
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(bringIntoView, message) {
        if (bringIntoView) {
            delay(260)
            requester.bringIntoView()
        }
    }
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        fontSize = 12.sp,
        modifier = Modifier.bringIntoViewRequester(requester).padding(start = 16.dp)
    )
}
@Composable private fun EmptyCard(text: String) { Surface(color = AppSurface, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, AppBorder)) { Text(text, Modifier.fillMaxWidth().padding(24.dp), color = AppTextSecondary, textAlign = TextAlign.Center) } }
@Composable private fun SelectionCard(title: String, meta: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(if (selected) AppSurfaceAlt else AppSurface),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) PrimaryBlue else AppBorder),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = AppTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if (selected) PrimaryBlue else AppTextSecondary)
            }
            if (meta.isNotBlank()) Text(meta, color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(description.ifBlank { "Plan listo para comenzar." }, color = AppTextSecondary, fontSize = 12.sp, maxLines = 3)
        }
    }
}

@Composable private fun WelcomeBottomBar(
    state: WelcomeUiState.Ready,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    canGoBackOverride: Boolean = false,
    continueEnabled: Boolean = true,
    labelOverride: String? = null,
    showContinue: Boolean = true,
    modifier: Modifier = Modifier
) {
    val step = state.snapshot.step
    val canGoBack = canGoBackOverride || step in setOf(WelcomeStep.PLAN, WelcomeStep.OBJECTIVES, WelcomeStep.FINAL_SELECTION)
    val label = labelOverride ?: when (step) {
        WelcomeStep.PERSONAL_DATA -> "Guardar y elegir plan"
        WelcomeStep.PLAN -> "Guardar plan y continuar"
        WelcomeStep.OBJECTIVES -> "Continuar configuración"
        WelcomeStep.FINAL_SELECTION -> if (state.snapshot.planName.contains("bronce", ignoreCase = true)) "Inscribirme y finalizar" else "Enviar solicitud y finalizar"
        WelcomeStep.COMPLETED -> "Finalizado"
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AppSurface,
        shadowElevation = 16.dp,
        border = BorderStroke(1.dp, AppBorder.copy(alpha = .7f))
    ) {
        Row(
            Modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canGoBack) {
                OutlinedButton(
                    onClick = onBack,
                    enabled = !state.isSaving,
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AppBorder)
                ) { Icon(Icons.Default.ArrowBack, "Volver al paso anterior") }
            }
            if (showContinue) {
                Button(
                    onClick = onContinue,
                    enabled = continueEnabled && !state.isSaving && step != WelcomeStep.COMPLETED,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(label, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.ArrowForward, null, Modifier.size(18.dp))
                }
            }
        }
    }
}
@Composable private fun LoadingState() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) { CircularProgressIndicator(color = PrimaryBlue); Text("Preparando tu experiencia…", color = AppTextSecondary) } } }
@Composable private fun ErrorState(message: String, retry: () -> Unit) { Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) { Icon(Icons.Default.CloudOff, null, tint = AppError, modifier = Modifier.size(42.dp)); Text(message, color = AppTextPrimary, textAlign = TextAlign.Center); Button(retry) { Text("Reintentar") } } } }
private fun error(state: WelcomeUiState.Ready, key: String) = state.validation.errors[key]
private fun isFirstError(state: WelcomeUiState.Ready, key: String) = state.validation.errors.keys.firstOrNull() == key
