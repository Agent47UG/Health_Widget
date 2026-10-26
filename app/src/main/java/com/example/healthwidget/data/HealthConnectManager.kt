package com.example.healthwidget.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.healthwidget.model.HealthData
import com.example.healthwidget.model.SleepData
import com.example.healthwidget.model.SleepStage
import com.example.healthwidget.model.SleepStageInterval
import com.example.healthwidget.model.VitalMetric
import com.example.healthwidget.model.VitalsData
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class HealthConnectManager(private val context: Context) {

    val healthConnectClient: HealthConnectClient? by lazy {
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }

    val requiredPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(RespiratoryRateRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(SkinTemperatureRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class),
        "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"
    )

    fun isSdkAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    suspend fun getGrantedPermissions(): Set<String> {
        val client = healthConnectClient ?: return emptySet()
        return try {
            client.permissionController.getGrantedPermissions()
        } catch (_: Exception) {
            emptySet()
        }
    }

    suspend fun hasAnyPermission(): Boolean {
        val granted = getGrantedPermissions()
        return requiredPermissions.any { it in granted }
    }

    suspend fun hasAllPermissions(): Boolean {
        val granted = getGrantedPermissions()
        return requiredPermissions.all { it in granted }
    }

    fun openHealthConnectSettings() {
        try {
            val intent = android.content.Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS")
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) { }
        }
    }

    suspend fun readTodayHealthData(currentGoals: HealthData): HealthData {
        val client = healthConnectClient ?: return currentGoals

        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endTime = now.toInstant()
        val timeFilter = TimeRangeFilter.between(startOfDay, endTime)

        var totalSteps = 0
        var totalCalories = 0
        var totalExerciseMinutes = 0
        var totalDistanceMeters = 0.0

        try {
            // Aggregate steps if possible
            val stepsResponse = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = timeFilter
                )
            )
            totalSteps = stepsResponse[StepsRecord.COUNT_TOTAL]?.toInt() ?: 0
        } catch (e: Exception) {
            // Fallback to reading records directly if aggregation is unavailable
            try {
                val stepsRecords = client.readRecords(
                    ReadRecordsRequest(
                        recordType = StepsRecord::class,
                        timeRangeFilter = timeFilter
                    )
                )
                totalSteps = stepsRecords.records.sumOf { it.count.toInt() }
            } catch (_: Exception) { }
        }

        try {
            // Total Calories Burned from Health Connect
            val totalCalResponse = client.aggregate(
                AggregateRequest(
                    metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                    timeRangeFilter = timeFilter
                )
            )
            val energy = totalCalResponse[TotalCaloriesBurnedRecord.ENERGY_TOTAL]
            totalCalories = energy?.inKilocalories?.toInt() ?: 0
        } catch (_: Exception) { }

        if (totalCalories == 0) {
            try {
                val totalCalRecords = client.readRecords(
                    ReadRecordsRequest(
                        recordType = TotalCaloriesBurnedRecord::class,
                        timeRangeFilter = timeFilter
                    )
                )
                totalCalories = totalCalRecords.records.sumOf { it.energy.inKilocalories }.toInt()
            } catch (_: Exception) { }
        }

        if (totalCalories == 0) {
            try {
                // Fallback to Active Calories if Total Calories is not available
                val caloriesResponse = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                        timeRangeFilter = timeFilter
                    )
                )
                val energy = caloriesResponse[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
                totalCalories = energy?.inKilocalories?.toInt() ?: 0
            } catch (_: Exception) { }
        }

        try {
            // Exercise duration from sessions
            val exerciseRecords = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = timeFilter
                )
            )
            val totalSeconds = exerciseRecords.records.sumOf {
                java.time.Duration.between(it.startTime, it.endTime).seconds
            }
            totalExerciseMinutes = (totalSeconds / 60).toInt()
        } catch (_: Exception) { }

        try {
            // Distance record
            val distanceResponse = client.aggregate(
                AggregateRequest(
                    metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                    timeRangeFilter = timeFilter
                )
            )
            val distance = distanceResponse[DistanceRecord.DISTANCE_TOTAL]
            totalDistanceMeters = distance?.inMeters ?: 0.0
        } catch (e: Exception) {
            try {
                val distanceRecords = client.readRecords(
                    ReadRecordsRequest(
                        recordType = DistanceRecord::class,
                        timeRangeFilter = timeFilter
                    )
                )
                totalDistanceMeters = distanceRecords.records.sumOf { it.distance.inMeters }
            } catch (_: Exception) { }
        }

        val distanceKm = (totalDistanceMeters / 1000.0).toFloat()

        var calculatedRatios = currentGoals.stepBarRatios
        try {
            val startOf7Days = LocalDate.now().minusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant()
            val weekFilter = TimeRangeFilter.between(startOf7Days, endTime)
            val weekStepsRecords = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = weekFilter
                )
            )
            if (weekStepsRecords.records.isNotEmpty()) {
                val dailyMap = weekStepsRecords.records.groupBy {
                    it.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
                }.mapValues { entry -> entry.value.sumOf { it.count } }

                val last7Days = (0..6).map { dayOffset ->
                    val date = LocalDate.now().minusDays((6 - dayOffset).toLong())
                    (dailyMap[date] ?: 0L).toFloat()
                }

                if (last7Days.any { it > 0f }) {
                    calculatedRatios = last7Days.map { count ->
                        (count / currentGoals.stepGoal.toFloat()).coerceIn(0.08f, 1.0f)
                    }
                }
            }
        } catch (_: Exception) { }

        return HealthData(
            steps = totalSteps,
            stepGoal = currentGoals.stepGoal,
            calories = totalCalories,
            calorieGoal = currentGoals.calorieGoal,
            exerciseMinutes = totalExerciseMinutes,
            exerciseGoal = currentGoals.exerciseGoal,
            distanceKm = distanceKm,
            isMockData = false,
            stepBarRatios = calculatedRatios
        )
    }

    suspend fun readTodaySleepData(): SleepData {
        val client = healthConnectClient ?: return SleepData.mock()

        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val startOfPastDays = LocalDate.now().minusDays(3).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endTime = now.toInstant()
        val timeFilter = TimeRangeFilter.between(startOfPastDays, endTime)

        try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = timeFilter
                )
            )
            val latestSession = response.records.maxByOrNull { it.endTime }
            if (latestSession != null) {
                var stageIntervals = latestSession.stages.map { stageRecord ->
                    val stage = when (stageRecord.stage) {
                        SleepSessionRecord.STAGE_TYPE_AWAKE -> SleepStage.AWAKE
                        SleepSessionRecord.STAGE_TYPE_REM -> SleepStage.REM
                        SleepSessionRecord.STAGE_TYPE_LIGHT -> SleepStage.LIGHT
                        SleepSessionRecord.STAGE_TYPE_DEEP -> SleepStage.DEEP
                        SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> SleepStage.OUT_OF_BED
                        else -> SleepStage.LIGHT
                    }
                    SleepStageInterval(
                        stage = stage,
                        startEpochSec = stageRecord.startTime.epochSecond,
                        endEpochSec = stageRecord.endTime.epochSecond
                    )
                }

                if (stageIntervals.isEmpty()) {
                    stageIntervals = listOf(
                        SleepStageInterval(
                            stage = SleepStage.LIGHT,
                            startEpochSec = latestSession.startTime.epochSecond,
                            endEpochSec = latestSession.endTime.epochSecond
                        )
                    )
                }

                return SleepData(
                    startTimeEpochSec = latestSession.startTime.epochSecond,
                    endTimeEpochSec = latestSession.endTime.epochSecond,
                    stages = stageIntervals,
                    isMockData = false
                )
            }
        } catch (_: Exception) { }

        return SleepData.mock()
    }

    suspend fun readVitalsData(): VitalsData {
        val client = healthConnectClient ?: return VitalsData.mock()

        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val startOf30Days = LocalDate.now().minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val timeFilter = TimeRangeFilter.between(startOf30Days, now.toInstant())

        fun padToSeven(list: List<Float>, fallbackVal: Float): List<Float> {
            if (list.isEmpty()) return List(7) { fallbackVal }
            if (list.size >= 7) return list.takeLast(7)
            val lastVal = list.last()
            val padding = List(7 - list.size) { lastVal }
            return padding + list
        }

        var isAnyRealData = false

        // 1. Breathing Rate (BR)
        var brValue = "18"
        var brWeekly = listOf(17f, 18f, 16f, 17.5f, 18.2f, 17.8f, 18.0f)
        var brStatus = "No records in Health Connect"

        try {
            val brRecords = client.readRecords(
                ReadRecordsRequest(recordType = RespiratoryRateRecord::class, timeRangeFilter = timeFilter)
            )
            if (brRecords.records.isNotEmpty()) {
                isAnyRealData = true
                val latest = brRecords.records.last()
                brValue = latest.rate.toInt().toString()
                val history = brRecords.records.map { it.rate.toFloat() }
                brWeekly = padToSeven(history, brValue.toFloatOrNull() ?: 18f)
                brStatus = "Health Connect"
            }
        } catch (e: SecurityException) {
            brStatus = "Permission required"
        } catch (_: Exception) { }

        // 2. Oxygen Saturation (SpO2)
        var spo2Value = "98"
        var spo2Weekly = listOf(98f, 97f, 98.5f, 98f, 97.5f, 98.2f, 98.0f)
        var spo2Status = "No records in Health Connect"

        try {
            val spo2Records = client.readRecords(
                ReadRecordsRequest(recordType = OxygenSaturationRecord::class, timeRangeFilter = timeFilter)
            )
            if (spo2Records.records.isNotEmpty()) {
                isAnyRealData = true
                val latest = spo2Records.records.last()
                spo2Value = latest.percentage.value.toInt().toString()
                val history = spo2Records.records.map { it.percentage.value.toFloat() }
                spo2Weekly = padToSeven(history, spo2Value.toFloatOrNull() ?: 98f)
                spo2Status = "Health Connect"
            }
        } catch (e: SecurityException) {
            spo2Status = "Permission required"
        } catch (_: Exception) { }

        // 3. Resting Heart Rate (RHR)
        var rhrValue = "72"
        var rhrWeekly = listOf(68f, 70f, 71f, 73f, 72.5f, 71.8f, 72.0f)
        var rhrStatus = "No records in Health Connect"

        try {
            val rhrRecords = client.readRecords(
                ReadRecordsRequest(recordType = RestingHeartRateRecord::class, timeRangeFilter = timeFilter)
            )
            if (rhrRecords.records.isNotEmpty()) {
                isAnyRealData = true
                val latest = rhrRecords.records.last()
                rhrValue = latest.beatsPerMinute.toInt().toString()
                val history = rhrRecords.records.map { it.beatsPerMinute.toFloat() }
                rhrWeekly = padToSeven(history, rhrValue.toFloatOrNull() ?: 72f)
                rhrStatus = "Health Connect"
            } else {
                val hrRecords = client.readRecords(
                    ReadRecordsRequest(recordType = HeartRateRecord::class, timeRangeFilter = timeFilter)
                )
                if (hrRecords.records.isNotEmpty()) {
                    isAnyRealData = true
                    val allSamples = hrRecords.records.flatMap { it.samples }
                    if (allSamples.isNotEmpty()) {
                        rhrValue = allSamples.last().beatsPerMinute.toInt().toString()
                        val sampleHistory = allSamples.map { it.beatsPerMinute.toFloat() }
                        rhrWeekly = padToSeven(sampleHistory, rhrValue.toFloatOrNull() ?: 72f)
                        rhrStatus = "Health Connect"
                    }
                }
            }
        } catch (e: SecurityException) {
            rhrStatus = "Permission required"
        } catch (_: Exception) { }

        // 4. Heart Rate Variability (HRV)
        var hrvValue = "45"
        var hrvWeekly = listOf(40f, 42f, 44f, 48f, 46f, 45f, 45.0f)
        var hrvStatus = "No records in Health Connect"

        try {
            val hrvRecords = client.readRecords(
                ReadRecordsRequest(recordType = HeartRateVariabilityRmssdRecord::class, timeRangeFilter = timeFilter)
            )
            if (hrvRecords.records.isNotEmpty()) {
                isAnyRealData = true
                val latest = hrvRecords.records.last()
                hrvValue = latest.heartRateVariabilityMillis.toInt().toString()
                val history = hrvRecords.records.map { it.heartRateVariabilityMillis.toFloat() }
                hrvWeekly = padToSeven(history, hrvValue.toFloatOrNull() ?: 45f)
                hrvStatus = "Health Connect"
            }
        } catch (e: SecurityException) {
            hrvStatus = "Permission required"
        } catch (_: Exception) { }

        // 5. Skin Temperature
        var tempValue = "+0.2"
        var tempWeekly = listOf(0.0f, 0.1f, 0.1f, 0.2f, 0.3f, 0.2f, 0.2f)
        var tempStatus = "No records in Health Connect"

        try {
            val tempRecords = client.readRecords(
                ReadRecordsRequest(recordType = SkinTemperatureRecord::class, timeRangeFilter = timeFilter)
            )
            if (tempRecords.records.isNotEmpty()) {
                isAnyRealData = true
                val allDeltas = tempRecords.records.flatMap { it.deltas }
                if (allDeltas.isNotEmpty()) {
                    val latest = allDeltas.last()
                    tempValue = String.format(java.util.Locale.US, "%+.1f", latest.delta.inCelsius)
                    val history = allDeltas.map { it.delta.inCelsius.toFloat() }
                    tempWeekly = padToSeven(history, latest.delta.inCelsius.toFloat())
                    tempStatus = "Health Connect"
                }
            }
        } catch (e: SecurityException) {
            tempStatus = "Permission required"
        } catch (_: Exception) { }

        // 6. Nutrition Records (Calories consumed, Protein, Fat, Carbs)
        var calConsumedValue = "1,850"
        var calConsumedWeekly = listOf(1750f, 1900f, 1800f, 2100f, 1650f, 1950f, 1850f)
        var calConsumedStatus = "No records in Health Connect"

        var proteinValue = "85"
        var proteinWeekly = listOf(75f, 90f, 80f, 95f, 70f, 88f, 85f)
        var proteinStatus = "No records in Health Connect"

        var fatValue = "62"
        var fatWeekly = listOf(58f, 65f, 60f, 72f, 55f, 68f, 62f)
        var fatStatus = "No records in Health Connect"

        var carbsValue = "210"
        var carbsWeekly = listOf(190f, 220f, 205f, 240f, 180f, 215f, 210f)
        var carbsStatus = "No records in Health Connect"

        try {
            val nutritionRecords = client.readRecords(
                ReadRecordsRequest(recordType = NutritionRecord::class, timeRangeFilter = timeFilter)
            )
            if (nutritionRecords.records.isNotEmpty()) {
                isAnyRealData = true

                val dailyNutritionMap = nutritionRecords.records.groupBy {
                    it.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
                }

                val todayDate = LocalDate.now()
                val todayRecords = dailyNutritionMap[todayDate] ?: emptyList()

                val todayCal = todayRecords.mapNotNull { it.energy?.inKilocalories }.sum().toInt()
                val todayProtein = todayRecords.mapNotNull { it.protein?.inGrams }.sum().toInt()
                val todayFat = todayRecords.mapNotNull { it.totalFat?.inGrams }.sum().toInt()
                val todayCarbs = todayRecords.mapNotNull { it.totalCarbohydrate?.inGrams }.sum().toInt()

                if (todayCal > 0) {
                    calConsumedValue = String.format(java.util.Locale.US, "%,d", todayCal)
                }
                calConsumedStatus = "Health Connect"

                if (todayProtein > 0) {
                    proteinValue = todayProtein.toString()
                }
                proteinStatus = "Health Connect"

                if (todayFat > 0) {
                    fatValue = todayFat.toString()
                }
                fatStatus = "Health Connect"

                if (todayCarbs > 0) {
                    carbsValue = todayCarbs.toString()
                }
                carbsStatus = "Health Connect"

                val last7DaysCal = (0..6).map { dayOffset ->
                    val date = LocalDate.now().minusDays((6 - dayOffset).toLong())
                    (dailyNutritionMap[date] ?: emptyList()).mapNotNull { it.energy?.inKilocalories }.sum().toFloat()
                }
                if (last7DaysCal.any { it > 0f }) calConsumedWeekly = last7DaysCal

                val last7DaysProtein = (0..6).map { dayOffset ->
                    val date = LocalDate.now().minusDays((6 - dayOffset).toLong())
                    (dailyNutritionMap[date] ?: emptyList()).mapNotNull { it.protein?.inGrams }.sum().toFloat()
                }
                if (last7DaysProtein.any { it > 0f }) proteinWeekly = last7DaysProtein

                val last7DaysFat = (0..6).map { dayOffset ->
                    val date = LocalDate.now().minusDays((6 - dayOffset).toLong())
                    (dailyNutritionMap[date] ?: emptyList()).mapNotNull { it.totalFat?.inGrams }.sum().toFloat()
                }
                if (last7DaysFat.any { it > 0f }) fatWeekly = last7DaysFat

                val last7DaysCarbs = (0..6).map { dayOffset ->
                    val date = LocalDate.now().minusDays((6 - dayOffset).toLong())
                    (dailyNutritionMap[date] ?: emptyList()).mapNotNull { it.totalCarbohydrate?.inGrams }.sum().toFloat()
                }
                if (last7DaysCarbs.any { it > 0f }) carbsWeekly = last7DaysCarbs
            }
        } catch (e: SecurityException) {
            calConsumedStatus = "Permission required"
            proteinStatus = "Permission required"
            fatStatus = "Permission required"
            carbsStatus = "Permission required"
        } catch (_: Exception) { }

        val metrics = listOf(
            VitalMetric("br", "Breathing rate (BR)", brValue, "brpm", brWeekly, brStatus),
            VitalMetric("spo2", "Blood oxygen (SpO2)", spo2Value, "%", spo2Weekly, spo2Status),
            VitalMetric("rhr", "Resting heart rate (RHR)", rhrValue, "bpm", rhrWeekly, rhrStatus),
            VitalMetric("hrv", "Heart rate variability (HRV)", hrvValue, "ms", hrvWeekly, hrvStatus),
            VitalMetric("temp", "Skin temperature variation", tempValue, "°C", tempWeekly, tempStatus),
            VitalMetric("cal_consumed", "Calories consumed", calConsumedValue, "kcal", calConsumedWeekly, calConsumedStatus),
            VitalMetric("protein", "Protein", proteinValue, "g", proteinWeekly, proteinStatus),
            VitalMetric("fat", "Fat", fatValue, "g", fatWeekly, fatStatus),
            VitalMetric("carbs", "Carbs", carbsValue, "g", carbsWeekly, carbsStatus)
        )

        return VitalsData(metrics = metrics, isMockData = !isAnyRealData)
    }
}
