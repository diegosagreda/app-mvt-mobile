package com.example.mvt.subscription.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.plans.model.SubscriptionScreenState
import com.example.mvt.plans.model.SubscriptionStatus
import com.example.mvt.plans.model.SubscriptionSummary
import com.example.mvt.plans.model.formatPlanDate
import com.example.mvt.subscription.viewmodel.SubscriptionViewModel
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppPrimarySoft
import com.example.mvt.ui.theme.AppSuccess
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue

private val SubscriptionWarning = Color(0xFFFFB84D)
private val SubscriptionDanger = Color(0xFFFF6572)

@Composable
fun SubscriptionScreen(
    athleteId: String,
    viewModel: SubscriptionViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(athleteId) { viewModel.load(athleteId) }

    Scaffold(containerColor = AppBackground, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        when (val current = state) {
            SubscriptionScreenState.Loading -> SubscriptionLoading(onBack, Modifier.padding(padding))
            SubscriptionScreenState.Empty -> SubscriptionEmpty(
                onBack,
                { viewModel.load(athleteId) },
                Modifier.padding(padding)
            )
            is SubscriptionScreenState.Error -> SubscriptionError(
                current.message,
                onBack,
                { viewModel.load(athleteId) },
                Modifier.padding(padding)
            )
            is SubscriptionScreenState.Ready -> SubscriptionContent(
                current.summary,
                onBack,
                { viewModel.load(athleteId) },
                Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun SubscriptionContent(
    summary: SubscriptionSummary,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SubscriptionHeader(onBack, onRefresh)
        Text(
            "Consulta el estado y la vigencia de tu plan actual.",
            color = AppTextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        SubscriptionSummaryCard(summary)
    }
}

@Composable
private fun SubscriptionSummaryCard(summary: SubscriptionSummary, modifier: Modifier = Modifier) {
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppBorder),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(AppPrimarySoft),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.CreditCard, null, tint = PrimaryBlue, modifier = Modifier.size(21.dp)) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Mi suscripción", color = AppTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(summary.planName.ifBlank { "Sin plan asignado" }, color = AppTextSecondary, fontSize = 12.sp)
                }
                StatusBadge(summary.status)
            }
            HorizontalDivider(color = AppBorder)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryMetric(
                    summary.remainingDays?.let { "$it días" } ?: "No aplica",
                    "restantes",
                    Icons.Default.Schedule,
                    Modifier.weight(1f)
                )
                SummaryMetric(
                    formatPlanDate(summary.registrationDateMillis),
                    "inicio",
                    Icons.Default.CalendarMonth,
                    Modifier.weight(1f)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryMetric(
                    summary.cutoffDateMillis?.let(::formatPlanDate) ?: "No aplica",
                    "corte",
                    Icons.Default.CalendarMonth,
                    Modifier.weight(1f)
                )
                SummaryMetric(
                    if (summary.registrationDateMillis == null) "No aplica" else "30 días",
                    "duración",
                    Icons.Default.Schedule,
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(value: String, label: String, icon: ImageVector, modifier: Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(13.dp)).background(AppSurfaceAlt).padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(17.dp))
        Column {
            Text(value, color = AppTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, color = AppTextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun StatusBadge(status: SubscriptionStatus) {
    val (label, color) = when (status) {
        SubscriptionStatus.ACTIVE -> "Activo" to AppSuccess
        SubscriptionStatus.EXPIRES_SOON -> "Por vencer" to SubscriptionWarning
        SubscriptionStatus.EXPIRED -> "Vencido" to SubscriptionDanger
        SubscriptionStatus.WITHOUT_DATE -> "Sin fecha" to AppTextSecondary
    }
    Surface(color = color.copy(alpha = 0.14f), shape = RoundedCornerShape(50), border = BorderStroke(1.dp, color.copy(alpha = 0.5f))) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
    }
}

@Composable
private fun SubscriptionHeader(onBack: () -> Unit, onRefresh: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = AppTextPrimary) }
        Column(Modifier.weight(1f)) {
            Text("Suscripción", color = AppTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Estado y vigencia de tu plan", color = AppTextSecondary, fontSize = 11.sp)
        }
        onRefresh?.let { IconButton(onClick = it) { Icon(Icons.Default.Refresh, "Recargar suscripción", tint = PrimaryBlue) } }
    }
}

@Composable
private fun SubscriptionLoading(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SubscriptionHeader(onBack)
        Card(
            Modifier.fillMaxWidth().height(245.dp),
            colors = CardDefaults.cardColors(containerColor = AppSurface),
            border = BorderStroke(1.dp, AppBorder),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(5) { index ->
                    Box(Modifier.fillMaxWidth(if (index == 0) 0.55f else 0.88f).height(if (index == 0) 22.dp else 14.dp).clip(RoundedCornerShape(8.dp)).background(AppSurfaceAlt))
                }
            }
        }
    }
}

@Composable
private fun SubscriptionEmpty(onBack: () -> Unit, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(8.dp)) {
        SubscriptionHeader(onBack, onRetry)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("No encontramos una suscripción asociada a tu cuenta.", color = AppTextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SubscriptionError(message: String, onBack: () -> Unit, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(8.dp)) {
        SubscriptionHeader(onBack)
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.ErrorOutline, null, tint = SubscriptionDanger, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text(message, color = AppTextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) { Text("Reintentar") }
        }
    }
}
