package com.example.mvt.ui.components.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.R
import com.example.mvt.ui.screens.UnderConstructionDestination
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue

private fun pendingRoute(feature: String): String = UnderConstructionDestination.routeFor(feature)

@Composable
fun DrawerContent(onItemClick: (String) -> Unit) {
    var datosPersonalesExpanded by remember { mutableStateOf(false) }
    var planesExpanded by remember { mutableStateOf(false) }
    var entrenamientoExpanded by remember { mutableStateOf(false) }
    var configuracionExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(AppBackground, AppSurface)
                )
            )
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        // Logo y encabezado
        Image(
            painter = painterResource(id = R.drawable.mvt),
            contentDescription = "Logo MVT",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(150.dp)
                .height(66.dp)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Menú Principal",
            fontWeight = FontWeight.SemiBold,
            color = AppTextSecondary,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // === SECCIONES ===
        DrawerSection(
            title = "Datos Personales",
            expanded = datosPersonalesExpanded,
            onToggle = { datosPersonalesExpanded = !datosPersonalesExpanded },
            items = listOf(
                DrawerItemData("Perfil", Icons.Default.Person, "profile"),
                DrawerItemData("Morfología", Icons.Default.Accessibility, "morphology"),
                DrawerItemData("Capacidad Física", Icons.Default.FitnessCenter, "fitness"),
                DrawerItemData("Rendimiento", Icons.Default.Timer, pendingRoute("performance")),
                DrawerItemData("Deportivo", Icons.Default.DirectionsBike, pendingRoute("sports")),
                DrawerItemData("Salud", Icons.Default.FavoriteBorder, pendingRoute("health")),
                DrawerItemData("Objetivos", Icons.Default.BarChart, pendingRoute("goals"))
            ),
            onItemClick = onItemClick
        )

        DrawerSection(
            title = "Planes",
            expanded = planesExpanded,
            onToggle = { planesExpanded = !planesExpanded },
            items = listOf(
                DrawerItemData("Planes", Icons.Default.Map, pendingRoute("plans")),
                DrawerItemData("Facturación", Icons.Default.ReceiptLong, pendingRoute("billing"))
            ),
            onItemClick = onItemClick
        )

        DrawerSection(
            title = "Entrenamiento",
            expanded = entrenamientoExpanded,
            onToggle = { entrenamientoExpanded = !entrenamientoExpanded },
            items = listOf(
                DrawerItemData("Tu Entrenador", Icons.Default.PersonPin, pendingRoute("trainer")),
                DrawerItemData("Entrenadores", Icons.Default.Groups, pendingRoute("coaches")),
                DrawerItemData("Rutinas", Icons.Default.CalendarMonth, "routines"),
                DrawerItemData("Explorar", Icons.Default.Search, pendingRoute("explore")),
                DrawerItemData("Sugerencias", Icons.Default.Lightbulb, pendingRoute("suggestions"))
            ),
            onItemClick = onItemClick
        )

        DrawerSection(
            title = "Configuración",
            expanded = configuracionExpanded,
            onToggle = { configuracionExpanded = !configuracionExpanded },
            items = listOf(
                DrawerItemData("Conexión", Icons.Default.Link, "connection"),
                DrawerItemData("Ayuda", Icons.Default.HelpOutline, pendingRoute("help")),
                DrawerItemData("Acerca de", Icons.Default.Info, pendingRoute("about"))
            ),
            onItemClick = onItemClick
        )
    }
}

@Composable
fun DrawerSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    items: List<DrawerItemData>,
    onItemClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceAlt),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            SectionHeader(title = title, expanded = expanded, onClick = onToggle)
            if (expanded) {
                Spacer(modifier = Modifier.height(6.dp))
                items.forEach { item ->
                    DrawerItem(
                        text = item.text,
                        icon = item.icon,
                        route = item.route,
                        onItemClick = onItemClick
                    )
                }
            }
        }
    }
}

data class DrawerItemData(
    val text: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun SectionHeader(title: String, expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryBlue,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = PrimaryBlue
        )
    }
}

@Composable
fun DrawerItem(text: String, icon: ImageVector, route: String, onItemClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onItemClick(route) }
            .background(AppSurface)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = text,
            color = AppTextPrimary,
            fontSize = 15.sp
        )
    }
}
