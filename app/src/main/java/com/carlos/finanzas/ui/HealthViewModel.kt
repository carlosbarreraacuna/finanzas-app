package com.carlos.finanzas.ui

import android.app.Application
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.carlos.finanzas.FinanzasApp
import com.carlos.finanzas.capture.FinanceNotificationListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    val capturesLast24h: Int = 0,
    val parseFailuresLast24h: Int = 0
)

class HealthViewModel(app: Application) : AndroidViewModel(app) {
    private val health = (app as FinanzasApp).db.health()
    private val permissions = MutableStateFlow(readPermissions())

    /** Llamar en onResume: los permisos cambian fuera de la app. */
    fun refreshPermissions() {
        permissions.value = readPermissions()
    }

    val state: StateFlow<HealthUiState> = run {
        val since = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        combine(
            permissions,
            health.lastOf("NOTIF_CAPTURED"),
            health.lastOf("SMS_CAPTURED"),
            health.countSince("NOTIF_CAPTURED", since),
            health.countSince("SMS_CAPTURED", since),
            health.countSince("PARSE_FAILED", since)
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            HealthUiState(
                permissions = values[0] as PermissionState,
                lastNotif = values[1] as Long?,
                lastSms = values[2] as Long?,
                capturesLast24h = (values[3] as Int) + (values[4] as Int),
                parseFailuresLast24h = values[5] as Int
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
