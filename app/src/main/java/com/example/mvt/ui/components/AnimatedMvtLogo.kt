package com.example.mvt.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.mvt.R

private val TracePoints = listOf(
    Offset(0.02f, 0.88f),
    Offset(0.15f, 0.38f),
    Offset(0.30f, 0.72f),
    Offset(0.44f, 0.31f),
    Offset(0.61f, 0.77f),
    Offset(0.73f, 0.25f),
    Offset(0.79f, 0.18f),
    Offset(0.98f, 0.08f)
)

@Composable
fun AnimatedMvtLogo(
    progress: Float,
    modifier: Modifier = Modifier,
    contentDescription: String = "Logo My Virtual Trainer"
) {
    val revealProgress = progress.coerceIn(0f, 1f)

    Image(
        painter = painterResource(id = R.drawable.mvt),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier.drawWithContent {
            if (revealProgress <= 0f) return@drawWithContent

            val revealX = size.width * revealProgress
            clipRect(right = revealX) {
                this@drawWithContent.drawContent()
            }

            if (revealProgress < 0.99f) {
                val tracePosition = tracePositionAt(revealProgress)
                val lightCenter = Offset(
                    x = revealX.coerceIn(0f, size.width),
                    y = size.height * tracePosition.y
                )
                val baseRadius = size.minDimension * 0.045f

                drawCircle(
                    color = Color.White.copy(alpha = 0.10f),
                    radius = baseRadius * 2.4f,
                    center = lightCenter
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.28f),
                    radius = baseRadius,
                    center = lightCenter
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.90f),
                    radius = baseRadius * 0.28f,
                    center = lightCenter
                )
            }
        }
    )
}

private fun tracePositionAt(progress: Float): Offset {
    val rightIndex = TracePoints.indexOfFirst { it.x >= progress }
        .takeIf { it >= 0 }
        ?: return TracePoints.last()
    if (rightIndex == 0) return TracePoints.first()

    val start = TracePoints[rightIndex - 1]
    val end = TracePoints[rightIndex]
    val segmentProgress = ((progress - start.x) / (end.x - start.x)).coerceIn(0f, 1f)
    val smoothProgress = segmentProgress * segmentProgress * (3f - 2f * segmentProgress)

    return Offset(
        x = progress,
        y = start.y + (end.y - start.y) * smoothProgress
    )
}
