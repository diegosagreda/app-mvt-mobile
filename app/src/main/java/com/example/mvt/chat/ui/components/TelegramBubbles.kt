package com.example.mvt.chat.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.R
import com.example.mvt.chat.data.model.ChatMessage
import com.example.mvt.chat.data.model.UiMessage
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppError
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

private val reactionEmojis = listOf("👍", "❤️", "🔥", "😂", "👏", "😮")

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

    val container = AppBackground
    val fieldBg = AppSurfaceAlt
    val stroke = AppBorder
    val hint = AppTextSecondary
    val textColor = AppTextPrimary

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
                    cursorColor = PrimaryBlue
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
                    .background(if (enabled) PrimaryBlue else AppSurfaceAlt)
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
    val bg = AppSurface
    val line = PrimaryBlue
    val text = AppTextPrimary
    val meta = AppTextSecondary

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
                tint = AppTextSecondary
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
            color = AppSurfaceAlt,
            modifier = Modifier.padding(vertical = 10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = AppTextSecondary,
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
    onDelete: (ChatMessage) -> Unit,
    onReact: (ChatMessage, String) -> Unit = { _, _ -> },
    myAvatarUrl: String = "",
    otherAvatarUrl: String = "",
    recipientHasSeen: Boolean = false,
    isReadByRecipient: Boolean = false
) {
    var menu by remember { mutableStateOf(false) }
    var expandedImageUrl by remember { mutableStateOf<String?>(null) }

    val isMine = ui.isMine

    val bubbleColor = if (isMine) PrimaryBlue.copy(alpha = 0.20f) else AppSurface
    val textColor = AppTextPrimary
    val metaColor = AppTextSecondary
    val accentColor = if (isMine) PrimaryBlue else AppTextSecondary.copy(alpha = 0.65f)
    val showMessageText = shouldRenderMessageText(ui.msg)
    val emojiOnly = showMessageText && isEmojiOnly(ui.msg.texto)
    val groupedReactions = ui.msg.reactions.values.groupingBy { it }.eachCount()

    val shape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isMine) 18.dp else 6.dp,
        bottomEnd = if (isMine) 6.dp else 18.dp
    )

    val sidePadding =
        if (isMine) PaddingValues(start = 34.dp, end = 0.dp)
        else PaddingValues(start = 0.dp, end = 34.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(sidePadding),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMine) {
            ChatMessageAvatar(photoUrl = otherAvatarUrl, contentDescription = "Foto del entrenador")
            Spacer(Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 264.dp)
                .wrapContentWidth()
                .padding(bottom = if (groupedReactions.isNotEmpty()) 18.dp else 0.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { menu = true }
                )
        ) {
            Column(
                modifier = Modifier
                    .shadow(5.dp, shape)
                    .border(1.dp, accentColor.copy(alpha = 0.35f), shape)
                    .clip(shape)
                    .background(bubbleColor)
                    .padding(start = 4.dp, end = 12.dp, top = 10.dp, bottom = 8.dp)
            ) {
                Row {
                    Column(
                        modifier = Modifier
                            .wrapContentWidth()
                            .widthIn(max = 236.dp)
                    ) {
                        if (ui.msg.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = ui.msg.imageUrl,
                                contentDescription = "Imagen enviada por chat",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 140.dp, max = 240.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { expandedImageUrl = ui.msg.imageUrl },
                                contentScale = ContentScale.Crop
                            )
                        }

                        if (showMessageText) {
                            if (ui.msg.imageUrl.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                            }
                            Text(
                                text = ui.msg.texto,
                                color = textColor,
                                style = if (emojiOnly) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyMedium,
                                lineHeight = if (emojiOnly) 28.sp else 20.sp
                            )
                        }

                        if (ui.msg.audioUrl.isNotBlank()) {
                            Spacer(Modifier.height(if (showMessageText || ui.msg.imageUrl.isNotBlank()) 6.dp else 0.dp))
                            AudioMessagePlayer(
                                audioUrl = ui.msg.audioUrl,
                                contentColor = textColor,
                                secondaryColor = metaColor
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        Row(
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
                                    color = if (isReadByRecipient) PrimaryBlue else metaColor
                                )
                            }
                        }
                    }
                }
            }

            if (groupedReactions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .align(if (isMine) Alignment.BottomEnd else Alignment.BottomStart)
                        .offset(y = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    groupedReactions.forEach { (emoji, count) ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = AppSurfaceAlt,
                            border = ButtonDefaults.outlinedButtonBorder
                        ) {
                            Text(
                                text = "$emoji $count",
                                style = MaterialTheme.typography.labelMedium,
                                color = AppTextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false }
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reactionEmojis.forEach { emoji ->
                        Surface(
                            onClick = {
                                menu = false
                                onReact(ui.msg, emoji)
                            },
                            shape = RoundedCornerShape(999.dp),
                            color = AppSurfaceAlt
                        ) {
                            Text(
                                text = emoji,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                DropdownMenuItem(
                    text = { Text("Responder") },
                    onClick = { menu = false; onReply(ui.msg) }
                )
                if (isMine) {
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

        if (isMine) {
            Spacer(Modifier.width(8.dp))
            ChatMessageAvatar(photoUrl = myAvatarUrl, contentDescription = "Foto del deportista")
        }
    }

    if (expandedImageUrl != null) {
        Dialog(onDismissRequest = { expandedImageUrl = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = AppSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box {
                    AsyncImage(
                        model = expandedImageUrl,
                        contentDescription = "Imagen ampliada del chat",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )

                    IconButton(
                        onClick = { expandedImageUrl = null },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar imagen",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BubbleTail(
    color: Color,
    isMine: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.size(width = 10.dp, height = 14.dp)
    ) {
        val path = androidx.compose.ui.graphics.Path().apply {
            if (isMine) {
                moveTo(size.width, 0f)
                lineTo(0f, size.height)
                lineTo(size.width, size.height * 0.72f)
            } else {
                moveTo(0f, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height * 0.72f)
            }
            close()
        }
        drawPath(path, color)
    }
}

@Composable
private fun AudioMessagePlayer(
    audioUrl: String,
    contentColor: Color,
    secondaryColor: Color
) {
    val context = LocalContext.current
    var isPlaying by remember(audioUrl) { mutableStateOf(false) }
    var isBuffering by remember(audioUrl) { mutableStateOf(false) }
    var hasError by remember(audioUrl) { mutableStateOf(false) }
    var isEnded by remember(audioUrl) { mutableStateOf(false) }
    var positionMs by remember(audioUrl) { mutableLongStateOf(0L) }
    var durationMs by remember(audioUrl) { mutableLongStateOf(0L) }
    var isMuted by remember(audioUrl) { mutableStateOf(false) }
    val player = remember(audioUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(audioUrl))
            prepare()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                isEnded = state == Player.STATE_ENDED
                if (isEnded) isPlaying = false
            }

            override fun onPlayerError(error: PlaybackException) {
                hasError = true
                isPlaying = false
                isBuffering = false
                isEnded = false
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, isPlaying, isBuffering) {
        while (true) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.takeIf { it > 0L } ?: 0L
            delay(200L)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                hasError = false
                if (isEnded) {
                    player.seekTo(0)
                    isEnded = false
                }
                if (player.isPlaying) player.pause() else player.play()
            },
            enabled = !isBuffering,
            modifier = Modifier.size(40.dp)
        ) {
            when {
                isBuffering -> CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                isEnded -> Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = "Reproducir audio nuevamente",
                    tint = Color.White
                )
                isPlaying -> Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pausar audio",
                    tint = Color.White
                )
                else -> Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Reproducir audio",
                    tint = Color.White
                )
            }
        }

        Column(Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hasError) "No se pudo reproducir el audio" else formatAudioTime(positionMs),
                    color = if (hasError) AppError else secondaryColor,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatAudioTime(durationMs),
                    color = secondaryColor,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Slider(
                value = if (durationMs > 0L) {
                    (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                },
                onValueChange = { progress ->
                    if (durationMs > 0L) {
                        isEnded = false
                        player.seekTo((progress * durationMs).toLong())
                    }
                },
                enabled = durationMs > 0L && !hasError,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryBlue,
                    activeTrackColor = PrimaryBlue,
                    inactiveTrackColor = secondaryColor.copy(alpha = 0.35f)
                )
            )
        }

        IconButton(
            onClick = {
                isMuted = !isMuted
                player.volume = if (isMuted) 0f else 1f
            },
            enabled = !hasError,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = if (isMuted) "Activar sonido" else "Silenciar audio",
                tint = secondaryColor
            )
        }
    }
}

private fun formatAudioTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(Locale.getDefault(), minutes, seconds)
}

