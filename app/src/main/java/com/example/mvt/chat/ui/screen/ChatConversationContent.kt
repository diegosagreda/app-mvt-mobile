package com.example.mvt.chat.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mvt.chat.ui.components.TelegramMessageBubble
import com.example.mvt.chat.viewmodel.ChatUiState
import com.example.mvt.chat.viewmodel.ChatViewModel

@Composable
fun ChatConversationContent(
    vm: ChatViewModel,
    state: ChatUiState
) {
    val listState = rememberLazyListState()

    // Auto-scroll al último mensaje (opcional)
    val lastIndex = remember(state.messages.size) { state.messages.lastIndex }
    LaunchedEffect(state.messages.size) {
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F5F7)) // tipo Telegram claro
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
            items(state.messages, key = { it.msg.id }) { ui ->
                TelegramMessageBubble(
                    ui = ui,
                    onReply = { vm.setReply(it) },
                    onEdit = { vm.setEdit(it) },
                    onDelete = { vm.deleteMessage(it.id) }
                )
            }
        }
    }
}