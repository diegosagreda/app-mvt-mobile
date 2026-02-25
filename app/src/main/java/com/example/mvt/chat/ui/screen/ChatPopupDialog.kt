// ===============================================
// FILE 1: com.example.mvt.chat.ui.screen.ChatPopup.kt
// ===============================================
package com.example.mvt.chat.ui.screen

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mvt.chat.data.repo.ChatRepository
import com.example.mvt.chat.viewmodel.ChatUiState
import com.example.mvt.chat.viewmodel.ChatViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

// =====================================================
// 1) DIALOG
// =====================================================
@Composable
fun ChatPopupDialog(
    onDismiss: () -> Unit,
    title: String = "Chat",
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 420.dp, max = 680.dp)
        ) {
            Column(Modifier.fillMaxSize()) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Divider()

                Box(modifier = Modifier.fillMaxSize()) {
                    content()
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
// 3) ChatPopup (usa VM interno + init + dialog)
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
    SideEffect {
        Log.e("ChatUI", "ChatPopup composed show=$show uid='$uid' otherUid='$otherUid' role=$role convoId=$conversationId")
    }

    if (!show) return

    val repo = remember(db, storage) { ChatRepository(db, storage) }
    val vm: ChatViewModel = viewModel(factory = ChatViewModelFactory(repo))

    LaunchedEffect(uid, otherUid, role, conversationId) {
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

    ChatPopupDialog(onDismiss = onDismiss, title = title) {
        content(vm, state)
    }
}