@Composable
private fun ChatMessageAvatar(
    photoUrl: String,
    contentDescription: String
) {
    Surface(
        shape = androidx.compose.foundation.shape.CircleShape,
        color = AppSurfaceAlt,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder),
        modifier = Modifier.size(30.dp)
    ) {
        if (photoUrl.isBlank()) {
            Icon(
                painter = painterResource(id = R.drawable.iconografia_02_svg),
                contentDescription = contentDescription,
                tint = AppTextSecondary,
                modifier = Modifier.padding(6.dp)
            )
        } else {
            AsyncImage(
                model = photoUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(androidx.compose.foundation.shape.CircleShape)
            )
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
    if (ts is String) {
        val trimmed = ts.trim()
        trimmed.toLongOrNull()?.let { raw ->
            return if (raw in 1_000_000_000L..9_999_999_999L) raw * 1000L else raw
        }
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
            fmt.parse(trimmed)?.time
        } catch (_: Exception) {
            null
        }
    }

    val n = when (ts) {
        null -> null
        is Long -> ts
        is Int -> ts.toLong()
        is Double -> ts.toLong()
        is Float -> ts.toLong()
        else -> null
    } ?: return null

    // Heurística: si parece segundos (10 dígitos aprox), pásalo a ms.
    return if (n in 1_000_000_000L..9_999_999_999L) n * 1000L else n
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
    val messageDate = Calendar.getInstance().apply { timeInMillis = ms }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        isSameCalendarDay(messageDate, today) -> "HOY"
        isSameCalendarDay(messageDate, yesterday) -> "AYER"
        else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(ms))
    }
}

private fun isSameCalendarDay(first: Calendar, second: Calendar): Boolean {
    return first.get(Calendar.ERA) == second.get(Calendar.ERA) &&
        first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
        first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
}

private fun isEmojiOnly(text: String): Boolean {
    val trimmed = text.trim()
    if (trimmed.isBlank() || trimmed.length > 8) return false
    return trimmed.none { it.isLetterOrDigit() }
}

private fun shouldRenderMessageText(message: ChatMessage): Boolean {
    val text = message.texto.trim()
    if (text.isBlank()) return false

    if (message.imageUrl.isNotBlank() && (text == message.imageUrl || isLikelyUrl(text) || isLocalPath(text))) {
        return false
    }

    if (message.audioUrl.isNotBlank() && text == message.audioUrl) {
        return false
    }

    return true
}

private fun isLikelyUrl(value: String): Boolean {
    return value.matches(Regex("^(https?://|www\\.).*", RegexOption.IGNORE_CASE))
}

private fun isLocalPath(value: String): Boolean {
    val trimmed = value.trim()
    return trimmed.contains("fakepath", ignoreCase = true) ||
        Regex("^[a-zA-Z]:\\\\").containsMatchIn(trimmed) ||
        trimmed.contains("\\")
}
