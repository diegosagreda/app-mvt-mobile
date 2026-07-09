package com.example.mvt.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary

@Composable
fun RoutineInfoSection(
    tipoEsfuerzo: String,
    tipoMedicion: String,
    tipoTerreno: String,
    descripcion: String,
    objetivos: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RoutineMetaCard(
                label = "Esfuerzo",
                value = formatRoutineText(tipoEsfuerzo),
                modifier = Modifier.weight(1f)
            )
            RoutineMetaCard(
                label = "Medicion",
                value = formatRoutineText(tipoMedicion),
                modifier = Modifier.weight(1f)
            )
            RoutineMetaCard(
                label = "Terreno",
                value = formatRoutineText(tipoTerreno),
                modifier = Modifier.weight(1f)
            )
        }

        RoutineLegendBlock(
            label = "Descripción",
            text = descripcion,
            placeholder = "Sin descripción registrada."
        )

        RoutineLegendBlock(
            label = "Objetivos",
            text = objetivos,
            placeholder = "Sin objetivos registrados."
        )
    }
}

@Composable
private fun RoutineMetaCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = AppSurface,
        border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                color = AppTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = value,
                color = AppTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee()
            )
        }
    }
}

@Composable
private fun RoutineLegendBlock(
    label: String,
    text: String,
    placeholder: String
) {
    val resolvedText = text.trim().ifBlank { placeholder }
    val isPlaceholder = text.isBlank()
    var expanded by remember(resolvedText) { mutableStateOf(false) }
    var canExpand by remember(resolvedText) { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            shape = RoundedCornerShape(16.dp),
            color = AppSurfaceAlt,
            border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Text(
                    text = resolvedText,
                    color = if (isPlaceholder) AppTextSecondary else AppTextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Justify,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = canExpand || expanded) {
                            expanded = !expanded
                        },
                    onTextLayout = { result: TextLayoutResult ->
                        if (!expanded) {
                            canExpand = result.hasVisualOverflow
                        }
                    }
                )
                if (canExpand || expanded) {
                    Text(
                        text = if (expanded) "Ver menos" else "Ver más",
                        color = AppTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clickable { expanded = !expanded }
                    )
                }
            }
        }

        Box(modifier = Modifier.padding(start = 12.dp)) {
            FormLegendLabel(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatRoutineText(value: String): String {
    val clean = value.trim()
    return if (clean.isBlank()) {
        "--"
    } else {
        clean.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
