package com.carlos.finanzas.ui

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.carlos.finanzas.FinanzasApp
import com.carlos.finanzas.capture.CaptureProcessor
import com.carlos.finanzas.capture.FinanceNotificationListener
import com.carlos.finanzas.data.HealthEventEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class PermissionState(
    val notificationAccess: Boolean = false,
    val sms: Boolean = false,
    val batteryExempt: Boolean = false,
    val listenerConnected: Boolean = false
)

data class HealthUiState(
    val permissions: PermissionState = PermissionState(),
    val lastNotif: Long? = null,
    val lastSms: Long? = null,
    val lastBackup: Long? = null,
    val capturesLast24h: Int = 0,
    val parseFailuresLast24h: Int = 0,
    val recentEvents: List<HealthEventEntity> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class HealthViewModel(app: Application) : AndroidViewModel(app) {
    private val health = (app as FinanzasApp).db.health()
    private val permissions = MutableStateFlow(readPermissions())

    /** Llamar en onResume: los permisos cambian fuera de la app. */
    fun refreshPermissions() {
        permissions.value = readPermissions()
    }

    /** Pide al sistema volver a enlazar el listener de notificaciones. */
    fun requestRebind() {
        val ctx: Context = getApplication()
        val component = ComponentName(ctx, FinanceNotificationListener::class.java)
        // Apagar y encender el componente fuerza al sistema a enlazar el listener de nuevo.
        ctx.packageManager.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        ctx.packageManager.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        NotificationListenerService.requestRebind(component)
        CaptureProcessor.logHealth(ctx, "REBIND_REQUESTED", "manual")
        refreshPermissions()
    }

    /** Emite la hora actual cada minuto para que la ventana de 24 h se mueva. */
    private val clock = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000)
        }
    }

    val state: StateFlow<HealthUiState> = clock.flatMapLatest { now ->
        val since = now - 24L * 60 * 60 * 1000
        combine(
            permissions,
            health.lastOf("NOTIF_CAPTURED"),
            health.lastOf("SMS_CAPTURED"),
            health.lastOf("BACKUP_OK"),
            health.countSince("NOTIF_CAPTURED", since),
            health.countSince("SMS_CAPTURED", since),
            health.countSince("PARSE_FAILED", since),
            health.recent(20)
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            HealthUiState(
                permissions = values[0] as PermissionState,
                lastNotif = values[1] as Long?,
                lastSms = values[2] as Long?,
                lastBackup = values[3] as Long?,
                capturesLast24h = (values[4] as Int) + (values[5] as Int),
                parseFailuresLast24h = values[6] as Int,
                recentEvents = values[7] as List<HealthEventEntity>
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HealthUiState())

    private fun readPermissions(): PermissionState {
        val ctx: Context = getApplication()
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        return PermissionState(
            notificationAccess = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName),
            sms = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED,
            batteryExempt = pm.isIgnoringBatteryOptimizations(ctx.packageName),
            listenerConnected = FinanceNotificationListener.connected
        )
    }
}
