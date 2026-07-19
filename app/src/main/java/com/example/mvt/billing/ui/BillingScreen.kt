package com.example.mvt.billing.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.mvt.billing.model.BillingPago
import com.example.mvt.billing.model.BillingStatus
import com.example.mvt.billing.viewmodel.BillingUiState
import com.example.mvt.billing.viewmodel.BillingViewModel
import com.example.mvt.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

// Formatea moneda COP
private fun formatCOP(monto: Int): String {
    val nf = NumberFormat.getNumberInstance(Locale("es", "CO"))
    return "$ ${nf.format(monto)}"
}

private fun formatDateTime(dateTime: String): String {
    if (dateTime.isBlank()) return "—"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val date = parser.parse(dateTime) ?: return dateTime
        val formatter = SimpleDateFormat("d 'de' MMM 'de' yyyy, h:mm a", Locale("es", "CO"))
        formatter.format(date)
            .replace("AM", "a. m.")
            .replace("PM", "p. m.")
            .replace("a. m.", "a. m.") // Fix para espacios de no ruptura en algunos locales
            .replace("p. m.", "p. m.")
    } catch (e: Exception) {
        dateTime
    }
}

@Composable
fun BillingScreen(
    navController: NavController,
    viewModel: BillingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val status  by viewModel.status.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadBilling() }

    when (uiState) {
        is BillingUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize().background(AppBackground),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue, strokeWidth = 3.dp)
            }
        }
        is BillingUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize().background(AppBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = AppError, modifier = Modifier.size(48.dp))
                    Text("Error al cargar facturación", color = AppTextSecondary, fontSize = 14.sp)
                    Button(
                        onClick = { viewModel.loadBilling() },
                        colors  = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape   = RoundedCornerShape(12.dp)
                    ) { Text("Reintentar") }
                }
            }
        }
        else -> BillingContent(navController = navController, status = status)
    }
}

@Composable
private fun BillingContent(
    navController: NavController,
    status: BillingStatus
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
                    .background(Brush.verticalGradient(colors = listOf(AppSurface, AppBackground)))
                    .padding(start = 4.dp, end = 12.dp, top = 16.dp, bottom = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        navController.navigate("routines") {
                            popUpTo("routines") { inclusive = false }
                            launchSingleTop = true
                        }
                    }) {
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
                        Text("Facturación", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppTextPrimary)
                        Text("Controla tus pagos y vigencia del plan.", fontSize = 13.sp, color = AppTextSecondary)
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 8.dp)) {

                // ==========================================
                // HERO CARD — Centro de facturación
                // ==========================================
                BillingHeroCard(status = status)

                Spacer(modifier = Modifier.height(12.dp))

                // ==========================================
                // TARJETAS RESUMEN (Plan activo, Fecha corte,
                // Total pagado, Descuentos)
                // ==========================================
                BillingSummaryCard(
                    icon      = Icons.Default.WorkspacePremium,
                    iconColor = Color(0xFF00A3FF),
                    label     = "Plan activo",
                    value     = status.nombrePlan.ifBlank { "—" }
                )

                Spacer(modifier = Modifier.height(10.dp))

                val fechaCorteLabel = if (status.esPlanGratuito) "No aplica"
                else status.fechaCorte
                val fechaSubtexto = if (status.esPlanGratuito) "Sin vencimiento"
                else "${status.diasRestantes} días disponibles"
                BillingSummaryCard(
                    icon      = Icons.Default.CalendarMonth,
                    iconColor = Color(0xFFFF8A65),
                    label     = "Fecha de corte",
                    value     = fechaCorteLabel,
                    subValue  = fechaSubtexto
                )

                Spacer(modifier = Modifier.height(10.dp))

                BillingSummaryCard(
                    icon      = Icons.Default.CreditCard,
                    iconColor = Color(0xFF4DB6AC),
                    label     = "Total pagado",
                    value     = formatCOP(status.totalPagado),
                    subValue  = "${status.totalPagos} pagos registrados"
                )

                Spacer(modifier = Modifier.height(10.dp))

                BillingSummaryCard(
                    icon      = Icons.Default.Discount,
                    iconColor = Color(0xFFAB47BC),
                    label     = "Descuentos",
                    value     = formatCOP(status.totalDescuentos),
                    subValue  = "${status.pagosConDescuento} pagos con código"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ==========================================
                // SUSCRIPCIÓN ACTUAL
                // ==========================================
                BillingVigenciaCard(status = status)

                Spacer(modifier = Modifier.height(16.dp))

                // ==========================================
                // ÚLTIMO PAGO
                // ==========================================
                BillingUltimoPagoCard(pago = status.ultimoPago)

                Spacer(modifier = Modifier.height(16.dp))

                // ==========================================
                // HISTORIAL DE PAGOS
                // ==========================================
                BillingHistorialCard(pagos = status.historialPagos)
            }
        }
    }
}

