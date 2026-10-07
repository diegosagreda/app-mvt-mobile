package com.example.mvt.trainer.calendar.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.mvt.ui.screens.RoutinesContentMode
import com.example.mvt.ui.screens.RoutinesScreen
import com.example.mvt.trainer.calendar.viewmodel.TrainerAthleteCalendarUiState
import com.example.mvt.trainer.calendar.viewmodel.TrainerAthleteCalendarViewModel
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppError
import com.example.mvt.ui.theme.AppSuccess
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue
import com.google.firebase.auth.FirebaseAuth

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TrainerAthleteCalendarScreen(
    athleteId: String,
    navController: NavController,
    onBack: () -> Unit,
    viewModel: TrainerAthleteCalendarViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var contentMode by remember { mutableStateOf(RoutinesContentMode.CALENDAR) }
    LaunchedEffect(athleteId) { viewModel.load(athleteId) }

    when (val current = state) {
        TrainerAthleteCalendarUiState.Loading -> CenteredState("Cargando calendario...")
        is TrainerAthleteCalendarUiState.Error -> CenteredState(current.message, AppError)
        is TrainerAthleteCalendarUiState.Success -> {
            val info = current.info
            Column(Modifier.fillMaxSize().background(AppBackground)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = AppTextPrimary)
                    }
                    Text("Calendario de rutinas", color = AppTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalendarModeButton(
                        label = "Calendario",
                        selected = contentMode == RoutinesContentMode.CALENDAR,
                        onClick = { contentMode = RoutinesContentMode.CALENDAR },
                        modifier = Modifier.weight(1f)
                    )
                    CalendarModeButton(
                        label = "Estadísticas",
                        selected = contentMode == RoutinesContentMode.STATISTICS,
                        onClick = { contentMode = RoutinesContentMode.STATISTICS },
                        modifier = Modifier.weight(1f)
                    )
                }
                RoutinesScreen(
                    navController = navController,
                    onRoutineClick = {},
                    currentAthleteId = athleteId,
                    ritmos = null,
                    zonas = null,
                    athleteName = info.name,
                    trainerId = FirebaseAuth.getInstance().currentUser?.uid,
                    contentMode = contentMode,
                    headerContent = if (contentMode == RoutinesContentMode.CALENDAR) {
                        { TrainingDaysBar(info.trainingDays) }
                    } else null,
                    footerContent = if (contentMode == RoutinesContentMode.CALENDAR) {
                        { AthleteSummaryCard(info) }
                    } else null,
                    onRoutineNavigate = {}
                )
            }

        }
    }
}

@Composable
private fun CalendarModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Text(
        text = label,
        modifier = modifier
            .clickable(onClick = onClick)
            .background(if (selected) PrimaryBlue else AppSurface, RoundedCornerShape(8.dp))
            .border(1.dp, if (selected) PrimaryBlue else AppBorder, RoundedCornerShape(8.dp))
            .padding(vertical = 9.dp),
        color = if (selected) Color.White else AppTextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

@Composable
private fun TrainingDaysBar(days: Map<String, Boolean>) {
    val labels = listOf(
        "lunes" to "Lunes", "martes" to "Martes", "miercoles" to "Miércoles",
        "jueves" to "Jueves", "viernes" to "Viernes", "sabado" to "Sábado",
        "domingo" to "Domingo"
    )
    Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
        Text("Días Entrenamiento:", color = AppTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            labels.forEach { (key, label) ->
                Text(
                    label.take(3),
                    modifier = Modifier
                        .weight(1f)
                        .background(if (days[key] == true) AppSuccess.copy(alpha = .18f) else AppSurface, RoundedCornerShape(6.dp))
                        .border(1.dp, if (days[key] == true) AppSuccess else AppBorder, RoundedCornerShape(6.dp))
                        .padding(vertical = 6.dp),
                    color = if (days[key] == true) AppSuccess else AppTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AthleteSummaryCard(info: com.example.mvt.trainer.calendar.viewmodel.TrainerAthleteCalendarInfo) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = info.photoUrl,
                contentDescription = "Foto de ${info.name}",
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.size(12.dp))
            Column {
                Text(info.name, color = AppTextPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CenteredState(message: String, color: Color = AppTextSecondary) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (color == AppTextSecondary) CircularProgressIndicator(color = PrimaryBlue)
        else Text(message, color = color, modifier = Modifier.padding(24.dp))
    }
}
