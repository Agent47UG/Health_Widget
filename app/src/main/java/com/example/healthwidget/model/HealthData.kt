package com.example.healthwidget.model

data class HealthData(
    val steps: Int = 5358,
    val stepGoal: Int = 8000,
    val calories: Int = 230,
    val calorieGoal: Int = 400,
    val exerciseMinutes: Int = 21,
    val exerciseGoal: Int = 30,
    val distanceKm: Float = 10.0f,
    val isMockData: Boolean = true,
    val stepBarRatios: List<Float> = listOf(0.42f, 0.70f, 0.25f, 0.48f, 0.45f, 0.82f, 0.65f)
) {
    val stepProgress: Float
        get() = if (stepGoal > 0) (steps.toFloat() / stepGoal).coerceAtLeast(0f) else 0f

    val calorieProgress: Float
        get() = if (calorieGoal > 0) (calories.toFloat() / calorieGoal).coerceAtLeast(0f) else 0f

    val exerciseProgress: Float
        get() = if (exerciseGoal > 0) (exerciseMinutes.toFloat() / exerciseGoal).coerceAtLeast(0f) else 0f
}
