package com.example.healthwidget.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.healthwidget.R
import com.example.healthwidget.model.HealthData

object RingColors {
    // Outer Ring - Steps (Cyan / Blue)
    const val STEPS_TRACK = 0xFF122A45.toInt()
    const val STEPS_PROGRESS = 0xFF0088FF.toInt()
    const val STEPS_HIGHLIGHT = 0xFF00E5FF.toInt()

    // Middle Ring - Calories (Orange)
    const val CALORIES_TRACK = 0xFF3B220A.toInt()
    const val CALORIES_PROGRESS = 0xFFFF8C00.toInt()
    const val CALORIES_HIGHLIGHT = 0xFFFFC107.toInt()

    // Inner Ring - Exercise (Green)
    const val EXERCISE_TRACK = 0xFF0A2A12.toInt()
    const val EXERCISE_PROGRESS = 0xFF00E640.toInt()
    const val EXERCISE_HIGHLIGHT = 0xFF66FF88.toInt()
}

/**
 * Bitmap Renderer for Steps Chart Box in Glance App Widget.
 */
/**
 * Bitmap Renderer for 7 Vertical Bars in Glance App Widget.
 */
fun renderStepBarsBitmap(
    context: Context,
    widthPx: Int,
    heightPx: Int,
    barRatios: FloatArray = floatArrayOf(0.42f, 0.70f, 0.25f, 0.48f, 0.45f, 0.82f, 0.65f)
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val density = context.resources.displayMetrics.density
    fun dpToPx(dp: Float) = dp * density

    // 7 Vertical Bars (Start at 8dp offset to align with "Today" text)
    val barWidth = dpToPx(9f)
    val chartTop = dpToPx(2f)
    val chartBottom = heightPx - dpToPx(2f)
    val chartHeight = chartBottom - chartTop

    val barCount = barRatios.size
    val startX = dpToPx(8f)
    val availableWidth = widthPx - startX
    val spacing = (availableWidth - (barCount * barWidth)) / (barCount - 1)

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#132238")
        style = Paint.Style.FILL
    }

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0066FF")
        style = Paint.Style.FILL
    }

    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00C8FF")
        style = Paint.Style.FILL
    }

    val dotRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00C8FF")
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1.2f)
    }

    val barRadius = dpToPx(4.5f)
    val rect = RectF()

    for (i in 0 until barCount) {
        val left = startX + i * (barWidth + spacing)
        val right = left + barWidth

        // Track Pillar Background
        rect.set(left, chartTop, right, chartBottom)
        canvas.drawRoundRect(rect, barRadius, barRadius, trackPaint)

        // Filled Progress Portion
        val fillHeight = chartHeight * barRatios[i]
        val fillTop = chartBottom - fillHeight
        rect.set(left, fillTop, right, chartBottom)
        canvas.drawRoundRect(rect, barRadius, barRadius, fillPaint)

        // Circular Dot Cap at top
        val dotRadius = dpToPx(3f)
        val dotX = left + barWidth / 2f
        val dotY = fillTop + dotRadius + dpToPx(0.5f)

        if (i == barCount - 1) {
            canvas.drawCircle(dotX, dotY, dotRadius, dotRingPaint)
        } else {
            canvas.drawCircle(dotX, dotY, dotRadius, dotPaint)
        }
    }

    return bitmap
}

/**
 * Android Canvas Bitmap Renderer for Glance Widget and Image export.
 */
fun renderHealthRingsBitmap(
    context: Context,
    healthData: HealthData,
    sizePx: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val centerX = sizePx / 2f
    val centerY = sizePx / 2f

    val strokeWidth = sizePx * 0.08f
    val ringSpacing = strokeWidth * 0.28f
    val padding = strokeWidth * 0.15f

    val outerRadius = (sizePx / 2f) - padding - (strokeWidth / 2f)
    val middleRadius = outerRadius - strokeWidth - ringSpacing
    val innerRadius = middleRadius - strokeWidth - ringSpacing

    // Paint setup
    val paintTrack = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    val paintProgress = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    val oval = RectF()

    fun drawSingleRing(
        radius: Float,
        trackColor: Int,
        progressColor: Int,
        highlightColor: Int,
        progress: Float,
        iconResId: Int
    ) {
        // Track
        paintTrack.color = trackColor
        paintTrack.strokeWidth = strokeWidth
        canvas.drawCircle(centerX, centerY, radius, paintTrack)

        // Progress Arc with Bright Glowing Tip and Seamless 12 o'clock Start Cap
        oval.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        paintProgress.strokeWidth = strokeWidth

        val p = progress.coerceIn(0.01f, 1f)
        val sweepAngle = p * 360f

        val stop1 = (p * 0.5f).coerceAtMost(0.90f)
        val stop2 = p.coerceAtMost(0.95f)
        val stop3 = (p + 0.08f).coerceAtMost(0.98f)

        val sweepGradient = SweepGradient(
            centerX,
            centerY,
            intArrayOf(progressColor, progressColor, highlightColor, highlightColor, progressColor),
            floatArrayOf(0f, stop1, stop2, stop3, 1f)
        ).apply {
            val matrix = android.graphics.Matrix()
            matrix.postRotate(-90f, centerX, centerY)
            setLocalMatrix(matrix)
        }
        paintProgress.shader = sweepGradient

        canvas.drawArc(oval, -90f, sweepAngle, false, paintProgress)

        // Draw icon at top (12 o'clock)
        val iconY = centerY - radius
        val iconSize = (strokeWidth * 0.75f).toInt()
        val drawable = ContextCompat.getDrawable(context, iconResId)
        drawable?.let {
            it.setBounds(
                (centerX - iconSize / 2).toInt(),
                (iconY - iconSize / 2).toInt(),
                (centerX + iconSize / 2).toInt(),
                (iconY + iconSize / 2).toInt()
            )
            it.setTint(Color.WHITE)
            it.draw(canvas)
        }
    }

    // 1. Outer Ring: Steps
    drawSingleRing(
        radius = outerRadius,
        trackColor = RingColors.STEPS_TRACK,
        progressColor = RingColors.STEPS_PROGRESS,
        highlightColor = RingColors.STEPS_HIGHLIGHT,
        progress = healthData.stepProgress,
        iconResId = R.drawable.ic_shoe
    )

    // 2. Middle Ring: Calories
    drawSingleRing(
        radius = middleRadius,
        trackColor = RingColors.CALORIES_TRACK,
        progressColor = RingColors.CALORIES_PROGRESS,
        highlightColor = RingColors.CALORIES_HIGHLIGHT,
        progress = healthData.calorieProgress,
        iconResId = R.drawable.ic_flame
    )

    // 3. Inner Ring: Exercise
    drawSingleRing(
        radius = innerRadius,
        trackColor = RingColors.EXERCISE_TRACK,
        progressColor = RingColors.EXERCISE_PROGRESS,
        highlightColor = RingColors.EXERCISE_HIGHLIGHT,
        progress = healthData.exerciseProgress,
        iconResId = R.drawable.ic_exercise
    )

    return bitmap
}

