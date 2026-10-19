package com.carlos.finanzas.capture

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.carlos.finanzas.MainActivity
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

/**
 * Xiaomi/HyperOS suele matar el proceso y no volver a enlazar el listener de notificaciones por su cuenta.
 * Esto lo revisa al abrir la app, al arrancar el proceso y cada 15 minutos, y lo reengancha si hace falta.
 */
object ListenerKeepAlive {
    private const val PREFS = "listener_keepalive"
    private const val KEY_LAST = "last_rebind"
    private const val MIN_GAP_MS = 2 * 60 * 1000L
    private const val WORK_NAME = "listener_watchdog"
    private const val CHANNEL = "listener_alerts"
    private const val ALERT_ID = 9001

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

    /** Avisa que la captura está caída. Es una sola notificación que se actualiza, sin repetir el sonido. */
    @SuppressLint("MissingPermission")
    fun notifyDown(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val accessMissing = !hasAccess(context)

        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName("Alertas de captura")
                .build()
        )
        val target = if (accessMissing) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        } else {
            Intent(context, MainActivity::class.java)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val open = PendingIntent.getActivity(
            context, ALERT_ID, target, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val text = if (accessMissing) {
            "El acceso a notificaciones está desactivado. Toca para activarlo; mientras tanto no se captura nada."
        } else {
            "No se pudo reconectar el servicio de captura. Abre la app y toca Reintentar enlace en Salud."
        }
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Finanzas dejó de capturar movimientos")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        try {
            manager.notify(ALERT_ID, notification)
            CaptureProcessor.logHealth(context, "ALERT_SHOWN", if (accessMissing) "sin_acceso" else "desconectado")
        } catch (e: SecurityException) {
            // sin permiso de notificaciones
        }
    }

    /** Quita el aviso cuando el listener vuelve a conectarse. */
    fun clearAlert(context: Context) {
        NotificationManagerCompat.from(context).cancel(ALERT_ID)
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
            val ctx = applicationContext
            if (hasAccess(ctx) && !FinanceNotificationListener.connected) {
                ensureBound(ctx, "watchdog")
                delay(10_000) // tiempo para que el sistema enlace el servicio
            }
            if (!hasAccess(ctx) || !FinanceNotificationListener.connected) notifyDown(ctx) else clearAlert(ctx)
            return Result.success()
        }
    }
}
