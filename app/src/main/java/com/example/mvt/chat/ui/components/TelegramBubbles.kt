package com.example.mvt.chat.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
fun ChatConversation(
    messages: List<UiMessage>,
    modifier: Modifier = Modifier,
    onSendText: (text: String, replyTo: ChatMessage?) -> Unit,
    onEdit: (ChatMessage) -> Unit,
    onDelete: (ChatMessage) -> Unit
) {
    var replyingTo by remember { mutableStateOf<ChatMessage?>(null) }

    Column(modifier = modifier.fillMaxSize()) {

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 10.dp),
            reverseLayout = true
        ) {
            val reversed = messages.asReversed()

            itemsIndexed(
                items = reversed,
                key = { _, item -> item.msg.hashCode() } // si tienes msg.id úsalo mejor
            ) { index, ui ->
                val currentDay = formatDay(ui.msg.timestamp)
                val prev = reversed.getOrNull(index + 1)
                val prevDay = prev?.let { formatDay(it.msg.timestamp) }

                if (prevDay != currentDay) {
                    DateHeader(currentDay)
                }

                TelegramMessageBubble(
                    ui = ui,
                    onReply = { replyingTo = it },
                    onEdit = onEdit,
                    onDelete = onDelete
                )
                Spacer(Modifier.height(6.dp))
            }
        }

        ChatComposer(
            replyingTo = replyingTo,
            onCancelReply = { replyingTo = null },
            onSend = { text ->
                val clean = text.trim()
                if (clean.isNotEmpty()) {
                    onSendText(clean, replyingTo)
                    replyingTo = null
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

    val container = Color(0xFF0B1220)
    val fieldBg = Color(0xFF111B33)
    val stroke = Color(0xFF223055)
    val hint = Color(0xFF9CA3AF)
    val textColor = Color(0xFFE5E7EB)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(container)
            .padding(horizontal = 10.dp, vertical = 10.dp)
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
                placeholder = { Text("Escribe un mensaje… (sin romper el pace)", color = hint) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = stroke,
                    unfocusedBorderColor = stroke,
                    focusedContainerColor = fieldBg,
                    unfocusedContainerColor = fieldBg,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = Color(0xFF93C5FD)
                ),
                shape = RoundedCornerShape(14.dp),
                maxLines = 5
            )

            Spacer(Modifier.width(10.dp))

            val enabled = text.trim().isNotEmpty()
            IconButton(
                onClick = {
                    onSend(text)
                    text = ""
                },
                enabled = enabled,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (enabled) Color(0xFF2563EB) else Color(0xFF1F2A44))
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
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
    val bg = Color(0xFF0F1B34)
    val line = Color(0xFF60A5FA)
    val text = Color(0xFFE5E7EB)
    val meta = Color(0xFF9CA3AF)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(line)
        )

        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = "Respondiendo…",
                color = meta,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = message.texto.ifBlank { "(mensaje sin texto)" },
                color = text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Cancelar respuesta",
                tint = Color(0xFFCBD5E1)
            )
        }
    }
}

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

    val bubbleColor = if (isMine) Color(0xFF0B3A6A) else Color(0xFF0F172A)
    val textColor = Color(0xFFE5E7EB)
    val metaColor = Color(0xFF9CA3AF)

    val shape = if (isMine) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 6.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 6.dp, bottomEnd = 18.dp)
    }

    val sidePadding =
        if (isMine) PaddingValues(start = 64.dp, end = 12.dp)
        else PaddingValues(start = 12.dp, end = 64.dp)

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
                if (ui.msg.texto.isNotBlank()) {
                    Text(
                        text = ui.msg.texto,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }

                if (ui.msg.imageUrl.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("🖼️ Imagen", color = metaColor, style = MaterialTheme.typography.labelMedium)
                }
                if (ui.msg.audioUrl.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text("🎤 Audio", color = metaColor, style = MaterialTheme.typography.labelMedium)
                }

                Spacer(Modifier.height(6.dp))

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
                        Text(text = "✓✓", fontSize = 11.sp, color = metaColor)
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

/**
 * Convierte un timestamp flexible (String/Long/Int/Double) a epochMillis.
 * Soporta:
 * - "1700000000" (segundos)
 * - "1700000000000" (milisegundos)
 * - números (Long/Int/Double)
 */
private fun toEpochMillis(ts: Any?): Long? {
    val n: Long? = when (ts) {
        null -> null
        is Long -> ts
        is Int -> ts.toLong()
        is Double -> ts.toLong()
        is Float -> ts.toLong()
        is String -> ts.trim().toLongOrNull()
        else -> null
    } ?: return null

    // Heurística: si parece segundos (10 dígitos aprox), pásalo a ms.
    return if (n in 1_000_000_000L..9_999_999_999L) n?.times(1000L) else n
}

private fun formatTime(timestamp: Any?): String {
    val ms = toEpochMillis(timestamp) ?: return ""
    return try {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))
    } catch (_: Exception) {
        ""
    }
}

private fun formatDay(timestamp: Any?): String {
    val ms = toEpochMillis(timestamp) ?: return ""
    return try {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(ms))
    } catch (_: Exception) {
        ""
    }
}