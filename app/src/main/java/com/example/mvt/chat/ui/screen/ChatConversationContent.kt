package com.example.mvt.chat.ui.screen

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mvt.chat.data.model.ChatMessage
import com.example.mvt.chat.ui.components.TelegramMessageBubble
import com.example.mvt.chat.ui.components.DateHeader
import com.example.mvt.chat.viewmodel.ChatUiState
import com.example.mvt.chat.viewmodel.ChatViewModel
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

private val composerEmojis = listOf("💪", "🔥", "👏", "🎯", "👍", "❤️", "😂", "😮")
private const val AUDIO_RECORDING_TAG = "ChatAudioRecording"

@Composable
fun ChatConversationContent(
    vm: ChatViewModel,
    state: ChatUiState,
    myAvatarUrl: String = "",
    otherAvatarUrl: String = ""
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
            .background(AppBackground)
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
            if (state.messages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Aún no hay mensajes.",
                            color = AppTextPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Inicia la conversación con tu deportista.",
                            color = AppTextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            // FIX: key estable con fallback si msg.id viene ""
            items(
                items = state.messages,
                key = { ui ->
                    "message_${ui.msg.id.takeIf { it.isNotBlank() }
                        ?: "${ui.msg.remitente}_${ui.msg.timestamp}"}"
                }
            ) { ui ->
                if (ui.showGroupHeader) {
                    DateHeader(ui.groupLabel)
                }
                TelegramMessageBubble(
                    ui = ui,
                    onReply = { vm.setReply(it) },
                    onEdit = { vm.setEdit(it) },
                    onDelete = { vm.deleteMessage(it.id) },
                    onReact = { message, emoji -> vm.toggleReaction(message.id, emoji) },
                    myAvatarUrl = myAvatarUrl,
                    otherAvatarUrl = otherAvatarUrl,
                    recipientHasSeen = state.recipientHasSeen,
                    isReadByRecipient = ui.isReadByRecipient
                )
            }
        }

        ChatComposer(
            replyingTo = state.replyingTo,
            imageUri = state.imageUri,
            audioUri = state.audioUri,
            isUploadingImage = state.isUploadingImage,
            isSendingMessage = state.isSendingMessage,
            onAttachImage = { vm.attachImage(it) },
            onAttachAudio = { vm.attachAudio(it) },
            onClearImage = { vm.attachImage(null) },
            onCancelReply = { vm.clearReply() },
            onSend = { text ->
                if (text.trim().isNotEmpty() || state.imageUri != null || state.audioUri != null) {
                    vm.sendTextMessage(
                        text = text.trim(),
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
    imageUri: Uri?,
    audioUri: Uri?,
    isUploadingImage: Boolean,
    isSendingMessage: Boolean,
    onAttachImage: (Uri?) -> Unit,
    onAttachAudio: (Uri?) -> Unit,
    onClearImage: () -> Unit,
    onCancelReply: () -> Unit,
    onSend: (String) -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }
    var showEmojiPicker by rememberSaveable { mutableStateOf(false) }
    var showAttachMenu by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var wasSending by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startAudioRecording(
                context = context,
                onRecorderCreated = { recorder = it },
                onFileCreated = { recordingFile = it },
                onRecordingChanged = { isRecording = it }
            )
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) onAttachImage(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            onAttachImage(pendingCameraUri)
        } else {
            onAttachImage(null)
        }
        pendingCameraUri = null
    }

    LaunchedEffect(isSendingMessage, imageUri) {
        if (wasSending && !isSendingMessage && imageUri == null) {
            text = ""
            showEmojiPicker = false
        }
        wasSending = isSendingMessage
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isRecording) {
                delay(1_000)
                recordingSeconds++
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder?.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppSurface)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (replyingTo != null) {
            ReplyPreview(
                message = replyingTo,
                onClose = onCancelReply
            )
            Spacer(Modifier.height(8.dp))
        }

        if (imageUri != null) {
            ImageAttachmentPreview(
                imageUri = imageUri,
                isUploadingImage = isUploadingImage,
                onClear = onClearImage
            )
            Spacer(Modifier.height(8.dp))
        }

        if (audioUri != null && !isRecording) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AppSurfaceAlt,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = PrimaryBlue)
                    Spacer(Modifier.width(8.dp))
                    Text("Audio listo para enviar", color = AppTextPrimary, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onAttachAudio(null) }) {
                        Icon(Icons.Default.Close, contentDescription = "Quitar audio", tint = AppTextSecondary)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (showEmojiPicker) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                composerEmojis.forEach { emoji ->
                    Surface(
                        onClick = {
                            text = buildString {
                                append(text)
                                append(emoji)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = AppSurfaceAlt,
                        tonalElevation = 1.dp
                    ) {
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
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
                    focusedContainerColor = AppSurfaceAlt,
                    unfocusedContainerColor = AppSurfaceAlt,
                    focusedBorderColor = AppBorder,
                    unfocusedBorderColor = AppBorder,
                    cursorColor = PrimaryBlue,
                    focusedTextColor = AppTextPrimary,
                    unfocusedTextColor = AppTextPrimary,
                    focusedPlaceholderColor = AppTextSecondary,
                    unfocusedPlaceholderColor = AppTextSecondary
                )
            )

            Spacer(Modifier.width(10.dp))

            val enabled = text.trim().isNotEmpty() || audioUri != null || imageUri != null
            IconButton(
                enabled = enabled,
                onClick = { onSend(text) },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (enabled) PrimaryBlue else AppSurfaceAlt,
                        RoundedCornerShape(14.dp)
                    )
            ) {
                if (isSendingMessage) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color.White)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                ComposerToolButton(
                    icon = Icons.Default.AttachFile,
                    contentDescription = "Adjuntar contenido",
                    onClick = { showAttachMenu = true }
                )

                DropdownMenu(
                    expanded = showAttachMenu,
                    onDismissRequest = { showAttachMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Galeria") },
                        onClick = {
                            showAttachMenu = false
                            galleryLauncher.launch("image/*")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Camara") },
                        onClick = {
                            showAttachMenu = false
                            val uri = createTempImageUri(context)
                            if (uri != null) {
                                pendingCameraUri = uri
                                cameraLauncher.launch(uri)
                            }
                        }
                    )
                }
            }

            ComposerToolButton(
                icon = Icons.Default.EmojiEmotions,
                contentDescription = if (showEmojiPicker) "Cerrar emojis" else "Abrir emojis",
                onClick = { showEmojiPicker = !showEmojiPicker }
            )

            ComposerToolButton(
                icon = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "Detener grabación" else "Grabar audio",
                onClick = {
                    if (isRecording) {
                        runCatching { recorder?.stop() }
                            .onFailure { Log.e(AUDIO_RECORDING_TAG, "No fue posible finalizar el audio", it) }
                        recorder?.release()
                        recorder = null
                        isRecording = false
                        recordingFile?.let { onAttachAudio(Uri.fromFile(it)) }
                    } else if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        startAudioRecording(
                            context = context,
                            onRecorderCreated = { recorder = it },
                            onFileCreated = { recordingFile = it },
                            onRecordingChanged = { isRecording = it }
                        )
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                enabled = !isSendingMessage && !isUploadingImage
            )

            if (isRecording) {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Grabando ${formatRecordingTime(recordingSeconds)}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            } else {
                Text(
                    text = "Adjunta una imagen o graba un audio",
                    color = AppTextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ComposerToolButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) AppSurfaceAlt else AppSurfaceAlt.copy(alpha = 0.5f),
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) AppTextPrimary else AppTextSecondary
            )
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
            .background(AppSurfaceAlt, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(34.dp)
                .background(PrimaryBlue, RoundedCornerShape(2.dp))
        )

        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = "Respondiendo a…",
                style = MaterialTheme.typography.labelMedium,
                color = AppTextSecondary
            )
            Text(
                text = message.texto.ifBlank { "(mensaje sin texto)" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = AppTextPrimary
            )
        }

        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancelar",
                tint = AppTextSecondary
            )
        }
    }
}

