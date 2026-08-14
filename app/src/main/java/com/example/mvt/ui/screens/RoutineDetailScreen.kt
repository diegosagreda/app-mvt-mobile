package com.example.mvt.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.mvt.data.firebase.models.Routine
import com.example.mvt.data.firebase.models.StravaActivity
import com.example.mvt.domain.repositories.StravaRepository
import com.example.mvt.ui.components.RoutineInfoSection
import com.example.mvt.ui.components.StravaActivityPickerSheet
import com.example.mvt.ui.components.StravaSyncResultPanel
import com.example.mvt.ui.components.RoutineSummaryCard
import com.example.mvt.ui.components.chart.RoutineChart
import com.example.mvt.ui.components.header.RoutineHeader
import com.example.mvt.ui.screens.components.RoutinePhasesSection
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDetailScreen(
    routine: Routine,
    ritmos: Map<String, Any>?,
    zonas: Map<String, Any>?,
    onBackClick: () -> Unit
) {
    var routineState by remember(routine.id) { mutableStateOf(routine) }
    val stravaRepository = remember { StravaRepository() }
    val scope = rememberCoroutineScope()

    val fechaFormatted = remember(routineState.fecha) {
        routineState.fecha?.toDate()?.let {
            SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(it)
        } ?: "Sin fecha"
    }

    Log.d("RoutineDetailScreen", "Rutina ID ${routineState.id} - Titulo ${routineState.titulo}")

    var expandedInfo by remember { mutableStateOf(true) }
    var expandedGraph by remember { mutableStateOf(false) }
    var expandedPhases by remember { mutableStateOf(false) }
    var expandedStrava by remember { mutableStateOf(false) }
    var showStravaActivityPicker by remember { mutableStateOf(false) }
    var isLoadingStravaActivities by remember { mutableStateOf(false) }
    var isSyncingSelectedStravaActivity by remember { mutableStateOf(false) }
    var showUnlinkStravaDialog by remember { mutableStateOf(false) }
    var isUnlinkingStravaActivity by remember { mutableStateOf(false) }
    var stravaActivitySearchQuery by remember { mutableStateOf("") }
    var stravaActivityQueryDate by remember { mutableStateOf(LocalDate.now()) }
    var stravaActivities by remember { mutableStateOf<List<StravaActivity>>(emptyList()) }
    var selectedStravaActivityId by remember { mutableStateOf<String?>(null) }
    var stravaActivitiesMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    val hasStravaAnalysis = routineState.strava?.analisis != null
    val hasStravaActivity = routineState.strava?.actividad != null
    val hasActiveStravaSync = hasStravaAnalysis || hasStravaActivity || routineState.isStravaSynced
    val stravaButtonLabel = if (hasActiveStravaSync) {
        "Resultados Strava"
    } else {
        "Sincronizar actividad"
    }
    val loadStravaActivities: (LocalDate) -> Unit = { targetDate ->
        isLoadingStravaActivities = true
        stravaActivities = emptyList()
        selectedStravaActivityId = null
        stravaActivitiesMessage = null

        scope.launch {
            runCatching {
                stravaRepository.listActivities(
                    idDeportista = routineState.id_deportista,
                    fecha = targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    limit = 20
                )
            }
                .onSuccess { result ->
                    stravaActivities = result.actividades
                    stravaActivitiesMessage = when {
                        result.estado == "sin_conexion" -> result.mensaje
                        result.actividades.isEmpty() -> result.mensaje.ifBlank {
                            "No encontramos actividades para la fecha consultada."
                        }
                        else -> null
                    }
                }
                .onFailure { error ->
                    stravaActivities = emptyList()
                    stravaActivitiesMessage =
                        error.message ?: "No fue posible listar las actividades de Strava."
                }

            isLoadingStravaActivities = false
        }
    }
    val openStravaActivityPicker = {
        val today = LocalDate.now()
        stravaActivitySearchQuery = ""
        stravaActivityQueryDate = today
        stravaActivitiesMessage = null
        showStravaActivityPicker = true
        loadStravaActivities(today)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // === CABECERA ===
            RoutineHeader(
                titulo = routineState.titulo,
                fecha = fechaFormatted,
                tipoMedicion = routineState.tipo_medicion,
                showStravaToggle = true,
                stravaExpanded = expandedStrava,
                stravaConnected = hasActiveStravaSync,
                stravaButtonLabel = stravaButtonLabel,
                onStravaToggle = { expandedStrava = !expandedStrava },
                onBackClick = onBackClick
            )

            AnimatedVisibility(
                visible = expandedStrava,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                StravaSyncResultPanel(
                    strava = routineState.strava,
                    onSyncClick = openStravaActivityPicker,
                    isSyncLoading = isLoadingStravaActivities,
                    onUnlinkClick = { showUnlinkStravaDialog = true },
                    isUnlinkLoading = isUnlinkingStravaActivity,
                    modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AccordionSection(
                title = "Información Rutina",
                subtitle = "Datos clave, descripción y objetivos",
                expanded = expandedInfo,
                onExpandChange = { expandedInfo = !expandedInfo },
                useContentContainer = false
            ) {
                RoutineInfoSection(
                    tipoEsfuerzo = routineState.tipo_esfuerzo,
                    tipoMedicion = routineState.tipo_medicion,
                    tipoTerreno = routineState.tipo_terreno,
                    descripcion = routineState.descripcion,
                    objetivos = routineState.objetivos
                )
            }

            AccordionSection(
                title = "Gráfica Rutina",
                subtitle = "Vista visual de la carga inicial",
                expanded = expandedGraph,
                onExpandChange = { expandedGraph = !expandedGraph },
                useContentContainer = false
            ) {
                val centralSeries =
                    routineState.sesiones_central?.get("series") as? List<*>
                val hasGraphContent =
                    !routineState.sesiones_calentamiento.isNullOrEmpty() ||
                    !routineState.sesiones_calma.isNullOrEmpty() ||
                    !centralSeries.isNullOrEmpty()

                if (hasGraphContent) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        RoutineChart(
                            routine = routineState,
                            ritmos = ritmos,
                            zonas = zonas,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Text(
                        text = "No hay ejercicios en la fase de calentamiento.",
                        color = AppTextSecondary
                    )
                }
            }

            AccordionSection(
                title = "Fases Rutina",
                subtitle = "Bloques, series y recursos de la sesión",
                expanded = expandedPhases,
                onExpandChange = { expandedPhases = !expandedPhases },
                useContentContainer = false,
                contentHorizontalPadding = 4.dp
            ) {
                RoutinePhasesSection(
                    calentamiento = routineState.sesiones_calentamiento ?: emptyList(),
                    central = routineState.sesiones_central ?: emptyMap(),
                    vuelta = routineState.sesiones_calma ?: emptyList(),
                    comentarios_fase_calentamiento = routineState.comentarios_fase_calentamiento,
                    comentarios_fase_central = routineState.comentarios_fase_central,
                    comentarios_fase_calma = routineState.comentarios_fase_calma,
                    recursosCalentamiento = routineState.videosCalentamiento,
                    recursosCentral = routineState.videosCentral,
                    recursosCalma = routineState.videosCalma,
                    ritmos = ritmos,   // ← pasa los datos
                    zonas = zonas,      // ← pasa los datos
                    tipoMedicion = routineState.tipo_medicion
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }


        // === ETIQUETA FLOTANTE (RESUMEN RUTINA) TOTALES ===
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            RoutineSummaryCard(
                routine = routineState,
                onEstadoActualizado = { nuevoEstado ->
                    routineState = routineState.copy(estado = nuevoEstado)
                }
            )
        }

    }

    if (showStravaActivityPicker) {
        StravaActivityPickerSheet(
            activities = stravaActivities,
            isLoading = isLoadingStravaActivities,
            queryDate = stravaActivityQueryDate,
            searchQuery = stravaActivitySearchQuery,
            selectedActivityId = selectedStravaActivityId,
            isSyncing = isSyncingSelectedStravaActivity,
            message = stravaActivitiesMessage,
            onDismiss = { showStravaActivityPicker = false },
            onSearchQueryChange = { stravaActivitySearchQuery = it },
            onDateChange = { selectedDate ->
                stravaActivityQueryDate = selectedDate
                loadStravaActivities(selectedDate)
            },
            onActivitySelected = { activity ->
                selectedStravaActivityId = activity.id
            },
            onSyncSelected = {
                val selectedActivity = stravaActivities.firstOrNull { it.id == selectedStravaActivityId }
                if (selectedActivity == null) {
                    stravaActivitiesMessage = "Selecciona una actividad antes de sincronizar."
                } else {
                    isSyncingSelectedStravaActivity = true
                    stravaActivitiesMessage = null

                    scope.launch {
                        runCatching {
                            stravaRepository.linkActivityToRoutine(
                                idDeportista = routineState.id_deportista,
                                routineId = routineState.id,
                                activityId = selectedActivity.id
                            )
                        }
                            .onSuccess { result ->
                                if (result.estado == "actividad_detectada" && result.strava != null) {
                                    val nuevoEstado = when (result.cumplimiento?.estado?.lowercase()) {
                                        "realizada" -> "Realizada"
                                        "parcial" -> "Parcial"
                                        else -> routineState.estado
                                    }

                                    routineState = routineState.copy(
                                        estado = nuevoEstado,
                                        isStravaSynced = true,
                                        stravaActivityId = result.actividad?.id ?: selectedActivity.id,
                                        strava = result.strava
                                    )
                                    expandedStrava = true
                                    showStravaActivityPicker = false
                                    selectedStravaActivityId = null
                                    stravaActivitiesMessage = null
                                } else {
                                    stravaActivitiesMessage = result.mensaje.ifBlank {
                                        "No fue posible sincronizar la actividad seleccionada."
                                    }
                                }
                            }
                            .onFailure { error ->
                                stravaActivitiesMessage =
                                    error.message ?: "No fue posible sincronizar la actividad seleccionada."
                            }

                        isSyncingSelectedStravaActivity = false
                    }
                }
            }
        )
    }

    if (showUnlinkStravaDialog) {
        Dialog(
            onDismissRequest = {
                if (!isUnlinkingStravaActivity) {
                    showUnlinkStravaDialog = false
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = AppSurface,
                    border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.8f)),
                    tonalElevation = 10.dp,
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(Color(0x18FC4C02), CircleShape)
                                    .border(1.dp, Color(0x33FC4C02), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HelpOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFFC4C02),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Desvincular actividad",
                                    color = AppTextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Se retirara de esta rutina.",
                                    color = AppTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Text(
                            text = "Si estas seguro, desvincula esta actividad de la rutina.",
                            color = AppTextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showUnlinkStravaDialog = false },
                                enabled = !isUnlinkingStravaActivity,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, AppBorder)
                            ) {
                                Text("Cancelar")
                            }

                            Button(
                                onClick = {
                                    isUnlinkingStravaActivity = true

                                    scope.launch {
                                        runCatching {
                                            stravaRepository.unlinkActivityFromRoutine(
                                                idDeportista = routineState.id_deportista,
                                                routineId = routineState.id
                                            )
                                        }
                                            .onSuccess { result ->
                                                if (result.estado == "sin_actividad" && result.strava != null) {
                                                    routineState = routineState.copy(
                                                        estado = "Pendiente",
                                                        isStravaSynced = false,
                                                        stravaActivityId = "",
                                                        strava = result.strava
                                                    )
                                                    expandedStrava = true
                                                    showUnlinkStravaDialog = false
                                                }
                                            }
                                            .onFailure { error ->
                                                routineState = routineState.copy(
                                                    strava = routineState.strava?.copy(
                                                        error = error.message ?: "No fue posible desvincular la actividad."
                                                    )
                                                )
                                                showUnlinkStravaDialog = false
                                            }

                                        isUnlinkingStravaActivity = false
                                    }
                                },
                                enabled = !isUnlinkingStravaActivity,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFC4C02),
                                    disabledContainerColor = Color(0x99FC4C02)
                                )
                            ) {
                                if (isUnlinkingStravaActivity) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Desvincular")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccordionSection(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onExpandChange: () -> Unit,
    useContentContainer: Boolean = true,
    contentHorizontalPadding: Dp = 14.dp,
    content: @Composable () -> Unit
) {
    val style = accordionSectionStyle(title)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = if (expanded) AppSurfaceAlt else AppSurface,
            border = BorderStroke(
                1.dp,
                if (expanded) style.accent.copy(alpha = 0.32f) else AppBorder.copy(alpha = 0.85f)
            ),
            tonalElevation = if (expanded) 4.dp else 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
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
                            .background(style.accent.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                            .border(
                                width = 1.dp,
                                color = style.accent.copy(alpha = 0.28f),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = style.icon,
                            contentDescription = null,
                            tint = style.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            color = AppTextPrimary,
                            fontSize = 16.sp
                        )
                        Text(
                            text = subtitle,
                            color = AppTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee()
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (expanded) style.accent.copy(alpha = 0.14f) else AppSurfaceAlt,
                        border = BorderStroke(
                            1.dp,
                            if (expanded) style.accent.copy(alpha = 0.24f) else AppBorder.copy(alpha = 0.75f)
                        )
                    ) {
                        Text(
                            text = if (expanded) "Abierto" else "Abrir",
                            color = if (expanded) style.accent else AppTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                }

                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    if (useContentContainer) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                                .background(AppSurface.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .border(1.dp, AppBorder.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            content()
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = contentHorizontalPadding,
                                    end = contentHorizontalPadding,
                                    bottom = 10.dp
                                )
                        ) {
                            content()
                        }
                    }
                }
            }
        }
    }
}

private data class AccordionSectionStyle(
    val icon: ImageVector,
    val accent: Color
)

private fun accordionSectionStyle(title: String): AccordionSectionStyle {
    return when (title) {
        "Información Rutina" -> AccordionSectionStyle(
            icon = Icons.Default.Article,
            accent = Color(0xFF7EA8FF)
        )
        "Gráfica Rutina" -> AccordionSectionStyle(
            icon = Icons.Default.ShowChart,
            accent = Color(0xFF4FD0A5)
        )
        "Fases Rutina" -> AccordionSectionStyle(
            icon = Icons.Default.FitnessCenter,
            accent = Color(0xFFFFA257)
        )
        else -> AccordionSectionStyle(
            icon = Icons.Default.Article,
            accent = PrimaryBlue
        )
    }
}
