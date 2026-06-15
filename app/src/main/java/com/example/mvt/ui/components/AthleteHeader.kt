package com.example.mvt.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mvt.R
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSuccess
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppTextSecondary

private val HeaderBadgeRed = Color(0xFFE64848)
private val HeaderBadgePulseRed = Color(0xFFE64848)
private val HeaderActionSize = 34.dp
private val HeaderActionSpacing = 8.dp
private val HeaderMainIconSize = 26.4.dp

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AthleteHeader(
    onMessageClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    profilePhotoUrl: String? = null,
    isStravaConnected: Boolean = false,
    unreadMessagesCount: Int = 0,
    unreadNotificationsCount: Int = 0
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground),
        navigationIcon = {
            if (isStravaConnected) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(HeaderActionSize),
                    contentAlignment = Alignment.Center
                ) {
                    StravaSyncBadge()
                }
            }
        },
        title = {},
        actions = {
            Row(
                modifier = Modifier.padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HeaderActionSpacing)
            ) {
                MessageActionButton(
                    unreadCount = unreadMessagesCount,
                    onClick = onMessageClick
                )
                NotificationActionButton(
                    unreadCount = unreadNotificationsCount,
                    onClick = onNotificationClick
                )

                Box(
                    modifier = Modifier
                        .size(HeaderActionSize)
                        .clip(CircleShape)
                        .background(AppSurface)
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (profilePhotoUrl == null) {
                        Image(
                            painter = painterResource(id = R.drawable.iconografia_02_svg),
                            contentDescription = "Foto de perfil",
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        AsyncImage(
                            model = profilePhotoUrl,
                            contentDescription = "Foto de perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Transparent)
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun NotificationActionButton(
    unreadCount: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(HeaderActionSize)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.Notifications,
            contentDescription = "Notificaciones",
            tint = Color.White,
            modifier = Modifier.size(HeaderMainIconSize)
        )

        if (unreadCount > 0) {
            CounterBadge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-2).dp),
                unreadCount = unreadCount
            )
        }
    }
}

@Composable
private fun MessageActionButton(
    unreadCount: Int,
    onClick: () -> Unit
) {
    val pulse = rememberInfiniteTransition(label = "chat_badge")
    val pulseScale by pulse.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "chat_badge_scale"
    )
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.20f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "chat_badge_alpha"
    )

    Box(
        modifier = Modifier
            .size(HeaderActionSize)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.Email,
            contentDescription = "Mensajes",
            tint = Color.White,
            modifier = Modifier.size(HeaderMainIconSize)
        )

        if (unreadCount > 0) {
            CounterBadge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-2).dp),
                unreadCount = unreadCount,
                pulseScale = pulseScale,
                pulseAlpha = pulseAlpha
            )
        }
    }
}

@Composable
private fun CounterBadge(
    modifier: Modifier = Modifier,
    unreadCount: Int,
    pulseScale: Float? = null,
    pulseAlpha: Float? = null
) {
    Box(modifier = modifier) {
        if (pulseScale != null && pulseAlpha != null) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                    .clip(CircleShape)
                    .background(HeaderBadgePulseRed.copy(alpha = pulseAlpha))
            )
        }

        Surface(
            modifier = Modifier.size(18.dp),
            shape = CircleShape,
            color = HeaderBadgeRed,
            contentColor = Color.White,
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, AppBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun StravaSyncBadge() {
    val syncPulse = rememberInfiniteTransition(label = "strava_sync")
    val pulseScale by syncPulse.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "strava_sync_scale"
    )
    val pulseAlpha by syncPulse.animateFloat(
        initialValue = 0.22f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "strava_sync_alpha"
    )

    Box(
        modifier = Modifier
            .size(HeaderActionSize)
            .semantics { contentDescription = "Strava conectado y sincronizado" },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 28.dp, height = 24.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFC6A2A), Color(0xFFFC4C02))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_strava_mark),
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 2.dp, bottom = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                        .clip(CircleShape)
                        .background(AppSuccess.copy(alpha = pulseAlpha))
                )
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(AppSuccess)
                        .border(1.dp, AppBorder, CircleShape)
                )
            }
        }
    }
}
