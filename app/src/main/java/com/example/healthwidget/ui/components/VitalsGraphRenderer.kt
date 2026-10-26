package com.example.healthwidget.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import com.example.healthwidget.model.VitalMetric
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

object VitalsColors {
    const val ACCENT_GREEN = 0xFF00E640.toInt()
    const val ACCENT_GREEN_FILL_START = 0x5000E640.toInt()
    const val ACCENT_GREEN_FILL_END = 0x0500E640.toInt()
    const val TEXT_SECONDARY = 0xFF8E8E93.toInt()
    const val TEXT_PRIMARY = 0xFFFFFFFF.toInt()
    const val ACTIVE_DAY_BG = 0xFF2C2C2E.toInt()
}

fun renderVitalTrendBitmap(
    metric: VitalMetric,
    widthPx: Int = 800,
    heightPx: Int = 220
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val topPadding = 10f
    val bottomPadding = 4f
    val timeAxisHeight = 44f
    val leftPadding = 16f
    val rightPadding = 16f

    val availableWidth = widthPx - leftPadding - rightPadding
    val chartHeight = heightPx - topPadding - timeAxisHeight - bottomPadding

    val values = metric.weeklyValues.ifEmpty { listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f) }
    val minVal = values.minOrNull() ?: 0f
    val maxVal = values.maxOrNull() ?: 1f
    val range = (maxVal - minVal).coerceAtLeast(1f)

    fun getX(index: Int): Float {
        return leftPadding + (index.toFloat() / 6f) * availableWidth
    }

    fun getY(value: Float): Float {
        val norm = (value - minVal) / range
        return topPadding + (1f - norm) * (chartHeight - 20f) + 10f
    }

    // 1. Shaded Gradient Area Fill under trend line
    val fillPath = Path()
    fillPath.moveTo(getX(0), topPadding + chartHeight)
    fillPath.lineTo(getX(0), getY(values[0]))

    for (i in 1 until values.size) {
        fillPath.lineTo(getX(i), getY(values[i]))
    }

    fillPath.lineTo(getX(values.size - 1), topPadding + chartHeight)
    fillPath.close()

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader = LinearGradient(
            0f, topPadding,
            0f, topPadding + chartHeight,
            VitalsColors.ACCENT_GREEN_FILL_START,
            VitalsColors.ACCENT_GREEN_FILL_END,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawPath(fillPath, fillPaint)

    // 2. Trend Line
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = VitalsColors.ACCENT_GREEN
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    for (i in 0 until values.size - 1) {
        canvas.drawLine(getX(i), getY(values[i]), getX(i + 1), getY(values[i + 1]), linePaint)
    }

    // 3. Node Points
    val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = VitalsColors.ACCENT_GREEN
        style = Paint.Style.FILL
    }

    val activeNodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = VitalsColors.ACCENT_GREEN
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
    }

    for (i in values.indices) {
        val x = getX(i)
        val y = getY(values[i])
        canvas.drawCircle(x, y, 5.5f, nodePaint)

        if (i == values.size - 1) {
            canvas.drawCircle(x, y, 10f, activeNodePaint)
        }
    }

    // 4. Day Ticks at bottom: 7 days ending Today
    val today = LocalDate.now()
    val dayLabels = (0..6).map { dayOffset ->
        val date = today.minusDays((6 - dayOffset).toLong())
        date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.US)
    }
    val timeAxisCenterY = topPadding + chartHeight + 22f

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = VitalsColors.TEXT_SECONDARY
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    val activeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = VitalsColors.ACTIVE_DAY_BG
        style = Paint.Style.FILL
    }

    val activeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = VitalsColors.TEXT_PRIMARY
        textSize = 24f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    val textY = timeAxisCenterY + 8f

    for (i in dayLabels.indices) {
        val x = getX(i)
        val label = dayLabels[i]

        if (i == dayLabels.size - 1) {
            // Highlight active day "S" / Today
            canvas.drawCircle(x, timeAxisCenterY, 15f, activeBgPaint)
            canvas.drawText(label, x, textY, activeTextPaint)
        } else {
            canvas.drawText(label, x, textY, textPaint)
        }
    }

    return bitmap
}
