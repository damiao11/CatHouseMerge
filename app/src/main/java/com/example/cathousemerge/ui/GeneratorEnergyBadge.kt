package com.example.cathousemerge.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun GeneratorEnergyBadge(blocked: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.testTag(if (blocked) "generator_no_energy" else "generator_energy_badge")) {
        val w = size.width
        val h = size.height
        if (!blocked) drawCircle(Color(0xFFF5C242).copy(alpha = 0.18f), radius = w * 0.57f)
        val bolt = Path().apply {
            moveTo(w * 0.58f, h * 0.05f); lineTo(w * 0.19f, h * 0.55f)
            lineTo(w * 0.44f, h * 0.55f); lineTo(w * 0.36f, h * 0.95f)
            lineTo(w * 0.82f, h * 0.40f); lineTo(w * 0.56f, h * 0.40f); close()
        }
        drawPath(bolt, Color.White, style = Stroke(3.dp.toPx(), join = StrokeJoin.Round))
        drawPath(bolt, if (blocked) Color(0xFF9A948B) else Color(0xFFF5C242))
        if (blocked) drawLine(Ink, Offset(w * 0.13f, h * 0.13f), Offset(w * 0.87f, h * 0.87f), 2.dp.toPx(), StrokeCap.Round)
    }
}
