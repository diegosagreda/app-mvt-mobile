package com.example.mvt.ui.screens.settings

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.text.format.DateUtils
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mvt.R
import com.example.mvt.data.firebase.models.StravaConnection
import com.example.mvt.ui.theme.AppBackground
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue
import com.example.mvt.ui.viewmodels.StravaConnectionViewModel

private val StravaOrange = Color(0xFFFC4C02)
private val Ink = AppTextPrimary
private val Muted = AppTextSecondary
private val SoftSurface = AppBackground
private val SoftPanel = AppSurface
private val SoftBlueBorder = AppBorder
private val SoftBlue = AppSurfaceAlt
private val HeroLine = AppBorder
private val HeroPulse = Color(0xFF1570EF)
private val GarminBlue = Color(0xFF1580C8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    navController: NavController,
    viewModel: StravaConnectionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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

    Scaffold(
        containerColor = SoftSurface,
        topBar = {
            TopAppBar(
                title = { Text("Conexiones") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SoftSurface)
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ConnectionHero()

            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    color = PrimaryBlue,
                    trackColor = AppBorder
                )
            }

            ConnectionCardsSection(
                connection = uiState.connection,
                isEnabled = uiState.isEnabled,
                isLoading = uiState.isLoading,
                onToggle = viewModel::toggleSync,
                onConnect = viewModel::openAuth
            )
        }
    }

    AnimatedVisibility(uiState.isAuthVisible) {
        StravaAuthSheet(
            authUrl = uiState.authUrl,
            redirectUri = uiState.redirectUri,
            onClose = viewModel::closeAuth,
            onCancelled = viewModel::onAuthCancelled,
            onAuthorizationCode = viewModel::exchangeCode
        )
    }
}

@Composable
private fun ConnectionHero() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SoftPanel),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SoftBlueBorder, RoundedCornerShape(16.dp))
    ) {
        BoxWithConstraints(
            modifier = Modifier.padding(28.dp)
        ) {
            val stacked = maxWidth < 760.dp

            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    HeroCopy()
                    HeroLogos(compact = true)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        HeroCopy()
                    }
                    HeroLogos(compact = false)
                }
            }
        }
    }
}

