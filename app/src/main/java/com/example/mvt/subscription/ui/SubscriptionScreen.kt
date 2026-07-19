package com.example.mvt.subscription.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mvt.subscription.model.Cobro
import com.example.mvt.subscription.model.SubscriptionStatus
import com.example.mvt.subscription.viewmodel.SubscriptionUiState
import com.example.mvt.subscription.viewmodel.SubscriptionViewModel
import com.example.mvt.ui.theme.*

@Composable
fun SubscriptionScreen(
    navController: NavController,
    viewModel: SubscriptionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val status  by viewModel.status.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadSubscription() }

    when (uiState) {
        is SubscriptionUiState.Loading -> {
            Box(
                modifier         = Modifier.fillMaxSize().background(AppBackground),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue, strokeWidth = 3.dp)
            }
        }
        is SubscriptionUiState.Error -> {
            Box(
                modifier         = Modifier.fillMaxSize().background(AppBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = AppError,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text      = "Error al cargar la suscripción",
                        color     = AppTextSecondary,
                        fontSize  = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { viewModel.loadSubscription() },
                        colors  = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape   = RoundedCornerShape(12.dp)
                    ) { Text("Reintentar") }
                }
            }
        }
        else -> {
            SubscriptionContent(
                navController = navController,
                status        = status
            )
        }
    }
}

@Composable
private fun SubscriptionContent(
    navController: NavController,
    status: SubscriptionStatus
) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 24.dp)
        ) {
            // ==========================================
            // HEADER
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(AppSurface, AppBackground)
                        )
                    )
                    .padding(start = 4.dp, end = 12.dp, top = 16.dp, bottom = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            navController.navigate("routines") {
                                popUpTo("routines") { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    ) {
                        Surface(
                            shape  = CircleShape,
                            color  = AppSurfaceAlt,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
                        ) {
                            Icon(
                                imageVector        = Icons.Default.ArrowBack,
                                contentDescription = "Volver",
                                tint               = AppTextPrimary,
                                modifier           = Modifier.padding(8.dp).size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text       = "Suscripción",
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color      = AppTextPrimary
                        )
                        Text(
                            text     = "Estado de tu plan activo.",
                            fontSize = 13.sp,
                            color    = AppTextSecondary
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 8.dp)) {

                // ==========================================
                // TARJETAS RESUMEN (Imagen 1)
                // ==========================================
                SummaryItemCard(
                    icon = Icons.Default.WorkspacePremium,
                    iconColor = Color(0xFF00A3FF),
                    label = "Plan actual",
                    value = status.userPlan.nombre.ifBlank { "—" }
                )

                Spacer(modifier = Modifier.height(12.dp))

                val vigenciaTexto = if (status.esPlanGratuito) "Sin vencimiento" else "${status.fechaInicio} a ${status.fechaCorte}"
                val vigenciaSub   = if (status.esPlanGratuito) "Plan gratuito sin fecha de corte" else "Ciclo de ${status.diasTotales} días"
                SummaryItemCard(
                    icon = Icons.Default.CalendarMonth,
                    iconColor = Color(0xFFFF8A65),
                    label = "Vigencia",
                    value = vigenciaTexto,
                    subValue = vigenciaSub
                )

                Spacer(modifier = Modifier.height(12.dp))

                val diasTexto = if (status.diasRestantes == -1) "Sin vencimiento" else "${status.diasRestantes}"
                val diasSub   = if (status.esPlanGratuito) "No consume ciclo de vigencia" else "${status.diasTotales - maxOf(0, status.diasRestantes)} días usados"
                SummaryItemCard(
                    icon = Icons.Default.Timer,
                    iconColor = Color(0xFF81C784),
                    label = "Días disponibles",
                    value = diasTexto,
                    subValue = diasSub
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ==========================================
                // VIGENCIA DETALLADA (Imagen 2)
                // ==========================================
                PlanVigenciaCard(status = status)

            }
        }
    }
}

@Composable
private fun SummaryItemCard(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    subValue: String? = null
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AppSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 94.dp) // Altura mínima para que las 3 sean iguales
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(iconColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(text = label, fontSize = 13.sp, color = AppTextSecondary)
                Text(
                    text = value,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTextPrimary
                )
                if (subValue != null) {
                    Text(text = subValue, fontSize = 12.sp, color = AppTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun PlanVigenciaCard(status: SubscriptionStatus) {
    val progressAnim by animateFloatAsState(
        targetValue   = status.progresoPlan,
        animationSpec = tween(durationMillis = 800),
        label         = "progress"
    )

    Surface(
        shape    = RoundedCornerShape(18.dp),
        color    = AppSurface,
        border   = androidx.compose.foundation.BorderStroke(1.dp, AppBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text       = "Vigencia del plan",
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                color      = AppTextPrimary
            )
            Text(
                text     = "Resumen operativo de la suscripción activa del deportista.",
                fontSize = 13.sp,
                color    = AppTextSecondary
            )

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                shape = CircleShape,
                color = if (status.esPlanGratuito) AppSurfaceMuted else AppSuccess.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (status.esPlanGratuito) Color(0xFF00A3FF).copy(alpha = 0.4f) else AppSuccess.copy(alpha = 0.4f)
                )
            ) {
                Text(
                    text     = if (status.esPlanGratuito) "Plan gratuito" else "Activa",
                    fontSize = 11.sp,
                    color    = if (status.esPlanGratuito) Color(0xFF00A3FF) else AppSuccess,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            VigenciaDetailRow(label = "Plan",            value = status.userPlan.nombre.ifBlank { "—" })
            HorizontalDivider(color = AppBorder.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 10.dp))
            VigenciaDetailRow(label = "Inicio",           value = status.fechaInicio)
            HorizontalDivider(color = AppBorder.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 10.dp))
            VigenciaDetailRow(label = "Fecha de corte",   value = if (status.esPlanGratuito) "No aplica" else status.fechaCorte)
            HorizontalDivider(color = AppBorder.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 10.dp))
            VigenciaDetailRow(
                label = "Disponibilidad",
                value = if (status.diasRestantes == -1) "Ilimitada" else "${status.diasRestantes} días restantes"
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text     = if (status.esPlanGratuito) "No consume ciclo de vigencia" else "${status.diasTotales - maxOf(0, status.diasRestantes)} días usados",
                    fontSize = 12.sp,
                    color    = AppTextSecondary
                )
                Text(
                    text     = if (status.esPlanGratuito) "Sin vencimiento" else "${status.diasTotales} días",
                    fontSize = 12.sp,
                    color    = AppTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(AppSurfaceAlt)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressAnim)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(PrimaryBlue, Color(0xFF00A3FF))
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun VigenciaDetailRow(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 13.sp, color = AppTextSecondary)
        Text(
            text       = value,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            color      = AppTextPrimary
        )
    }
}

@Composable
private fun CobroItem(cobro: Cobro) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .background(PrimaryBlue.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Payment,
                    contentDescription = null,
                    tint     = PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = cobro.plan, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppTextPrimary)
                Text(text = cobro.fecha, fontSize = 12.sp, color = AppTextSecondary)
            }
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = AppSuccess.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppSuccess.copy(alpha = 0.3f))
        ) {
            Text(
                text     = "Pagado",
                fontSize = 11.sp,
                color    = AppSuccess,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}
