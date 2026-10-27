package com.example.mvt.chat.ui.screen

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.mvt.chat.data.model.ConversationPreview
import com.example.mvt.chat.data.repo.ChatRepository
import com.example.mvt.data.firebase.services.UserService
import com.example.mvt.chat.viewmodel.ChatViewModel
import com.example.mvt.chat.viewmodel.TrainerChatUiState
import com.example.mvt.chat.viewmodel.TrainerChatViewModel
import com.example.mvt.trainer.athletes.model.ApprovedAthlete
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppError
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun TrainerChatScreen(
    db: FirebaseFirestore,
    storage: FirebaseStorage,
    onBack: () -> Unit,
    viewModel: TrainerChatViewModel = viewModel(
        factory = TrainerChatViewModelFactory(ChatRepository(db, storage))
    )
) {
    val trainerId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val state by viewModel.uiState.collectAsState()
    var selectedAthlete by remember { mutableStateOf<ApprovedAthlete?>(null) }

    LaunchedEffect(trainerId) {
        if (trainerId.isNotBlank()) viewModel.load(trainerId)
    }

    val athlete = selectedAthlete
    if (athlete != null) {
        TrainerConversationScreen(
            trainerId = trainerId,
            athlete = athlete,
            db = db,
            storage = storage,
            onBack = { selectedAthlete = null }
        )
        return
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TrainerChatTopBar(title = "Chat", onBack = onBack)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            when (val current = state) {
                TrainerChatUiState.Loading -> LoadingState()
                is TrainerChatUiState.Error -> ErrorState(current.message) {
                    viewModel.load(trainerId)
                }
                is TrainerChatUiState.Success -> {
                    SearchField(current.search, viewModel::setSearch)
                    Spacer(Modifier.size(12.dp))
                    val athletes = viewModel.filteredAthletes(current)
                    if (athletes.isEmpty()) {
                        EmptyListState(
                            message = if (current.athletes.isEmpty()) {
                                "No tienes deportistas vinculados actualmente."
                            } else {
                                "No se encontraron deportistas."
                            }
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(athletes, key = { it.id }) { item ->
                                TrainerConversationItem(
                                    athlete = item,
                                    preview = current.previews[item.id],
                                    onClick = { selectedAthlete = item }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainerConversationScreen(
    trainerId: String,
    athlete: ApprovedAthlete,
    db: FirebaseFirestore,
    storage: FirebaseStorage,
    onBack: () -> Unit
) {
    val repo = remember(db, storage) { ChatRepository(db, storage) }
    val userService = remember { UserService() }
    val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(repo))
    val chatState by chatViewModel.state.collectAsState()
    var trainerPhotoUrl by remember { mutableStateOf("") }

    LaunchedEffect(trainerId, athlete.id) {
        chatViewModel.init(trainerId, athlete.id, "entrenador", null)
    }
    LaunchedEffect(trainerId) {
        trainerPhotoUrl = userService.getCurrentUser()?.foto_url.orEmpty()
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppBackground)
                    .padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Volver", tint = AppTextPrimary)
                }
                Avatar(athlete.photoUrl, athlete.name, Modifier.size(40.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = athlete.name,
                    color = AppTextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (chatState.isLoading) {
                LoadingState()
            } else {
                ChatConversationContent(
                    vm = chatViewModel,
                    state = chatState,
                    myAvatarUrl = trainerPhotoUrl,
                    otherAvatarUrl = athlete.photoUrl.orEmpty()
                )
            }
        }
    }
}

@Composable
private fun TrainerChatTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, "Volver", tint = AppTextPrimary)
        }
        Text(title, color = AppTextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        placeholder = { Text("Buscar deportista...") },
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
private fun TrainerConversationItem(
    athlete: ApprovedAthlete,
    preview: ConversationPreview?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = AppSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(athlete.photoUrl, athlete.name, Modifier.size(48.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(athlete.name, color = AppTextPrimary, fontWeight = FontWeight.Bold)
                Text(
                    text = preview?.lastMessage?.previewText() ?: "Aún no hay mensajes.",
                    color = AppTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                preview?.lastMessage?.timestamp?.let { timestamp ->
                    Text(formatTime(timestamp), color = AppTextSecondary, style = MaterialTheme.typography.labelSmall)
                }
                if (preview != null && preview.unreadCount > 0) {
                    Surface(color = PrimaryBlue, shape = CircleShape) {
                        Text(
                            preview.unreadCount.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Avatar(photoUrl: String?, name: String, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = AppSurfaceAlt,
        border = BorderStroke(1.dp, AppBorder)
    ) {
        if (photoUrl.isNullOrBlank()) {
            Icon(Icons.Default.Person, contentDescription = name, tint = AppTextSecondary, modifier = Modifier.padding(10.dp))
        } else {
            AsyncImage(
                model = photoUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = PrimaryBlue)
    }
}

@Composable
private fun EmptyListState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = AppTextSecondary)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = AppError)
        Spacer(Modifier.size(12.dp))
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}

private fun com.example.mvt.chat.data.model.ChatMessage.previewText(): String {
    return texto.ifBlank {
        when {
            imageUrl.isNotBlank() -> "Imagen compartida"
            audioUrl.isNotBlank() -> "Audio compartido"
            else -> "Nuevo mensaje"
        }
    }
}

private fun formatTime(timestamp: String): String {
    return runCatching {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(parser.parse(timestamp)!!.time))
    }.getOrDefault("")
}

class TrainerChatViewModelFactory(
    private val chatRepository: ChatRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrainerChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TrainerChatViewModel(chatRepository = chatRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
