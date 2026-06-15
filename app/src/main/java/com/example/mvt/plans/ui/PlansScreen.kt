package com.example.mvt.plans.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.mvt.R
import com.example.mvt.plans.model.PlanCatalogState
import com.example.mvt.plans.model.PlanFeature
import com.example.mvt.plans.model.SubscriptionActionHandler
import com.example.mvt.plans.model.SubscriptionPlan
import com.example.mvt.plans.model.matchesPlanName
import com.example.mvt.plans.viewmodel.PlansViewModel
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSuccess
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue

private val PlanDanger = Color(0xFFFF6572)

private data class PlanVisualStyle(
    val accent: Color,
    val accentSoft: Color,
    val surfaceStart: Color,
    val surfaceEnd: Color,
    val imageAlignment: Alignment,
    val badge: String,
    val description: String
)

@Composable
fun PlansScreen(
    athleteId: String,
    viewModel: PlansViewModel,
    onBack: () -> Unit,
    actionHandler: SubscriptionActionHandler = SubscriptionActionHandler { }
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(athleteId) { viewModel.load(athleteId) }

    Scaffold(containerColor = AppBackground, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        when (val current = state) {
            PlanCatalogState.Loading -> PlansLoading(onBack, Modifier.padding(padding))
            PlanCatalogState.Empty -> PlansEmpty(onBack, { viewModel.load(athleteId) }, Modifier.padding(padding))
            is PlanCatalogState.Error -> PlansError(current.message, onBack, { viewModel.load(athleteId) }, Modifier.padding(padding))
            is PlanCatalogState.Ready -> PlansContent(
                plans = current.plans,
                currentPlanName = current.currentPlanName,
                onBack = onBack,
                onRefresh = { viewModel.load(athleteId) },
                actionHandler = actionHandler,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun PlansContent(
    plans: List<SubscriptionPlan>,
    currentPlanName: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    actionHandler: SubscriptionActionHandler,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PlansHeader(onBack, onRefresh, Modifier.padding(8.dp)) }
        item { PlansCarousel(plans, currentPlanName, actionHandler) }
    }
}

@Composable
private fun PlansCarousel(
    plans: List<SubscriptionPlan>,
    currentPlanName: String?,
    actionHandler: SubscriptionActionHandler
) {
    val currentPlanIndex = plans.indexOfFirst { it.matchesPlanName(currentPlanName) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentPlanIndex.coerceAtLeast(0))
    LaunchedEffect(currentPlanIndex) {
        if (currentPlanIndex >= 0) listState.scrollToItem(currentPlanIndex)
    }
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val cardHeight = (450 + (plans.maxOfOrNull { it.features.size } ?: 0) * 42).dp
    val focusedIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo.minByOrNull { item ->
                kotlin.math.abs((item.offset + item.size / 2) - viewportCenter)
            }?.index ?: 0
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cardWidth = when {
            maxWidth >= 900.dp -> (maxWidth - 40.dp) / 3
            maxWidth >= 600.dp -> (maxWidth - 28.dp) / 2
            else -> maxWidth * 0.88f
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LazyRow(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(plans, key = { _, plan -> plan.id }) { index, plan ->
                    PlanCard(
                        plan = plan,
                        width = cardWidth,
                        height = cardHeight,
                        focused = index == focusedIndex,
                        current = index == currentPlanIndex,
                        onSelect = { actionHandler.onSelectPlan(plan.id) }
                    )
                }
            }
            if (plans.size > 1) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    plans.indices.forEach { index ->
                        val selected = index == focusedIndex
                        Box(
                            Modifier.padding(horizontal = 3.dp)
                                .size(if (selected) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (selected) PrimaryBlue else AppBorder)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: SubscriptionPlan,
    width: Dp,
    height: Dp,
    focused: Boolean,
    current: Boolean,
    onSelect: () -> Unit
) {
    val style = planVisualStyle(plan)
    val scale by animateFloatAsState(
        targetValue = if (focused) 1f else 0.94f,
        animationSpec = tween(durationMillis = 240),
        label = "planCardScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (focused) 1f else 0.82f,
        animationSpec = tween(durationMillis = 240),
        label = "planCardAlpha"
    )
    Card(
        modifier = Modifier
            .width(width)
            .height(height)
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(if (current) 2.dp else 1.dp, if (current) style.accent else style.accent.copy(alpha = 0.68f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(style.surfaceStart, style.surfaceEnd, AppSurface))
            )
        ) {
            Box(
                Modifier.align(Alignment.TopEnd).size(190.dp)
                    .background(Brush.radialGradient(listOf(style.accent.copy(alpha = 0.18f), Color.Transparent)))
            )
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f).padding(top = 4.dp)) {
                        if (current) {
                            Surface(
                                color = PrimaryBlue.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, PrimaryBlue)
                            ) {
                                Text(
                                    "PLAN ACTUAL",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                        Surface(
                            color = style.accentSoft,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, style.accent.copy(alpha = 0.5f))
                        ) {
                            Text(
                                style.badge,
                                color = style.accent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(plan.name, color = AppTextPrimary, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    PlanMedal(style)
                }

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        if (plan.price <= 0) "Gratis" else plan.formattedPrice.removeSuffix(" COP"),
                        color = style.accent,
                        fontSize = if (plan.price <= 0) 31.sp else 30.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.7).sp
                    )
                    if (plan.price > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text("COP", color = AppTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 5.dp))
                    }
                }
                Text(
                    if (plan.price > 0) "por cada ciclo de 30 días" else "para comenzar tu proceso",
                    color = AppTextSecondary,
                    fontSize = 10.sp
                )

                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(style.accent.copy(alpha = 0.09f))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(style.description, color = AppTextPrimary, fontSize = 12.sp, lineHeight = 18.sp)
                }

                Spacer(Modifier.height(15.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("LO QUE INCLUYE", color = AppTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    HorizontalDivider(color = AppBorder, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(11.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    plan.features.forEach { FeatureRow(it, style.accent, style.accentSoft) }
                }
                Button(
                    onClick = onSelect,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = style.accent.copy(alpha = 0.2f),
                        disabledContentColor = style.accent
                    )
                ) {
                    Text(
                        when {
                            current -> "Plan actual"
                            plan.price > 0 -> "Disponible próximamente"
                            else -> "Plan gratuito"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanMedal(style: PlanVisualStyle) {
    Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(86.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(style.accent.copy(alpha = 0.22f), Color.Transparent)))
        )
        Image(
            painter = painterResource(R.drawable.plan_medals),
            contentDescription = null,
            modifier = Modifier.size(98.dp),
            contentScale = ContentScale.Crop,
            alignment = style.imageAlignment
        )
    }
}

@Composable
private fun FeatureRow(feature: PlanFeature, accent: Color, accentSoft: Color) {
    val color = when (feature.included) {
        true -> AppSuccess
        false -> PlanDanger
        null -> accent
    }
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.size(22.dp).clip(RoundedCornerShape(7.dp))
                .background(if (feature.included == false) PlanDanger.copy(alpha = 0.1f) else accentSoft),
            contentAlignment = Alignment.Center
        ) {
            when (feature.included) {
                true -> Icon(Icons.Default.Check, null, tint = color, modifier = Modifier.size(14.dp))
                false -> Icon(Icons.Default.Close, null, tint = color, modifier = Modifier.size(14.dp))
                null -> Box(Modifier.size(5.dp).clip(CircleShape).background(color))
            }
        }
        Text(
            if (feature.value.isNullOrBlank()) feature.label else "${feature.label}: ${feature.value}",
            color = if (feature.included == false) AppTextSecondary else AppTextPrimary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

private fun planVisualStyle(plan: SubscriptionPlan): PlanVisualStyle {
    val identity = "${plan.id} ${plan.name}".lowercase()
    return when {
        "oro" in identity || "gold" in identity -> PlanVisualStyle(
            accent = Color(0xFFFFC857),
            accentSoft = Color(0x22FFC857),
            surfaceStart = Color(0xFF3A3325),
            surfaceEnd = Color(0xFF2E3442),
            imageAlignment = Alignment.CenterEnd,
            badge = "EXPERIENCIA PREMIUM",
            description = "Máximo acompañamiento para atletas que buscan llevar su rendimiento al siguiente nivel."
        )
        "plata" in identity || "silver" in identity -> PlanVisualStyle(
            accent = Color(0xFFD7E0EA),
            accentSoft = Color(0x22D7E0EA),
            surfaceStart = Color(0xFF343D49),
            surfaceEnd = Color(0xFF2B3544),
            imageAlignment = Alignment.Center,
            badge = "MÁS EQUILIBRADO",
            description = "Seguimiento constante y herramientas completas para avanzar con estructura y confianza."
        )
        else -> PlanVisualStyle(
            accent = Color(0xFFD98B55),
            accentSoft = Color(0x22D98B55),
            surfaceStart = Color(0xFF3A2F2A),
            surfaceEnd = Color(0xFF2B3442),
            imageAlignment = Alignment.CenterStart,
            badge = "PUNTO DE PARTIDA",
            description = "Lo esencial para iniciar tu entrenamiento y descubrir una forma más clara de progresar."
        )
    }
}

@Composable
private fun PlansHeader(onBack: () -> Unit, onRefresh: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = AppTextPrimary) }
        Column(Modifier.weight(1f)) {
            Text("Planes", color = AppTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Conoce las opciones disponibles", color = AppTextSecondary, fontSize = 11.sp)
        }
        onRefresh?.let { IconButton(onClick = it) { Icon(Icons.Default.Refresh, "Recargar planes", tint = PrimaryBlue) } }
    }
}

@Composable
private fun PlansLoading(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PlansHeader(onBack)
        Card(
            Modifier.fillMaxWidth(0.88f).height(420.dp),
            colors = CardDefaults.cardColors(containerColor = AppSurface),
            border = BorderStroke(1.dp, AppBorder),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(6) { index ->
                    Box(Modifier.fillMaxWidth(if (index == 0) 0.55f else 0.85f).height(if (index == 0) 22.dp else 13.dp).clip(RoundedCornerShape(8.dp)).background(AppSurfaceAlt))
                }
            }
        }
    }
}

@Composable
private fun PlansEmpty(onBack: () -> Unit, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(8.dp)) {
        PlansHeader(onBack, onRetry)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("No hay planes disponibles en este momento.", color = AppTextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun PlansError(message: String, onBack: () -> Unit, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(8.dp)) {
        PlansHeader(onBack)
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.ErrorOutline, null, tint = PlanDanger, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text(message, color = AppTextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) { Text("Reintentar") }
        }
    }
}
