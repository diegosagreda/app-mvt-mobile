package com.example.mvt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BuildCircle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.R
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppPrimarySoft
import com.example.mvt.ui.theme.AppSuccess
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue

object UnderConstructionDestination {
    const val baseRoute = "under_construction"
    const val featureArg = "feature"
    const val routePattern = "$baseRoute/{$featureArg}"

    fun routeFor(feature: String): String = "$baseRoute/$feature"
}

private data class UnderConstructionFeature(
    val id: String,
    val title: String,
    val section: String,
    val icon: ImageVector,
    val summary: String
)

private val availableMenuOptions = listOf(
    "Perfil",
    "Morfologia",
    "Capacidad fisica",
    "Rutinas",
    "Conexion"
)

private val defaultFeature = UnderConstructionFeature(
    id = "general",
    title = "Nueva funcionalidad",
    section = "Experiencia MVT",
    icon = Icons.Default.BuildCircle,
    summary = "Esta opcion aun no se encuentra disponible. Estamos trabajando en su implementacion para integrarla de forma estable dentro de la plataforma."
)

private val underConstructionFeatures = listOf(
    UnderConstructionFeature(
        id = "performance",
        title = "Rendimiento",
        section = "Datos Personales",
        icon = Icons.Default.Timer,
        summary = "Aqui podras consultar indicadores y evolucion del rendimiento del atleta. Esta funcionalidad sigue en construccion."
    ),
    UnderConstructionFeature(
        id = "sports",
        title = "Deportivo",
        section = "Datos Personales",
        icon = Icons.Default.DirectionsBike,
        summary = "Este modulo reunira informacion deportiva complementaria para el seguimiento del proceso. Aun se encuentra en desarrollo."
    ),
    UnderConstructionFeature(
        id = "health",
        title = "Salud",
        section = "Datos Personales",
        icon = Icons.Default.FavoriteBorder,
        summary = "Esta vista mostrara informacion relevante para el control y acompanamiento del estado de salud. Aun esta en construccion."
    ),
    UnderConstructionFeature(
        id = "goals",
        title = "Objetivos",
        section = "Datos Personales",
        icon = Icons.Default.BarChart,
        summary = "Esta seccion permitira organizar y hacer seguimiento a los objetivos del atleta. Por ahora continua en construccion."
    ),
    UnderConstructionFeature(
        id = "plans",
        title = "Planes",
        section = "Planes",
        icon = Icons.Default.Map,
        summary = "Esta opcion centralizara la informacion de planes y beneficios asociados al servicio. Aun no esta disponible."
    ),
    UnderConstructionFeature(
        id = "billing",
        title = "Facturacion",
        section = "Planes",
        icon = Icons.Default.ReceiptLong,
        summary = "Aqui se presentara el detalle administrativo y de facturacion del usuario. La funcionalidad esta en construccion."
    ),
    UnderConstructionFeature(
        id = "trainer",
        title = "Tu Entrenador",
        section = "Entrenamiento",
        icon = Icons.Default.PersonPin,
        summary = "Esta vista ampliara la informacion sobre el entrenador asignado y su seguimiento. Todavia se encuentra en desarrollo."
    ),
    UnderConstructionFeature(
        id = "coaches",
        title = "Entrenadores",
        section = "Entrenamiento",
        icon = Icons.Default.Groups,
        summary = "Esta seccion permitira consultar el directorio de entrenadores. Por el momento sigue en construccion."
    ),
    UnderConstructionFeature(
        id = "explore",
        title = "Explorar",
        section = "Entrenamiento",
        icon = Icons.Default.Search,
        summary = "Aqui se mostraran recursos y accesos complementarios para el atleta. Esta experiencia aun no ha sido implementada."
    ),
    UnderConstructionFeature(
        id = "suggestions",
        title = "Sugerencias",
        section = "Entrenamiento",
        icon = Icons.Default.Lightbulb,
        summary = "Esta opcion mostrara recomendaciones y sugerencias dentro del proceso de entrenamiento. Aun esta en construccion."
    ),
    UnderConstructionFeature(
        id = "help",
        title = "Ayuda",
        section = "Configuracion",
        icon = Icons.Default.HelpOutline,
        summary = "El centro de ayuda todavia no esta disponible. Estamos preparando esta funcionalidad para una siguiente etapa."
    ),
    UnderConstructionFeature(
        id = "about",
        title = "Acerca de",
        section = "Configuracion",
        icon = Icons.Default.Info,
        summary = "Esta seccion mostrara informacion institucional y de la aplicacion. Por ahora continua en construccion."
    )
).associateBy { it.id }

private fun resolveFeature(featureId: String?): UnderConstructionFeature {
    return underConstructionFeatures[featureId] ?: defaultFeature
}

@Composable
fun UnderConstructionScreen(
    featureId: String?,
    onGoToRoutines: () -> Unit
) {
    val feature = resolveFeature(featureId)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AppBackground,
                        AppSurface,
                        AppBackground
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderCard(feature = feature)
            ConstructionCard(summary = feature.summary)
            AvailableMenuCard(items = availableMenuOptions)

            Button(
                onClick = onGoToRoutines,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ir a Rutinas")
            }
        }
    }
}

@Composable
private fun HeaderCard(feature: UnderConstructionFeature) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceAlt),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = AppPrimarySoft
                ) {
                    Text(
                        text = feature.section,
                        color = AppTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.mvt),
                    contentDescription = "MVT",
                    modifier = Modifier.width(84.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppTextPrimary
                )
                Text(
                    text = "Funcionalidad en construccion",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ConstructionCard(summary: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = AppSurface
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(AppPrimarySoft, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BuildCircle,
                        contentDescription = null,
                        tint = PrimaryBlue
                    )
                }
                Text(
                    text = "Estamos trabajando en esta opcion",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTextPrimary
                )
            }

            Text(
                text = summary,
                style = MaterialTheme.typography.bodyLarge,
                color = AppTextSecondary,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun AvailableMenuCard(items: List<String>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = AppSurfaceAlt
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Opciones disponibles del menu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppTextPrimary
            )

            items.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { item ->
                        AvailabilityItem(
                            title = item,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun AvailabilityItem(
    title: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppBackground.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = AppSuccess,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                color = AppTextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
