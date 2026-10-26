package com.example.healthwidget.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import com.example.healthwidget.model.SleepData
import com.example.healthwidget.model.SleepStage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object SleepColors {
    const val AWAKE = 0xFFFFC107.toInt()       // Amber/Yellow
    const val REM = 0xFFC084FC.toInt()         // Purple
    const val LIGHT = 0xFF509BF5.toInt()       // Light Blue
    const val DEEP = 0xFF3B52D4.toInt()        // Indigo / Dark Blue
    const val MAGENTA_TAG = 0xFFFF0080.toInt() // Magenta Tag
    const val TEXT_PRIMARY = 0xFFFFFFFF.toInt()
    const val TEXT_SECONDARY = 0xFF8E8E93.toInt()
    const val GRID_LINE = 0xFF2C2C2E.toInt()

    fun getColor(stage: SleepStage): Int {
        return when (stage) {
            SleepStage.AWAKE, SleepStage.OUT_OF_BED -> AWAKE
            SleepStage.REM -> REM
            SleepStage.LIGHT -> LIGHT
            SleepStage.DEEP -> DEEP
        }
    }

    fun getYLevelIndex(stage: SleepStage): Int {
        return when (stage) {
            SleepStage.AWAKE, SleepStage.OUT_OF_BED -> 0
            SleepStage.REM -> 1
            SleepStage.LIGHT -> 2
            SleepStage.DEEP -> 3
        }
    }
}

fun renderSleepGraphBitmap(
    sleepData: SleepData,
    widthPx: Int = 800,
    heightPx: Int = 400
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Layout margins
    val topMargin = 12f
    val legendHeight = 64f
    val timeAxisHeight = 32f
    val leftPadding = 24f
    val rightPadding = 24f

    val availableWidth = widthPx - leftPadding - rightPadding
    val chartHeight = heightPx - topMargin - timeAxisHeight - legendHeight
    val rowHeight = chartHeight / 4f

    fun getYCenter(stage: SleepStage): Float {
        val index = SleepColors.getYLevelIndex(stage)
        return topMargin + (index + 0.5f) * rowHeight
    }

    val startSec = sleepData.startTimeEpochSec
    val endSec = sleepData.endTimeEpochSec
    val totalSec = (endSec - startSec).coerceAtLeast(1L)

    fun getX(epochSec: Long): Float {
        val progress = (epochSec - startSec).toFloat() / totalSec
        return leftPadding + (progress.coerceIn(0f, 1f) * availableWidth)
    }

    // 1. Horizontal dashed swimlane lines
    val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = SleepColors.GRID_LINE
        strokeWidth = 2f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
    }

    for (i in 0..3) {
        val y = topMargin + (i + 0.5f) * rowHeight
        canvas.drawLine(leftPadding, y, widthPx - rightPadding, y, gridPaint)
    }

    val sortedStages = sleepData.stages.sortedBy { it.startEpochSec }

    // 2. Horizontal stage line segments
    val lineStrokeWidth = 6f
    val segmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = lineStrokeWidth
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }

    for (interval in sortedStages) {
        val xStart = getX(interval.startEpochSec)
        val xEnd = getX(interval.endEpochSec).coerceAtLeast(xStart + 1f)
        val yCenter = getYCenter(interval.stage)

        segmentPaint.color = SleepColors.getColor(interval.stage)
        canvas.drawLine(xStart, yCenter, xEnd, yCenter, segmentPaint)
    }

    // 3. Vertical transition lines with linear gradients between stage nodes
    val transitionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = lineStrokeWidth
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }

    for (i in 0 until sortedStages.size - 1) {
        val current = sortedStages[i]
        val next = sortedStages[i + 1]

        val x = getX(current.endEpochSec)
        val y1 = getYCenter(current.stage)
        val y2 = getYCenter(next.stage)

        if (y1 != y2) {
            val color1 = SleepColors.getColor(current.stage)
            val color2 = SleepColors.getColor(next.stage)

            transitionPaint.shader = android.graphics.LinearGradient(
                x, y1, x, y2,
                color1, color2,
                android.graphics.Shader.TileMode.CLAMP
            )
            canvas.drawLine(x, y1, x, y2, transitionPaint)
        }
    }

    // 4. Time Axis Labels
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = SleepColors.TEXT_SECONDARY
        textSize = 20f
        textAlign = Paint.Align.CENTER
    }

    val timeY = topMargin + chartHeight + 22f
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    val shortTimeFormatter = DateTimeFormatter.ofPattern("h a", Locale.US)

    // Start time
    val startStr = Instant.ofEpochSecond(startSec).atZone(ZoneId.systemDefault()).format(timeFormatter)
    textPaint.textAlign = Paint.Align.LEFT
    canvas.drawText(startStr, leftPadding, timeY, textPaint)

    // End time
    val endStr = Instant.ofEpochSecond(endSec).atZone(ZoneId.systemDefault()).format(timeFormatter)
    textPaint.textAlign = Paint.Align.RIGHT
    canvas.drawText(endStr, widthPx - rightPadding, timeY, textPaint)

    // Intermediate time ticks
    textPaint.textAlign = Paint.Align.CENTER
    val stepSec = 2 * 3600L
    var currentTick = ((startSec / stepSec) + 1) * stepSec
    while (currentTick < endSec - 1800) {
        val xTick = getX(currentTick)
        if (xTick - leftPadding > 70f && (widthPx - rightPadding) - xTick > 70f) {
            val tickStr = Instant.ofEpochSecond(currentTick).atZone(ZoneId.systemDefault()).format(shortTimeFormatter)
            canvas.drawText(tickStr, xTick, timeY, textPaint)
        }
        currentTick += stepSec
    }

    // 5. Bottom Stats Legend Row (Awake, REM, Light, Deep with larger color dots & text)
    val legendY = heightPx - 16f
    val dotRadius = 9f
    val legendItems = listOf(
        Triple("Awake", "${sleepData.awakeMinutes}m", SleepColors.AWAKE),
        Triple("REM", "${sleepData.remMinutes / 60}h ${sleepData.remMinutes % 60}m", SleepColors.REM),
        Triple("Light", "${sleepData.lightMinutes / 60}h ${sleepData.lightMinutes % 60}m", SleepColors.LIGHT),
        Triple("Deep", "${sleepData.deepMinutes / 60}h ${sleepData.deepMinutes % 60}m", SleepColors.DEEP)
    )

    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    val legendLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = SleepColors.TEXT_PRIMARY
        textSize = 23f
        textAlign = Paint.Align.LEFT
        isFakeBoldText = true
    }
    val legendValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = SleepColors.TEXT_SECONDARY
        textSize = 20f
        textAlign = Paint.Align.LEFT
    }

    val colWidth = availableWidth / 4f

    for (idx in legendItems.indices) {
        val (label, duration, color) = legendItems[idx]
        val colLeft = leftPadding + idx * colWidth

        val labelWidth = legendLabelPaint.measureText(label)
        val valWidth = legendValPaint.measureText(duration)
        val textWidth = maxOf(labelWidth, valWidth)
        val itemTotalWidth = dotRadius * 2f + 10f + textWidth

        val itemStart = colLeft + (colWidth - itemTotalWidth) / 2f

        // Color dot
        dotPaint.color = color
        canvas.drawCircle(itemStart + dotRadius, legendY - 14f, dotRadius, dotPaint)

        // Label + Duration
        val textX = itemStart + dotRadius * 2f + 10f
        canvas.drawText(label, textX, legendY - 16f, legendLabelPaint)
        canvas.drawText(duration, textX, legendY + 12f, legendValPaint)
    }

    return bitmap
}
