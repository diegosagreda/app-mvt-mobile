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

private data class ThemeModeOption(
    val mode: AppThemeMode,
    val title: String,
    val icon: ImageVector
)

@Composable
fun AccountSettingsScreen(
    showConnection: Boolean = true,
    showAthleteSections: Boolean = false,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val accountSettingsOptions = listOfNotNull(
        if (showAthleteSections) AccountSettingsOption(
            title = "Perfil",
            subtitle = "Datos personales y deportivos",
            route = "profile",
            icon = Icons.Default.Person,
            accent = PrimaryBlue
        ) else null,
        if (showAthleteSections) AccountSettingsOption(
            title = "Plan",
            subtitle = "Suscripción, pagos y entrenador",
            route = "plan",
            icon = Icons.Default.Map,
            accent = PrimaryBlue
        ) else null,
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
                text = "Configuración",
                color = AppTextPrimary,
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

        item {
            AppearanceModeCard(
                selectedMode = themeMode,
                onThemeModeChange = onThemeModeChange
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
    val titleColor = if (option.destructive) option.accent else AppTextPrimary

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

@Composable
private fun AppearanceModeCard(
    selectedMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit
) {
    val options = listOf(
        ThemeModeOption(
            mode = AppThemeMode.LIGHT,
            title = "Claro",
            icon = Icons.Default.WbSunny
        ),
        ThemeModeOption(
            mode = AppThemeMode.DARK,
            title = "Oscuro",
            icon = Icons.Default.DarkMode
        )
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, AppBorder),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PrimaryBlue.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Apariencia",
                        color = AppTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Elige cómo quieres ver la app",
                        color = AppTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { option ->
                    AppearanceModeChip(
                        option = option,
                        selected = selectedMode == option.mode,
                        onClick = { onThemeModeChange(option.mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppearanceModeChip(
    option: ThemeModeOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) PrimaryBlue else AppSurfaceAlt
    val contentColor = if (selected) Color.White else AppTextPrimary

    Surface(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) PrimaryBlue else AppBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = option.title,
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
