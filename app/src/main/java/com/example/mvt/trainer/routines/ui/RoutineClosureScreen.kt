package com.example.mvt.trainer.routines.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mvt.trainer.routines.model.RoutineClosureItem
import com.example.mvt.trainer.routines.viewmodel.RoutineClosureUiState
import com.example.mvt.trainer.routines.viewmodel.RoutineClosureViewModel
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppError
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun RoutineClosureScreen(
    onBack: () -> Unit,
    viewModel: RoutineClosureViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var search by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todas") }

    Column(Modifier.fillMaxSize().background(AppBackground).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Volver", tint = AppTextPrimary)
            }
            Text("Rutinas por cerrar", color = AppTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text("Buscar rutina o atleta...") },
            shape = RoundedCornerShape(8.dp)
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Todas", "Pendientes", "Borradores").forEach { filter ->
                FilterChip(filter, selectedFilter == filter) { selectedFilter = filter }
            }
        }
        Spacer(Modifier.height(16.dp))

        when (val current = state) {
            RoutineClosureUiState.Loading -> CenteredLoading()
            is RoutineClosureUiState.Error -> ErrorContent(current.message, viewModel::load)
            is RoutineClosureUiState.Success -> {
                val filtered = current.routines.filter { routine ->
                    val matchesFilter = selectedFilter == "Todas" ||
                        (selectedFilter == "Pendientes" && !routine.isDraft) ||
                        (selectedFilter == "Borradores" && routine.isDraft)
                    matchesFilter && (
                        routine.title.contains(search.trim(), true) ||
                            routine.athleteName.contains(search.trim(), true)
                        )
                }
                if (filtered.isEmpty()) {
                    EmptyContent(if (search.isBlank()) "No hay rutinas pendientes o borradores." else "No se encontraron resultados.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filtered, key = { "${it.id}-${it.status}" }) {
                            RoutineClosureCard(it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.border(1.dp, if (selected) PrimaryBlue else AppBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) PrimaryBlue else AppSurface,
        onClick = onClick
    ) {
        Text(label, color = if (selected) Color.White else AppTextSecondary, fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

@Composable
private fun RoutineClosureCard(routine: RoutineClosureItem) {
    Card(
        Modifier.fillMaxWidth().border(1.dp, AppBorder, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(routine.title, color = AppTextPrimary, fontWeight = FontWeight.Bold)
                    Text("Atleta: ${routine.athleteName}", color = AppTextSecondary, fontSize = 12.sp)
                }
                Text(
                    routine.status,
                    color = if (routine.isDraft) AppTextSecondary else PrimaryBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            routine.date?.toDate()?.let {
                Text(
                    SimpleDateFormat("dd MMM yyyy", Locale("es", "ES")).format(it),
                    color = AppTextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable private fun CenteredLoading() {
    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = PrimaryBlue)
    }
}

@Composable private fun EmptyContent(message: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(message, color = AppTextSecondary)
    }
}

@Composable private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, color = AppError)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(6.dp))
            Text("Reintentar")
        }
    }
}
