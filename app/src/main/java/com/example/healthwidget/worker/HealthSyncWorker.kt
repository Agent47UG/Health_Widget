package com.example.healthwidget.worker

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.healthwidget.data.HealthConnectManager
import com.example.healthwidget.data.HealthRepository
import com.example.healthwidget.widget.HealthWidget
import com.example.healthwidget.widget.SleepVitalsWidget
import com.example.healthwidget.widget.SleepWidget
import com.example.healthwidget.widget.VitalsWidget

class HealthSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val repository = HealthRepository(context)
        val healthConnectManager = HealthConnectManager(context)

        try {
            if (healthConnectManager.isSdkAvailable() && healthConnectManager.hasAnyPermission()) {
                val currentHealthData = repository.getHealthData()
                if (!currentHealthData.isMockData) {
                    val freshHealthData = healthConnectManager.readTodayHealthData(currentHealthData)
                    repository.saveHealthData(freshHealthData)

                    val freshSleepData = healthConnectManager.readTodaySleepData()
                    repository.saveSleepData(freshSleepData)

                    val freshVitalsData = healthConnectManager.readVitalsData()
                    repository.saveVitalsData(freshVitalsData)
                }
            }

            // Trigger Glance Widget updates
            HealthWidget().updateAll(context)
            SleepWidget().updateAll(context)
            VitalsWidget().updateAll(context)
            SleepVitalsWidget().updateAll(context)

            return Result.success()
        } catch (_: Exception) {
            return Result.retry()
        }
    }
}
