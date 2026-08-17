package com.example.mvt.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.ui.theme.*

data class AccountSettingsOption(
    val title: String,
    val subtitle: String,
    val route: String?,
    val icon: ImageVector,
    val accent: Color,
    val destructive: Boolean = false
)

@Composable
fun AccountSettingsScreen(
    showConnection: Boolean = true,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val accountSettingsOptions = listOfNotNull(
        if (showConnection) AccountSettingsOption(
            title = "Conexión",
            subtitle = "Sincronización y servicios externos",
            route = "connection",
            icon = Icons.Default.Link,
            accent = Color(0xFF63C7FF)
        ) else null,
        AccountSettingsOption(
            title = "Ayuda",
            subtitle = "Soporte y orientación de la app",
            route = UnderConstructionDestination.routeFor("help"),
            icon = Icons.Outlined.HelpOutline,
            accent = Color(0xFFC29BFF)
        ),
        AccountSettingsOption(
            title = "Acerca de",
            subtitle = "Información de My Virtual Trainer",
            route = UnderConstructionDestination.routeFor("about"),
            icon = Icons.Default.Info,
            accent = Color(0xFF62D9A8)
        ),
        AccountSettingsOption(
            title = "Cerrar sesión",
            subtitle = "Salir de tu cuenta actual",
            route = null,
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            accent = Color(0xFFFF8A8A),
            destructive = true
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 24.dp)
    ) {
        item {
            Text(
                text = "Cuenta y configuración",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }

        item {
            Text(
                text = "Gestiona conexiones, soporte e información de tu cuenta.",
                color = AppTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        accountSettingsOptions.forEach { option ->
            item {
                AccountSettingsRow(
                    option = option,
                    onClick = {
                        option.route?.let(onNavigate) ?: onLogout()
                    }
                )
            }
        }
    }
}

@Composable
private fun AccountSettingsRow(
    option: AccountSettingsOption,
    onClick: () -> Unit
) {
    val titleColor = if (option.destructive) option.accent else Color.White

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = AppSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, AppBorder),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(option.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = null,
                    tint = option.accent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = option.title,
                    color = titleColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = option.subtitle,
                    color = AppTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = AppTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
