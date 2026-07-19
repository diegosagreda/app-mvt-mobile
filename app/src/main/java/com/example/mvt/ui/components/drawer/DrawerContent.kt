package com.example.mvt.ui.components.drawer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.R
import com.example.mvt.ui.screens.UnderConstructionDestination
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary

private fun pendingRoute(feature: String): String = UnderConstructionDestination.routeFor(feature)

@Composable
fun DrawerContent(
    currentRoute: String?,
    onItemClick: (String) -> Unit
) {
    val datosPersonalesItems = listOf(
        DrawerItemData("Perfil", Icons.Default.Person, "profile"),
        DrawerItemData("Morfologia", Icons.Default.Accessibility, "morphology"),
        DrawerItemData("Capacidad Fisica", Icons.Default.FitnessCenter, "fitness"),
        DrawerItemData("Rendimiento", Icons.Default.Timer, "performance"),
        DrawerItemData("Deportivo", Icons.Default.DirectionsBike, "sports"),
        DrawerItemData("Salud", Icons.Default.FavoriteBorder, "health"),
        DrawerItemData("Objetivos", Icons.Default.BarChart, "goals"),
        DrawerItemData("Disponibilidad", Icons.Default.EventAvailable, "availability")
    )
    val planesItems = listOf(
        DrawerItemData("Planes", Icons.Default.Map, "plans"),
        DrawerItemData("Suscripción", Icons.Default.CreditCard, "subscription"),
        DrawerItemData("Facturación", Icons.Default.ReceiptLong, "billing")
    )
    val entrenamientoItems = listOf(
        DrawerItemData("Tu Entrenador", Icons.Default.PersonPin, "trainer"),
        DrawerItemData("Entrenadores", Icons.Default.Groups, "trainers"),
        DrawerItemData("Rutinas", Icons.Default.CalendarMonth, "routines"),
        DrawerItemData("Explorar", Icons.Default.Search, pendingRoute("explore")),
        DrawerItemData("Sugerencias", Icons.Default.Lightbulb, pendingRoute("suggestions"))
    )
    val configuracionItems = listOf(
        DrawerItemData("Conexion", Icons.Default.Link, "connection"),
        DrawerItemData("Ayuda", Icons.Default.HelpOutline, pendingRoute("help")),
        DrawerItemData("Acerca de", Icons.Default.Info, pendingRoute("about"))
    )

    var datosPersonalesExpanded by remember { mutableStateOf(currentRoute in datosPersonalesItems.map { it.route }) }
    var planesExpanded by remember { mutableStateOf(currentRoute in planesItems.map { it.route }) }
    var entrenamientoExpanded by remember { mutableStateOf(currentRoute in entrenamientoItems.map { it.route }) }
    var configuracionExpanded by remember { mutableStateOf(currentRoute in configuracionItems.map { it.route }) }

    LaunchedEffect(currentRoute) {
        datosPersonalesExpanded = currentRoute in datosPersonalesItems.map { it.route }
        planesExpanded = currentRoute in planesItems.map { it.route }
        entrenamientoExpanded = currentRoute in entrenamientoItems.map { it.route }
        configuracionExpanded = currentRoute in configuracionItems.map { it.route }
    }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(324.dp)
            .background(AppBackground)
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        DrawerHeader(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 30.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 128.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DrawerSection(
                    title = "Datos Personales",
                    accent = Color(0xFF63C7FF),
                    expanded = datosPersonalesExpanded,
                    onToggle = { datosPersonalesExpanded = !datosPersonalesExpanded },
                    currentRoute = currentRoute,
                    items = datosPersonalesItems,
                    onItemClick = onItemClick
                )

                DrawerSection(
                    title = "Planes",
                    accent = Color(0xFF62D9A8),
                    expanded = planesExpanded,
                    onToggle = { planesExpanded = !planesExpanded },
                    currentRoute = currentRoute,
                    items = planesItems,
                    onItemClick = onItemClick
                )

                DrawerSection(
                    title = "Entrenamiento",
                    accent = Color(0xFFFF9A64),
                    expanded = entrenamientoExpanded,
                    onToggle = { entrenamientoExpanded = !entrenamientoExpanded },
                    currentRoute = currentRoute,
                    items = entrenamientoItems,
                    onItemClick = onItemClick
                )

                DrawerSection(
                    title = "Configuracion",
                    accent = Color(0xFFC29BFF),
                    expanded = configuracionExpanded,
                    onToggle = { configuracionExpanded = !configuracionExpanded },
                    currentRoute = currentRoute,
                    items = configuracionItems,
                    onItemClick = onItemClick
                )
            }
        }
    }
}

@Composable
private fun DrawerHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.mvt),
            contentDescription = "Logo MVT",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(124.dp)
                .height(52.dp)
        )

        Text(
            text = "Menu principal",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DrawerSection(
    title: String,
    accent: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
    currentRoute: String?,
    items: List<DrawerItemData>,
    onItemClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(
            title = title,
            accent = accent,
            expanded = expanded,
            onClick = onToggle
        )

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(start = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items.forEach { item ->
                    DrawerItem(
                        item = item,
                        accent = accent,
                        selected = currentRoute == item.route,
                        onItemClick = onItemClick
                    )
                }
            }
        }
    }
}

private data class DrawerItemData(
    val text: String,
    val icon: ImageVector,
    val route: String
)

@Composable
private fun SectionHeader(
    title: String,
    accent: Color,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "drawer-arrow"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.widthIn(max = 230.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 36.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent)
            )

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    color = AppTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = null,
            tint = accent,
            modifier = Modifier
                .padding(8.dp)
                .size(18.dp)
                .graphicsLayer { rotationZ = rotation }
        )
    }
}

@Composable
private fun DrawerItem(
    item: DrawerItemData,
    accent: Color,
    selected: Boolean,
    onItemClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) accent.copy(alpha = 0.10f) else Color.Transparent
            )
            .clickable { onItemClick(item.route) }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.widthIn(max = 230.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) accent.copy(alpha = 0.18f) else AppSurfaceAlt.copy(alpha = 0.72f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = if (selected) accent else AppTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = item.text,
                color = AppTextPrimary,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
            )
        }

        if (selected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
        } else {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AppTextSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
