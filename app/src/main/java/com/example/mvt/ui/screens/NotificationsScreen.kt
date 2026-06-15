package com.example.mvt.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.data.firebase.models.AppNotification
import com.example.mvt.R
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue
import com.example.mvt.ui.viewmodels.NotificationsUiState
import com.example.mvt.utils.NotificationSelectionBus
import java.util.concurrent.TimeUnit
import androidx.compose.runtime.snapshotFlow

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NotificationsScreen(
    uiState: NotificationsUiState,
    onClose: () -> Unit,
    onDismissError: () -> Unit,
    onLoadMore: () -> Unit,
    onNotificationClick: (AppNotification) -> Unit
) {
    val listState = rememberLazyListState()
    val visibleNotifications = uiState.notifications
    val notificationsBackground = Color(0xFF243144)

    LaunchedEffect(
        listState,
        visibleNotifications.size,
        uiState.hasMore,
        uiState.isLoadingMore
    ) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        }
            .collect { lastVisibleIndex ->
                val shouldLoadMore = visibleNotifications.isNotEmpty() &&
                    uiState.hasMore &&
                    !uiState.isLoadingMore &&
                    lastVisibleIndex >= visibleNotifications.lastIndex - 1

                if (!shouldLoadMore) return@collect
                onLoadMore()
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(notificationsBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Notificaciones",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(PrimaryBlue)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.clickable(onClick = onClose)
                ) {
                    Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = AppTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = Color(0x22FF8A6B),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0x44FF8A6B)),
                    onClick = onDismissError
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.errorMessage.orEmpty(),
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Cerrar",
                            color = PrimaryBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }

                visibleNotifications.isEmpty() -> {
                    EmptyNotificationsState()
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 28.dp)
                    ) {
                        items(visibleNotifications, key = { it.id }) { notification ->
                            NotificationCard(
                                notification = notification,
                                onClick = { onNotificationClick(notification) }
                            )
                        }

                        if (uiState.isLoadingMore) {
                            item(key = "notifications_loading_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = PrimaryBlue,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationDetailScreen() {
    val notification by NotificationSelectionBus.selected.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(16.dp)
    ) {
        if (notification == null) {
            EmptyNotificationsState(
                emptyText = "No hay una notificacion seleccionada."
            )
        } else {
            val selectedNotification = requireNotNull(notification)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = AppSurface,
                border = BorderStroke(1.dp, AppBorder)
            ) {
                Box(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = formatRelativeTime(selectedNotification.timestampMillis),
                        color = AppTextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, end = 84.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NotificationAvatar(notification = selectedNotification)
                        Text(
                            text = selectedNotification.txt.ifBlank { "Sin contenido disponible." },
                            color = AppTextPrimary,
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun NotificationCard(
    notification: AppNotification,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (notification.isUnread) {
                    Color(0x122C7BE5)
                } else {
                    Color.White.copy(alpha = 0.025f)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(3.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(
                    if (notification.isUnread) {
                        PrimaryBlue
                    } else {
                        Color.White.copy(alpha = 0.16f)
                    }
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (notification.isUnread) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFC4C02))
                    )
                }

                Text(
                    text = formatRelativeTime(notification.timestampMillis),
                    color = AppTextSecondary,
                    fontSize = 12.sp
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 84.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NotificationAvatar(notification = notification)
                Text(
                    text = notification.txt.ifBlank { "Sin contenido disponible." },
                    color = AppTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NotificationAvatar(notification: AppNotification) {
    val colors = notificationVisualAccent(notification)

    Surface(
        modifier = Modifier.size(34.dp),
        shape = CircleShape,
        color = colors.background.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.65f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (notification.isStravaRoutine) {
                Image(
                    painter = painterResource(id = R.drawable.ic_strava_mark),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    colorFilter = ColorFilter.tint(colors.foreground)
                )
            } else {
                Icon(
                    imageVector = notificationIcon(notification),
                    contentDescription = null,
                    tint = colors.foreground,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyNotificationsState(
    emptyText: String = "No hay notificaciones disponibles."
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.04f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = AppTextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Text(
                text = "Sin notificaciones",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = emptyText,
                color = AppTextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

private fun notificationIcon(notification: AppNotification): ImageVector {
    return when {
        notification.isStravaRoutine -> Icons.Default.DirectionsRun
        notification.tipo.equals("rutina", ignoreCase = true) &&
            notification.txt.contains("actual", ignoreCase = true) -> Icons.Default.SyncAlt
        notification.tipo.equals("rutina", ignoreCase = true) -> Icons.Default.CalendarMonth
        notification.tipo.equals("match", ignoreCase = true) -> Icons.Default.Groups
        notification.tipo.equals("solicitud", ignoreCase = true) -> Icons.Default.PersonAddAlt1
        notification.tipo.equals("msg", ignoreCase = true) ||
            notification.tipo.equals("chat", ignoreCase = true) -> Icons.Default.Email
        notification.tipo.equals("nuevoObjetivo", ignoreCase = true) -> Icons.Default.EmojiEvents
        notification.tipo.equals("rendimiento", ignoreCase = true) -> Icons.Default.Bolt
        notification.tipo.equals("salud", ignoreCase = true) -> Icons.Default.Favorite
        notification.tipo.equals("morfologia", ignoreCase = true) -> Icons.Default.AccessibilityNew
        notification.tipo.equals("capacidad_fisica", ignoreCase = true) -> Icons.Default.FitnessCenter
        notification.tipo.equals("deportiva", ignoreCase = true) -> Icons.Default.DirectionsRun
        else -> Icons.Default.Tag
    }
}

private data class NotificationAccentColors(
    val background: Color,
    val foreground: Color,
    val border: Color
)

private fun notificationVisualAccent(notification: AppNotification): NotificationAccentColors {
    return when {
        notification.isStravaRoutine -> NotificationAccentColors(
            background = Color(0x14FC4C02),
            foreground = Color(0xFFFFB28D),
            border = Color(0x33FC4C02)
        )
        notification.tipo.equals("rutina", ignoreCase = true) -> NotificationAccentColors(
            background = Color(0xFF2C7BE5),
            foreground = Color.White,
            border = Color(0xFF74C2FF)
        )
        notification.tipo.equals("match", ignoreCase = true) -> NotificationAccentColors(
            background = Color(0xFF1F6FE0),
            foreground = Color.White,
            border = Color(0xFF6EBEFF)
        )
        notification.isMessageLike -> NotificationAccentColors(
            background = Color(0xFF255EC9),
            foreground = Color.White,
            border = Color(0xFF7CBFFF)
        )
        else -> NotificationAccentColors(
            background = Color(0xFF355E9B),
            foreground = Color.White,
            border = Color(0xFF7EBEFF)
        )
    }
}

private fun formatRelativeTime(timestampMillis: Long?): String {
    if (timestampMillis == null) return "Ahora"

    val deltaMillis = (System.currentTimeMillis() - timestampMillis).coerceAtLeast(0L)
    val days = TimeUnit.MILLISECONDS.toDays(deltaMillis)
    if (days > 0) return if (days == 1L) "1 dia" else "$days dias"

    val hours = TimeUnit.MILLISECONDS.toHours(deltaMillis)
    if (hours > 0) return if (hours == 1L) "1 hora" else "$hours horas"

    val minutes = TimeUnit.MILLISECONDS.toMinutes(deltaMillis)
    if (minutes > 0) return if (minutes == 1L) "1 minuto" else "$minutes minutos"

    val seconds = TimeUnit.MILLISECONDS.toSeconds(deltaMillis).coerceAtLeast(1L)
    return if (seconds == 1L) "1 segundo" else "$seconds segundos"
}
