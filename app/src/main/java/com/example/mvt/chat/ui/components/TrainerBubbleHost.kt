package com.example.mvt.chat.ui.components

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.mvt.chat.ui.screen.ChatConversationContent
import com.example.mvt.chat.ui.screen.ChatPopup
import com.example.mvt.chat.viewmodel.ChatViewModel
import com.example.mvt.chat.viewmodel.TrainerBubbleViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

@Composable
fun TrainerBubbleHost(
    athleteId: String,
    vm: TrainerBubbleViewModel,
    modifier: Modifier = Modifier
) {
    val trainer by vm.trainer.collectAsState()
    val trainerId by vm.trainerId.collectAsState()
    var showChat by remember { mutableStateOf(false) }

    // Instancias estables (NO getInstance() dentro del ChatPopup)
    val db = remember { FirebaseFirestore.getInstance() }
    val storage = remember { FirebaseStorage.getInstance() }

    LaunchedEffect(athleteId) {
        vm.start(athleteId)
    }

    TrainerChatBubble(
        photoUrl = trainer.foto_url,
        modifier = modifier,
        onClick = {
            Log.e("ChatUI", "Bubble click -> showChat=true (trainerId='$trainerId')")
            showChat = true
        }
    )

    ChatPopup(
        show = showChat,
        onDismiss = { showChat = false },
        title = "Chat con entrenador",
        uid = athleteId,
        otherUid = trainerId,
        role = "deportista",
        conversationId = null,
        db = db,
        storage = storage
    ) { chatVm: ChatViewModel, state ->
        if (trainerId.isBlank()) {
            Text("Cargando entrenador...")
        } else {
            // UI real del chat
            ChatConversationContent(vm = chatVm, state = state)
        }
    }
}