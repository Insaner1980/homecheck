package com.finnvek.homecheck.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

@Composable
fun HomeStatusIllustration(allClear: Boolean, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(if (allClear) 1f else 0.72f, tween(420), label = "statusCheck")
    val color = if (allClear) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    Canvas(modifier.clearAndSetSemantics { }.size(112.dp)) {
        val stroke = size.minDimension * 0.055f
        val home = Path().apply {
            moveTo(size.width * 0.16f, size.height * 0.48f)
            lineTo(size.width * 0.5f, size.height * 0.17f)
            lineTo(size.width * 0.84f, size.height * 0.48f)
            lineTo(size.width * 0.84f, size.height * 0.82f)
            lineTo(size.width * 0.23f, size.height * 0.82f)
            lineTo(size.width * 0.23f, size.height * 0.53f)
        }
        drawPath(home, muted, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val a = Offset(size.width * 0.38f, size.height * 0.57f)
        val b = Offset(size.width * 0.5f, size.height * 0.69f)
        val c = Offset(size.width * 0.75f, size.height * 0.38f)
        if (progress <= 0.5f) {
            drawLine(color, a, interpolate(a, b, progress * 2f), stroke, StrokeCap.Round)
        } else {
            drawLine(color, a, b, stroke, StrokeCap.Round)
            drawLine(color, b, interpolate(b, c, (progress - 0.5f) * 2f), stroke, StrokeCap.Round)
        }
    }
}

private fun interpolate(start: Offset, end: Offset, fraction: Float) = Offset(
    x = start.x + (end.x - start.x) * fraction,
    y = start.y + (end.y - start.y) * fraction,
)
