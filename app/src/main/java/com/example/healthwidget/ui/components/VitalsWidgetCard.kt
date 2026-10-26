package com.example.healthwidget.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthwidget.model.VitalMetric
import com.example.healthwidget.model.VitalsData

@Composable
fun VitalsWidgetCard(
    modifier: Modifier = Modifier,
    vitalsData: VitalsData = remember { VitalsData.mock() },
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(vitalsData.metrics, key = { it.id }) { metric ->
            VitalCardItem(
                metric = metric,
                modifier = Modifier.width(340.dp)
            )
        }
    }
}

@Composable
fun VitalCardItem(
    metric: VitalMetric,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFF141416), shape = RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Title
            Text(
                text = metric.title,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Body: Left stats + Right trend graph
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Values
                Column(
                    modifier = Modifier.weight(0.35f)
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = metric.valueText,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = metric.unitText,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = metric.statusText,
                        style = TextStyle(
                            color = Color(VitalsColors.TEXT_SECONDARY),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }

                // Right Trend Graph
                InteractiveVitalTrendGraph(
                    weeklyValues = metric.weeklyValues,
                    modifier = Modifier
                        .weight(0.65f)
                        .height(72.dp)
                )
            }
        }
    }
}

@Composable
private fun InteractiveVitalTrendGraph(
    weeklyValues: List<Float>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    androidx.compose.foundation.Canvas(
        modifier = modifier
    ) {
        val widthPx = size.width
        val heightPx = size.height

        val topPadding = 6.dp.toPx()
        val timeAxisHeight = 20.dp.toPx()
        val leftPadding = 4.dp.toPx()
        val rightPadding = 4.dp.toPx()

        val availableWidth = widthPx - leftPadding - rightPadding
        val chartHeight = heightPx - topPadding - timeAxisHeight

        val values = weeklyValues.ifEmpty { listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f) }
        val minVal = values.minOrNull() ?: 0f
        val maxVal = values.maxOrNull() ?: 1f
        val range = (maxVal - minVal).coerceAtLeast(1f)

        fun getX(index: Int): Float {
            return leftPadding + (index.toFloat() / 6f) * availableWidth
        }

        fun getY(value: Float): Float {
            val norm = (value - minVal) / range
            return topPadding + (1f - norm) * (chartHeight - 8.dp.toPx()) + 4.dp.toPx()
        }

        // 1. Shaded Gradient Fill
        val fillPath = Path().apply {
            moveTo(getX(0), topPadding + chartHeight)
            lineTo(getX(0), getY(values[0]))
            for (i in 1 until values.size) {
                lineTo(getX(i), getY(values[i]))
            }
            lineTo(getX(values.size - 1), topPadding + chartHeight)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(VitalsColors.ACCENT_GREEN_FILL_START),
                    Color(VitalsColors.ACCENT_GREEN_FILL_END)
                ),
                startY = topPadding,
                endY = topPadding + chartHeight
            )
        )

        // 2. Trend Line
        val linePath = Path().apply {
            moveTo(getX(0), getY(values[0]))
            for (i in 1 until values.size) {
                lineTo(getX(i), getY(values[i]))
            }
        }

        drawPath(
            path = linePath,
            color = Color(VitalsColors.ACCENT_GREEN),
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // 3. Circular Node Points
        for (i in values.indices) {
            val x = getX(i)
            val y = getY(values[i])
            drawCircle(
                color = Color(VitalsColors.ACCENT_GREEN),
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )

            if (i == values.size - 1) {
                drawCircle(
                    color = Color(VitalsColors.ACCENT_GREEN),
                    radius = 5.dp.toPx(),
                    center = Offset(x, y),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        // 4. Day Ticks at bottom: 7 days ending Today
        val today = java.time.LocalDate.now()
        val dayLabels = (0..6).map { dayOffset ->
            val date = today.minusDays((6 - dayOffset).toLong())
            date.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.US)
        }
        val dayY = heightPx - 2.dp.toPx()

        for (i in dayLabels.indices) {
            val x = getX(i)
            val label = dayLabels[i]

            if (i == dayLabels.size - 1) {
                drawCircle(
                    color = Color(VitalsColors.ACTIVE_DAY_BG),
                    radius = 8.dp.toPx(),
                    center = Offset(x, dayY - 5.dp.toPx())
                )
                drawAxisDayText(
                    textMeasurer = textMeasurer,
                    text = label,
                    x = x,
                    y = dayY - 11.dp.toPx(),
                    isActive = true
                )
            } else {
                drawAxisDayText(
                    textMeasurer = textMeasurer,
                    text = label,
                    x = x,
                    y = dayY - 11.dp.toPx(),
                    isActive = false
                )
            }
        }
    }
}

private fun DrawScope.drawAxisDayText(
    textMeasurer: TextMeasurer,
    text: String,
    x: Float,
    y: Float,
    isActive: Boolean
) {
    val layout = textMeasurer.measure(
        text = text,
        style = TextStyle(
            color = if (isActive) Color.White else Color(VitalsColors.TEXT_SECONDARY),
            fontSize = 9.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    )

    drawText(
        textLayoutResult = layout,
        topLeft = Offset(x - layout.size.width / 2f, y)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun VitalsWidgetCardPreview() {
    VitalsWidgetCard(vitalsData = VitalsData.mock())
}
