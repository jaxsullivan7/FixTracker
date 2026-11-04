package com.fixtracker.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fixtracker.app.data.FixRepository
import com.fixtracker.app.data.Reminder
import com.fixtracker.app.util.daysUntil
import java.time.DayOfWeek
import java.time.LocalDate

class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (!NotificationHelper.canPost(context)) return Result.success()

        val state = FixRepository(context).state.value
        val today = LocalDate.now()
        val preferences = context.getSharedPreferences("fix_notifications", Context.MODE_PRIVATE)
        val lastDigest = preferences.getString("last_digest_date", null)
        if (lastDigest == today.toString()) return Result.success()

        val alerts = mutableListOf<Pair<String, Boolean>>()

        state.reminders
            .filterNot { it.done }
            .filterNot { it.snoozedUntil?.isAfter(today) == true }
            .forEach { reminder ->
                val vehicle = state.vehicle(reminder.vehicleId) ?: return@forEach
                val days = reminder.dueDate?.let { daysUntil(it, today) }
                val kmLeft = reminder.dueKm?.let { it - vehicle.km }
                val relevant = (days != null && days <= 30) || (kmLeft != null && kmLeft <= 1_500)
                if (!relevant) return@forEach

                val urgent = (days != null && days <= 3) || (kmLeft != null && kmLeft <= 500)
                alerts += "${reminder.title} · ${describe(reminder, vehicle.km, days, kmLeft)}" to urgent
            }

        if (alerts.isEmpty()) {
            NotificationHelper.cancelDigest(context)
            return Result.success()
        }

        val anyUrgent = alerts.any { it.second }
        val isMonday = today.dayOfWeek == DayOfWeek.MONDAY
        if (!anyUrgent && !isMonday) return Result.success()

        NotificationHelper.showDigest(
            context = context,
            alerts = alerts.sortedByDescending { it.second }.map { it.first },
            urgent = anyUrgent,
        )
        preferences.edit().putString("last_digest_date", today.toString()).apply()
        return Result.success()
    }

    private fun describe(reminder: Reminder, km: Int, days: Long?, kmLeft: Int?): String {
        val parts = mutableListOf<String>()
        if (days != null) {
            parts += when {
                days < -1 -> "hace ${-days} días"
                days == -1L -> "ayer"
                days == 0L -> "hoy"
                days == 1L -> "mañana"
                else -> "en $days días"
            }
        }
        if (kmLeft != null) {
            parts += if (kmLeft > 0) "quedan ${formatKm(kmLeft)} km" else "km superados"
        }
        return parts.joinToString(" · ").ifBlank { "pendiente" }
    }

    private fun formatKm(value: Int): String =
        java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("es-ES")).format(value)
}
