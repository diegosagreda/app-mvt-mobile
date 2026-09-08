package com.example.mvt.trainer.catalog.ui

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.mvt.R
import com.example.mvt.trainer.catalog.model.AssignmentStatus
import com.example.mvt.trainer.catalog.model.TrainerProfile
import com.example.mvt.trainer.catalog.model.TrainerRating
import com.example.mvt.trainer.catalog.model.TrainerRatingsState
import com.example.mvt.trainer.catalog.model.TrainerScreenState
import com.example.mvt.trainer.catalog.viewmodel.RatingSubmissionState
import com.example.mvt.trainer.catalog.viewmodel.TrainerViewModel
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppPrimarySoft
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val StarGold = Color(0xFFFFC857)

@Composable
fun TrainerScreen(
    athleteId: String,
    viewModel: TrainerViewModel,
    onBack: () -> Unit,
    onUpgradePlan: () -> Unit
) {
    val state by viewModel.screenState.collectAsState()
    val ratings by viewModel.ratingsState.collectAsState()
    val submission by viewModel.submissionState.collectAsState()
    var showRatingDialog by remember { mutableStateOf(false) }
    var showPhoto by remember { mutableStateOf(false) }

    LaunchedEffect(athleteId) { viewModel.start(athleteId) }
    LaunchedEffect(submission.completed) {
        if (submission.completed) showRatingDialog = false
    }

    Box(Modifier.fillMaxSize().background(AppBackground)) {
        when (val current = state) {
            TrainerScreenState.Loading -> LoadingState()
            is TrainerScreenState.Error -> MessageState(
                icon = Icons.Default.ErrorOutline,
                title = "No pudimos cargar tu entrenador",
                message = current.message,
                actionLabel = "Reintentar",
                onAction = { viewModel.start(athleteId) },
                onBack = onBack
            )
            is TrainerScreenState.Unassigned -> UnassignedState(current.latestStatus, onUpgradePlan, onBack)
            is TrainerScreenState.Assigned -> AssignedContent(
                trainer = current.trainer,
                ratings = ratings,
                canRate = current.assignment.trainerId == current.trainer.id &&
                    current.athlete.lastRatingMonth != currentMonth(),
                onBack = onBack,
                onPhotoClick = { showPhoto = true },
                onRateClick = {
                    viewModel.clearSubmissionResult()
                    showRatingDialog = true
                }
            )
        }
    }

    val assigned = state as? TrainerScreenState.Assigned
    if (showRatingDialog && assigned != null) {
        RatingDialog(
            trainerName = assigned.trainer.fullName,
            state = submission,
            onDismiss = {
                if (!submission.isSubmitting) {
                    showRatingDialog = false
                    viewModel.clearSubmissionResult()
                }
            },
            onSubmit = viewModel::submitRating
        )
    }
    if (showPhoto && assigned != null) {
        PhotoDialog(assigned.trainer, onDismiss = { showPhoto = false })
    }
}

@Composable
private fun AssignedContent(
    trainer: TrainerProfile,
    ratings: TrainerRatingsState,
    canRate: Boolean,
    onBack: () -> Unit,
    onPhotoClick: () -> Unit,
    onRateClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { ScreenTitle("Tu entrenador", "Acompañamiento y perfil profesional", onBack) }
        item { TrainerHero(trainer, onPhotoClick) }
        item { RatingSummary(trainer.estrellas, canRate, onRateClick) }
        item {
            SectionCard("Información personal", Icons.Default.LocationOn) {
                CompactDetailGrid(
                    listOf(
                        "Género" to trainer.genero,
                        "Fecha de nacimiento" to trainer.fechaNacimiento
                    )
                )
                InlineDetail("Origen", joinLocation(trainer.pais, trainer.ciudad))
                InlineDetail("Ubicación actual", joinLocation(trainer.paisActual, trainer.ciudadActual))
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
        if (ratings.isLoading) item {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(24.dp), color = PrimaryBlue, strokeWidth = 2.dp)
            }
        } else {
            ratings.error?.let { message ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Text(message, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
                    }
                }
            }
            if (ratings.mine.isNotEmpty()) item { RatingsHeader("Tus calificaciones") }
            items(ratings.mine, key = { "mine-${it.id}" }) { RatingItem(it) }
            if (ratings.others.isNotEmpty()) item { RatingsHeader("Opiniones de otros deportistas") }
            items(ratings.others, key = { "other-${it.id}" }) { RatingItem(it) }
        }
    }
}

