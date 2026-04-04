package pg.geobingo.one.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pg.geobingo.one.ui.theme.ColorOnSurface
import pg.geobingo.one.ui.theme.ColorOnSurfaceVariant
import pg.geobingo.one.ui.theme.ColorSurface
import pg.geobingo.one.ui.theme.ColorSurfaceContainerHigh

// ─────────────────────────────────────────────────────────────────────────────
//  Mini Bar Chart — horizontal bars with label left, value right
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MiniBarChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF6366F1),
    maxBars: Int = 5,
) {
    val displayData = data.take(maxBars)
    if (displayData.isEmpty()) return

    val maxValue = displayData.maxOfOrNull { it.second }?.takeIf { it > 0f } ?: 1f

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        displayData.forEach { (label, value) ->
            val fraction = (value / maxValue).coerceIn(0f, 1f)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Label — fixed width so bars are aligned
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorOnSurfaceVariant,
                    modifier = Modifier.width(88.dp),
                    maxLines = 1,
                )

                // Bar track + filled bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(ColorSurfaceContainerHigh, RoundedCornerShape(4.dp)),
                ) {
                    if (fraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .background(barColor, RoundedCornerShape(4.dp)),
                        )
                    }
                }

                // Value label
                Text(
                    text = formatBarValue(value),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = ColorOnSurface,
                    modifier = Modifier.width(44.dp),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

private fun formatBarValue(value: Float): String {
    return if (value == value.toLong().toFloat()) {
        // Whole number — show without decimal (e.g. "75")
        value.toLong().toString()
    } else {
        // One decimal place (e.g. "3.4")
        val rounded = (value * 10).toInt()
        "${rounded / 10}.${rounded % 10}"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Pie / Donut Chart — drawArc segments with legend below
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PieChart(
    segments: List<Pair<String, Float>>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    if (segments.isEmpty()) return

    val total = segments.sumOf { it.second.toDouble() }.toFloat().takeIf { it > 0f } ?: return
    val strokeWidth = 28f
    val gapDegrees = 3f

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Donut chart canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
        ) {
            val diameter = minOf(size.width, size.height) - strokeWidth
            val topLeft = Offset(
                x = (size.width - diameter) / 2f,
                y = (size.height - diameter) / 2f,
            )
            val arcSize = Size(diameter, diameter)

            var startAngle = -90f

            segments.forEachIndexed { index, (_, value) ->
                val sweep = (value / total) * (360f - gapDegrees * segments.size)
                val color = colors.getOrElse(index) { Color.Gray }

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )

                startAngle += sweep + gapDegrees
            }
        }

        // Legend — two columns, walk through segments with a flat index
        val indexed = segments.mapIndexed { i, seg -> Triple(i, seg.first, seg.second) }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            indexed.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    pair.forEach { (globalIndex, label, value) ->
                        val color = colors.getOrElse(globalIndex) { Color.Gray }
                        val pct = ((value / total) * 100).toInt()
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(color, RoundedCornerShape(2.dp)),
                            )
                            Text(
                                text = "$label ($pct%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorOnSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                    // Pad last row if odd count
                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Mini Line Chart (Sparkline) — filled area below the line, no axis labels
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MiniLineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF6366F1),
    fillColor: Color = Color(0x336366F1),
) {
    if (data.size < 2) return

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minVal = data.min()
        val maxVal = data.max()
        val range = (maxVal - minVal).takeIf { it > 0f } ?: 1f

        fun xAt(i: Int) = i.toFloat() / (data.size - 1) * w
        fun yAt(v: Float) = h - ((v - minVal) / range) * (h * 0.85f) - h * 0.07f

        // Build line path
        val linePath = Path()
        data.forEachIndexed { i, v ->
            val x = xAt(i)
            val y = yAt(v)
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }

        // Build fill path (close below the line)
        val fillPath = Path()
        fillPath.addPath(linePath)
        fillPath.lineTo(w, h)
        fillPath.lineTo(0f, h)
        fillPath.close()

        drawPath(fillPath, color = fillColor)

        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(
                width = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        // Dot at end
        val lastX = xAt(data.size - 1)
        val lastY = yAt(data.last())
        drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(lastX, lastY))
        drawCircle(color = ColorSurface, radius = 2.dp.toPx(), center = Offset(lastX, lastY))
    }
}
