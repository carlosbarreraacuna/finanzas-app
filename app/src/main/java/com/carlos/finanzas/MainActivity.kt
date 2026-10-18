package com.carlos.finanzas

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import com.carlos.finanzas.ui.HealthScreen
import com.carlos.finanzas.ui.HealthViewModel

class MainActivity : ComponentActivity() {
    private val healthViewModel: HealthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { healthViewModel.refreshPermissions() }

            LaunchedEffect(Unit) {
                val perms = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
                launcher.launch(perms.toTypedArray())
            }

            MaterialTheme {
                Surface { HealthScreen(healthViewModel) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        healthViewModel.refreshPermissions()
    }
}
