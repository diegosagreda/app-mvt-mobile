package com.example.mvt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import com.example.mvt.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineInfoSection(
    tipoEsfuerzo: String,
    tipoMedicion: String,
    tipoTerreno: String,
    descripcion: String,
    objetivos: String
) {
    Column(modifier = Modifier.padding(8.dp)) {

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoField("🏋️ Tipo de Esfuerzo", tipoEsfuerzo.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
            InfoField("⏱️ Tipo de Medición", tipoMedicion.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
            InfoField("⛰️ Tipo de Terreno", tipoTerreno.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoBox(
                title = "📘 Descripción",
                text = descripcion
            )
            InfoBox(
                title = "⚡ Objetivos",
                text = objetivos
            )
        }
    }
}

@Composable
fun InfoField(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            color = PrimaryBlue,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        TextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = AppSurfaceAlt,
                unfocusedContainerColor = AppSurfaceAlt,
                disabledContainerColor = AppSurfaceAlt,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = AppTextPrimary,
                unfocusedTextColor = AppTextPrimary,
                disabledTextColor = AppTextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AppBorder, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp)
        )
    }
}


@Composable
fun InfoBox(title: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppSurfaceAlt, RoundedCornerShape(8.dp))
            .border(1.dp, AppBorder, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = title,
            color = PrimaryBlue,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = text.ifEmpty { "Sin información disponible." },
            color = AppTextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Justify
        )
    }
}
