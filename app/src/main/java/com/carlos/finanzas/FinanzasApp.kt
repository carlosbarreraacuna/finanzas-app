package com.carlos.finanzas

import android.app.Application
import com.carlos.finanzas.data.AppDatabase

class FinanzasApp : Application() {
    val db: AppDatabase by lazy { AppDatabase.get(this) }
}
