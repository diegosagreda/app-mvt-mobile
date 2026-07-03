package com.example.mvt.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mvt.R
import com.example.mvt.data.firebase.models.StravaConnection
import com.example.mvt.ui.theme.AccentRed
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppPrimarySoft
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue
import com.example.mvt.utils.StravaAuthRedirectBus
import com.example.mvt.ui.viewmodels.StravaConnectionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

private val StravaOrange = Color(0xFFFC4C02)
private val GarminBlue = Color(0xFF1580C8)
private val HeroPanel = Color(0xFF26324A)
private val HeroPanelAlt = Color(0xFF1F2A3B)
private val HeroText = Color(0xFFF8FAFC)
private val HeroTextMuted = Color(0xFF93A4C2)
private val HeroStravaGlow = Color(0xFF4A3137)
private val HeroPulseGlow = Color(0xFF153A66)
private val HeroGarminGlow = Color(0xFF173B57)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    navController: androidx.navigation.NavController,
    viewModel: StravaConnectionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val currentUiState by rememberUpdatedState(uiState)
    val currentOnCancelled by rememberUpdatedState(newValue = { viewModel.onAuthCancelled() })
    var authLeftApp by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(uiState.isAuthVisible, uiState.authUrl) {
        if (!uiState.isAuthVisible || uiState.authUrl.isBlank()) return@LaunchedEffect

        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uiState.authUrl)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
            context.startActivity(intent)
        }.onFailure {
            viewModel.onAuthLaunchFailed()
        }
    }

    LaunchedEffect(Unit) {
        StravaAuthRedirectBus.redirects.collect { redirect ->
            if (redirect == null) return@collect

            handleRedirect(
                url = redirect.toString(),
                redirectUri = currentUiState.redirectUri,
                expectedState = currentUiState.authState,
                onCancelled = currentOnCancelled,
                onAuthorizationCode = viewModel::exchangeCode
            )
            StravaAuthRedirectBus.clear()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (currentUiState.isAuthVisible) {
                        authLeftApp = true
                    }
                }

                Lifecycle.Event.ON_RESUME -> {
                    if (authLeftApp) {
                        authLeftApp = false
                        coroutineScope.launch {
                            delay(350)
                            if (currentUiState.isAuthVisible) {
                                currentOnCancelled()
                            }
                        }
                    }
                }

                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(start = 8.dp, end = 8.dp, bottom = 10.dp)
                .navigationBarsPadding(),
        ) {
            Spacer(modifier = Modifier.height(6.dp))
            ConnectionHero()

            Column(
                modifier = Modifier.padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (uiState.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(999.dp)),
                        color = PrimaryBlue,
                        trackColor = AppBorder
                    )
                }

                Box {
                    SectionCaption("Disponible ahora")
                }
                StravaConnectionCard(
                    connection = uiState.connection,
                    isEnabled = uiState.isEnabled,
                    isLoading = uiState.isLoading,
                    onToggle = viewModel::toggleSync,
                    onConnect = viewModel::openAuth
                )

                Box {
                    SectionCaption("En preparación")
                }
                GarminPreviewCard()
            }
        }
    }
}

@Composable
private fun ConnectionHero(modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = HeroPanel),
        modifier = Modifier
            .then(modifier)
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(HeroPanel, HeroPanelAlt, HeroPanel)
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(
                    text = "Conecta tus apps deportivas",
                    color = HeroText,
                    fontSize = 26.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Actividad, rutinas y rendimiento en un solo lugar.",
                    color = HeroTextMuted,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 22.sp
                )
                HeroNodes()
            }
        }
    }
}

@Composable
private fun HeroNodes() {
    val transferAnimation = rememberInfiniteTransition(label = "hero_transfer")
    val transferProgress by transferAnimation.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing)
        ),
        label = "hero_transfer_progress"
    )
    val mvtPulse by transferAnimation.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_mvt_pulse"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeroBrandNode(
            background = HeroStravaGlow,
            contentColor = StravaOrange,
            drawableRes = R.drawable.ic_strava_mark,
            size = 60.dp,
            iconSize = 28.dp
        )
        HeroNodeConnector(
            modifier = Modifier.weight(1f),
            progress = transferProgress,
            sourceColor = StravaOrange,
            reverse = false
        )
        HeroBrandNode(
            background = HeroPulseGlow,
            contentColor = PrimaryBlue,
            drawableRes = R.drawable.mvt,
            size = 76.dp,
            iconSize = 52.dp,
            modifier = Modifier.graphicsLayer {
                scaleX = mvtPulse
                scaleY = mvtPulse
            }
        )
        HeroNodeConnector(
            modifier = Modifier.weight(1f),
            progress = transferProgress,
            sourceColor = GarminBlue,
            reverse = true
        )
        HeroBrandNode(
            background = HeroGarminGlow,
            contentColor = GarminBlue,
            label = "GARMIN",
            fontSize = 8.sp,
            size = 60.dp
        )
    }
}

