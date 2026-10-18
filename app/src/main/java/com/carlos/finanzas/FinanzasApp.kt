package com.carlos.finanzas

import android.app.Application
import com.carlos.finanzas.data.AppDatabase

class FinanzasApp : Application() {
    /** Siempre se pide a AppDatabase.get() para tolerar el cierre durante una restauración. */
    val db: AppDatabase get() = AppDatabase.get(this)
}
