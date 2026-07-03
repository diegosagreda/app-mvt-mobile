package com.example.mvt.ui.components.chart

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mvt.data.firebase.models.Routine
import com.example.mvt.ui.theme.AppBorder
import com.example.mvt.ui.theme.AppSurface
import com.example.mvt.ui.theme.AppSurfaceAlt
import com.example.mvt.ui.theme.AppTextPrimary
import com.example.mvt.ui.theme.AppTextSecondary
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

private enum class ChartPhase {
    CALENTAMIENTO,
    CENTRAL,
    CALMA
}

private enum class ChartEffortType {
    SENSACIONES,
    FC,
    RITMOS
}

private data class FlattenedRoutineBlock(
    val phase: ChartPhase,
    val tipo: String,
    val intensidad: String,
    val distanciaKm: Double,
    val tiempoMin: Double
)

private data class ChartSample(
    val x: Float,
    val y: Float,
    val advance: Float,
    val phase: ChartPhase,
    val tipo: String,
    val intensidadRaw: String,
    val intensidadIndex: Int,
    val distanciaKm: Double,
    val tiempoMin: Double,
    val ultimoBloque: Boolean
)

private data class YAxisTick(
    val value: Float,
    val label: String,
    val compactLabel: String
)

private data class ChartModel(
    val effortType: ChartEffortType,
    val points: List<ChartSample>,
    val maxX: Float,
    val maxY: Float,
    val maxYTickValue: Float,
    val totalCalentamiento: Double,
    val totalCentral: Double,
    val totalCalma: Double,
    val visualCalentamiento: Float,
    val visualCentral: Float,
    val visualCalma: Float,
    val yAxisTitle: String,
    val xAxisTitle: String,
    val yTicks: List<YAxisTick>,
    val ritmos: Map<String, String>,
    val zonas: Map<String, String>
)

private val defaultZonas = mapOf(
    "z0min" to "80",
    "z0max" to "113",
    "z1min" to "114",
    "z1max" to "125",
    "z2min" to "126",
    "z2max" to "138",
    "z3min" to "139",
    "z3max" to "150",
    "z4min" to "151",
    "z4max" to "163",
    "z5min" to "164",
    "z5max" to "175"
)

private val defaultRitmos = mapOf(
    "r0min" to "12:05",
    "r0max" to "10:05",
    "r1min" to "10:04",
    "r1max" to "8:44",
    "r2min" to "8:44",
    "r2max" to "7:43",
    "r3min" to "7:42",
    "r3max" to "6:54",
    "r3pmin" to "6:53",
    "r3pmax" to "6:15",
    "r4min" to "6:14",
    "r4max" to "5:28",
    "r5min" to "5:27",
    "r5max" to "4:41",
    "r6min" to "4:40",
    "r6max" to "0"
)

private val sensationLabels = listOf(
    "Nada",
    "Muy muy suave",
    "Muy suave",
    "Suave",
    "No tan suave",
    "Moderado",
    "No tan fuerte",
    "Medianamente fuerte",
    "Fuerte",
    "Muy fuerte",
    "Muy muy fuerte"
)

private val warmPhaseColor = Color(0xFF0066CC)
private val centralPhaseColor = Color(0xFF004678)
private val calmPhaseColor = Color(0xFF1975D1)

private data class ChartBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(1f)
    val height: Float get() = (bottom - top).coerceAtLeast(1f)
}

