package com.carlos.finanzas

import android.app.Application
import com.carlos.finanzas.capture.ListenerKeepAlive
import com.carlos.finanzas.data.AppDatabase
import com.carlos.finanzas.debt.DebtReminderWorker

class FinanzasApp : Application() {
    /** Siempre se pide a AppDatabase.get() para tolerar el cierre durante una restauración. */
    val db: AppDatabase get() = AppDatabase.get(this)

    override fun onCreate() {
        super.onCreate()
        DebtReminderWorker.schedule(this)
        ListenerKeepAlive.schedule(this)
        ListenerKeepAlive.ensureBound(this, "app_start")
    }
}
