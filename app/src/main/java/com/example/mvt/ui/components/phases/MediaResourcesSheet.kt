package com.example.mvt.ui.screens.components.phases

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.mvt.ui.theme.PrimaryBlue
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaResourcesSheet(
    recursos: List<String>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val maxHeight = screenHeight / 2 // mitad de pantalla

    var currentIndex by remember { mutableStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White,
        dragHandle = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFFEAF3FF), Color.White))
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicador superior
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(5.dp)
                        .background(Color.Gray.copy(0.4f), RoundedCornerShape(50))
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Recursos",
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Spacer(Modifier.height(10.dp))

                if (recursos.isNotEmpty()) {
                    val currentRecurso = recursos[currentIndex]

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 450.dp)
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures { _, dragAmount ->
                                    if (abs(dragAmount) > 40) {
                                        if (dragAmount > 0 && currentIndex > 0) {
                                            currentIndex--
                                        } else if (dragAmount < 0 && currentIndex < recursos.lastIndex) {
                                            currentIndex++
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        ResourceViewer(
                            recurso = currentRecurso
                        )

                        // Flecha izquierda
                        if (recursos.size > 1 && currentIndex > 0) {
                            IconButton(
                                onClick = { currentIndex-- },
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 4.dp)
                                    .size(40.dp)
                                    .background(Color.White.copy(0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBackIos,
                                    contentDescription = "Anterior",
                                    tint = PrimaryBlue
                                )
                            }
                        }

                        // Flecha derecha
                        if (recursos.size > 1 && currentIndex < recursos.lastIndex) {
                            IconButton(
                                onClick = { currentIndex++ },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 4.dp)
                                    .size(40.dp)
                                    .background(Color.White.copy(0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForwardIos,
                                    contentDescription = "Siguiente",
                                    tint = PrimaryBlue
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    if (recursos.size > 1) {
                        Text(
                            text = "${currentIndex + 1} / ${recursos.size}",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cerrar", color = Color.White)
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun ResourceViewer(recurso: String) {
    val tag = "ResourceViewer"
    Log.d(tag, "Composable ResourceViewer lanzado. Recurso: $recurso")

    val baseUrl = recurso.substringBefore('?').lowercase()
    val isVideo = baseUrl.endsWith(".mp4") || baseUrl.endsWith(".mov") || baseUrl.contains("/videos/")
    val context = LocalContext.current

    if (isVideo) {
        Log.d(tag, "Detectado tipo VIDEO")

        val exoPlayer = remember(recurso) {
            Log.d(tag, "Creando instancia de ExoPlayer...")
            ExoPlayer.Builder(context).build().apply {
                try {
                    val uri = Uri.parse(recurso)
                    Log.d(tag, "URI parseado correctamente: $uri")

                    val mediaItem = MediaItem.fromUri(uri)
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = true
                    Log.d(tag, "MediaItem asignado y preparado.")

                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            val estado = when (state) {
                                Player.STATE_BUFFERING -> "BUFFERING"
                                Player.STATE_READY -> "READY"
                                Player.STATE_ENDED -> "ENDED"
                                Player.STATE_IDLE -> "IDLE"
                                else -> "UNKNOWN"
                            }
                            Log.d(tag, "Cambio de estado: $estado")
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            Log.e(tag, "Error de reproducción: ${error.message}", error)
                        }
                    })
                } catch (e: Exception) {
                    Log.e(tag, "Error configurando ExoPlayer: ${e.message}", e)
                }
            }
        }

        DisposableEffect(Unit) {
            Log.d(tag, "DisposableEffect creado: Player activo")
            onDispose {
                Log.d(tag, "Liberando ExoPlayer en onDispose()")
                exoPlayer.release()
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    Log.d(tag, "Creando PlayerView en AndroidView.factory()")
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    }.also {
                        Log.d(tag, "PlayerView asociado correctamente al ExoPlayer")
                    }
                },
                update = { view ->
                    Log.d(tag, "AndroidView.update() ejecutado, reasignando player")
                    view.player = exoPlayer
                }
            )
        }
    } else {
        Log.d(tag, "Detectado tipo IMAGEN")
        var scale by remember { mutableStateOf(1f) }
        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 450.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Log.d(tag, "Mostrando imagen AsyncImage.")
            AsyncImage(
                model = recurso,
                contentDescription = null,
                modifier = Modifier
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
            )
        }
    }
}
