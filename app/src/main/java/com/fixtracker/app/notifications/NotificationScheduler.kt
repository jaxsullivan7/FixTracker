package com.fixtracker.app.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    private const val DAILY_WORK = "fixtracker-daily-reminders"
    private const val CHECK_WORK = "fixtracker-check-now"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun runNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            CHECK_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