// ==========================================
// HERO CARD
// ==========================================
@Composable
private fun BillingHeroCard(status: BillingStatus) {
    Surface(
        shape    = RoundedCornerShape(18.dp),
        color    = AppSurface,
        border   = androidx.compose.foundation.BorderStroke(1.dp, AppBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text     = "CENTRO DE FACTURACIÓN",
                fontSize = 11.sp,
                color    = PrimaryBlue,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text       = "Controla tus pagos y la vigencia de tu plan",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = AppTextPrimary,
                lineHeight = 26.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text     = "Consulta tu plan activo, pagos realizados, códigos de descuento aplicados y fecha de corte dentro de My Virtual Trainer.",
                fontSize = 13.sp,
                color    = AppTextSecondary,
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Badge estado
            Surface(
                shape = CircleShape,
                color = if (status.esPlanGratuito) AppSurfaceMuted else AppSuccess.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (status.esPlanGratuito) Color(0xFF00A3FF).copy(alpha = 0.4f) else AppSuccess.copy(alpha = 0.4f)
                )
            ) {
                Text(
                    text       = if (status.esPlanGratuito) "Plan gratuito" else if (status.diasRestantes != 0) "Activa" else "Vencida",
                    fontSize   = 11.sp,
                    color      = if (status.esPlanGratuito) Color(0xFF00A3FF) else if (status.diasRestantes != 0) AppSuccess else AppError,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Íconos de proceso: plan → pago → calendario (Estilo Web)
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                BillingStepIcon(icon = Icons.Default.CardMembership, tint = PrimaryBlue)
                HorizontalDivider(
                    modifier  = Modifier.weight(1f).padding(horizontal = 8.dp),
                    color     = PrimaryBlue.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
                BillingStepIcon(icon = Icons.Default.CreditCard, tint = Color(0xFF00A3FF))
                HorizontalDivider(
                    modifier  = Modifier.weight(1f).padding(horizontal = 8.dp),
                    color     = PrimaryBlue.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
                BillingStepIcon(icon = Icons.Default.CalendarMonth, tint = Color(0xFFFF8A65))
            }
        }
    }
}

@Composable
private fun BillingStepIcon(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(tint.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = tint,
            modifier           = Modifier.size(22.dp)
        )
    }
}

// ==========================================
// TARJETA RESUMEN INDIVIDUAL
// ==========================================
@Composable
private fun BillingSummaryCard(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    subValue: String? = null
) {
    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = AppSurface,
        border   = androidx.compose.foundation.BorderStroke(1.dp, AppBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = label, fontSize = 12.sp, color = AppTextSecondary)
                Text(text = value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppTextPrimary)
                if (subValue != null) {
                    Text(text = subValue, fontSize = 12.sp, color = AppTextSecondary)
                }
            }
        }
    }
}

