package com.example.mvt.ui.components.chart

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.MPPointF
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.mvt.data.firebase.models.Routine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter

// =======================================================
// === MARKER PERSONALIZADO ===
// =======================================================
class PhaseMarkerView(
    context: Context,
    private val fase: String,
    private val total: String
) : MarkerView(context, android.R.layout.simple_list_item_1) {

    private val textView: TextView = findViewById(android.R.id.text1)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        textView.text = "Fase: $fase\nTotal: $total"
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF((-width / 2).toFloat(), (-height - 20).toFloat())
    }
}

// =======================================================
// === CONVERSIÓN A KM ===
// =======================================================
private fun toKilometros(valor: Any?, tipoMedicion: Any?): Double {
    val v = when (valor) {
        is Number -> valor.toDouble()
        is String -> valor.replace(",", ".").toDoubleOrNull() ?: 0.0
        else -> 0.0
    }
    val unidad = tipoMedicion?.toString()?.lowercase() ?: ""
    return if (unidad.contains("metro")) v / 1000.0 else v
}

// =======================================================
// === GRÁFICO DE RUTINA ===
// =======================================================
@Composable
fun RoutineChart(
    routine: Routine,
    modifier: Modifier = Modifier
) {
    val tipoMedicion = routine.tipo_medicion.lowercase()
    val tipoExcluido = listOf("Flexibilidad", "Movilidad Articular", "Fortalecimiento")

    fun mapIntensidad(i: String?): Float {
        val s = i ?: "R1"
        var v = when {
            s.startsWith("Z", true) -> s.drop(1).toFloatOrNull() ?: 0f
            s.startsWith("R", true) -> (s.drop(1).toFloatOrNull() ?: 0f) + 5f
            else -> s.toFloatOrNull() ?: 0f
        }
        if (v <= 0f) v = 0.5f
        if (v == 1f) v = 1.2f
        return v
    }

    fun procesarSesiones(lista: List<Map<String, Any>>?, offsetInicial: Double): Pair<List<Entry>, Double> {
        if (lista.isNullOrEmpty()) return emptyList<Entry>() to offsetInicial
        val entries = mutableListOf<Entry>()
        var acumuladoX = offsetInicial

        for (e in lista) {
            val tipo = (e["tipo"] as? String)?.trim() ?: ""
            if (tipoExcluido.contains(tipo)) continue

            val intensidad = (e["intensidad"] as? String)?.ifEmpty { "R1" }
            val yVal = mapIntensidad(intensidad)

            val incremento = when (tipoMedicion) {
                "distancia" -> {
                    val distancia = e["distancia"]
                    val medicion = e["tipo_medicion"] ?: e["medicion"]
                    toKilometros(distancia, medicion)
                }
                "tiempo" -> {
                    val min = (e["duracion_min"] as? Number)?.toDouble() ?: 0.0
                    val seg = (e["duracion_seg"] as? Number)?.toDouble() ?: 0.0
                    min + seg / 60.0
                }
                else -> 1.0
            }

            val ancho = if (incremento > 0) incremento else 0.05
            entries.add(Entry(acumuladoX.toFloat(), yVal))
            acumuladoX += ancho
            entries.add(Entry(acumuladoX.toFloat(), yVal))
        }
        return entries to acumuladoX
    }

    var offset = 0.0
    val dataSets = mutableListOf<LineDataSet>()

    val colores = listOf(
        Triple("#00B0FF", "#80E0FF", "Calentamiento"),
        Triple("#004C99", "#4B79A1", "Central"),
        Triple("#0091EA", "#80D8FF", "Vuelta a la Calma")
    )

    // === Calentamiento ===
    val (cal, off1) = procesarSesiones(routine.sesiones_calentamiento, offset)
    val totalCal = off1 - offset
    if (cal.isNotEmpty()) {
        dataSets += LineDataSet(cal, "Calentamiento").apply {
            color = Color.parseColor(colores[0].first)
            mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 0f
            setDrawFilled(true)
            fillDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor(colores[0].first), Color.parseColor(colores[0].second))
            )
        }
    }
    offset = off1

    // === Central ===
    val centralMap = routine.sesiones_central as? Map<String, Any>
    val series = (centralMap?.get("series") as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()
    val centralEntries = mutableListOf<Entry>()
    for (serie in series) {
        val sesiones = (serie["sesiones"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: continue
        val rep = (serie["repeticiones"] as? Number)?.toInt() ?: 1
        repeat(rep) {
            val (ent, newOff) = procesarSesiones(sesiones, offset)
            centralEntries += ent
            offset = newOff
        }
    }
    val totalCentral = offset - off1
    if (centralEntries.isNotEmpty()) {
        dataSets += LineDataSet(centralEntries, "Central").apply {
            color = Color.parseColor(colores[1].first)
            mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 0f
            setDrawFilled(true)
            fillDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor(colores[1].first), Color.parseColor(colores[1].second))
            )
        }
    }

    // === Vuelta a la calma ===
    val (vuelta, offFinal) = procesarSesiones(routine.sesiones_calma, offset)
    val totalVuelta = offFinal - offset
    if (vuelta.isNotEmpty()) {
        dataSets += LineDataSet(vuelta, "Vuelta a la Calma").apply {
            color = Color.parseColor(colores[2].first)
            mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 0f
            setDrawFilled(true)
            fillDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor(colores[2].first), Color.parseColor(colores[2].second))
            )
        }
    }

    // === Render ===
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                axisRight.isEnabled = false
                setBackgroundColor(Color.parseColor("#F7F9FC"))
                setViewPortOffsets(80f, 80f, 60f, 60f)

                val data = LineData()
                dataSets.forEach { data.addDataSet(it) }
                this.data = data

                // === EJE X ===
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = Color.DKGRAY
                    textSize = 12f
                    setDrawGridLines(false)
                    granularity = 0.5f
                    axisMinimum = 0f
                    axisMaximum = offFinal.toFloat()
                    yOffset = 15f
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val isLastLabel = value >= offFinal.toFloat() - 0.5f
                            return if (isLastLabel) {
                                if (tipoMedicion == "distancia") "${"%.0f".format(value)} km"
                                else "${"%.0f".format(value)} min"
                            } else {
                                "${"%.0f".format(value)}"
                            }
                        }
                    }
                }

                // === EJE Y ===
                axisLeft.apply {
                    textColor = Color.DKGRAY
                    textSize = 12f
                    setDrawGridLines(true)
                    axisMinimum = 0f
                    axisMaximum = 10f
                    granularity = 1f
                    setLabelCount(11, true)
                    setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
                }

                // === MARCADORES POR FASE ===
                val markerCal = PhaseMarkerView(context, "Calentamiento",
                    if (tipoMedicion == "distancia") "%.2f km".format(totalCal) else "%.2f min".format(totalCal))
                val markerCentral = PhaseMarkerView(context, "Central",
                    if (tipoMedicion == "distancia") "%.2f km".format(totalCentral) else "%.2f min".format(totalCentral))
                val markerVuelta = PhaseMarkerView(context, "Vuelta a la Calma",
                    if (tipoMedicion == "distancia") "%.2f km".format(totalVuelta) else "%.2f min".format(totalVuelta))

                setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        val x = e?.x ?: 0f
                        marker = when {
                            x < (off1).toFloat() -> markerCal
                            x < (offset).toFloat() -> markerCentral
                            else -> markerVuelta
                        }
                    }

                    override fun onNothingSelected() {}
                })

                legend.textColor = Color.DKGRAY
                legend.textSize = 13f
                legend.isWordWrapEnabled = true

                invalidate()
            }
        }
    )
}
