package com.example.healthwidget.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class SleepStage(val displayName: String) {
    AWAKE("Awake"),
    REM("REM"),
    LIGHT("Light"),
    DEEP("Deep"),
    OUT_OF_BED("Awake")
}

data class SleepStageInterval(
    val stage: SleepStage,
    val startEpochSec: Long,
    val endEpochSec: Long
) {
    val durationMinutes: Int
        get() = ((endEpochSec - startEpochSec) / 60).toInt().coerceAtLeast(0)
}

data class SleepData(
    val startTimeEpochSec: Long,
    val endTimeEpochSec: Long,
    val stages: List<SleepStageInterval>,
    val isMockData: Boolean = false
) {
    val totalSleepMinutes: Int
        get() = ((endTimeEpochSec - startTimeEpochSec) / 60).toInt().coerceAtLeast(0)

    val awakeMinutes: Int
        get() = stages.filter { it.stage == SleepStage.AWAKE || it.stage == SleepStage.OUT_OF_BED }.sumOf { it.durationMinutes }

    val actualSleepMinutes: Int
        get() = (totalSleepMinutes - awakeMinutes).coerceAtLeast(0)

    val remMinutes: Int
        get() = stages.filter { it.stage == SleepStage.REM }.sumOf { it.durationMinutes }

    val lightMinutes: Int
        get() = stages.filter { it.stage == SleepStage.LIGHT }.sumOf { it.durationMinutes }

    val deepMinutes: Int
        get() = stages.filter { it.stage == SleepStage.DEEP }.sumOf { it.durationMinutes }

    val formattedDuration: String
        get() {
            val netMinutes = actualSleepMinutes
            val hours = netMinutes / 60
            val mins = netMinutes % 60
            return "${hours}h ${mins}m"
        }

    val formattedTimeframe: String
        get() {
            if (startTimeEpochSec <= 0 || endTimeEpochSec <= 0) return "No sleep recorded"
            val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
            val startStr = Instant.ofEpochSecond(startTimeEpochSec).atZone(ZoneId.systemDefault()).format(formatter)
            val endStr = Instant.ofEpochSecond(endTimeEpochSec).atZone(ZoneId.systemDefault()).format(formatter)
            return "$startStr - $endStr"
        }

    val sleepInsight: String
        get() {
            val netMinutes = actualSleepMinutes
            if (netMinutes <= 0) return "No sleep data logged today."
            val totalHours = netMinutes / 60f
            val deepPct = deepMinutes.toFloat() / netMinutes
            val remPct = remMinutes.toFloat() / netMinutes

            return when {
                deepPct >= 0.20f && remPct >= 0.22f ->
                    "Excellent sleep quality with optimal deep and REM recovery."
                deepPct >= 0.20f ->
                    "Great deep sleep session — ideal for physical recovery & repair."
                remPct >= 0.25f ->
                    "High REM sleep ratio — great for mental clarity and cognitive refresh."
                deepPct < 0.12f && totalSleepMinutes > 360 ->
                    "Slightly light on deep sleep. Try avoiding late caffeine or screen time."
                totalHours < 6.0f ->
                    "Shorter sleep session than usual. Prioritize extra rest tonight."
                totalHours >= 8.5f ->
                    "Restful extended sleep session. Perfect for full recharge."
                else ->
                    "Balanced rest session with healthy sleep stage cycles."
            }
        }

    companion object {
        fun mock(): SleepData {
            // Mock session: 3:01 AM to 12:01 PM (8h 41m duration = 521 minutes)
            val endSec = Instant.now().epochSecond
            val startSec = endSec - (8 * 3600 + 41 * 60)

            val totalSec = endSec - startSec
            fun relSec(fraction: Double): Long = startSec + (totalSec * fraction).toLong()

            // Generate realistic stage blocks matching inspiration
            val mockStages = listOf(
                SleepStageInterval(SleepStage.AWAKE, startSec, relSec(0.03)),
                SleepStageInterval(SleepStage.LIGHT, relSec(0.03), relSec(0.10)),
                SleepStageInterval(SleepStage.DEEP, relSec(0.10), relSec(0.22)),
                SleepStageInterval(SleepStage.LIGHT, relSec(0.22), relSec(0.30)),
                SleepStageInterval(SleepStage.REM, relSec(0.30), relSec(0.38)),
                SleepStageInterval(SleepStage.LIGHT, relSec(0.38), relSec(0.42)),
                SleepStageInterval(SleepStage.AWAKE, relSec(0.42), relSec(0.44)),
                SleepStageInterval(SleepStage.LIGHT, relSec(0.44), relSec(0.52)),
                SleepStageInterval(SleepStage.DEEP, relSec(0.52), relSec(0.62)),
                SleepStageInterval(SleepStage.LIGHT, relSec(0.62), relSec(0.68)),
                SleepStageInterval(SleepStage.REM, relSec(0.68), relSec(0.78)),
                SleepStageInterval(SleepStage.LIGHT, relSec(0.78), relSec(0.85)),
                SleepStageInterval(SleepStage.AWAKE, relSec(0.85), relSec(0.88)),
                SleepStageInterval(SleepStage.REM, relSec(0.88), relSec(0.96)),
                SleepStageInterval(SleepStage.LIGHT, relSec(0.96), relSec(0.99)),
                SleepStageInterval(SleepStage.AWAKE, relSec(0.99), endSec)
            )

            return SleepData(
                startTimeEpochSec = startSec,
                endTimeEpochSec = endSec,
                stages = mockStages,
                isMockData = true
            )
        }
    }
}