@Composable
private fun HeroCopy() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "CENTRO DE CONEXIONES",
            color = PrimaryBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        Text(
            text = "Integra tus actividades deportivas con My Virtual Trainer",
            color = Ink,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Conecta tus aplicaciones favoritas para centralizar entrenamientos, actividades y seguimiento de rendimiento en un solo lugar.",
            color = Muted,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun HeroLogos(compact: Boolean) {
    Row(
        modifier = if (compact) Modifier.fillMaxWidth() else Modifier,
        horizontalArrangement = if (compact) Arrangement.Center else Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeroBrandCircle(
            background = Color(0xFFFFEAE0),
            contentColor = StravaOrange,
            drawableRes = R.drawable.ic_strava_mark
        )
        HeroConnector()
        HeroBrandCircle(
            background = AppSurfaceAlt,
            contentColor = HeroPulse,
            label = "∿"
        )
        HeroConnector()
        HeroBrandCircle(
            background = AppSurfaceAlt,
            contentColor = GarminBlue,
            label = "GARMIN",
            fontSize = 8.sp
        )
    }
}

@Composable
private fun HeroConnector() {
    Box(
        modifier = Modifier
            .width(54.dp)
            .height(2.dp)
            .background(HeroLine)
    )
}

@Composable
private fun HeroBrandCircle(
    background: Color,
    contentColor: Color,
    label: String? = null,
    drawableRes: Int? = null,
    fontSize: TextUnit = 22.sp
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        if (drawableRes != null) {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = null,
                modifier = Modifier.size(26.dp),
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
private fun ConnectionCardsSection(
    connection: StravaConnection?,
    isEnabled: Boolean,
    isLoading: Boolean,
    onToggle: (Boolean) -> Unit,
    onConnect: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stacked = maxWidth < 960.dp

        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                StravaStatusCard(
                    connection = connection,
                    isEnabled = isEnabled,
                    isLoading = isLoading,
                    onToggle = onToggle,
                    onConnect = onConnect
                )
                GarminComingSoonCard()
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                StravaStatusCard(
                    connection = connection,
                    isEnabled = isEnabled,
                    isLoading = isLoading,
                    onToggle = onToggle,
                    onConnect = onConnect,
                    modifier = Modifier.weight(1f)
                )
                GarminComingSoonCard(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StravaStatusCard(
    connection: StravaConnection?,
    isEnabled: Boolean,
    isLoading: Boolean,
    onToggle: (Boolean) -> Unit,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SoftPanel),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SoftBlueBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BrandSquare(
                    background = StravaOrange,
                    drawableRes = R.drawable.ic_strava_mark
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DISPONIBLE AHORA",
                        color = PrimaryBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.9.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Strava",
                        color = Ink,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                FilterChip(
                    selected = true,
                    onClick = { },
                    enabled = false,
                    label = { Text("Disponible") }
                )
            }

            Text(
                text = "Sincroniza tus actividades de Strava para asociarlas a tus rutinas, comparar cumplimiento y entregar información más precisa a tu entrenador.",
                color = Muted,
                style = MaterialTheme.typography.bodyLarge
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val compactMetrics = maxWidth < 620.dp

                if (compactMetrics) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DetailInfoCard(
                            title = "CUENTA",
                            value = connection?.athleteName?.takeIf { it.isNotBlank() } ?: "Sin conectar"
                        )
                        DetailInfoCard(
                            title = "ÚLTIMA REVISIÓN",
                            value = connection?.expiresAt?.let(::formatRelativeExpiry) ?: "Sin sincronización reciente"
                        )
                        DetailInfoCard(
                            title = "PERMISOS",
                            value = connection?.scope?.takeIf { it.isNotBlank() }?.let(::humanizeScope) ?: "Lectura de actividades"
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DetailInfoCard(
                            title = "CUENTA",
                            value = connection?.athleteName?.takeIf { it.isNotBlank() } ?: "Sin conectar",
                            modifier = Modifier.weight(1f)
                        )
                        DetailInfoCard(
                            title = "ÚLTIMA REVISIÓN",
                            value = connection?.expiresAt?.let(::formatRelativeExpiry) ?: "Sin sincronización reciente",
                            modifier = Modifier.weight(1f)
                        )
                        DetailInfoCard(
                            title = "PERMISOS",
                            value = connection?.scope?.takeIf { it.isNotBlank() }?.let(::humanizeScope) ?: "Lectura de actividades",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onConnect,
                    enabled = connection == null && isEnabled && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        disabledContainerColor = Color(0xFFA9CCF7)
                    )
                ) {
                    Text("Conectar Strava")
                }

                if (connection != null) {
                    OutlinedButton(
                        onClick = { onToggle(false) },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Desconectar")
                    }
                } else {
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onToggle,
                        enabled = !isLoading
                    )
                }
            }
        }
    }
}

@Composable
private fun GarminComingSoonCard(modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SoftPanel),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SoftBlueBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BrandSquare(
                    background = GarminBlue,
                    label = "GARMIN",
                    fontSize = 10.sp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PRÓXIMAMENTE",
                        color = PrimaryBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.9.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Garmin Connect",
                        color = Ink,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                FilterChip(
                    selected = true,
                    onClick = { },
                    enabled = false,
                    label = { Text("En desarrollo") }
                )
            }

            Text(
                text = "Estamos preparando la integración con Garmin para sincronizar métricas de dispositivos, sesiones registradas y datos avanzados de rendimiento.",
                color = Muted,
                style = MaterialTheme.typography.bodyLarge
            )

            InfoCalloutCard(
                icon = Icons.Default.Schedule,
                title = "Garmin llegará a este centro",
                message = "La arquitectura queda lista para sumar nuevas conexiones sin cambiar tu flujo."
            )
        }
    }
}

@Composable
private fun BrandSquare(
    background: Color,
    label: String? = null,
    drawableRes: Int? = null,
    fontSize: TextUnit = 22.sp
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        if (drawableRes != null) {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = null,
                modifier = Modifier.size(22.dp)
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

@Composable
private fun DetailInfoCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceAlt),
        modifier = modifier.border(1.dp, AppBorder, RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                color = Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.6.sp
            )
            Text(
                text = value,
                color = Ink,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun InfoCalloutCard(
    icon: ImageVector,
    title: String,
    message: String
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceAlt),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppBorder, RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = SoftBlue,
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
                    color = Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message,
                    color = Muted,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

private fun humanizeScope(scope: String): String {
    return if (scope.contains("activity", ignoreCase = true)) {
        "Lectura de actividades"
    } else {
        scope
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StravaAuthSheet(
    authUrl: String,
    redirectUri: String,
    onClose: () -> Unit,
    onCancelled: () -> Unit,
    onAuthorizationCode: (String, String?) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onCancelled,
        modifier = Modifier.fillMaxSize(),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Autorizar Strava",
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = onClose) {
                    Text("Cerrar")
                }
            }
            StravaAuthWebView(
                authUrl = authUrl,
                redirectUri = redirectUri,
                onCancelled = onCancelled,
                onAuthorizationCode = onAuthorizationCode
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun StravaAuthWebView(
    authUrl: String,
    redirectUri: String,
    onCancelled: () -> Unit,
    onAuthorizationCode: (String, String?) -> Unit
) {
    val context = LocalContext.current
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    AndroidView(
        factory = {
            webView.apply {
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        handleRedirect(url, redirectUri, onCancelled, onAuthorizationCode)
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return handleRedirect(
                            request?.url?.toString(),
                            redirectUri,
                            onCancelled,
                            onAuthorizationCode
                        )
                    }
                }
                loadUrl(authUrl)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun handleRedirect(
    url: String?,
    redirectUri: String,
    onCancelled: () -> Unit,
    onAuthorizationCode: (String, String?) -> Unit
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
    if (!code.isNullOrBlank()) {
        onAuthorizationCode(code, scope)
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
