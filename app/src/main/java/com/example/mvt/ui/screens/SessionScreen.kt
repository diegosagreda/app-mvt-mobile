package com.example.mvt.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mvt.ui.components.AnimatedMvtLogo
import com.example.mvt.ui.theme.AppBackground
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.example.mvt.data.firebase.AuthService
import com.example.mvt.registration.data.AthleteRegistrationService
import android.net.Uri
import kotlinx.coroutines.delay

private const val LogoRevealDelayMillis = 180
private const val LogoTraceDurationMillis = 1650
private const val TextStartDelayMillis = 120L
private const val TextCharDelayMillis = 52L
private const val TextSpaceDelayMillis = 34L
private const val PostAnimationHoldMillis = 500L

private val BrandTextEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private const val BrandName = "My Virtual Trainer"

@Composable
fun SessionScreen(navController: NavController) {
    var startIntro by remember { mutableStateOf(false) }
    var typedCharacters by remember { mutableIntStateOf(0) }
    val logoTraceProgress = remember { Animatable(0f) }

    val logoScale by animateFloatAsState(
        targetValue = if (startIntro) 1f else 0.84f,
        animationSpec = tween(durationMillis = 900, easing = BrandTextEasing),
        label = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (startIntro) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = BrandTextEasing),
        label = "logoAlpha"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (typedCharacters > 0) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = BrandTextEasing),
        label = "textAlpha"
    )

    LaunchedEffect(Unit) {
        startIntro = true
        delay(LogoRevealDelayMillis.toLong())
        logoTraceProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = LogoTraceDurationMillis,
                easing = BrandTextEasing
            )
        )
        delay(TextStartDelayMillis)
        BrandName.forEachIndexed { index, char ->
            typedCharacters = index + 1
            delay(if (char == ' ') TextSpaceDelayMillis else TextCharDelayMillis)
        }
        delay(PostAnimationHoldMillis)

        val currentUser = FirebaseAuth.getInstance().currentUser
        val authService = AuthService()
        val registrationService = AthleteRegistrationService()
        val isGoogleUser = currentUser?.providerData?.any {
            it.providerId == GoogleAuthProvider.PROVIDER_ID
        } == true
        val userRole = currentUser?.let { user ->
            runCatching {
                val role = authService.getUserRole(user.uid)
                if (role.isBlank() && isGoogleUser) {
                    registrationService.initializeGoogleAthlete(user)
                    "Deportista"
                } else {
                    role
                }
            }.getOrDefault("")
        }.orEmpty()
        val isAthlete = userRole.equals("Deportista", ignoreCase = true)
        if (currentUser != null && !currentUser.isEmailVerified && !isGoogleUser) {
            val email = currentUser.email.orEmpty()
            FirebaseAuth.getInstance().signOut()
            navController.navigate("verify_email/${Uri.encode(email)}") {
                popUpTo(0) { inclusive = true }
            }
        } else if (currentUser != null && isAthlete) {
            val destination = runCatching {
                if (authService.hasCompletedWelcome(currentUser.uid)) "athleteMain" else "welcome"
            }.getOrDefault("login")
            if (destination == "login") FirebaseAuth.getInstance().signOut()
            navController.navigate(destination) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            if (currentUser != null) FirebaseAuth.getInstance().signOut()
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            AnimatedMvtLogo(
                progress = logoTraceProgress.value,
                modifier = Modifier
                    .width(220.dp)
                    .height(96.dp)
                    .graphicsLayer {
                        alpha = logoAlpha
                        scaleX = logoScale
                        scaleY = logoScale
                    }
            )

            Spacer(modifier = Modifier.height(26.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.height(28.dp)
            ) {
                Text(
                    text = BrandName,
                    fontSize = 22.sp,
                    color = Color.Transparent,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.8.sp
                )
                Text(
                    text = BrandName.take(typedCharacters),
                    fontSize = 22.sp,
                    color = Color.White.copy(alpha = 0.94f),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.8.sp,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .graphicsLayer { alpha = textAlpha }
                )
            }
        }
    }
}
