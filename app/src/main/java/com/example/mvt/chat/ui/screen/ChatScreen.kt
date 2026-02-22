package com.example.mvt.chat.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mvt.chat.ui.components.DateHeader
import com.example.mvt.chat.ui.components.TelegramMessageBubble
import com.example.mvt.chat.viewmodel.ChatViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic

@Composable
fun ChatScreen(
    vm: ChatViewModel,
    onPickImage: () -> Unit,
    onRecordAudio: () -> Unit
) {
    val st by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {

        // Área mensajes
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (st.isLoading) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(Modifier.padding(16.dp))
                    Text("Cargando conversación…", Modifier.padding(start = 16.dp))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(st.messages) { ui ->
                        if (ui.showDateHeader) DateHeader(ui.dateGroup)
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

        // Composer estilo Telegram (simple)
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onPickImage) {
                Icon(Icons.Default.Image, contentDescription = "Imagen")
            }
            IconButton(onClick = onRecordAudio) {
                Icon(Icons.Default.Mic, contentDescription = "Audio")
            }
            OutlinedTextField(
                value = st.messageText,
                onValueChange = vm::onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe tu mensaje…") },
                singleLine = true
            )
            IconButton(onClick = vm::sendMessage) {
                Icon(Icons.Default.Send, contentDescription = "Enviar")
            }
        }
    }
}