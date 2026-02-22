package com.example.mvt

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mvt.navigation.AppNavigation
import com.example.mvt.ui.theme.MVTTheme
import com.example.mvt.utils.NotificationHelper
import com.example.mvt.utils.NotificationUtils
import com.example.mvt.utils.WorkScheduler
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- 1. Crear canal de notificaciones ---
        NotificationUtils.createNotificationChannel(this)

        // --- 2. Solicitar permiso de notificaciones (Android 13+) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    200
                )
            }
        }

        // --- 3. Programar revisión diaria (usa el ID real del atleta autenticado) ---
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            WorkScheduler.scheduleDailyRoutineChecks(this, userId)
        }



        // --- 5. Solicitar permisos de ubicación (ya lo tenías) ---
        requestLocationPermission()

        setContent {
            MVTTheme {
                AppNavigation()
            }
        }
    }

    private fun requestLocationPermission() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!allGranted) {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
}
