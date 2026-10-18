package com.carlos.finanzas.debt

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.carlos.finanzas.FinanzasApp
import com.carlos.finanzas.MainActivity
import com.carlos.finanzas.ui.formatMoney
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * Cada día a las 8:00 avisa de pagos o cobros que vencen en los próximos 3 días y de tarjetas con el
 * cupo usado al 80 % o más. Cada aviso se envía una sola vez (por fecha de vencimiento o por mes).
 */
class DebtReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as FinanzasApp
        val debts = app.db.debts().activeNow()
        val today = LocalDate.now()
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        for (d in debts) {
            if (d.balanceCents <= 0) continue

            val next = DebtDates.nextDueDate(today, d.dueDay)
            if (next != null) {
                val days = ChronoUnit.DAYS.between(today, next)
                if (days in 0L..3L) {
                    val title = if (d.direction == "OWE") "Pago próximo: ${d.name}" else "Cobro próximo: ${d.name}"
                    val whenText = when (days) {
                        0L -> "hoy"
                        1L -> "mañana"
                        else -> "en $days días"
                    }
                    val amount = if (d.monthlyPaymentCents > 0) " (${formatMoney(d.monthlyPaymentCents)})" else ""
                    notifyOnce(prefs, "due:${d.id}:$next", d.id.toInt() * 10, title, "Vence $whenText$amount")
                }
            }

            if (d.kind == "CARD" && d.direction == "OWE" && d.creditLimitCents > 0 &&
                d.balanceCents * 100 >= d.creditLimitCents * 80
            ) {
                val pct = d.balanceCents * 100 / d.creditLimitCents
                notifyOnce(
                    prefs, "usage:${d.id}:${YearMonth.from(today)}", d.id.toInt() * 10 + 1,
                    "Cupo alto: ${d.name}", "Estás usando el $pct % de tu cupo"
                )
            }
        }
        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun notifyOnce(prefs: SharedPreferences, key: String, id: Int, title: String, text: String) {
        if (prefs.getBoolean(key, false)) return
        val manager = NotificationManagerCompat.from(applicationContext)
        if (!manager.areNotificationsEnabled()) return

        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName("Recordatorios de deudas")
                .build()
        )
        val open = PendingIntent.getActivity(
            applicationContext,
            id,
            Intent(applicationContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(id, notification)
        } catch (e: SecurityException) {
            return // sin permiso de notificaciones: se reintenta mañana
        }
        prefs.edit().putBoolean(key, true).apply()
    }

    companion object {
        private const val NAME = "debt_reminders"
        private const val PREFS = "debt_reminders"
        private const val CHANNEL = "debt_reminders"

        /** Programa la revisión diaria; si ya existe no la reinicia. */
        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            var first = now.toLocalDate().atTime(8, 0)
            if (!first.isAfter(now)) first = first.plusDays(1)
            val request = PeriodicWorkRequestBuilder<DebtReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(Duration.between(now, first).toMinutes(), TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
