package com.example.mvt.ui.components.summary

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RunCircle
import androidx.compose.ui.graphics.Color
import com.example.mvt.ui.components.SummaryBaseItem

@Composable
fun CalentamientoSummaryItem(
    sesiones: List<Map<String, Any>>?,
    tipoMedicion: String,
    modifier: Modifier = Modifier,
    onTotalChange: (Double) -> Unit
) {
    var total by remember { mutableStateOf(0.0) }

    LaunchedEffect(sesiones, tipoMedicion) {
        val tipoExcluido = listOf("Flexibilidad", "Movilidad Articular", "Fortalecimiento")

        total = if (sesiones.isNullOrEmpty()) {
            0.0
        } else if (tipoMedicion.lowercase() == "tiempo") {

            sesiones.sumOf {
                val min = (it["duracion_min"] as? Number)?.toDouble() ?: 0.0
                val seg = (it["duracion_seg"] as? Number)?.toDouble() ?: 0.0
                min + seg / 60.0
            }

        } else {

            sesiones.sumOf {
                val tipo = (it["tipo"] as? String)?.trim() ?: ""
                if (tipoExcluido.contains(tipo)) return@sumOf 0.0

                val dist = (it["distancia"] as? Number)?.toDouble() ?: 0.0
                val uni = (it["tipo_medicion"] as? String)?.lowercase() ?: ""

                if (uni == "metros") dist / 1000.0 else dist
            }
        }

        Log.d("CALENTAMIENTO", "Total calculado = $total")
        onTotalChange(total)
    }

    val valueFormatted =
        if (tipoMedicion.lowercase() == "tiempo")
            formatTiempo(total)
        else
            "%.2f km".format(total)

    SummaryBaseItem(
        icon = Icons.Default.RunCircle,
        label = "Calentamiento",
        value = valueFormatted,
        color = Color(0xFFBBDEFB),
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
