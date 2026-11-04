package com.fixtracker.app

import android.app.Application
import com.fixtracker.app.notifications.NotificationHelper
import com.fixtracker.app.notifications.NotificationScheduler

class FixTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
        NotificationScheduler.schedule(this)
    }
}
