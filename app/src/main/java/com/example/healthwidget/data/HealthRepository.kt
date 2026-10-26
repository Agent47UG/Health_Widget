package com.example.healthwidget.data

import android.content.Context
import android.content.SharedPreferences
import com.example.healthwidget.model.HealthData
import com.example.healthwidget.model.SleepData
import com.example.healthwidget.model.SleepStage
import com.example.healthwidget.model.SleepStageInterval
import com.example.healthwidget.model.VitalMetric
import com.example.healthwidget.model.VitalsData

class HealthRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val healthConnectManager = HealthConnectManager(context)

    fun getHealthData(): HealthData {
        val isMock = prefs.getBoolean(KEY_IS_MOCK, false)
        val defaultSteps = if (isMock) 5358 else 0
        val defaultCalories = if (isMock) 230 else 0
        val defaultExercise = if (isMock) 21 else 0
        val defaultDistance = if (isMock) 10.0f else 0.0f

        val steps = prefs.getInt(KEY_STEPS, defaultSteps)
        val stepGoal = prefs.getInt(KEY_STEP_GOAL, 8000)
        val calories = prefs.getInt(KEY_CALORIES, defaultCalories)
        val calorieGoal = prefs.getInt(KEY_CALORIE_GOAL, 400)
        val exerciseMinutes = prefs.getInt(KEY_EXERCISE, defaultExercise)
        val exerciseGoal = prefs.getInt(KEY_EXERCISE_GOAL, 30)
        val distanceKm = prefs.getFloat(KEY_DISTANCE, defaultDistance)

        return HealthData(
            steps = steps,
            stepGoal = stepGoal,
            calories = calories,
            calorieGoal = calorieGoal,
            exerciseMinutes = exerciseMinutes,
            exerciseGoal = exerciseGoal,
            distanceKm = distanceKm,
            isMockData = isMock
        )
    }

    fun saveHealthData(data: HealthData) {
        prefs.edit()
            .putInt(KEY_STEPS, data.steps)
            .putInt(KEY_STEP_GOAL, data.stepGoal)
            .putInt(KEY_CALORIES, data.calories)
            .putInt(KEY_CALORIE_GOAL, data.calorieGoal)
            .putInt(KEY_EXERCISE, data.exerciseMinutes)
            .putInt(KEY_EXERCISE_GOAL, data.exerciseGoal)
            .putFloat(KEY_DISTANCE, data.distanceKm)
            .putBoolean(KEY_IS_MOCK, data.isMockData)
            .apply()
    }

    fun updateGoals(stepGoal: Int, calorieGoal: Int, exerciseGoal: Int) {
        prefs.edit()
            .putInt(KEY_STEP_GOAL, stepGoal)
            .putInt(KEY_CALORIE_GOAL, calorieGoal)
            .putInt(KEY_EXERCISE_GOAL, exerciseGoal)
            .apply()
    }

    fun setMockMode(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_IS_MOCK, enabled)
            .apply()
    }

    fun saveSleepData(data: SleepData) {
        if (data.isMockData) return
        val stagesString = data.stages.joinToString(";") { "${it.stage.name},${it.startEpochSec},${it.endEpochSec}" }
        prefs.edit()
            .putLong(KEY_SLEEP_START, data.startTimeEpochSec)
            .putLong(KEY_SLEEP_END, data.endTimeEpochSec)
            .putString(KEY_SLEEP_STAGES, stagesString)
            .apply()
    }

    fun getCachedSleepData(): SleepData? {
        val startSec = prefs.getLong(KEY_SLEEP_START, 0L)
        val endSec = prefs.getLong(KEY_SLEEP_END, 0L)
        val stagesStr = prefs.getString(KEY_SLEEP_STAGES, null) ?: return null
        if (startSec <= 0L || endSec <= 0L) return null

        val stages = try {
            stagesStr.split(";").mapNotNull { part ->
                val tokens = part.split(",")
                if (tokens.size == 3) {
                    val stage = SleepStage.valueOf(tokens[0])
                    val start = tokens[1].toLong()
                    val end = tokens[2].toLong()
                    SleepStageInterval(stage, start, end)
                } else null
            }
        } catch (_: Exception) {
            emptyList()
        }

        return SleepData(
            startTimeEpochSec = startSec,
            endTimeEpochSec = endSec,
            stages = stages,
            isMockData = false
        )
    }

    suspend fun getSleepData(): SleepData {
        val isMock = prefs.getBoolean(KEY_IS_MOCK, false)
        if (isMock) return SleepData.mock()

        if (healthConnectManager.isSdkAvailable() && healthConnectManager.hasAnyPermission()) {
            val freshData = healthConnectManager.readTodaySleepData()
            if (!freshData.isMockData) {
                saveSleepData(freshData)
                return freshData
            }
        }

        return getCachedSleepData() ?: SleepData.mock()
    }

    fun saveVitalsData(data: VitalsData) {
        if (data.isMockData) return
        val vitalsString = data.metrics.joinToString(";") { m ->
            val weeklyStr = m.weeklyValues.joinToString(",")
            "${m.id}|${m.title}|${m.valueText}|${m.unitText}|${m.statusText}|$weeklyStr"
        }
        prefs.edit()
            .putString(KEY_VITALS_METRICS, vitalsString)
            .apply()
    }

    fun getCachedVitalsData(): VitalsData? {
        val vitalsStr = prefs.getString(KEY_VITALS_METRICS, null) ?: return null
        val metrics = try {
            vitalsStr.split(";").mapNotNull { part ->
                val tokens = part.split("|")
                if (tokens.size >= 5) {
                    val id = tokens[0]
                    val title = tokens[1]
                    val valText = tokens[2]
                    val unitText = tokens[3]
                    val statusText = tokens[4]
                    val trend = if (tokens.size > 5 && tokens[5].isNotEmpty()) {
                        tokens[5].split(",").mapNotNull { it.toFloatOrNull() }
                    } else emptyList()

                    VitalMetric(id, title, valText, unitText, trend, statusText)
                } else null
            }
        } catch (_: Exception) {
            emptyList()
        }

        return if (metrics.isNotEmpty()) VitalsData(metrics = metrics, isMockData = false) else null
    }

    suspend fun getVitalsData(): VitalsData {
        val isMock = prefs.getBoolean(KEY_IS_MOCK, false)
        if (isMock) return VitalsData.mock()

        if (healthConnectManager.isSdkAvailable() && healthConnectManager.hasAnyPermission()) {
            val freshData = healthConnectManager.readVitalsData()
            if (!freshData.isMockData) {
                saveVitalsData(freshData)
                return freshData
            }
        }

        return getCachedVitalsData() ?: VitalsData.mock()
    }

    companion object {
        private const val PREFS_NAME = "health_widget_prefs"
        private const val KEY_STEPS = "key_steps"
        private const val KEY_STEP_GOAL = "key_step_goal"
        private const val KEY_CALORIES = "key_calories"
        private const val KEY_CALORIE_GOAL = "key_calorie_goal"
        private const val KEY_EXERCISE = "key_exercise"
        private const val KEY_EXERCISE_GOAL = "key_exercise_goal"
        private const val KEY_DISTANCE = "key_distance"
        private const val KEY_IS_MOCK = "key_is_mock"
        private const val KEY_SLEEP_START = "key_sleep_start"
        private const val KEY_SLEEP_END = "key_sleep_end"
        private const val KEY_SLEEP_STAGES = "key_sleep_stages"
        private const val KEY_VITALS_METRICS = "key_vitals_metrics"
    }
}
