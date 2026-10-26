package com.example.healthwidget.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthwidget.model.SleepData
import com.example.healthwidget.model.SleepStage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SleepWidgetCard(
    modifier: Modifier = Modifier,
    sleepData: SleepData = remember { SleepData.mock() },
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF141416), shape = RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 1. Header: Magenta bar tag + "Sleep" Title
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(18.dp)
                        .background(
                            Color(SleepColors.MAGENTA_TAG),
                            shape = RoundedCornerShape(2.dp)
                        )
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "Sleep",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Metrics Header Row: Total Duration + Timeframe
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = sleepData.formattedDuration,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = sleepData.formattedTimeframe,
                    style = TextStyle(
                        color = Color(SleepColors.TEXT_SECONDARY),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dynamic Natural Language Insight Text
            Text(
                text = sleepData.sleepInsight,
                style = TextStyle(
                    color = Color(0xFFC7C7CC),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Interactive Draggable Sleep Architecture Canvas
            InteractiveSleepGraph(
                sleepData = sleepData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(165.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Legend Breakdown Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StageLegendItem(
                    label = "Awake",
                    durationText = "${sleepData.awakeMinutes}m",
                    color = Color(SleepColors.AWAKE),
                    modifier = Modifier.weight(1f)
                )
                StageLegendItem(
                    label = "REM",
                    durationText = "${sleepData.remMinutes / 60}h ${sleepData.remMinutes % 60}m",
                    color = Color(SleepColors.REM),
                    modifier = Modifier.weight(1f)
                )
                StageLegendItem(
                    label = "Light",
                    durationText = "${sleepData.lightMinutes / 60}h ${sleepData.lightMinutes % 60}m",
                    color = Color(SleepColors.LIGHT),
                    modifier = Modifier.weight(1f)
                )
                StageLegendItem(
                    label = "Deep",
                    durationText = "${sleepData.deepMinutes / 60}h ${sleepData.deepMinutes % 60}m",
                    color = Color(SleepColors.DEEP),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StageLegendItem(
    label: String,
    durationText: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(10.dp)
                .background(color, shape = RoundedCornerShape(5.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = label,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = durationText,
                style = TextStyle(
                    color = Color(SleepColors.TEXT_SECONDARY),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun InteractiveSleepGraph(
    sleepData: SleepData,
    modifier: Modifier = Modifier
) {
    var scrubTouchX by remember { mutableStateOf<Float?>(null) }
    val sortedStages = remember(sleepData.stages) { sleepData.stages.sortedBy { it.startEpochSec } }
    val textMeasurer = rememberTextMeasurer()

    androidx.compose.foundation.Canvas(
        modifier = modifier
            .pointerInput(sleepData) {
                detectTapGestures(
                    onPress = { offset ->
                        scrubTouchX = offset.x
                        tryAwaitRelease()
                        scrubTouchX = null
                    }
                )
            }
            .pointerInput(sleepData) {
                detectDragGestures(
                    onDragStart = { offset -> scrubTouchX = offset.x },
                    onDrag = { change, _ ->
                        change.consume()
                        scrubTouchX = change.position.x
                    },
                    onDragEnd = { scrubTouchX = null },
                    onDragCancel = { scrubTouchX = null }
                )
            }
    ) {
        val widthPx = size.width
        val heightPx = size.height

        val topPadding = 16.dp.toPx()
        val bottomPadding = 28.dp.toPx()
        val leftPadding = 12.dp.toPx()
        val rightPadding = 12.dp.toPx()

        val availableWidth = widthPx - leftPadding - rightPadding
        val availableHeight = heightPx - topPadding - bottomPadding
        val rowHeight = availableHeight / 4f

        fun getYCenter(stage: SleepStage): Float {
            val idx = SleepColors.getYLevelIndex(stage)
            return topPadding + (idx + 0.5f) * rowHeight
        }

        val startSec = sleepData.startTimeEpochSec
        val endSec = sleepData.endTimeEpochSec
        val totalSec = (endSec - startSec).coerceAtLeast(1L)

        fun getX(epochSec: Long): Float {
            val progress = (epochSec - startSec).toFloat() / totalSec
            return leftPadding + (progress.coerceIn(0f, 1f) * availableWidth)
        }

        // 1. Dotted grid lines
        val gridEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        for (i in 0..3) {
            val y = topPadding + (i + 0.5f) * rowHeight
            drawLine(
                color = Color(SleepColors.GRID_LINE),
                start = Offset(leftPadding, y),
                end = Offset(widthPx - rightPadding, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = gridEffect
            )
        }

        val strokeThickness = 3.dp.toPx()

        // 2. Horizontal stage line segments
        for (interval in sortedStages) {
            val xStart = getX(interval.startEpochSec)
            val xEnd = getX(interval.endEpochSec).coerceAtLeast(xStart + 1f)
            val yCenter = getYCenter(interval.stage)

            drawLine(
                color = Color(SleepColors.getColor(interval.stage)),
                start = Offset(xStart, yCenter),
                end = Offset(xEnd, yCenter),
                strokeWidth = strokeThickness,
                cap = StrokeCap.Butt
            )
        }

        // 3. Vertical transition lines with linear gradients between stage colors
        for (i in 0 until sortedStages.size - 1) {
            val current = sortedStages[i]
            val next = sortedStages[i + 1]

            val x = getX(current.endEpochSec)
            val y1 = getYCenter(current.stage)
            val y2 = getYCenter(next.stage)

            if (y1 != y2) {
                val gradientBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(SleepColors.getColor(current.stage)),
                        Color(SleepColors.getColor(next.stage))
                    ),
                    start = Offset(x, y1),
                    end = Offset(x, y2)
                )

                drawLine(
                    brush = gradientBrush,
                    start = Offset(x, y1),
                    end = Offset(x, y2),
                    strokeWidth = strokeThickness,
                    cap = StrokeCap.Butt
                )
            }
        }

        // 4. X-Axis Time Ticks
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        val shortFormatter = DateTimeFormatter.ofPattern("h a", Locale.US)

        val startStr = Instant.ofEpochSecond(startSec).atZone(ZoneId.systemDefault()).format(timeFormatter)
        val endStr = Instant.ofEpochSecond(endSec).atZone(ZoneId.systemDefault()).format(timeFormatter)

        drawAxisText(
            textMeasurer = textMeasurer,
            text = startStr,
            x = leftPadding,
            y = heightPx - 14.dp.toPx(),
            alignLeft = true
        )

        drawAxisText(
            textMeasurer = textMeasurer,
            text = endStr,
            x = widthPx - rightPadding,
            y = heightPx - 14.dp.toPx(),
            alignRight = true
        )

        val stepSec = 2 * 3600L
        var currentTick = ((startSec / stepSec) + 1) * stepSec
        while (currentTick < endSec - 1800) {
            val xTick = getX(currentTick)
            if (xTick - leftPadding > 50.dp.toPx() && (widthPx - rightPadding) - xTick > 50.dp.toPx()) {
                val tickStr = Instant.ofEpochSecond(currentTick).atZone(ZoneId.systemDefault()).format(shortFormatter)
                drawAxisText(
                    textMeasurer = textMeasurer,
                    text = tickStr,
                    x = xTick,
                    y = heightPx - 14.dp.toPx()
                )
            }
            currentTick += stepSec
        }

        // 5. Draggable Scrubber Line & Tooltip
        scrubTouchX?.let { touchX ->
            val clampedX = touchX.coerceIn(leftPadding, widthPx - rightPadding)
            val progress = (clampedX - leftPadding) / availableWidth
            val scrubTimeSec = startSec + (progress * totalSec).toLong()

            val activeInterval = sleepData.stages.firstOrNull {
                scrubTimeSec >= it.startEpochSec && scrubTimeSec <= it.endEpochSec
            } ?: sleepData.stages.minByOrNull { kotlin.math.abs(it.startEpochSec - scrubTimeSec) }

            // Vertical indicator line
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(clampedX, topPadding / 2f),
                end = Offset(clampedX, heightPx - bottomPadding / 2f),
                strokeWidth = 2.dp.toPx()
            )

            // Scrubber point circle
            activeInterval?.let { interval ->
                val yCenter = getYCenter(interval.stage)
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(clampedX, yCenter)
                )
                drawCircle(
                    color = Color(SleepColors.getColor(interval.stage)),
                    radius = 2.dp.toPx(),
                    center = Offset(clampedX, yCenter)
                )

                // Tooltip text
                val scrubTimeStr = Instant.ofEpochSecond(scrubTimeSec)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))

                val tooltipText = "$scrubTimeStr • ${interval.stage.displayName}"
                val textLayout = textMeasurer.measure(
                    text = tooltipText,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                val tooltipWidth = textLayout.size.width + 16.dp.toPx()
                val tooltipHeight = textLayout.size.height + 8.dp.toPx()

                var tooltipX = clampedX - tooltipWidth / 2f
                tooltipX = tooltipX.coerceIn(leftPadding, widthPx - rightPadding - tooltipWidth)

                val tooltipY = (topPadding / 2f) - tooltipHeight / 2f

                drawRoundRect(
                    color = Color(0xFF2C2C2E),
                    topLeft = Offset(tooltipX, tooltipY),
                    size = Size(tooltipWidth, tooltipHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(tooltipX + 8.dp.toPx(), tooltipY + 4.dp.toPx())
                )
            }
        }
    }
}

private fun DrawScope.drawAxisText(
    textMeasurer: TextMeasurer,
    text: String,
    x: Float,
    y: Float,
    alignLeft: Boolean = false,
    alignRight: Boolean = false
) {
    val layout = textMeasurer.measure(
        text = text,
        style = TextStyle(
            color = Color(SleepColors.TEXT_SECONDARY),
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal
        )
    )

    val drawX = when {
        alignLeft -> x
        alignRight -> x - layout.size.width
        else -> x - layout.size.width / 2f
    }

    drawText(
        textLayoutResult = layout,
        topLeft = Offset(drawX, y)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SleepWidgetCardPreview() {
    SleepWidgetCard(sleepData = SleepData.mock())
}
