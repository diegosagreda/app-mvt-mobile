package com.example.mvt.sports.ui

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.sports.model.OtherSports
import com.example.mvt.sports.model.SportsProfile
import com.example.mvt.sports.model.SportsValidationErrors
import com.example.mvt.sports.viewmodel.SportsScreenState
import com.example.mvt.sports.viewmodel.SportsViewModel
import com.example.mvt.ui.components.FormConfirmationDialog
import com.example.mvt.ui.components.FormErrorNotification
import com.example.mvt.ui.components.FormFieldShape
import com.example.mvt.ui.components.FormSuccessNotification
import com.example.mvt.ui.components.formFieldColors
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppPrimarySoft
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue

@Composable
fun SportsScreen(
    athleteId: String,
    viewModel: SportsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val action by viewModel.action.collectAsState()
    val ready = state as? SportsScreenState.Ready
    var showDiscard by remember { mutableStateOf(false) }

    LaunchedEffect(athleteId) { viewModel.load(athleteId) }
    BackHandler(enabled = ready?.hasUnsavedChanges == true) { showDiscard = true }

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (ready?.hasUnsavedChanges == true) {
                SportsSaveBar(action.isSaving, viewModel::save)
            }
        }
    ) { padding ->
        when (val current = state) {
            SportsScreenState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = PrimaryBlue) }

            is SportsScreenState.LoadError -> SportsLoadError(
                message = current.message,
                onBack = onBack,
                onRetry = { viewModel.load(athleteId) },
                modifier = Modifier.padding(padding)
            )

            is SportsScreenState.Ready -> SportsContent(
                state = current,
                onBack = { if (current.hasUnsavedChanges) showDiscard = true else onBack() },
                onUpdate = viewModel::update,
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (showDiscard) {
        FormConfirmationDialog(
            title = "Cambios sin guardar",
            message = "Si sales ahora, perderás las modificaciones de tu información deportiva.",
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
private fun SportsContent(
    state: SportsScreenState.Ready,
    onBack: () -> Unit,
    onUpdate: ((SportsProfile) -> SportsProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = state.profile
    val errors = state.errors
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { SportsHeader(onBack) }
        item {
            SportsSection("Nivel y entrenamiento", Icons.Default.DirectionsBike) {
                Text("Nivel deportivo subjetivo", color = AppTextPrimary, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SUBJECTIVE_LEVELS.forEach { (label, level) ->
                        FilterChip(
                            selected = profile.subjectiveLevel == level,
                            onClick = {
                                onUpdate {
                                    it.copy(subjectiveLevel = level.takeUnless { selected -> selected == it.subjectiveLevel })
                                }
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AppPrimarySoft,
                                selectedLabelColor = Color.White,
                                labelColor = AppTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = profile.subjectiveLevel == level,
                                borderColor = AppBorder,
                                selectedBorderColor = PrimaryBlue
                            )
                        )
                    }
                }
                Text("¿Usas pulsómetro?", color = AppTextPrimary, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(true to "Sí", false to "No").forEach { (usesMonitor, label) ->
                        OutlinedButton(
                            onClick = {
                                onUpdate {
                                    it.copy(
                                        usesHeartRateMonitor = usesMonitor,
                                        heartRateMonitorBrand = if (usesMonitor) it.heartRateMonitorBrand else ""
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (profile.usesHeartRateMonitor == usesMonitor) AppPrimarySoft else Color.Transparent
                            ),
                            border = BorderStroke(1.dp, if (profile.usesHeartRateMonitor == usesMonitor) PrimaryBlue else AppBorder)
                        ) { Text(label, color = if (profile.usesHeartRateMonitor == usesMonitor) Color.White else AppTextSecondary) }
                    }
                }
                AnimatedVisibility(profile.usesHeartRateMonitor) {
                    OutlinedTextField(
                        value = profile.heartRateMonitorBrand,
                        onValueChange = { value -> if (value.length <= 40) onUpdate { it.copy(heartRateMonitorBrand = value) } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Marca o modelo *") },
                        placeholder = { Text("Ej. Garmin 255") },
                        singleLine = true,
                        isError = errors.heartRateMonitorBrand != null,
                        supportingText = errors.heartRateMonitorBrand?.let { message -> { Text(message) } },
                        shape = FormFieldShape,
                        colors = formFieldColors()
                    )
                }
            }
        }
        item {
            SportsSection("Tu trayectoria", Icons.Default.SportsScore) {
                Text(
                    "Cuéntale a tu entrenador de dónde vienes y qué quieres conseguir.",
                    color = AppTextSecondary,
                    fontSize = 12.sp
                )
                SportsTextArea(
                    value = profile.review,
                    onValueChange = { value -> if (value.length <= 1000) onUpdate { it.copy(review = value) } },
                    label = "Reseña deportiva",
                    placeholder = "Tu experiencia practicando deporte...",
                    required = false
                )
                SportsTextArea(
                    value = profile.coachMessage,
                    onValueChange = { value -> if (value.length <= 1000) onUpdate { it.copy(coachMessage = value) } },
                    label = "Mensaje para tu entrenador *",
                    placeholder = "Qué buscas mejorar y cómo te gustaría trabajar...",
                    required = true,
                    error = errors.coachMessage
                )
                SportsDropdown(
                    label = "Experiencia deportiva *",
                    value = profile.sportsAge,
                    options = SPORTS_AGE_OPTIONS,
                    onSelect = { selected -> onUpdate { it.copy(sportsAge = selected) } },
                    error = errors.sportsAge
                )
            }
        }
        item {
            SportsSection("Otros deportes", Icons.Default.Favorite) {
                Text(
                    "Selecciona todas las disciplinas que practicas.",
                    color = AppTextSecondary,
                    fontSize = 12.sp
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    OTHER_SPORTS.chunked(2).forEach { rowItems ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowItems.forEach { item ->
                                val selected = item.isSelected(profile.otherSports)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        onUpdate { current ->
                                            current.copy(
                                                otherSports = item.toggle(current.otherSports),
                                                otherSportName = if (item.key == "other" && selected) "" else current.otherSportName
                                            )
                                        }
                                    },
                                    label = { Text(item.label) },
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
                            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                AnimatedVisibility(profile.otherSports.other) {
                    OutlinedTextField(
                        value = profile.otherSportName,
                        onValueChange = { value -> if (value.length <= 80) onUpdate { it.copy(otherSportName = value) } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("¿Cuál otro deporte? *") },
                        singleLine = true,
                        isError = errors.otherSportName != null,
                        supportingText = errors.otherSportName?.let { message -> { Text(message) } },
                        shape = FormFieldShape,
                        colors = formFieldColors()
                    )
                }
            }
        }
    }
}

@Composable
private fun SportsTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    required: Boolean,
    error: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        minLines = 4,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        isError = error != null,
        supportingText = {
            Row(Modifier.fillMaxWidth()) {
                Text(error.orEmpty(), modifier = Modifier.weight(1f))
                Text("${value.length}/1000")
            }
        },
        shape = FormFieldShape,
        colors = formFieldColors()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SportsDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    error: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            isError = error != null,
            supportingText = error?.let { message -> { Text(message) } },
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
}

@Composable
private fun SportsSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    var expanded by remember { mutableStateOf(true) }
    Card(
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppBorder),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(AppPrimarySoft),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(19.dp)) }
                Text(title, color = AppTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ExpandMore, null, tint = AppTextSecondary, modifier = Modifier.rotate(if (expanded) 180f else 0f))
            }
            AnimatedVisibility(expanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(color = AppBorder)
                    content()
                }
            }
        }
    }
}

