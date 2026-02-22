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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.R
import com.example.mvt.ui.theme.PrimaryBlue

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
                    colors = listOf(Color(0xFFF7F9FB), Color(0xFFEFF3F9))
                )
            )
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        // Logo y encabezado
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo MVT",
            modifier = Modifier
                .size(90.dp)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Menú Principal",
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF555B61),
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
                DrawerItemData("Rendimiento", Icons.Default.Timer, "performance"),
                DrawerItemData("Deportivo", Icons.Default.DirectionsBike, "sports"),
                DrawerItemData("Salud", Icons.Default.FavoriteBorder, "health"),
                DrawerItemData("Objetivos", Icons.Default.BarChart, "goals")
            ),
            onItemClick = onItemClick
        )

        DrawerSection(
            title = "Planes",
            expanded = planesExpanded,
            onToggle = { planesExpanded = !planesExpanded },
            items = listOf(
                DrawerItemData("Planes", Icons.Default.Map, "plans"),
                DrawerItemData("Facturación", Icons.Default.ReceiptLong, "billing")
            ),
            onItemClick = onItemClick
        )

        DrawerSection(
            title = "Entrenamiento",
            expanded = entrenamientoExpanded,
            onToggle = { entrenamientoExpanded = !entrenamientoExpanded },
            items = listOf(
                DrawerItemData("Tu Entrenador", Icons.Default.PersonPin, "trainer"),
                DrawerItemData("Entrenadores", Icons.Default.Groups, "coaches"),
                DrawerItemData("Rutinas", Icons.Default.CalendarMonth, "routines"),
                DrawerItemData("Explorar", Icons.Default.Search, "explore"),
                DrawerItemData("Sugerencias", Icons.Default.Lightbulb, "suggestions")
            ),
            onItemClick = onItemClick
        )

        DrawerSection(
            title = "Configuración",
            expanded = configuracionExpanded,
            onToggle = { configuracionExpanded = !configuracionExpanded },
            items = listOf(
                DrawerItemData("Conexión", Icons.Default.Link, "connection"),
                DrawerItemData("Ayuda", Icons.Default.HelpOutline, "help"),
                DrawerItemData("Acerca de", Icons.Default.Info, "about")
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
            .background(Color(0xFFF5F8FB))
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
            color = Color(0xFF2B2E34),
            fontSize = 15.sp
        )
    }
}
