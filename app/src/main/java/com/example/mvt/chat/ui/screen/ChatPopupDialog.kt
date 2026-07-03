// ===============================================
// FILE 1: com.example.mvt.chat.ui.screen.ChatPopup.kt
// ===============================================
package com.example.mvt.chat.ui.screen

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mvt.chat.data.repo.ChatRepository
import com.example.mvt.chat.viewmodel.ChatUiState
import com.example.mvt.chat.viewmodel.ChatViewModel
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

// =====================================================
// 1) PANEL INTEGRADO
// =====================================================
@Composable
fun ChatBottomPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String = "Chat",
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(140))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f))
                    .clickable(onClick = onDismiss, indication = null, interactionSource = remember { MutableInteractionSource() })
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(220)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(160)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = AppSurface,
                border = BorderStroke(1.dp, AppBorder),
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.62f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(AppSurface, AppSurfaceAlt)
                            )
                        )
                ) {
                    Column(Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(4.dp)
                                    .fillMaxWidth(0.12f)
                                    .clip(CircleShape)
                                    .background(AppTextSecondary.copy(alpha = 0.35f))
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AppTextPrimary
                                )
                                Text(
                                    text = "Disponible mientras navegas tus rutinas",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AppTextSecondary
                                )
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = AppTextPrimary
                                )
                            }
                        }

                        Divider(color = AppBorder)

                        Box(modifier = Modifier.fillMaxSize()) {
                            content()
                        }
                    }
                }
            }
        }
    }
}

// =====================================================
// 2) FACTORY PARA INYECTAR repo EN VM
// =====================================================
class ChatViewModelFactory(
    private val repo: ChatRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// =====================================================
// 3) ChatPopup (usa VM interno + init + panel)
// =====================================================
@Composable
fun ChatPopup(
    show: Boolean,
    onDismiss: () -> Unit,
    title: String = "Chat",
    uid: String,
    otherUid: String,
    role: String = "deportista",
    conversationId: String? = null,
    db: FirebaseFirestore,
    storage: FirebaseStorage,
    content: @Composable (vm: ChatViewModel, state: ChatUiState) -> Unit
) {
    val repo = remember(db, storage) { ChatRepository(db, storage) }
    val vm: ChatViewModel = viewModel(factory = ChatViewModelFactory(repo))

    LaunchedEffect(show, uid, otherUid, role, conversationId) {
        if (!show) return@LaunchedEffect
        Log.e("ChatUI", "LaunchedEffect fired -> repo.debugPing + vm.init")
        repo.debugPing("ChatPopup.LaunchedEffect")
        vm.init(uid = uid, otherUid = otherUid, role = role, conversationId = conversationId)
    }

    val state by vm.state.collectAsState()

    LaunchedEffect(state.totalMessages, state.messages.size, state.isLoading) {
        Log.e(
            "ChatUI",
            "ChatPopup state -> total=${state.totalMessages} uiMessages=${state.messages.size} isLoading=${state.isLoading}"
        )
    }

    ChatBottomPanel(
        visible = show,
        onDismiss = onDismiss,
        title = title
    ) {
        if (otherUid.isBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue, strokeWidth = 3.dp)
            }
        } else {
            content(vm, state)
        }
    }
}
