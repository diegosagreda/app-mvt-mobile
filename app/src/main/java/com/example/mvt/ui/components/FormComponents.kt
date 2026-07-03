package com.example.mvt.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.example.mvt.ui.theme.AppError
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSuccess
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue

// Patrón base para todos los campos nuevos de la aplicación.
val FormFieldShape = RoundedCornerShape(14.dp)

// ==========================================
// LABEL CON ASTERISCO de obligatorio Y TOOLTIP
// ==========================================

@Composable
fun FormLabel(
    text: String,
    required: Boolean = false,
    info: String? = null
) {
    FormLegendLabel(
        text = text,
        required = required,
        info = info,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
fun FormLegendLabel(
    text: String,
    required: Boolean = false,
    info: String? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = AppSurfaceAlt,
        border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.85f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = buildAnnotatedString {
                    append(text)
                    if (required) {
                        append(" ")
                        withStyle(SpanStyle(color = AppError)) { append("*") }
                    }
                },
                fontSize = fontSize,
                fontWeight = fontWeight,
                lineHeight = fontSize,
                color = AppTextSecondary
            )
            if (info != null) {
                Spacer(modifier = Modifier.width(2.dp))
                FormTooltip(info)
            }
        }
    }
}

@Composable
fun FormLegendField(
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    Box(modifier = modifier) {
        content(Modifier.padding(top = 14.dp))
        Box(
            modifier = Modifier
                .padding(start = 12.dp)
                .zIndex(1f)
        ) {
            label()
        }
    }
}

// ==========================================
// ESPACIADO ENTRE CAMPOS
// ==========================================
@Composable
fun FormSpacer() {
    Spacer(modifier = Modifier.height(16.dp))
}

// ==========================================
// COLORES CAMPO EDITABLE
// ==========================================
@Composable
fun formFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = PrimaryBlue,
    unfocusedBorderColor = AppBorder,
    cursorColor          = PrimaryBlue,
    focusedLabelColor    = PrimaryBlue,
    focusedTextColor     = AppTextPrimary,
    unfocusedTextColor   = AppTextPrimary
)

// ==========================================
// COLORES CAMPO SOLO LECTURA
// ==========================================
@Composable
fun formReadOnlyColors() = OutlinedTextFieldDefaults.colors(
    disabledBorderColor    = AppBorder,
    disabledTextColor      = AppTextSecondary,
    disabledContainerColor = AppSurface,
    disabledLabelColor     = AppTextSecondary
)

// ==========================================
// TOOLTIP / ICONO DE INTERROGACIÓN
// ==========================================
@Composable
fun FormTooltip(message: String) {
    var show by remember { mutableStateOf(false) }
    Box {
        Icon(
            imageVector = Icons.Default.HelpOutline,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier
                .size(18.dp)
                .clickable { show = true }
        )
        if (show) {
            Dialog(onDismissRequest = { show = false }) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AppSurface,
                    border = BorderStroke(2.dp, PrimaryBlue),
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .widthIn(min = 200.dp, max = 280.dp)
                    ) {
                        Text(text = message, fontSize = 14.sp, color = AppTextPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { show = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("OK", color = PrimaryBlue)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// NOTIFICACIÓN ÉXITO
// ==========================================
@Composable
fun FormSuccessNotification(message: String, onDismiss: () -> Unit) {
    FormFloatingNotification(
        title = "Cambios guardados",
        message = message,
        accentColor = AppSuccess,
        icon = Icons.Default.CheckCircle,
        onDismiss = onDismiss
    )
}

// ==========================================
// NOTIFICACIÓN ERROR
// ==========================================
@Composable
fun FormErrorNotification(message: String, onDismiss: () -> Unit) {
    FormFloatingNotification(
        title = "Revisa el formulario",
        message = message,
        accentColor = AppError,
        icon = Icons.Default.HelpOutline,
        onDismiss = onDismiss
    )
}

@Composable
fun FormConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    enabled: Boolean = true
) {
    val iconAccentColor = if (destructive) Color(0xFFFFA24C) else PrimaryBlue
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(max = 420.dp),
            color = AppSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, AppBorder),
            shadowElevation = 14.dp,
            tonalElevation = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(13.dp),
                        color = iconAccentColor.copy(alpha = 0.14f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = iconAccentColor,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = AppTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Confirmación requerida",
                            color = AppTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                Text(
                    text = message,
                    color = AppTextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, AppBorder)
                    ) {
                        Text("Cancelar", color = AppTextSecondary)
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text(confirmLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun FormFloatingNotification(
    title: String,
    message: String,
    accentColor: Color,
    icon: ImageVector,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2200)
        onDismiss()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 420.dp),
            color = AppSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
            shadowElevation = 14.dp,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = accentColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        color = AppTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = message,
                        color = AppTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(34.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = accentColor.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(999.dp)
                    ) {}
                }
            }
        }
    }
}