@Composable
private fun ImageAttachmentPreview(
    imageUri: Uri,
    isUploadingImage: Boolean,
    onClear: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AppSurfaceAlt,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box {
            AsyncImage(
                model = imageUri,
                contentDescription = "Vista previa de imagen",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )

            if (isUploadingImage) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.32f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp)
                }
            }

            IconButton(
                onClick = onClear,
                enabled = !isUploadingImage,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Quitar imagen",
                    tint = Color.White
                )
            }
        }
    }
}

private fun createTempImageUri(context: Context): Uri? {
    return runCatching {
        val directory = File(context.cacheDir, "chat_images").apply { mkdirs() }
        val file = File(directory, "chat_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }.getOrNull()
}

private fun startAudioRecording(
    context: Context,
    onRecorderCreated: (MediaRecorder) -> Unit,
    onFileCreated: (File) -> Unit,
    onRecordingChanged: (Boolean) -> Unit
) {
    val file = File(
        context.cacheDir,
        "chat_audio_${System.currentTimeMillis()}.m4a"
    )
    val audioRecorder = MediaRecorder().apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setAudioEncodingBitRate(128_000)
        setAudioSamplingRate(44_100)
        setOutputFile(file.absolutePath)
        prepare()
        start()
    }
    onFileCreated(file)
    onRecorderCreated(audioRecorder)
    onRecordingChanged(true)
}

private fun formatRecordingTime(seconds: Int): String {
    return "%d:%02d".format(Locale.getDefault(), seconds / 60, seconds % 60)
}
