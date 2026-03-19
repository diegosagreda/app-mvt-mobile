package com.example.mvt.chat.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mvt.chat.data.model.ChatMessage
import com.example.mvt.chat.ui.components.TelegramMessageBubble
import com.example.mvt.chat.viewmodel.ChatUiState
import com.example.mvt.chat.viewmodel.ChatViewModel

@Composable
fun ChatConversationContent(
    vm: ChatViewModel,
    state: ChatUiState
) {
    val listState = rememberLazyListState()

    // Auto-scroll al último mensaje (lista NO invertida)
    val lastIndex = remember(state.messages.size) { state.messages.lastIndex }
    LaunchedEffect(state.messages.size) {
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F5F7))
    ) {
        Divider()

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            // FIX: key estable con fallback si msg.id viene ""
            items(
                items = state.messages,
                key = { ui ->
                    ui.msg.id.takeIf { it.isNotBlank() }
                        ?: "${ui.msg.remitente}_${ui.msg.timestamp}"
                }
            ) { ui ->
                TelegramMessageBubble(
                    ui = ui,
                    onReply = { vm.setReply(it) },
                    onEdit = { vm.setEdit(it) },
                    onDelete = { vm.deleteMessage(it.id) }
                )
            }
        }

        ChatComposer(
            replyingTo = state.replyingTo,
            onCancelReply = { vm.clearReply() },
            onSend = { text ->
                val clean = text.trim()
                if (clean.isNotEmpty()) {
                    vm.sendTextMessage(
                        text = clean,
                        replyToMessageId = state.replyingTo?.id // puede ser null
                    )
                }
            }
        )
    }
}

@Composable
private fun ChatComposer(
    replyingTo: ChatMessage?,
    onCancelReply: () -> Unit,
    onSend: (String) -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFFFFF))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (replyingTo != null) {
            ReplyPreview(
                message = replyingTo,
                onClose = onCancelReply
            )
            Spacer(Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                placeholder = { Text("Escribe un mensaje…") },
                shape = RoundedCornerShape(14.dp),
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFFFFFFF),
                    unfocusedContainerColor = Color(0xFFFFFFFF),
                    focusedBorderColor = Color(0xFFE5E7EB),
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    cursorColor = Color(0xFF2563EB),
                    focusedTextColor = Color(0xFF111827),
                    unfocusedTextColor = Color(0xFF111827)
                )
            )

            Spacer(Modifier.width(10.dp))

            val enabled = text.trim().isNotEmpty()
            IconButton(
                enabled = enabled,
                onClick = {
                    onSend(text)
                    text = ""
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (enabled) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Enviar",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun ReplyPreview(
    message: ChatMessage,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(34.dp)
                .background(Color(0xFF2563EB), RoundedCornerShape(2.dp))
        )

        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = "Respondiendo a…",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF64748B)
            )
            Text(
                text = message.texto.ifBlank { "(mensaje sin texto)" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF0F172A)
            )
        }

        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancelar",
                tint = Color(0xFF64748B)
            )
        }
    }
}