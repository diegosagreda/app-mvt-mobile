package com.example.mvt

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mvt.navigation.AppNavigation
import com.example.mvt.utils.ChatNotificationBus
import com.example.mvt.ui.theme.MVTTheme
import com.example.mvt.utils.NotificationUtils
import com.example.mvt.utils.StravaAuthRedirectBus
import com.example.mvt.utils.WorkScheduler
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private companion object {
        const val TAG = "MVT_MainActivity"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleStravaRedirect(intent)
        handleChatNotificationIntent(intent)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleStravaRedirect(intent)
        handleChatNotificationIntent(intent)
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

    private fun handleStravaRedirect(intent: Intent?) {
        val data = intent?.data ?: return
        val redirectUri = android.net.Uri.parse(BuildConfig.STRAVA_REDIRECT_URI)
        val matchesRedirect =
            data.scheme == redirectUri.scheme &&
                data.host == redirectUri.host &&
                data.path?.startsWith(redirectUri.path.orEmpty()) == true

        if (matchesRedirect) {
            StravaAuthRedirectBus.publish(data)
        }
    }

    private fun handleChatNotificationIntent(intent: Intent?) {
        val shouldOpenChat =
            intent?.getBooleanExtra(ChatNotificationBus.EXTRA_OPEN_CHAT, false) == true ||
                intent?.action == "FLUTTER_NOTIFICATION_CLICK" ||
                intent?.getStringExtra("event") == "chat_message" ||
                intent?.getStringExtra("type") == "chat" ||
                intent?.getStringExtra("routeName") == "deportista-chat"

        Log.d(
            TAG,
            "handleChatNotificationIntent shouldOpenChat=$shouldOpenChat action=${intent?.action} event=${intent?.getStringExtra("event")} type=${intent?.getStringExtra("type")} route=${intent?.getStringExtra("routeName")}"
        )

        if (shouldOpenChat) {
            ChatNotificationBus.publish()
        }
    }
}