@Composable
private fun HeroNodeConnector(
    progress: Float,
    sourceColor: Color,
    reverse: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .height(56.dp)
            .padding(horizontal = 6.dp)
    ) {
        val centerY = size.height / 2f
        val lineThickness = size.height * 0.08f

        drawLine(
            brush = Brush.horizontalGradient(
                colors = if (reverse) {
                    listOf(
                        PrimaryBlue.copy(alpha = 0.82f),
                        AccentRed.copy(alpha = 0.24f),
                        sourceColor.copy(alpha = 0.16f)
                    )
                } else {
                    listOf(
                        sourceColor.copy(alpha = 0.16f),
                        AccentRed.copy(alpha = 0.24f),
                        PrimaryBlue.copy(alpha = 0.82f)
                    )
                }
            ),
            start = androidx.compose.ui.geometry.Offset(0f, centerY),
            end = androidx.compose.ui.geometry.Offset(size.width, centerY),
            strokeWidth = lineThickness,
            cap = StrokeCap.Round
        )

        val phases = listOf(0f, 0.22f, 0.48f)
        phases.forEachIndexed { index, phase ->
            val cycleProgress = (progress + phase) % 1f
            val travelProgress = if (reverse) 1f - cycleProgress else cycleProgress
            val particleX = size.width * travelProgress
            val particleRadius = size.height * if (index == 0) 0.12f else 0.085f
            val particleColor = lerp(sourceColor, PrimaryBlue, cycleProgress)

            drawCircle(
                color = particleColor.copy(alpha = if (index == 0) 0.95f else 0.72f),
                radius = particleRadius,
                center = androidx.compose.ui.geometry.Offset(particleX, centerY)
            )
            drawCircle(
                color = particleColor.copy(alpha = 0.18f),
                radius = particleRadius * 2.8f,
                center = androidx.compose.ui.geometry.Offset(particleX, centerY)
            )
        }
    }
}