@Composable
private fun ScreenTitle(title: String, subtitle: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppTextPrimary)
        }
        Column {
            Text(title, color = AppTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = AppTextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
internal fun TrainerHero(trainer: TrainerProfile, onPhotoClick: () -> Unit) {
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
                model = trainer.fotoUrl.ifBlank { R.drawable.placeholder },
                contentDescription = "Fotografía de ${trainer.fullName}",
                modifier = Modifier.size(96.dp).clip(CircleShape).clickable(onClick = onPhotoClick),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(trainer.fullName, color = AppTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                if (trainer.especialidad.isNotBlank()) {
                    Text(trainer.especialidad, color = AppTextSecondary, fontSize = 14.sp)
                }
                Spacer(Modifier.height(4.dp))
                Stars(trainer.estrellas)
                Text(
                    if (trainer.estrellas > 0) String.format(Locale.getDefault(), "%.1f de 5", trainer.estrellas) else "Sin calificaciones",
                    color = AppTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
internal fun SectionCard(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(true) }
    Card(
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppBorder),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(AppPrimarySoft), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(19.dp))
                }
                Text(title, color = AppTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Colapsar $title" else "Expandir $title",
                    tint = AppTextSecondary,
                    modifier = Modifier.rotate(if (expanded) 180f else 0f)
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = AppBorder)
                    content()
                }
            }
        }
    }
}

@Composable
internal fun Detail(label: String, value: String) {
    if (value.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = AppTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        BodyText(value)
    }
}

@Composable
internal fun CompactDetailGrid(details: List<Pair<String, String>>) {
    val visibleDetails = details.filter { it.second.isNotBlank() }
    visibleDetails.chunked(2).forEach { rowDetails ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rowDetails.forEach { (label, value) ->
                CompactDetail(label, value, Modifier.weight(1f))
            }
            if (rowDetails.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun CompactDetail(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppSurfaceAlt)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(label, color = AppTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(value, color = AppTextPrimary, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
internal fun InlineDetail(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppSurfaceAlt)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            label,
            color = AppTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            value,
            color = AppTextPrimary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.weight(0.58f)
        )
    }
}

@Composable
internal fun ExpandableDetail(label: String, value: String) {
    if (value.isBlank()) return
    var expanded by remember(value) { mutableStateOf(false) }
    var hasOverflow by remember(value) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = AppTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(
            text = value,
            color = AppTextPrimary,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (!expanded) hasOverflow = result.hasVisualOverflow
            }
        )
        if (hasOverflow || expanded) {
            Text(
                text = if (expanded) "Ver menos" else "Ver más",
                color = PrimaryBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { expanded = !expanded }.padding(vertical = 2.dp)
            )
        }
    }
}

@Composable private fun BodyText(value: String) = Text(value, color = AppTextPrimary, fontSize = 14.sp, lineHeight = 21.sp)

@Composable
private fun RatingSummary(rating: Double, canRate: Boolean, onRateClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppSurfaceAlt),
        border = BorderStroke(1.dp, PrimaryBlue),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(AppPrimarySoft), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Star, null, tint = StarGold, modifier = Modifier.size(23.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Califica tu experiencia", color = AppTextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("Tu opinión ayuda a mejorar el acompañamiento", color = AppTextSecondary, fontSize = 12.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Stars(rating)
                    Text(
                        if (rating > 0) String.format(Locale.getDefault(), "%.1f / 5", rating) else "Aún no hay opiniones",
                        color = AppTextSecondary, fontSize = 12.sp
                    )
                }
                Button(
                    onClick = onRateClick,
                    enabled = canRate,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (canRate) "Calificar ahora" else "Calificado este mes") }
            }
        }
    }
}

@Composable
private fun Stars(value: Double, onSelect: ((Int) -> Unit)? = null) {
    Row {
        repeat(5) { index ->
            val selected = index < value.roundToInt()
            Icon(
                Icons.Default.Star,
                contentDescription = if (onSelect != null) "${index + 1} estrellas" else null,
                tint = if (selected) StarGold else AppBorder,
                modifier = Modifier.size(if (onSelect != null) 38.dp else 22.dp)
                    .then(if (onSelect != null) Modifier.clickable { onSelect(index + 1) } else Modifier)
            )
        }
    }
}