/**
 * Jetpack Compose Composable Canvas for App Screen UI.
 */
@Composable
fun HealthRingsCanvas(
    healthData: HealthData,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Canvas(modifier = modifier) {
        val sizePx = size.minDimension
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        val strokeWidth = sizePx * 0.08f
        val ringSpacing = strokeWidth * 0.28f
        val padding = strokeWidth * 0.15f

        val outerRadius = (sizePx / 2f) - padding - (strokeWidth / 2f)
        val middleRadius = outerRadius - strokeWidth - ringSpacing
        val innerRadius = middleRadius - strokeWidth - ringSpacing

        fun drawRing(
            radius: Float,
            trackColor: androidx.compose.ui.graphics.Color,
            progressColor: Int,
            highlightColor: Int,
            progress: Float,
            iconResId: Int
        ) {
            // Track
            drawCircle(
                color = trackColor,
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress Arc with Bright Glowing Tip and Seamless 12 o'clock Start Cap
            val p = progress.coerceIn(0.01f, 1f)
            val sweepAngle = p * 360f

            val stop1 = (p * 0.5f).coerceAtMost(0.90f)
            val stop2 = p.coerceAtMost(0.95f)
            val stop3 = (p + 0.08f).coerceAtMost(0.98f)

            val sweepGradientShader = SweepGradient(
                centerX,
                centerY,
                intArrayOf(progressColor, progressColor, highlightColor, highlightColor, progressColor),
                floatArrayOf(0f, stop1, stop2, stop3, 1f)
            ).apply {
                val matrix = android.graphics.Matrix()
                matrix.postRotate(-90f, centerX, centerY)
                setLocalMatrix(matrix)
            }
            val brush = androidx.compose.ui.graphics.ShaderBrush(sweepGradientShader)

            drawArc(
                brush = brush,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Icon at top (12 o'clock)
            val iconY = centerY - radius
            drawContext.canvas.nativeCanvas.let { nativeCanvas ->
                val drawable = ContextCompat.getDrawable(context, iconResId)
                drawable?.let {
                    val iconSize = (strokeWidth * 0.75f).toInt()
                    it.setBounds(
                        (centerX - iconSize / 2).toInt(),
                        (iconY - iconSize / 2).toInt(),
                        (centerX + iconSize / 2).toInt(),
                        (iconY + iconSize / 2).toInt()
                    )
                    it.setTint(Color.WHITE)
                    it.draw(nativeCanvas)
                }
            }
        }

        // Outer (Steps)
        drawRing(
            radius = outerRadius,
            trackColor = androidx.compose.ui.graphics.Color(RingColors.STEPS_TRACK),
            progressColor = RingColors.STEPS_PROGRESS,
            highlightColor = RingColors.STEPS_HIGHLIGHT,
            progress = healthData.stepProgress,
            iconResId = R.drawable.ic_shoe
        )

        // Middle (Calories)
        drawRing(
            radius = middleRadius,
            trackColor = androidx.compose.ui.graphics.Color(RingColors.CALORIES_TRACK),
            progressColor = RingColors.CALORIES_PROGRESS,
            highlightColor = RingColors.CALORIES_HIGHLIGHT,
            progress = healthData.calorieProgress,
            iconResId = R.drawable.ic_flame
        )

        // Inner (Exercise)
        drawRing(
            radius = innerRadius,
            trackColor = androidx.compose.ui.graphics.Color(RingColors.EXERCISE_TRACK),
            progressColor = RingColors.EXERCISE_PROGRESS,
            highlightColor = RingColors.EXERCISE_HIGHLIGHT,
            progress = healthData.exerciseProgress,
            iconResId = R.drawable.ic_exercise
        )
    }
}
