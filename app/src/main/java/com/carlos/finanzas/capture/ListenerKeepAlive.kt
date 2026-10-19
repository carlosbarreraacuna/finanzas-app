package com.carlos.finanzas.capture

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Xiaomi/HyperOS suele matar el proceso y no volver a enlazar el listener de notificaciones por su cuenta.
 * Esto lo revisa al abrir la app, al arrancar el proceso y cada 15 minutos, y lo reengancha si hace falta.
 */
object ListenerKeepAlive {
    private const val PREFS = "listener_keepalive"
    private const val KEY_LAST = "last_rebind"
    private const val MIN_GAP_MS = 2 * 60 * 1000L
    private const val WORK_NAME = "listener_watchdog"

    fun hasAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /** Si el permiso está dado pero el listener no está conectado, fuerza un nuevo enlace. */
    fun ensureBound(context: Context, reason: String) {
        if (!hasAccess(context) || FinanceNotificationListener.connected) return

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - prefs.getLong(KEY_LAST, 0L) < MIN_GAP_MS) return
        prefs.edit().putLong(KEY_LAST, now).apply()

        val component = ComponentName(context, FinanceNotificationListener::class.java)
        val pm = context.packageManager
        // Apagar y encender el componente obliga al sistema a enlazarlo de nuevo.
        pm.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        pm.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        NotificationListenerService.requestRebind(component)
        CaptureProcessor.logHealth(context, "REBIND_AUTO", reason)
    }

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<Watchdog>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.NONE)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    class Watchdog(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            ensureBound(applicationContext, "watchdog")
            return Result.success()
        }
    }
}