// ==========================================
// SUSCRIPCIÓN ACTUAL CON BARRA
// ==========================================
@Composable
private fun BillingVigenciaCard(status: BillingStatus) {
    var isExpanded by remember { mutableStateOf(true) }
    val progressAnim by animateFloatAsState(
        targetValue   = status.progreso,
        animationSpec = tween(800),
        label         = "progress"
    )

    Surface(
        shape    = RoundedCornerShape(18.dp),
        color    = AppSurface,
        border   = androidx.compose.foundation.BorderStroke(1.dp, AppBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Suscripción actual", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppTextPrimary)
                    Text(
                        text     = if (status.esPlanGratuito) "Plan gratuito sin ciclo de vigencia."
                        else "Vigencia de ${status.diasTotales} días.",
                        fontSize = 12.sp,
                        color    = AppTextSecondary
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = AppIconMuted,
                    modifier = Modifier.size(24.dp)
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isExpanded,
                enter = androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(18.dp))
                    // Filas de detalle
                    BillingDetailRow(label = "Inicio", value = status.fechaInicio)
                    HorizontalDivider(color = AppBorder.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 10.dp))
                    BillingDetailRow(
                        label = "Corte",
                        value = if (status.esPlanGratuito) "No aplica" else status.fechaCorte
                    )
                    HorizontalDivider(color = AppBorder.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 10.dp))
                    
                    val medioPagoStr = if (status.ultimoPago != null) {
                        if (status.ultimoPago.bankName.isNotBlank()) status.ultimoPago.bankName
                        else "${status.ultimoPago.medioPago} - ${status.ultimoPago.franquicia}"
                    } else "Sin pagos registrados"

                    BillingDetailRow(
                        label = "Último método registrado",
                        value = medioPagoStr,
                        valueColor = AppTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text     = if (status.esPlanGratuito) "No consume ciclo de vigencia" else "${status.diasUsados} días usados",
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
                            Brush.horizontalGradient(listOf(PrimaryBlue, Color(0xFF00A3FF)))
                        )
                )
            }
        }
    }
}

@Composable
private fun BillingDetailRow(
    label: String,
    value: String,
    valueColor: Color = AppTextPrimary
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = AppTextSecondary, modifier = Modifier.weight(1f))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

