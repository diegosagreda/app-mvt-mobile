package com.example.mvt.ui.components.summary

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.ui.graphics.Color
import com.example.mvt.ui.components.SummaryBaseItem

@Composable
fun CentralSummaryItem(
    sesionesCentral: Any?,
    tipoMedicion: String,
    modifier: Modifier = Modifier,
    onTotalChange: (Double) -> Unit
) {
    var total by remember { mutableStateOf(0.0) }

    LaunchedEffect(sesionesCentral, tipoMedicion) {
        val tag = "CENTRAL_TOTAL"
        val tipoExcluido = listOf("Flexibilidad", "Movilidad Articular", "Fortalecimiento")

        val centralMap = sesionesCentral as? Map<String, Any>
        val series = (centralMap?.get("series") as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()

        if (series.isEmpty()) {
            total = 0.0
            onTotalChange(0.0)
            return@LaunchedEffect
        }

        total = if (tipoMedicion.lowercase() == "distancia") {

            var sum = 0.0

            for ((idx, serie) in series.withIndex()) {
                val sesiones = (serie["sesiones"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: continue
                var subtotal = 0.0

                val ini = (serie["inicio"] as? Number)?.toInt() ?: -1
                val fin = (serie["final"] as? Number)?.toInt() ?: -1
                val rep = (serie["repeticiones"] as? Number)?.toDouble() ?: 1.0

                val lista = if (ini != -1 && fin != -1 &&
                    ini < sesiones.size && fin < sesiones.size
                ) sesiones.subList(ini, fin + 1)
                else sesiones

                for (sesion in lista) {
                    val tipo = (sesion["tipo"] as? String)?.trim() ?: ""
                    if (tipoExcluido.contains(tipo)) continue

                    val dist = (sesion["distancia"] as? Number)?.toDouble() ?: 0.0
                    val uni = (sesion["tipo_medicion"] as? String)?.lowercase() ?: ""
                    subtotal += if (uni == "metros") dist / 1000.0 else dist
                }

                subtotal *= rep
                sum += subtotal
            }

            sum
        } else {

            var sum = 0.0

            for (serie in series) {
                val sesiones = (serie["sesiones"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: continue
                val rep = (serie["repeticiones"] as? Number)?.toDouble() ?: 1.0

                var subtotal = sesiones.sumOf {
                    val min = (it["duracion_min"] as? Number)?.toDouble() ?: 0.0
                    val sec = (it["duracion_seg"] as? Number)?.toDouble() ?: 0.0
                    min + sec / 60.0
                }

                subtotal *= rep
                sum += subtotal
            }

            sum
        }

        Log.d(tag, "TOTAL CENTRAL = $total")
        onTotalChange(total)
    }

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