@Composable
private fun HeroBrandNode(
    background: Color,
    contentColor: Color,
    label: String? = null,
    drawableRes: Int? = null,
    fontSize: TextUnit = 20.sp,
    size: Dp = 52.dp,
    iconSize: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        if (drawableRes != null) {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                colorFilter = ColorFilter.tint(contentColor)
            )
        } else if (label != null) {
            Text(
                text = label,
                color = contentColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SectionCaption(text: String) {
    Text(
        text = text.uppercase(),
        color = AppTextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.8.sp
    )
}

@Composable
private fun StravaConnectionCard(
    connection: StravaConnection?,
    isEnabled: Boolean,
    isLoading: Boolean,
    onToggle: (Boolean) -> Unit,
    onConnect: () -> Unit
) {
    val stateLabel = when {
        connection != null -> "Conectada"
        isLoading -> "Actualizando"
        isEnabled -> "Disponible"
        else -> "Desactivada"
    }

    val headline = when {
        connection != null -> "Tus actividades ya se sincronizan con MVT."
        isEnabled -> "Asocia tus actividades a tus rutinas."
        else -> "Activa Strava y centraliza tu actividad."
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BrandSquare(
                    background = StravaOrange,
                    drawableRes = R.drawable.ic_strava_mark,
                    size = 52.dp,
                    iconSize = 28.dp,
                    cornerRadius = 14.dp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "STRAVA",
                        color = AppTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )

                }
                StatusPill(
                    text = stateLabel,
                    accent = when {
                        connection != null -> Color(0xFF32D296)
                        isEnabled -> PrimaryBlue
                        else -> AppTextSecondary
                    }
                )
            }

            Text(
                text = "Conexión principal",
                color = AppTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = headline,
                color = AppTextSecondary,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 22.sp
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val singleColumn = maxWidth < 340.dp
                if (singleColumn) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ConnectionStatTile(
                            label = "Cuenta",
                            value = connection?.athleteName?.takeIf { it.isNotBlank() } ?: "Sin conectar"
                        )
                        ConnectionStatTile(
                            label = "Última sync",
                            value = connection?.expiresAt?.let(::formatRelativeExpiry) ?: "Sin registro"
                        )
                        ConnectionStatTile(
                            label = "Permisos",
                            value = connection?.scope?.takeIf { it.isNotBlank() }?.let(::humanizeScope) ?: "Lectura"
                        )
                        ConnectionStatTile(
                            label = "Estado",
                            value = when {
                                connection != null -> "Activa"
                                isEnabled -> "Lista"
                                else -> "Desactivada"
                            }
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ConnectionStatTile(
                                label = "Cuenta",
                                value = connection?.athleteName?.takeIf { it.isNotBlank() } ?: "Sin conectar",
                                modifier = Modifier.weight(1f)
                            )
                            ConnectionStatTile(
                                label = "Última sync",
                                value = connection?.expiresAt?.let(::formatRelativeExpiry) ?: "Sin registro",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ConnectionStatTile(
                                label = "Permisos",
                                value = connection?.scope?.takeIf { it.isNotBlank() }?.let(::humanizeScope) ?: "Lectura",
                                modifier = Modifier.weight(1f)
                            )
                            ConnectionStatTile(
                                label = "Estado",
                                value = when {
                                    connection != null -> "Activa"
                                    isEnabled -> "Lista"
                                    else -> "Desactivada"
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (connection == null) {
                Button(
                    onClick = {
                        if (!isEnabled) onToggle(true)
                        onConnect()
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        disabledContainerColor = AppSurfaceAlt
                    )
                ) {
                    Text(if (isEnabled) "Conectar Strava" else "Activar y conectar")
                }
            } else {
                OutlinedButton(
                    onClick = { onToggle(false) },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.LinkOff, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Desconectar")
                }
            }
        }
    }
}

@Composable
private fun GarminPreviewCard() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BrandSquare(
                    background = GarminBlue,
                    label = "GARMIN",
                    fontSize = 8.sp,
                    size = 52.dp,
                    iconSize = 18.dp,
                    cornerRadius = 14.dp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "GARMIN",
                        color = AppTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                }
                StatusPill(text = "Próximamente", accent = GarminBlue)
            }

            Text(
                text = "Garmin Connect",
                color = AppTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Métricas de dispositivo y sesiones automáticas.",
                color = AppTextSecondary,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 22.sp
            )

            InfoStrip(
                icon = Icons.Default.Schedule,
                title = "Preparando arquitectura",
                message = "La base ya está lista para sumar nuevas conexiones."
            )
        }
    }
}

@Composable
private fun ConnectionStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceAlt),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label.uppercase(),
                color = AppTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.7.sp
            )
            Text(
                text = value,
                color = AppTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    accent: Color
) {
    Surface(
        color = accent.copy(alpha = 0.12f),
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Text(
            text = text,
            color = if (accent == AppTextSecondary) AppTextPrimary else accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun InfoStrip(
    icon: ImageVector,
    title: String,
    message: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppSurfaceAlt)
            .border(1.dp, AppBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = AppPrimarySoft,
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.padding(10.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                color = AppTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = message,
                color = AppTextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun BrandSquare(
    background: Color,
    label: String? = null,
    drawableRes: Int? = null,
    fontSize: TextUnit = 22.sp,
    size: Dp = 46.dp,
    iconSize: Dp = 22.dp,
    cornerRadius: Dp = 12.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        if (drawableRes != null) {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = null,
                modifier = Modifier.size(iconSize)
            )
        } else if (label != null) {
            Text(
                text = label,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize
            )
        }
    }
}

private fun humanizeScope(scope: String): String {
    return if (scope.contains("activity", ignoreCase = true)) {
        "Lectura"
    } else {
        scope
    }
}

private fun handleRedirect(
    url: String?,
    redirectUri: String,
    expectedState: String,
    onCancelled: () -> Unit,
    onAuthorizationCode: (String, String?, String?) -> Unit
): Boolean {
    if (url.isNullOrBlank() || !url.startsWith(redirectUri)) return false

    val uri = Uri.parse(url)
    val error = uri.getQueryParameter("error")
    if (!error.isNullOrBlank()) {
        onCancelled()
        return true
    }

    val code = uri.getQueryParameter("code")
    val scope = uri.getQueryParameter("scope")
    val state = uri.getQueryParameter("state")
    if (expectedState.isNotBlank() && state != expectedState) {
        onCancelled()
        return true
    }

    if (!code.isNullOrBlank()) {
        onAuthorizationCode(code, scope, state)
        return true
    }

    return false
}

private fun formatRelativeExpiry(timestampSeconds: Long): String {
    val millis = timestampSeconds * 1000
    return DateUtils.getRelativeTimeSpanString(
        millis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()
}
