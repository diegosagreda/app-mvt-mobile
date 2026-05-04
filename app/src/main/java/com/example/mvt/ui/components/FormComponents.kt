package com.example.mvt.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mvt.ui.theme.PrimaryBlue

// ==========================================
// LABEL CON ASTERISCO de obligatorio Y TOOLTIP
// ==========================================

@Composable
fun FormLabel(
    text: String,
    required: Boolean = false,
    info: String? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = buildAnnotatedString {
                append(text)
                if (required) {
                    append(" ")
                    withStyle(SpanStyle(color = Color.Red)) { append("*") }
                }
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF555B61)
        )
        if (info != null) {
            Spacer(modifier = Modifier.width(6.dp))
            FormTooltip(info)
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
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
    unfocusedBorderColor = Color(0xFFCCCCCC),
    cursorColor          = PrimaryBlue,
    focusedLabelColor    = PrimaryBlue,
    focusedTextColor     = Color(0xFF2B2E34),
    unfocusedTextColor   = Color(0xFF2B2E34)
)

// ==========================================
// COLORES CAMPO SOLO LECTURA
// ==========================================
@Composable
fun formReadOnlyColors() = OutlinedTextFieldDefaults.colors(
    disabledBorderColor    = Color(0xFFCCCCCC),
    disabledTextColor      = Color(0xFF888888),
    disabledContainerColor = Color(0xFFF5F5F5),
    disabledLabelColor     = Color(0xFF888888)
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
                    color = Color.White,
                    border = BorderStroke(2.dp, PrimaryBlue),
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .widthIn(min = 200.dp, max = 280.dp)
                    ) {
                        Text(text = message, fontSize = 14.sp, color = Color(0xFF333333))
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
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        onDismiss()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            color = Color(0xFF4CAF50),
            shape = RoundedCornerShape(14.dp),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = message, color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

// ==========================================
// NOTIFICACIÓN ERROR
// ==========================================
@Composable
fun FormErrorNotification(message: String, onDismiss: () -> Unit) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        onDismiss()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 20.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            color = Color(0xFFD32F2F),
            shape = RoundedCornerShape(14.dp),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}