package com.example.mvt.trainer.requests.ui

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.mvt.trainer.requests.model.PendingTrainerRequest
import com.example.mvt.trainer.requests.viewmodel.PendingTrainerRequestsUiState
import com.example.mvt.trainer.requests.viewmodel.PendingTrainerRequestsViewModel
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppError
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue

@Composable
fun PendingTrainerRequestsScreen(
    onBack: () -> Unit,
    viewModel: PendingTrainerRequestsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var search by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().background(AppBackground).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Volver", tint = AppTextPrimary)
            }
            Text("Solicitudes pendientes", color = AppTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Buscar atleta...") },
            shape = RoundedCornerShape(8.dp)
        )
        Spacer(Modifier.height(16.dp))

        when (val current = state) {
            PendingTrainerRequestsUiState.Loading -> CenteredLoading()
            is PendingTrainerRequestsUiState.Error -> ErrorContent(current.message, viewModel::load)
            is PendingTrainerRequestsUiState.Success -> {
                val filtered = current.requests.filter {
                    it.athleteName.contains(search.trim(), ignoreCase = true)
                }
                if (filtered.isEmpty()) {
                    EmptyContent(if (search.isBlank()) "No tienes solicitudes pendientes." else "No se encontraron resultados.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filtered, key = { it.id }) { request ->
                            PendingRequestCard(
                                request = request,
                                onAccept = { viewModel.updateStatus(request, "Aprobado") },
                                onReject = { viewModel.updateStatus(request, "Rechazado") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingRequestCard(
    request: PendingTrainerRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().border(1.dp, AppBorder, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(54.dp).clip(CircleShape).background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    if (request.photoUrl.isNullOrBlank()) {
                        Icon(Icons.Default.Person, null, tint = Color.White)
                    } else {
                        AsyncImage(
                            model = request.photoUrl,
                            contentDescription = "Foto de ${request.athleteName}",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(request.athleteName, color = AppTextPrimary, fontWeight = FontWeight.Bold)
                    Text(request.sport, color = AppTextSecondary, fontSize = 12.sp)
                }
                Text("Pendiente", color = AppTextSecondary, fontSize = 11.sp)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) { Text("Aceptar", color = Color.White, fontSize = 12.sp) }
                Button(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AppError),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) { Text("Rechazar", color = Color.White, fontSize = 12.sp) }
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
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = AppError)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(6.dp))
            Text("Reintentar")
        }
    }
}
