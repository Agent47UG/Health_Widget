package com.example.healthwidget.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object HealthSyncScheduler {

    private const val WORK_NAME = "HealthWidgetPeriodicSyncWork"

    fun schedulePeriodicSync(context: Context) {
        val syncWorkRequest = PeriodicWorkRequestBuilder<HealthSyncWorker>(
            1, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
    }
}
