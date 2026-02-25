package com.example.mvt.chat.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.chat.data.model.ChatMessage
import com.example.mvt.chat.data.model.UiMessage
import java.text.SimpleDateFormat
import java.util.Date
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
    var menu by remember { mutableStateOf(false) }

    val isMine = ui.isMine

    // Colores tipo WhatsApp/Telegram
    val bubbleColor = if (isMine) Color(0xFFDCF8C6) else Color(0xFFFFFFFF)
    val textColor = Color(0xFF111827)
    val metaColor = Color(0xFF6B7280)

    val shape = if (isMine) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 6.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 6.dp, bottomEnd = 18.dp)
    }

    // Separación tipo chat real (deja margen al lado contrario)
    val sidePadding = if (isMine) PaddingValues(start = 64.dp, end = 12.dp) else PaddingValues(start = 12.dp, end = 64.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(sidePadding),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { menu = true }
                )
        ) {
            Column(
                modifier = Modifier
                    .clip(shape)
                    .background(bubbleColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Texto
                if (ui.msg.texto.isNotBlank()) {
                    Text(
                        text = ui.msg.texto,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }

                // Adjuntos (placeholder; luego puedes meter previews reales)
                if (ui.msg.imageUrl.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("🖼️ Imagen", color = metaColor, style = MaterialTheme.typography.labelMedium)
                }
                if (ui.msg.audioUrl.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text("🎤 Audio", color = metaColor, style = MaterialTheme.typography.labelMedium)
                }

                Spacer(Modifier.height(6.dp))

                // Hora + checks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(ui.msg.timestamp),
                        fontSize = 11.sp,
                        color = metaColor,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                    if (isMine) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "✓✓",
                            fontSize = 11.sp,
                            color = metaColor
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Responder") },
                    onClick = { menu = false; onReply(ui.msg) }
                )
                DropdownMenuItem(
                    text = { Text("Editar") },
                    onClick = { menu = false; onEdit(ui.msg) }
                )
                DropdownMenuItem(
                    text = { Text("Eliminar") },
                    onClick = { menu = false; onDelete(ui.msg) }
                )
            }
        }
    }
}

private fun formatTime(epochSeconds: Int): String {
    return try {
        val d = Date(epochSeconds.toLong() * 1000L)
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(d)
    } catch (_: Exception) {
        ""
    }
}