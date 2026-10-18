package com.carlos.finanzas.capture

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class FinanceNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        connected = true
        CaptureProcessor.logHealth(this, "LISTENER_CONNECTED")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        connected = false
        CaptureProcessor.logHealth(this, "LISTENER_DISCONNECTED")
        // Pide al sistema que vuelva a enlazar el servicio.
        requestRebind(ComponentName(this, FinanceNotificationListener::class.java))
        CaptureProcessor.logHealth(this, "REBIND_REQUESTED")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.bigText")?.toString()
            ?: extras.getCharSequence("android.text")?.toString().orEmpty()
        val full = "$title $text".trim()
        if (full.isBlank()) return
        CaptureProcessor.process(this, "NOTIF", sbn.packageName, full, sbn.postTime)
    }

    companion object {
        /** Estado en memoria; la pantalla de salud lo combina con el permiso real. */
        @Volatile var connected: Boolean = false
    }
}
