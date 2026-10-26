package com.example.healthwidget.model

data class VitalMetric(
    val id: String,
    val title: String,
    val valueText: String,
    val unitText: String,
    val weeklyValues: List<Float>,
    val statusText: String = "Today"
)

data class VitalsData(
    val metrics: List<VitalMetric>,
    val isMockData: Boolean = false
) {
    companion object {
        fun mock(): VitalsData {
            val items = listOf(
                VitalMetric(
                    id = "br",
                    title = "Breathing rate (BR)",
                    valueText = "18",
                    unitText = "brpm",
                    weeklyValues = listOf(17f, 18f, 16f, 17.5f, 18.2f, 17.8f, 18.0f),
                    statusText = "Today • Demo Data"
                ),
                VitalMetric(
                    id = "spo2",
                    title = "Blood oxygen (SpO2)",
                    valueText = "98",
                    unitText = "%",
                    weeklyValues = listOf(98f, 97f, 98.5f, 98f, 97.5f, 98.2f, 98.0f),
                    statusText = "Today • Demo Data"
                ),
                VitalMetric(
                    id = "rhr",
                    title = "Resting heart rate (RHR)",
                    valueText = "72",
                    unitText = "bpm",
                    weeklyValues = listOf(68f, 70f, 71f, 73f, 72.5f, 71.8f, 72.0f),
                    statusText = "Today • Demo Data"
                ),
                VitalMetric(
                    id = "hrv",
                    title = "Heart rate variability (HRV)",
                    valueText = "45",
                    unitText = "ms",
                    weeklyValues = listOf(40f, 42f, 44f, 48f, 46f, 45f, 45.0f),
                    statusText = "Today • Demo Data"
                ),
                VitalMetric(
                    id = "temp",
                    title = "Skin temperature variation",
                    valueText = "+0.2",
                    unitText = "°C",
                    weeklyValues = listOf(0.0f, 0.1f, 0.1f, 0.2f, 0.3f, 0.2f, 0.2f),
                    statusText = "Today • Demo Data"
                ),
                VitalMetric(
                    id = "cal_consumed",
                    title = "Calories consumed",
                    valueText = "1,850",
                    unitText = "kcal",
                    weeklyValues = listOf(1750f, 1900f, 1800f, 2100f, 1650f, 1950f, 1850f),
                    statusText = "Today • Demo Data"
                ),
                VitalMetric(
                    id = "protein",
                    title = "Protein",
                    valueText = "85",
                    unitText = "g",
                    weeklyValues = listOf(75f, 90f, 80f, 95f, 70f, 88f, 85f),
                    statusText = "Today • Demo Data"
                ),
                VitalMetric(
                    id = "fat",
                    title = "Fat",
                    valueText = "62",
                    unitText = "g",
                    weeklyValues = listOf(58f, 65f, 60f, 72f, 55f, 68f, 62f),
                    statusText = "Today • Demo Data"
                ),
                VitalMetric(
                    id = "carbs",
                    title = "Carbs",
                    valueText = "210",
                    unitText = "g",
                    weeklyValues = listOf(190f, 220f, 205f, 240f, 180f, 215f, 210f),
                    statusText = "Today • Demo Data"
                )
            )
            return VitalsData(metrics = items, isMockData = true)
        }
    }
}