// ==========================================
// ÚLTIMO PAGO
// ==========================================
@Composable
private fun BillingUltimoPagoCard(pago: BillingPago?) {
    var isExpanded by remember { mutableStateOf(true) }

    Surface(
        shape    = RoundedCornerShape(18.dp),
        color    = AppSurface,
        border   = androidx.compose.foundation.BorderStroke(1.dp, AppBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = pago != null) { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Último pago", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppTextPrimary)
                    Text("Detalle del movimiento más reciente.", fontSize = 12.sp, color = AppTextSecondary)
                }
                if (pago != null) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = AppIconMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (pago == null) {
                // Estado vacío
                Column(
                    modifier            = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier         = Modifier.size(52.dp).background(AppSurfaceAlt, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ReceiptLong, null, tint = AppIconMuted, modifier = Modifier.size(26.dp))
                    }
                    Text("Sin pagos registrados", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppTextPrimary, textAlign = TextAlign.Center)
                    Text("Cuando realices un pago, aparecerá aquí.", fontSize = 13.sp, color = AppTextSecondary, textAlign = TextAlign.Center)
                }
            } else {
                // Bloque destacado de monto (Siempre visible)
                Surface(
                    shape    = RoundedCornerShape(12.dp),
                    color    = AppSurfaceAlt,
                    border   = androidx.compose.foundation.BorderStroke(1.dp, AppBorder.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Text(
                            text = formatCOP(pago.monto),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTextPrimary
                        )
                        Text(
                            text = pago.plan,
                            fontSize = 13.sp,
                            color = AppTextSecondary
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = isExpanded,
                    enter = androidx.compose.animation.expandVertically(),
                    exit = androidx.compose.animation.shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(18.dp))

                        // Filas de detalle (Estilo web)
                        BillingDetailRowWeb("Fecha",  formatDateTime(pago.fechaTransaccion))
                        HorizontalDivider(color = AppBorder.copy(alpha = 0.4f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                        
                        BillingDetailRowWeb("Recibo", pago.recibo)
                        HorizontalDivider(color = AppBorder.copy(alpha = 0.4f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                        
                        BillingDetailRowWeb("Estado", pago.estado, isBoldValue = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun BillingDetailRowWeb(
    label: String,
    value: String = "",
    isBoldValue: Boolean = false,
    customValue: (@Composable () -> Unit)? = null
) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = AppTextSecondary
        )
        if (customValue != null) {
            customValue()
        } else {
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = if (isBoldValue) FontWeight.Bold else FontWeight.Medium,
                color = AppTextPrimary,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun PagoDetailRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = AppTextSecondary, modifier = Modifier.weight(1f))
        Text(text = value, fontSize = 12.sp, color = AppTextPrimary, fontWeight = FontWeight.Medium)
    }
}

// ==========================================
// HISTORIAL DE PAGOS
// ==========================================
@Composable
private fun BillingHistorialCard(pagos: List<BillingPago>) {
    Surface(
        shape    = RoundedCornerShape(18.dp),
        color    = AppSurface,
        border   = androidx.compose.foundation.BorderStroke(1.dp, AppBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Historial de pagos", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppTextPrimary)
            Text("Movimientos registrados desde ePayco en la plataforma.", fontSize = 12.sp, color = AppTextSecondary)

            Spacer(modifier = Modifier.height(16.dp))

            if (pagos.isEmpty()) {
                Column(
                    modifier            = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier         = Modifier.size(52.dp).background(AppSurfaceAlt, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.History, null, tint = AppIconMuted, modifier = Modifier.size(26.dp))
                    }
                    Text("Aún no tienes pagos", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppTextPrimary, textAlign = TextAlign.Center)
                    Text("Tu historial se actualizará automáticamente después de cada pago confirmado.", fontSize = 13.sp, color = AppTextSecondary, textAlign = TextAlign.Center)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    pagos.forEach { pago ->
                        HistorialPagoItem(pago = pago)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistorialPagoItem(pago: BillingPago) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape  = RoundedCornerShape(14.dp),
        color  = if (isExpanded) AppSurfaceAlt else AppSurfaceMuted.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(PrimaryBlue.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.Payment,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = pago.plan,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTextPrimary
                        )
                        Text(
                            text = formatCOP(pago.monto),
                            fontSize = 14.sp,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Surface(
                    shape  = RoundedCornerShape(8.dp),
                    color  = AppSuccess.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppSuccess.copy(alpha = 0.3f))
                ) {
                    Text(
                        text     = "Aceptada",
                        fontSize = 11.sp,
                        color    = AppSuccess,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            if (!isExpanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDateTime(pago.fechaTransaccion),
                        fontSize = 12.sp,
                        color = AppTextSecondary,
                        modifier = Modifier.padding(start = 54.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = AppTextPrimary,
                        modifier = Modifier.size(40.dp).padding(end = 18.dp)
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isExpanded,
                enter = androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .background(AppSurfaceAlt, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    BillingDetailRowWeb("Fecha", formatDateTime(pago.fechaTransaccion))
                    HorizontalDivider(color = AppBorder.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                    
                    BillingDetailRowWeb("Recibo", pago.recibo)
                    HorizontalDivider(color = AppBorder.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                    
                    val medioStr = if (pago.bankName.isNotBlank()) pago.bankName else "${pago.medioPago} ${pago.franquicia}"
                    BillingDetailRowWeb("Medio", medioStr)
                    HorizontalDivider(color = AppBorder.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                    
                    BillingDetailRowWeb(
                        label = "Código",
                        value = "Sin código",
                        customValue = if (pago.codigoDescuento.isNotBlank()) {
                            {
                                Surface(
                                    shape  = CircleShape,
                                    color  = Color(0xFFAB47BC).copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFAB47BC).copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text     = pago.codigoDescuento.uppercase(),
                                        fontSize = 11.sp,
                                        color    = Color(0xFFAB47BC),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        } else null
                    )
                    HorizontalDivider(color = AppBorder.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                    
                    BillingDetailRowWeb("Descuento", formatCOP(pago.descuento))
                }
            }
        }
    }
}
