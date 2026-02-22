package com.example.mvt.chat.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mvt.chat.data.model.ChatMessage
import com.example.mvt.chat.data.model.UiMessage
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DateHeader(label: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.padding(vertical = 10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TelegramMessageBubble(
    ui: UiMessage,
    onReply: (ChatMessage) -> Unit,
    onEdit: (ChatMessage) -> Unit,
    onDelete: (ChatMessage) -> Unit
) {
    val bubbleShape = if (ui.isMine) {
        RoundedCornerShape(18.dp, 6.dp, 18.dp, 18.dp) // parecido Telegram: “pico” suave
    } else {
        RoundedCornerShape(6.dp, 18.dp, 18.dp, 18.dp)
    }

    val alignment = if (ui.isMine) Alignment.End else Alignment.Start
    val bubblePadding = if (ui.isMine) PaddingValues(start = 64.dp, end = 12.dp) else PaddingValues(start = 12.dp, end = 64.dp)

    // Long-press actions (menú real se puede añadir luego con DropdownMenu)
    Box(
        Modifier.fillMaxWidth().padding(bubblePadding),
        contentAlignment = alignment as Alignment
    ) {
        Column(
            modifier = Modifier
                .clip(bubbleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        // Por ahora: prioriza borrar en pruebas rápidas.
                        // En producción: muestra menú con Reply/Edit/Delete.
                    }
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (ui.msg.texto.isNotBlank()) {
                Text(
                    text = ui.msg.texto,
                    style = MaterialTheme.typography.bodyMedium,
                    overflow = TextOverflow.Visible
                )
            }

            if (ui.msg.imageUrl.isNotBlank()) {
                Text("🖼️ Imagen", style = MaterialTheme.typography.labelMedium)
                // Luego: AsyncImage (Coil) para render real
            }
            if (ui.msg.audioUrl.isNotBlank()) {
                Text("🎤 Audio", style = MaterialTheme.typography.labelMedium)
                // Luego: player simple
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = formatTime(ui.msg.timestamp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

private fun formatTime(iso: String): String {
    // Simplificado (si falla: devuelve vacío)
    return try {
        // Si tu ISO siempre viene UTC con milisegundos:
        val inFmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val d = inFmt.parse(iso) ?: return ""
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(d)
    } catch (_: Exception) {
        ""
    }
}