package com.example.mvt.ui.components.summary

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.ui.graphics.Color
import com.example.mvt.ui.components.SummaryBaseItem

@Composable
fun CentralSummaryItem(
    total: Double,
    tipoMedicion: String,
    modifier: Modifier = Modifier
) {
    val valueFormatted =
        if (tipoMedicion.lowercase() == "tiempo")
            formatTiempo(total)
        else
            "%.2f km".format(total)

    SummaryBaseItem(
        icon = Icons.Default.FitnessCenter,
        label = "Central",
        value = valueFormatted,
        color = Color(0xFF90CAF9),
        modifier = modifier
    )
}

private fun formatTiempo(totalMinutos: Double): String {
    val totalSegundos = (totalMinutos * 60).toInt()
    val h = totalSegundos / 3600
    val m = (totalSegundos % 3600) / 60
    val s = totalSegundos % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}
