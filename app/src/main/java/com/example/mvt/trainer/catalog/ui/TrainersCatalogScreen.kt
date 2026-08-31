package com.example.mvt.trainer.catalog.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mvt.R
import com.example.mvt.trainer.catalog.model.AthletePlan
import com.example.mvt.trainer.catalog.model.TrainerCardModel
import com.example.mvt.trainer.catalog.model.TrainerRelationship
import com.example.mvt.trainer.catalog.model.TrainersCatalogState
import com.example.mvt.trainer.catalog.viewmodel.TrainersCatalogViewModel
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue
import java.text.Normalizer
import kotlin.math.ceil
import kotlin.math.roundToInt

private val CatalogStar = Color(0xFFFFC857)
private val CatalogAccentBlue = Color(0xFF4DA3FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainersCatalogScreen(
    athleteId: String,
    viewModel: TrainersCatalogViewModel,
    onBack: () -> Unit,
    onOpenAssignedTrainer: () -> Unit,
    onUpgradePlan: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val action by viewModel.action.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var search by remember { mutableStateOf("") }
    var pageSize by remember { mutableIntStateOf(5) }
    var page by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<TrainerCardModel?>(null) }
    var sendTarget by remember { mutableStateOf<TrainerCardModel?>(null) }
    var cancelTarget by remember { mutableStateOf<TrainerCardModel?>(null) }
    var warningMessage by remember { mutableStateOf<String?>(null) }
    var warningOffersUpgrade by remember { mutableStateOf(false) }

    LaunchedEffect(athleteId) { viewModel.start(athleteId) }
    LaunchedEffect(action.message) {
        action.message?.let { snackbar.showSnackbar(it) }
        if (action.message != null) viewModel.clearAction()
    }
    LaunchedEffect(search, pageSize) { page = 0 }

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        when (val current = state) {
            TrainersCatalogState.Loading -> CatalogLoading(Modifier.padding(padding))
            TrainersCatalogState.Empty -> CatalogMessage("No hay entrenadores disponibles.", onBack, Modifier.padding(padding))
            is TrainersCatalogState.Error -> CatalogMessage(current.message, onBack, Modifier.padding(padding))
            is TrainersCatalogState.Ready -> {
                val filtered = remember(current.trainers, search) {
                    val term = normalize(search)
                    current.trainers.filter { term.isBlank() || normalize(it.trainer.fullName).contains(term) }
                }
                val pages = ceil(filtered.size / pageSize.toDouble()).toInt().coerceAtLeast(1)
                if (page >= pages) page = pages - 1
                val visible = filtered.drop(page * pageSize).take(pageSize)
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { CatalogHeader(onBack) }
                    if (current.assignedTrainerId.isBlank()) item {
                        Text(
                            "Selecciona un entrenador que se ajuste a tus necesidades deportivas.",
                            color = AppTextSecondary, fontSize = 14.sp, lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    item {
                        CatalogToolbar(search, { search = it }, pageSize, { pageSize = it })
                    }
                    if (visible.isEmpty()) item {
                        EmptySearch()
                    } else {
                        items(visible, key = { it.trainer.id }) { card ->
                            TrainerCatalogCard(
                                card = card,
                                busy = action.isWorking,
                                onProfile = {
                                    if (card.relationship == TrainerRelationship.ASSIGNED) onOpenAssignedTrainer()
                                    else selected = card
                                },
                                onSend = {
                                    val warning = requestWarning(current.plan, current.assignedTrainerId)
                                    if (warning != null) {
                                        warningMessage = warning
                                        warningOffersUpgrade = current.assignedTrainerId.isBlank()
                                    } else sendTarget = card
                                },
                                onCancel = { cancelTarget = card }
                            )
                        }
                    }
                    item { Pagination(page, pages, filtered.size, pageSize, { page-- }, { page++ }) }
                }

                selected?.let { card ->
                    TrainerPreviewDialog(
                        card = card,
                        busy = action.isWorking,
                        onDismiss = { selected = null },
                        onSend = {
                            val warning = requestWarning(current.plan, current.assignedTrainerId)
                            if (warning == null) {
                                selected = null
                                sendTarget = card
                            } else {
                                warningMessage = warning
                                warningOffersUpgrade = current.assignedTrainerId.isBlank()
                            }
                        },
                        onCancel = {
                            selected = null
                            cancelTarget = card
                        }
                    )
                }
                sendTarget?.let { card ->
                    ConfirmDialog(
                        title = "Enviar solicitud",
                        message = "¿Quieres enviar una solicitud a ${card.trainer.fullName}?",
                        confirmLabel = "Enviar",
                        busy = action.isWorking,
                        onDismiss = { sendTarget = null },
                        onConfirm = {
                            viewModel.sendRequest(current.athlete, card.trainer)
                            sendTarget = null
                            selected = null
                        }
                    )
                }
                cancelTarget?.pendingRequest?.let { request ->
                    ConfirmDialog(
                        title = "Cancelar solicitud",
                        message = "¿Quieres cancelar la solicitud enviada a ${cancelTarget?.trainer?.fullName}?",
                        confirmLabel = "Cancelar solicitud",
                        busy = action.isWorking,
                        destructive = true,
                        onDismiss = { cancelTarget = null },
                        onConfirm = {
                            viewModel.cancelRequest(athleteId, request)
                            cancelTarget = null
                            selected = null
                        }
                    )
                }
                warningMessage?.let { warning ->
                    AlertDialog(
                        onDismissRequest = { warningMessage = null },
                        containerColor = AppSurface,
                        title = { Text("Solicitud no disponible") },
                        text = { Text(warning) },
                        confirmButton = {
                            if (warningOffersUpgrade) {
                                Button(onClick = { warningMessage = null; onUpgradePlan() }) {
                                    Text("Mejorar plan")
                                }
                            } else {
                                Button(onClick = { warningMessage = null }) { Text("Entendido") }
                            }
                        },
                        dismissButton = {
                            if (warningOffersUpgrade) TextButton(onClick = { warningMessage = null }) { Text("Cerrar") }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogHeader(onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = AppTextPrimary) }
        Column {
            Text("Entrenadores", color = AppTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Encuentra tu próximo entrenador", color = AppTextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CatalogToolbar(search: String, onSearch: (String) -> Unit, pageSize: Int, onPageSize: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = AppSurface), border = BorderStroke(1.dp, AppBorder), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = search, onValueChange = onSearch, modifier = Modifier.weight(1f), singleLine = true,
                placeholder = { Text("Buscar por nombre") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(12.dp)
            )
            Box {
                OutlinedButton(onClick = { expanded = true }, contentPadding = PaddingValues(horizontal = 12.dp)) { Text(pageSize.toString()) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf(5, 10, 15, 20).forEach { size ->
                        DropdownMenuItem(text = { Text("$size registros") }, onClick = { onPageSize(size); expanded = false })
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainerCatalogCard(
    card: TrainerCardModel,
    busy: Boolean,
    onProfile: () -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit
) {
    val assigned = card.relationship == TrainerRelationship.ASSIGNED
    Card(
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, if (assigned) CatalogAccentBlue else AppBorder),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncImage(
                    model = card.trainer.fotoUrl.ifBlank { R.drawable.placeholder }, contentDescription = null,
                    modifier = Modifier.size(68.dp).clip(CircleShape).clickable(onClick = onProfile), contentScale = ContentScale.Crop
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(card.trainer.fullName, color = AppTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    CatalogStars(card.trainer.estrellas)
                    when (card.relationship) {
                        TrainerRelationship.ASSIGNED -> StatusText("Este es tu entrenador", CatalogAccentBlue)
                        TrainerRelationship.PENDING -> StatusText("Solicitud pendiente", Color(0xFFFFB74D))
                        TrainerRelationship.AVAILABLE -> if (card.trainer.especialidad.isNotBlank()) Text(card.trainer.especialidad, color = AppTextSecondary, fontSize = 12.sp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onProfile, modifier = Modifier.weight(1f), enabled = !busy) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Visitar perfil")
                }
                when (card.relationship) {
                    TrainerRelationship.PENDING -> Button(
                        onClick = onCancel, modifier = Modifier.weight(1f), enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Cancel, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Cancelar")
                    }
                    TrainerRelationship.AVAILABLE -> Button(
                        onClick = onSend, modifier = Modifier.weight(1f), enabled = !busy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Solicitar")
                    }
                    TrainerRelationship.ASSIGNED -> Unit
                }
            }
        }
    }
}

@Composable private fun StatusText(text: String, color: Color) = Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

@Composable
private fun CatalogStars(value: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { index -> Icon(Icons.Default.Star, null, tint = if (index < value.roundToInt()) CatalogStar else AppBorder, modifier = Modifier.size(17.dp)) }
        Text(if (value > 0) " %.1f".format(value) else " Sin calificar", color = AppTextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun Pagination(page: Int, pages: Int, total: Int, pageSize: Int, previous: () -> Unit, next: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = previous, enabled = page > 0) { Icon(Icons.Default.ChevronLeft, "Anterior") }
        Text(if (total == 0) "0 resultados" else "Página ${page + 1} de $pages · $total entrenadores", color = AppTextSecondary, fontSize = 12.sp)
        IconButton(onClick = next, enabled = page + 1 < pages) { Icon(Icons.Default.ChevronRight, "Siguiente") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainerPreviewDialog(card: TrainerCardModel, busy: Boolean, onDismiss: () -> Unit, onSend: () -> Unit, onCancel: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showPhoto by remember { mutableStateOf(false) }
    val trainer = card.trainer

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppBackground,
        contentColor = AppTextPrimary,
        dragHandle = {
            Box(
                Modifier.padding(top = 10.dp, bottom = 6.dp).size(width = 44.dp, height = 4.dp)
                    .clip(CircleShape).background(AppBorder)
            )
        }
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.94f)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text("Perfil del entrenador", color = AppTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Información profesional y deportiva", color = AppTextSecondary, fontSize = 12.sp)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cerrar", tint = AppTextPrimary) }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { TrainerHero(trainer, onPhotoClick = { showPhoto = true }) }
                item { CatalogProfileRating(trainer.estrellas, card.relationship) }
                item {
                    SectionCard("Información personal", Icons.Default.LocationOn) {
                        CompactDetailGrid(
                            listOf(
                                "Género" to trainer.genero,
                                "Fecha de nacimiento" to trainer.fechaNacimiento
                            )
                        )
                        InlineDetail("Origen", joinCatalogLocation(trainer.pais, trainer.ciudad))
                        InlineDetail("Ubicación actual", joinCatalogLocation(trainer.paisActual, trainer.ciudadActual))
                    }
                }
                if (listOf(trainer.resena, trainer.hitos, trainer.especialidad, trainer.experiencia).any(String::isNotBlank)) item {
                    SectionCard("Trayectoria deportiva", Icons.Default.FitnessCenter) {
                        ExpandableDetail("Reseña profesional", trainer.resena)
                        ExpandableDetail("Hitos deportivos", trainer.hitos)
                        CompactDetailGrid(
                            listOf(
                                "Especialidad" to trainer.especialidad,
                                "Experiencia" to trainer.experiencia
                            )
                        )
                    }
                }
                if (listOf(trainer.perfil, trainer.formacionAcademica, trainer.certificaciones).any(String::isNotBlank)) item {
                    SectionCard("Perfil y formación", Icons.Default.School) {
                        Detail("Perfil profesional", trainer.perfil)
                        Detail("Formación académica", trainer.formacionAcademica)
                        Detail("Certificaciones", trainer.certificaciones)
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppSurface),
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
            ) {
                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                    when (card.relationship) {
                        TrainerRelationship.PENDING -> Button(
                            onClick = onCancel, enabled = !busy, modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Icon(Icons.Default.Cancel, null); Spacer(Modifier.size(8.dp)); Text("Cancelar solicitud") }
                        TrainerRelationship.AVAILABLE -> Button(
                            onClick = onSend, enabled = !busy, modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryBlue,
                                contentColor = Color.White
                            )
                        ) { Icon(Icons.AutoMirrored.Filled.Send, null); Spacer(Modifier.size(8.dp)); Text("Enviar solicitud") }
                        TrainerRelationship.ASSIGNED -> StatusText("Este es tu entrenador", CatalogAccentBlue)
                    }
                }
            }
        }
    }
    if (showPhoto) PhotoDialog(trainer, onDismiss = { showPhoto = false })
}

@Composable
private fun CatalogProfileRating(rating: Double, relationship: TrainerRelationship) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppSurfaceAlt),
        border = BorderStroke(1.dp, PrimaryBlue),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Calificación del entrenador", color = AppTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                CatalogStars(rating)
            }
            when (relationship) {
                TrainerRelationship.PENDING -> StatusText("Solicitud pendiente", Color(0xFFFFB74D))
                TrainerRelationship.AVAILABLE -> StatusText("Disponible", CatalogAccentBlue)
                TrainerRelationship.ASSIGNED -> StatusText("Tu entrenador", CatalogAccentBlue)
            }
        }
    }
}

@Composable
private fun ConfirmDialog(title: String, message: String, confirmLabel: String, busy: Boolean, destructive: Boolean = false, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = AppSurface,
        title = { Text(title) }, text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (destructive) MaterialTheme.colorScheme.error else PrimaryBlue,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    if (destructive) Icons.Default.Cancel else Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Volver") } }
    )
}

@Composable private fun CatalogLoading(modifier: Modifier) = Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryBlue) }

@Composable
private fun CatalogMessage(message: String, onBack: () -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(8.dp)) {
        CatalogHeader(onBack)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.ErrorOutline, null, tint = AppTextSecondary, modifier = Modifier.size(42.dp))
                Text(message, color = AppTextSecondary, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun EmptySearch() {
    Column(Modifier.fillMaxWidth().padding(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.PersonSearch, null, tint = AppTextSecondary, modifier = Modifier.size(42.dp))
        Text("No se encontraron entrenadores.", color = AppTextSecondary)
    }
}

private fun requestWarning(plan: AthletePlan, assignedTrainerId: String): String? = when {
    assignedTrainerId.isNotBlank() -> "Ya tienes un entrenador asignado."
    plan.name == "Bronce" -> "Tu plan Bronce no permite enviar solicitudes."
    plan.requestLimit <= 0 || plan.sentRequests >= plan.requestLimit -> "Alcanzaste el límite de solicitudes."
    else -> null
}

private fun normalize(value: String): String = Normalizer.normalize(value.lowercase().trim(), Normalizer.Form.NFD).replace("\\p{M}+".toRegex(), "")
private fun joinCatalogLocation(country: String, city: String) = listOf(country, city).filter(String::isNotBlank).joinToString(", ")
