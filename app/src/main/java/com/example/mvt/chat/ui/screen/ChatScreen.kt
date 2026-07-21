package com.example.mvt.chat.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.mvt.R
import com.example.mvt.chat.data.model.ChatMessage
import com.example.mvt.chat.data.repo.ChatRepository
import com.example.mvt.chat.viewmodel.ChatUiState
import com.example.mvt.chat.viewmodel.ChatViewModel
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSuccess
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uid: String,
    athleteName: String,
    athletePhotoUrl: String,
    trainerId: String,
    trainerName: String,
    trainerPhotoUrl: String,
    onBack: () -> Unit,
    db: FirebaseFirestore,
    storage: FirebaseStorage
) {
    val repo = remember(db, storage) { ChatRepository(db, storage) }
    val vm: ChatViewModel = viewModel(factory = ChatViewModelFactory(repo))
    val state by vm.state.collectAsState()

    LaunchedEffect(uid, trainerId) {
        if (uid.isNotBlank() && trainerId.isNotBlank()) {
            vm.init(uid = uid, otherUid = trainerId, role = "deportista", conversationId = null)
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = AppTextPrimary)
                    }
                },
                title = {
                    Text(
                        text = "Chat",
                        color = AppTextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(padding)
        ) {
            ConversationHeader(
                state = state,
                athleteName = athleteName,
                athletePhotoUrl = athletePhotoUrl,
                trainerName = trainerName,
                trainerPhotoUrl = trainerPhotoUrl,
                hasTrainer = trainerId.isNotBlank()
            )

            Divider(color = AppBorder)

            when {
                trainerId.isBlank() -> ChatUnavailableState()
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue, strokeWidth = 3.dp)
                }
                else -> ChatConversationContent(
                    vm = vm,
                    state = state,
                    myAvatarUrl = athletePhotoUrl,
                    otherAvatarUrl = trainerPhotoUrl
                )
            }
        }
    }
}

@Composable
private fun ConversationHeader(
    state: ChatUiState,
    athleteName: String,
    athletePhotoUrl: String,
    trainerName: String,
    trainerPhotoUrl: String,
    hasTrainer: Boolean
) {
    val lastMessage = state.messages.lastOrNull()?.msg
    val lastTime = lastMessage?.timestamp?.let(::formatChatHeaderTime).orEmpty()
    val preview = lastMessage?.previewText().orEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(AppBackground, AppSurface)
                )
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChatParticipantStack(
                trainerPhotoUrl = trainerPhotoUrl,
                athletePhotoUrl = athletePhotoUrl
            )

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = trainerName.ifBlank { "Tu entrenador" },
                    color = AppTextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (hasTrainer) "Acompanamiento activo con ${athleteName.ifBlank { "tu perfil" }}" else "Sin entrenador asignado",
                    color = AppTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            StatusPill(active = hasTrainer)
        }

        Surface(
            color = AppSurfaceAlt,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, AppBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = preview.ifBlank { "Empieza la conversacion con tu entrenador." },
                        color = AppTextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (state.totalMessages > 0) "${state.totalMessages} mensajes en seguimiento" else "Chat listo para coordinar tu entrenamiento",
                        color = AppTextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (lastTime.isNotBlank()) {
                    Text(
                        text = lastTime,
                        color = AppTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatParticipantStack(
    trainerPhotoUrl: String,
    athletePhotoUrl: String
) {
    Box(modifier = Modifier.size(width = 74.dp, height = 52.dp)) {
        HeaderAvatar(
            photoUrl = trainerPhotoUrl,
            contentDescription = "Foto del entrenador",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(52.dp)
        )
        HeaderAvatar(
            photoUrl = athletePhotoUrl,
            contentDescription = "Foto del deportista",
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(42.dp)
                .border(2.dp, AppBackground, CircleShape)
        )
    }
}

@Composable
private fun HeaderAvatar(
    photoUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = AppSurfaceAlt,
        border = BorderStroke(1.dp, AppBorder),
        modifier = modifier
    ) {
        if (photoUrl.isBlank()) {
            Image(
                painter = painterResource(id = R.drawable.iconografia_02_svg),
                contentDescription = contentDescription,
                modifier = Modifier.padding(9.dp)
            )
        } else {
            AsyncImage(
                model = photoUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun StatusPill(active: Boolean) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (active) AppSuccess.copy(alpha = 0.14f) else AppSurfaceAlt,
        border = BorderStroke(1.dp, if (active) AppSuccess.copy(alpha = 0.34f) else AppBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (active) AppSuccess else AppTextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (active) "Activo" else "Pendiente",
                color = if (active) AppSuccess else AppTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ChatUnavailableState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Aun no tienes entrenador asignado",
                color = AppTextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Cuando tengas un entrenador activo, este chat quedara disponible.",
                color = AppTextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun ChatMessage.previewText(): String {
    return when {
        texto.isNotBlank() -> texto
        imageUrl.isNotBlank() -> "Imagen compartida"
        audioUrl.isNotBlank() -> "Audio compartido"
        else -> "Nuevo mensaje"
    }
}

private fun formatChatHeaderTime(timestamp: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(timestamp) ?: return ""
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(date.time))
    } catch (_: Exception) {
        ""
    }
}