@Composable
private fun SportsHeader(onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = AppTextPrimary) }
        Column {
            Text("Información deportiva", color = AppTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Tu experiencia y contexto de entrenamiento", color = AppTextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SportsSaveBar(saving: Boolean, onSave: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = AppSurface), shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)) {
        Button(
            onClick = onSave,
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            if (saving) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            else {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.size(8.dp))
                Text("Actualizar información")
            }
        }
    }
}

@Composable
private fun SportsLoadError(message: String, onBack: () -> Unit, onRetry: () -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(8.dp)) {
        SportsHeader(onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(message, color = AppTextSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) { Text("Reintentar") }
        }
    }
}

private data class OtherSportOption(
    val key: String,
    val label: String,
    val isSelected: (OtherSports) -> Boolean,
    val toggle: (OtherSports) -> OtherSports
)

private val SPORTS_AGE_OPTIONS = listOf("< 1 Año", "1 - 2 Años", "3 - 5 Años", "6 - 10 Años", "10 + Años")
private val SUBJECTIVE_LEVELS = listOf("Básico" to 1, "Intermedio" to 5, "Avanzado" to 8)
private val OTHER_SPORTS = listOf(
    OtherSportOption("cycling", "Ciclismo", { it.cycling }, { it.copy(cycling = !it.cycling) }),
    OtherSportOption("swimming", "Natación", { it.swimming }, { it.copy(swimming = !it.swimming) }),
    OtherSportOption("triathlon", "Triatlón", { it.triathlon }, { it.copy(triathlon = !it.triathlon) }),
    OtherSportOption("gym", "Gimnasio", { it.gym }, { it.copy(gym = !it.gym) }),
    OtherSportOption("trail", "Trail", { it.trail }, { it.copy(trail = !it.trail) }),
    OtherSportOption("other", "Otro", { it.other }, { it.copy(other = !it.other) })
)