@Composable
private fun RatingsHeader(title: String) {
    Text(title, color = AppTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun RatingItem(rating: TrainerRating) {
    Card(colors = CardDefaults.cardColors(containerColor = AppSurfaceAlt), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AsyncImage(
                model = rating.athletePhotoUrl.ifBlank { R.drawable.placeholder },
                contentDescription = null,
                modifier = Modifier.size(42.dp).clip(CircleShape), contentScale = ContentScale.Crop
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(rating.athleteName.ifBlank { "Deportista" }, color = AppTextPrimary, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(rating.dateText, color = AppTextSecondary, fontSize = 11.sp)
                }
                Stars(rating.score.toDouble())
                if (rating.comment.isNotBlank()) BodyText(rating.comment)
            }
        }
    }
}

@Composable
private fun UnassignedState(status: AssignmentStatus?, onUpgradePlan: () -> Unit, onBack: () -> Unit) {
    val (title, message) = when (status) {
        AssignmentStatus.PENDING -> "Solicitud en revisión" to "Tu solicitud fue enviada. Te avisaremos cuando un entrenador la apruebe."
        AssignmentStatus.REJECTED -> "Solicitud no aprobada" to "Puedes revisar las opciones disponibles y solicitar otro entrenador."
        AssignmentStatus.CANCELLED, AssignmentStatus.LOST -> "Sin entrenador activo" to "Tu relación anterior finalizó. Explora los planes para iniciar un nuevo acompañamiento."
        else -> "Entrenamiento personalizado" to "Accede al acompañamiento de un entrenador, seguimiento de tu progreso y planes adaptados a tus objetivos."
    }
    MessageState(Icons.Default.Badge, title, message, "Ver opciones", onUpgradePlan, onBack)
}

@Composable
private fun MessageState(icon: ImageVector, title: String, message: String, actionLabel: String, onAction: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        ScreenTitle("Tu entrenador", "Acompañamiento personalizado", onBack)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Card(colors = CardDefaults.cardColors(containerColor = AppSurface), border = BorderStroke(1.dp, AppBorder), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(68.dp).clip(CircleShape).background(AppPrimarySoft), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(34.dp))
                    }
                    Text(title, color = AppTextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(message, color = AppTextSecondary, fontSize = 14.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
                    Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
                        Icon(if (actionLabel == "Reintentar") Icons.Default.Refresh else Icons.Default.WorkHistory, null)
                        Spacer(Modifier.size(8.dp))
                        Text(actionLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = PrimaryBlue)
            Text("Buscando tu entrenador…", color = AppTextSecondary)
        }
    }
}

@Composable
private fun RatingDialog(
    trainerName: String,
    state: RatingSubmissionState,
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit
) {
    var score by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        titleContentColor = AppTextPrimary,
        textContentColor = AppTextSecondary,
        title = { Text("Califica a $trainerName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Tu opinión ayuda a mejorar el acompañamiento.", textAlign = TextAlign.Center)
                Stars(score.toDouble(), onSelect = { score = it })
                OutlinedTextField(
                    value = comment,
                    onValueChange = { if (it.length <= 500) comment = it },
                    label = { Text("Comentario opcional") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("${comment.length}/500") }
                )
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(score, comment) }, enabled = score > 0 && !state.isSubmitting) {
                if (state.isSubmitting) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Enviar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.isSubmitting) { Text("Cancelar") } }
    )
}

@Composable
internal fun PhotoDialog(trainer: TrainerProfile, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(AppSurface)) {
            AsyncImage(
                model = trainer.fotoUrl.ifBlank { R.drawable.placeholder },
                contentDescription = "Fotografía de ${trainer.fullName}",
                modifier = Modifier.fillMaxWidth().height(480.dp), contentScale = ContentScale.Fit
            )
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(AppBackground.copy(alpha = .7f), CircleShape)) {
                Icon(Icons.Default.Close, "Cerrar", tint = AppTextPrimary)
            }
        }
    }
}

private fun joinLocation(country: String, city: String) = listOf(country, city).filter(String::isNotBlank).joinToString(", ")
private fun currentMonth() = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
