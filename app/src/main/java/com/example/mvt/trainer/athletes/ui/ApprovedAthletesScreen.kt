package com.example.mvt.trainer.athletes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import coil.compose.AsyncImage
import com.example.mvt.trainer.athletes.model.ApprovedAthlete
import com.example.mvt.trainer.athletes.viewmodel.ApprovedAthletesUiState
import com.example.mvt.trainer.athletes.viewmodel.ApprovedAthletesViewModel
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppError
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppSuccess
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue

@Composable
fun ApprovedAthletesScreen(
    onBack: () -> Unit,
    viewModel: ApprovedAthletesViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var search by remember { mutableStateOf("") }
    var selectedPlan by remember { mutableStateOf("Plata") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = AppTextPrimary)
            }
            Text(
                text = "Deportistas aprobados",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppTextPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Buscar deportista...") },
            shape = RoundedCornerShape(8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        PlanFilter(
            selectedPlan = selectedPlan,
            onPlanSelected = { selectedPlan = it }
        )
        Spacer(modifier = Modifier.height(16.dp))

        when (val current = state) {
            ApprovedAthletesUiState.Loading -> LoadingState()
            is ApprovedAthletesUiState.Error -> ErrorState(current.message, viewModel::load)
            is ApprovedAthletesUiState.Success -> {
                val athletes = current.athletes.filter {
                    (selectedPlan == "Todos" || it.planName.contains(selectedPlan, ignoreCase = true)) &&
                        it.name.contains(search.trim(), ignoreCase = true)
                }
                if (athletes.isEmpty()) {
                    EmptyState(selectedPlan)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(athletes, key = { it.id }) { athlete ->
                            ApprovedAthleteCard(athlete)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanFilter(selectedPlan: String, onPlanSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            shape = RoundedCornerShape(8.dp),
            color = AppSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
        ) {
            Text(
                text = "Plan: $selectedPlan",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                color = AppTextPrimary,
                fontSize = 13.sp
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("Plata", "Bronce", "Todos").forEach { plan ->
                DropdownMenuItem(
                    text = { Text(plan) },
                    onClick = {
                        expanded = false
                        onPlanSelected(plan)
                    }
                )
            }
        }
    }
}

@Composable
private fun ApprovedAthleteCard(athlete: ApprovedAthlete) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    if (athlete.photoUrl.isNullOrBlank()) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AppTextPrimary)
                    } else {
                        AsyncImage(
                            model = athlete.photoUrl,
                            contentDescription = "Foto de ${athlete.name}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(athlete.name, fontWeight = FontWeight.Bold, color = AppTextPrimary)
                    Text(
                        text = formatPlanName(athlete.planName),
                        fontSize = 12.sp,
                        color = PrimaryBlue
                    )
                    Text(athlete.sport, fontSize = 12.sp, color = AppTextSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Deportista",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTextPrimary
                    )
                    Text(
                        text = athlete.status,
                        fontSize = 10.sp,
                        color = AppTextSecondary
                    )
                    Text(
                        text = "Sus. Activa",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppSuccess
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text("Crear Rutina", color = Color.White, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text("Ver Perfil", color = PrimaryBlue, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun formatPlanName(planName: String): String {
    val cleanName = planName.trim()
    return if (cleanName.startsWith("Plan ", ignoreCase = true)) {
        cleanName
    } else {
        "Plan $cleanName"
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = PrimaryBlue)
    }
}

@Composable
private fun EmptyState(selectedPlan: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            if (selectedPlan == "Todos") "No se han encontrado deportistas."
            else "No hay deportistas en $selectedPlan.",
            color = AppTextSecondary
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = AppError)
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reintentar")
        }
    }
}