@Composable
fun RoutineChart(
    routine: Routine,
    ritmos: Map<String, Any>? = null,
    zonas: Map<String, Any>? = null,
    modifier: Modifier = Modifier
) {
    val chartModel = remember(routine, ritmos, zonas) {
        buildChartModel(routine, ritmos, zonas)
    }
    var selectedSample by remember(chartModel) { mutableStateOf<ChartSample?>(null) }
    var selectedYAxisTick by remember(chartModel) { mutableStateOf<YAxisTick?>(null) }
    val density = LocalDensity.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val boxWidthPx = with(density) { maxWidth.toPx() }
            val boxHeightPx = with(density) { maxHeight.toPx() }
            val chartArea = remember(boxWidthPx, boxHeightPx) {
                chartBounds(boxWidthPx, boxHeightPx)
            }
            val yAxisTooltipOffset = selectedYAxisTick?.let { tick ->
                val targetY = chartYToCanvasY(tick.value, chartModel.maxY, chartArea)
                with(density) { (targetY - 28f).toDp() }
                    .coerceIn(20.dp, (maxHeight - 88.dp).coerceAtLeast(20.dp))
            } ?: 0.dp

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(chartModel) {
                        detectTapGestures { tapOffset ->
                            val bounds = chartBounds(size.width.toFloat(), size.height.toFloat())
                            val tappedYAxis = tapOffset.x in 0f..bounds.left &&
                                tapOffset.y in bounds.top..bounds.bottom

                            if (tappedYAxis) {
                                selectedYAxisTick = chartModel.yTicks.minByOrNull { tick ->
                                    abs(chartYToCanvasY(tick.value, chartModel.maxY, bounds) - tapOffset.y)
                                }
                                selectedSample = null
                                return@detectTapGestures
                            }

                            if (tapOffset.x < bounds.left || tapOffset.x > bounds.right ||
                                tapOffset.y < bounds.top || tapOffset.y > bounds.bottom
                            ) {
                                selectedSample = null
                                selectedYAxisTick = null
                                return@detectTapGestures
                            }

                            val xValue = (((tapOffset.x - bounds.left) / bounds.width) * chartModel.maxX)
                                .coerceIn(0f, chartModel.maxX)

                            selectedSample = chartModel.points
                                .filterNot {
                                    chartModel.xAxisTitle == "Km" && it.ultimoBloque
                                }
                                .minByOrNull { abs(it.x - xValue) }
                            selectedYAxisTick = null
                        }
                    }
            ) {
                val bounds = chartBounds(size.width, size.height)
                drawRoutineChart(bounds, chartModel, selectedSample, selectedYAxisTick)
            }

            selectedSample?.let { sample ->
                val tooltipLines = buildTooltipLines(chartModel, sample)
                if (tooltipLines.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = AppSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder.copy(alpha = 0.8f))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            tooltipLines.forEachIndexed { index, line ->
                                Text(
                                    text = line,
                                    color = if (index == 0) AppTextPrimary else AppTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = selectedYAxisTick != null,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 10.dp, y = yAxisTooltipOffset),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                selectedYAxisTick?.let { tick ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = AppSurfaceAlt.copy(alpha = 0.96f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder.copy(alpha = 0.7f))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Text(
                                text = chartModel.yAxisTitle,
                                color = AppTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = tick.label,
                                color = AppTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        ChartPhaseLegend(
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ChartPhaseLegend(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = AppSurfaceAlt.copy(alpha = 0.58f),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ChartPhaseLegendItem(
                label = "Calentamiento",
                color = warmPhaseColor,
                modifier = Modifier.weight(1f)
            )
            ChartPhaseLegendItem(
                label = "Central",
                color = centralPhaseColor,
                modifier = Modifier.weight(1f)
            )
            ChartPhaseLegendItem(
                label = "Vuelta a la calma",
                color = calmPhaseColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ChartPhaseLegendItem(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = AppSurface.copy(alpha = 0.72f),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder.copy(alpha = 0.62f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, RoundedCornerShape(999.dp))
            )
            Text(
                text = label,
                color = AppTextPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee()
            )
        }
    }
}

private fun chartBounds(width: Float, height: Float): ChartBounds {
    val left = 118f
    val top = 30f
    val right = width - 22f
    val bottom = height - 44f
    return ChartBounds(left = left, top = top, right = right, bottom = bottom)
}

private fun buildChartModel(
    routine: Routine,
    ritmos: Map<String, Any>?,
    zonas: Map<String, Any>?
): ChartModel {
    val normalizedRitmos = normalizeProfileMap(ritmos, defaultRitmos)
    val normalizedZonas = normalizeProfileMap(zonas, defaultZonas)
    val effortType = resolveEffortType(routine.tipo_esfuerzo)
    val isDistance = routine.tipo_medicion.equals("distancia", ignoreCase = true)

    val calentamiento = routine.sesiones_calentamiento.orEmpty()
        .map { toFlattenedBlock(it, ChartPhase.CALENTAMIENTO) }
    val central = flattenCentralBlocks(routine.sesiones_central)
    val calma = routine.sesiones_calma.orEmpty()
        .map { toFlattenedBlock(it, ChartPhase.CALMA) }

    val filteredBlocks = (calentamiento + central + calma).filter { block ->
        if (!isDistance) {
            true
        } else {
            block.tipo !in listOf("Flexibilidad", "Movilidad Articular", "Fortalecimiento")
        }
    }

    val totalCalentamiento = filteredBlocks
        .filter { it.phase == ChartPhase.CALENTAMIENTO }
        .sumOf { chartBlockQuantity(it, isDistance) }
    val totalCentral = filteredBlocks
        .filter { it.phase == ChartPhase.CENTRAL }
        .sumOf { chartBlockQuantity(it, isDistance) }
    val totalCalma = filteredBlocks
        .filter { it.phase == ChartPhase.CALMA }
        .sumOf { chartBlockQuantity(it, isDistance) }

    val samples = mutableListOf<ChartSample>()
    var cumulativeX = 0.0
    var maxDetectedIntensity = 0
    var visualCalentamiento = 0f
    var visualCentral = 0f
    var visualCalma = 0f

    filteredBlocks.forEach { block ->
        val intensityIndex = intensityIndexFor(block.intensidad, effortType)
        maxDetectedIntensity = max(maxDetectedIntensity, intensityIndex)
        val segments = buildMicroSegments(block, isDistance)

        segments.forEachIndexed { index, segment ->
            cumulativeX += segment
            if (totalCalma == 0.0) {
                cumulativeX += 0.01
            }
            val segmentAdvance = segment.toFloat()
            when (block.phase) {
                ChartPhase.CALENTAMIENTO -> visualCalentamiento += segmentAdvance
                ChartPhase.CENTRAL -> visualCentral += segmentAdvance
                ChartPhase.CALMA -> visualCalma += segmentAdvance
            }
            val x = ((cumulativeX * 100.0).roundToInt() / 100.0).toFloat()
            samples += ChartSample(
                x = x,
                y = chartYValue(intensityIndex, effortType),
                advance = segmentAdvance,
                phase = block.phase,
                tipo = block.tipo,
                intensidadRaw = block.intensidad,
                intensidadIndex = intensityIndex,
                distanciaKm = block.distanciaKm,
                tiempoMin = block.tiempoMin,
                ultimoBloque = isDistance && block.distanciaKm < 0.1 && index == segments.lastIndex
            )
        }
    }

    val maxY = when (effortType) {
        ChartEffortType.SENSACIONES -> maxDetectedIntensity.coerceAtLeast(1).toFloat()
        ChartEffortType.FC -> (maxDetectedIntensity + 1).coerceIn(1, 6).toFloat()
        ChartEffortType.RITMOS -> (maxDetectedIntensity + 1).coerceIn(1, 7).toFloat()
    }
    val maxYTickValue = when (effortType) {
        ChartEffortType.SENSACIONES -> maxDetectedIntensity.toFloat()
        ChartEffortType.FC -> (maxDetectedIntensity + 1).coerceIn(1, 6).toFloat()
        ChartEffortType.RITMOS -> (maxDetectedIntensity + 1).coerceIn(1, 7).toFloat()
    }

    val yTicks = buildYAxisTicks(effortType, maxYTickValue, normalizedZonas, normalizedRitmos)

    return ChartModel(
        effortType = effortType,
        points = samples,
        maxX = samples.lastOrNull()?.x ?: 1f,
        maxY = maxY,
        maxYTickValue = maxYTickValue,
        totalCalentamiento = totalCalentamiento,
        totalCentral = totalCentral,
        totalCalma = totalCalma,
        visualCalentamiento = visualCalentamiento,
        visualCentral = visualCentral,
        visualCalma = visualCalma,
        yAxisTitle = when (effortType) {
            ChartEffortType.SENSACIONES -> "Esfuerzo"
            ChartEffortType.FC -> "FC(ppm)"
            ChartEffortType.RITMOS -> "Ritmos (min/Km)"
        },
        xAxisTitle = if (isDistance) "Km" else "min",
        yTicks = yTicks,
        ritmos = normalizedRitmos,
        zonas = normalizedZonas
    )
}

private fun DrawScope.drawRoutineChart(
    bounds: ChartBounds,
    model: ChartModel,
    selectedSample: ChartSample?,
    selectedYAxisTick: YAxisTick?
) {
    val dummyPoint = ChartSample(
        x = 0f,
        y = 0f,
        advance = 0f,
        phase = ChartPhase.CALENTAMIENTO,
        tipo = "",
        intensidadRaw = "",
        intensidadIndex = 0,
        distanciaKm = 0.0,
        tiempoMin = 0.0,
        ultimoBloque = false
    )
    val allPoints = listOf(dummyPoint) + model.points

    val labelPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#A2A5B9")
        textSize = 24f
    }

    val titlePaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#D9E1F2")
        textSize = 24f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    val toCanvasX: (Float) -> Float = { x ->
        bounds.left + (x / model.maxX.coerceAtLeast(0.01f)) * bounds.width
    }
    val toCanvasY: (Float) -> Float = { y ->
        bounds.bottom - (y / model.maxY.coerceAtLeast(1f)) * bounds.height
    }

    val fillPath = Path()
    val linePath = Path()

    val first = allPoints.first()
    val firstX = toCanvasX(first.x)
    val firstY = toCanvasY(first.y)

    fillPath.moveTo(firstX, bounds.bottom)
    fillPath.lineTo(firstX, firstY)
    linePath.moveTo(firstX, firstY)

    for (index in 1 until allPoints.size) {
        val current = allPoints[index]
        val previous = allPoints[index - 1]
        val previousX = toCanvasX(previous.x)
        val currentX = toCanvasX(current.x)
        val currentY = toCanvasY(current.y)

        linePath.lineTo(previousX, currentY)
        linePath.lineTo(currentX, currentY)

        fillPath.lineTo(previousX, currentY)
        fillPath.lineTo(currentX, currentY)
    }

    val lastX = toCanvasX(allPoints.last().x)
    fillPath.lineTo(lastX, bounds.bottom)
    fillPath.close()

    val fillBrush = Brush.horizontalGradient(
        colorStops = buildPhaseColorStops(model),
        startX = bounds.left,
        endX = bounds.right
    )

    drawPath(path = fillPath, brush = fillBrush, style = Fill)
    drawPath(
        path = linePath,
        color = Color(0xFF84B6F4),
        style = Stroke(width = 2f, cap = StrokeCap.Round)
    )

    selectedSample?.let { sample ->
        val guideX = toCanvasX(sample.x)
        drawLine(
            color = Color.White.copy(alpha = 0.24f),
            start = Offset(guideX, bounds.top),
            end = Offset(guideX, bounds.bottom),
            strokeWidth = 1.5f
        )
    }

    selectedYAxisTick?.let { tick ->
        val guideY = toCanvasY(tick.value)
        drawLine(
            color = Color.White.copy(alpha = 0.16f),
            start = Offset(bounds.left, guideY),
            end = Offset(bounds.right, guideY),
            strokeWidth = 1.5f
        )
    }

    drawContext.canvas.nativeCanvas.apply {
        labelPaint.textAlign = Paint.Align.RIGHT
        model.yTicks.forEach { tick ->
            labelPaint.color = if (tick == selectedYAxisTick) {
                android.graphics.Color.parseColor("#F4F7FF")
            } else {
                android.graphics.Color.parseColor("#A2A5B9")
            }
            val y = toCanvasY(tick.value) + 8f
            drawText(tick.compactLabel, bounds.left - 12f, y, labelPaint)
        }

        titlePaint.textAlign = Paint.Align.LEFT
        drawText(model.yAxisTitle, bounds.left, bounds.top - 18f, titlePaint)

        labelPaint.textAlign = Paint.Align.CENTER
        buildXAxisTicks(model.maxX).forEach { tick ->
            val x = toCanvasX(tick)
            drawText(String.format("%.1f", tick), x, bounds.bottom + 28f, labelPaint)
        }

        titlePaint.textAlign = Paint.Align.CENTER
        drawText(model.xAxisTitle, bounds.left + (bounds.width / 2f), bounds.bottom + 54f, titlePaint)
    }
}

private fun buildPhaseColorStops(model: ChartModel): Array<Pair<Float, Color>> {
    val epsilon = 0.0025f

    val warmVisual = if (model.visualCalentamiento == 0f) 0.01f else model.visualCalentamiento
    val calmVisual = if (model.visualCalma == 0f) 0.01f else model.visualCalma
    val centralVisual = model.visualCentral.coerceAtLeast(0f)
    val total = (warmVisual + centralVisual + calmVisual).coerceAtLeast(0.01f)

    val warmEnd = (warmVisual / total).coerceIn(0f, 1f)
    val centralEnd = ((warmVisual + centralVisual) / total).coerceIn(warmEnd, 1f)
    val warmEdge = (warmEnd + epsilon).coerceIn(0f, 1f)
    val centralEdge = (centralEnd + epsilon).coerceIn(0f, 1f)

    return arrayOf(
        0f to warmPhaseColor,
        warmEnd.coerceAtLeast(0.01f) to warmPhaseColor,
        warmEdge to centralPhaseColor,
        centralEnd.coerceAtLeast(warmEdge) to centralPhaseColor,
        centralEdge to calmPhaseColor,
        1f to calmPhaseColor
    )
}

private fun buildXAxisTicks(maxX: Float): List<Float> {
    if (maxX <= 0f) return listOf(0f)

    val niceStep = niceStep(maxX)
    val ticks = mutableListOf<Float>()
    var current = 0f

    while (current < maxX) {
        ticks += ((current * 10f).roundToInt() / 10f)
        current += niceStep
    }

    val lastTick = ((maxX * 10f).roundToInt() / 10f)
    if (ticks.isEmpty() || ticks.last() != lastTick) {
        ticks += lastTick
    }

    return ticks.distinct()
}

private fun niceStep(maxX: Float): Float {
    val targetSegments = 4f
    val rough = (maxX / targetSegments).coerceAtLeast(1f)
    val magnitude = 10.0.pow(kotlin.math.floor(kotlin.math.log10(rough.toDouble()))).toFloat()
    val normalized = rough / magnitude

    val niceNormalized = when {
        normalized <= 1f -> 1f
        normalized <= 2f -> 2f
        normalized <= 2.5f -> 2.5f
        normalized <= 5f -> 5f
        else -> 10f
    }

    return niceNormalized * magnitude
}

private fun buildTooltipLines(
    model: ChartModel,
    sample: ChartSample
): List<String> {
    if (model.xAxisTitle == "Km" && sample.ultimoBloque) {
        return emptyList()
    }

    val primary = if (model.xAxisTitle == "Km") {
        "Distancia: ${formatDistance(sample.distanciaKm)}"
    } else {
        "Tiempo: ${formatDurationMinutes(sample.tiempoMin)}"
    }

    val intensity = when (model.effortType) {
        ChartEffortType.SENSACIONES -> sensationText(sample.intensidadIndex)
        ChartEffortType.FC -> {
            val zoneKey = "z${sample.intensidadIndex.coerceIn(0, 5)}"
            "${model.zonas["${zoneKey}min"] ?: "-"} - ${model.zonas["${zoneKey}max"] ?: "-"}"
        }
        ChartEffortType.RITMOS -> {
            val key = rhythmKeyForIndex(sample.intensidadIndex)
            "${model.ritmos["${key}max"] ?: "-"} - ${model.ritmos["${key}min"] ?: "-"}"
        }
    }

    return listOf(
        primary,
        "Intensidad: $intensity",
        "Tipo: ${sample.tipo}"
    )
}

private fun buildYAxisTicks(
    effortType: ChartEffortType,
    maxYTickValue: Float,
    zonas: Map<String, String>,
    ritmos: Map<String, String>
): List<YAxisTick> {
    val ticks = when (effortType) {
        ChartEffortType.SENSACIONES -> {
            (0..maxYTickValue.toInt()).map { value ->
                val label = sensationText(value)
                YAxisTick(
                    value = value.toFloat(),
                    label = label,
                    compactLabel = compactSensationLabel(label)
                )
            }
        }
        ChartEffortType.FC -> {
            listOf(
                YAxisTick(0f, zonas["z0min"] ?: "-", zonas["z0min"] ?: "-"),
                YAxisTick(1f, zonas["z1min"] ?: "-", zonas["z1min"] ?: "-"),
                YAxisTick(2f, zonas["z2min"] ?: "-", zonas["z2min"] ?: "-"),
                YAxisTick(3f, zonas["z3min"] ?: "-", zonas["z3min"] ?: "-"),
                YAxisTick(4f, zonas["z4min"] ?: "-", zonas["z4min"] ?: "-"),
                YAxisTick(5f, zonas["z5min"] ?: "-", zonas["z5min"] ?: "-"),
                YAxisTick(6f, zonas["z5max"] ?: "-", zonas["z5max"] ?: "-")
            )
        }
        ChartEffortType.RITMOS -> {
            listOf(
                YAxisTick(0f, ritmos["r0max"] ?: "-", ritmos["r0max"] ?: "-"),
                YAxisTick(1f, ritmos["r1max"] ?: "-", ritmos["r1max"] ?: "-"),
                YAxisTick(2f, ritmos["r2max"] ?: "-", ritmos["r2max"] ?: "-"),
                YAxisTick(3f, ritmos["r3max"] ?: "-", ritmos["r3max"] ?: "-"),
                YAxisTick(4f, ritmos["r3pmax"] ?: "-", ritmos["r3pmax"] ?: "-"),
                YAxisTick(5f, ritmos["r4max"] ?: "-", ritmos["r4max"] ?: "-"),
                YAxisTick(6f, ritmos["r5max"] ?: "-", ritmos["r5max"] ?: "-"),
                YAxisTick(7f, ritmos["r6max"] ?: "-", ritmos["r6max"] ?: "-")
            )
        }
    }

    return ticks.filter { it.value <= maxYTickValue }
}

private fun buildMicroSegments(
    block: FlattenedRoutineBlock,
    isDistance: Boolean
): List<Double> {
    return if (isDistance) {
        if (block.distanciaKm < 0.1) {
            List(max(ceil(block.distanciaKm * 100).toInt(), 1)) { 0.01 }
        } else {
            List(max(ceil(block.distanciaKm * 10).toInt(), 1)) { 0.1 }
        }
    } else {
        List(max(ceil(block.tiempoMin * 10).toInt(), 1)) { 0.1 }
    }
}

private fun chartYValue(
    intensityIndex: Int,
    effortType: ChartEffortType
): Float {
    return when (effortType) {
        ChartEffortType.SENSACIONES -> {
            val raw = intensityIndex.toFloat()
            if (raw == 0f) 0.3f else raw
        }
        ChartEffortType.FC -> {
            val value = intensityIndex + 0.9f
            if (value == 0f) 0.2f else value
        }
        ChartEffortType.RITMOS -> {
            val raw = intensityIndex.toFloat()
            if (raw == 0f) 0.2f else raw
        }
    }
}

private fun chartBlockQuantity(block: FlattenedRoutineBlock, isDistance: Boolean): Double {
    return if (isDistance) block.distanciaKm else block.tiempoMin
}

private fun resolveEffortType(tipoEsfuerzo: String): ChartEffortType {
    val key = tipoEsfuerzo.trim().lowercase()
    return when {
        key.contains("sens") -> ChartEffortType.SENSACIONES
        key.contains("fc") -> ChartEffortType.FC
        key.contains("rit") -> ChartEffortType.RITMOS
        else -> ChartEffortType.SENSACIONES
    }
}

private fun intensityIndexFor(
    intensidad: String,
    effortType: ChartEffortType
): Int {
    val normalized = intensidad.trim().uppercase()
    return when (effortType) {
        ChartEffortType.SENSACIONES -> normalized.toIntOrNull() ?: 0
        ChartEffortType.FC -> when (normalized) {
            "Z0" -> 0
            "Z1" -> 1
            "Z2" -> 2
            "Z3" -> 3
            "Z4" -> 4
            "Z5" -> 5
            else -> 0
        }
        ChartEffortType.RITMOS -> when (normalized) {
            "R0" -> 0
            "R1" -> 1
            "R2" -> 2
            "R3" -> 3
            "R3+" , "R3P" -> 4
            "R4" -> 5
            "R5" -> 6
            "R6" -> 7
            else -> 0
        }
    }
}

private fun flattenCentralBlocks(
    central: Map<String, Any>?
): List<FlattenedRoutineBlock> {
    val series = (central?.get("series") as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()
    val result = mutableListOf<FlattenedRoutineBlock>()

    series.forEach { serie ->
        val sesiones = (serie["sesiones"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()
        val inicio = asInt(serie["inicio"], -1)
        val fin = asInt(serie["final"], -1)
        val repeticiones = max(asInt(serie["repeticiones"], 1), 1)

        if (inicio != -1 && fin != -1 && inicio <= fin && inicio in sesiones.indices && fin in sesiones.indices) {
            val repetible = sesiones.subList(inicio, fin + 1)
            repeat(repeticiones) {
                repetible.forEach { result += toFlattenedBlock(it, ChartPhase.CENTRAL) }
            }
            sesiones.subList(0, inicio).forEach { result += toFlattenedBlock(it, ChartPhase.CENTRAL) }
            sesiones.subList(fin + 1, sesiones.size).forEach { result += toFlattenedBlock(it, ChartPhase.CENTRAL) }
        } else {
            sesiones.forEach { result += toFlattenedBlock(it, ChartPhase.CENTRAL) }
        }
    }

    return result
}

private fun toFlattenedBlock(
    raw: Map<String, Any>,
    phase: ChartPhase
): FlattenedRoutineBlock {
    return FlattenedRoutineBlock(
        phase = phase,
        tipo = raw["tipo"]?.toString().orEmpty(),
        intensidad = raw["intensidad"]?.toString().orEmpty(),
        distanciaKm = blockDistanceKm(raw),
        tiempoMin = blockTimeMinutes(raw)
    )
}

private fun blockDistanceKm(raw: Map<String, Any>): Double {
    val distanceValue = numberValue(raw["distancia"])
    val unit = raw["tipo_medicion"]?.toString() ?: raw["medicion"]?.toString().orEmpty()
    return if (unit == "Kilometros") distanceValue else distanceValue / 1000.0
}

private fun blockTimeMinutes(raw: Map<String, Any>): Double {
    val minutes = numberValue(raw["duracion_min"])
    val seconds = numberValue(raw["duracion_seg"])
    return minutes + (seconds / 60.0)
}

private fun numberValue(value: Any?): Double {
    return when (value) {
        is Number -> value.toDouble()
        is String -> value.replace(",", ".").toDoubleOrNull() ?: 0.0
        else -> 0.0
    }
}

private fun asInt(value: Any?, fallback: Int): Int {
    return when (value) {
        is Number -> value.toInt()
        is String -> value.toIntOrNull() ?: fallback
        else -> fallback
    }
}

private fun normalizeProfileMap(
    source: Map<String, Any>?,
    defaults: Map<String, String>
): Map<String, String> {
    val merged = defaults.toMutableMap()
    source.orEmpty().forEach { (key, value) ->
        merged[normalizeProfileKey(key)] = value.toString()
    }
    return merged
}

private fun normalizeProfileKey(key: String): String {
    return key.trim()
        .lowercase()
        .replace("+", "p")
        .replace("á", "a")
        .replace("é", "e")
        .replace("í", "i")
        .replace("ó", "o")
        .replace("ú", "u")
}

private fun rhythmKeyForIndex(index: Int): String {
    return when (index) {
        0 -> "r0"
        1 -> "r1"
        2 -> "r2"
        3 -> "r3"
        4 -> "r3p"
        5 -> "r4"
        6 -> "r5"
        else -> "r6"
    }
}

private fun sensationText(index: Int): String {
    return sensationLabels.getOrElse(index.coerceIn(0, sensationLabels.lastIndex)) { "-" }
}

private fun compactSensationLabel(label: String): String {
    return when (label) {
        "Muy muy suave" -> "MM suave"
        "Muy suave" -> "M suave"
        "No tan suave" -> "N suave"
        "Medianamente fuerte" -> "Med fuerte"
        "Muy fuerte" -> "M fuerte"
        "Muy muy fuerte" -> "MM fuerte"
        "No tan fuerte" -> "N fuerte"
        else -> label
    }
}

private fun chartYToCanvasY(
    y: Float,
    maxY: Float,
    bounds: ChartBounds
): Float {
    return bounds.bottom - (y / maxY.coerceAtLeast(1f)) * bounds.height
}

private fun formatDistance(distanceKm: Double): String {
    return if (distanceKm > 0.99) {
        val text = String.format("%.2f", distanceKm)
        "${text.trimEnd('0').trimEnd('.')} km"
    } else {
        "${(distanceKm * 1000.0).roundToInt()} m"
    }
}

private fun formatDurationMinutes(minutes: Double): String {
    val totalSeconds = (minutes * 60.0).roundToInt()
    val hours = totalSeconds / 3600
    val remainingMinutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hours, remainingMinutes, seconds)
}
