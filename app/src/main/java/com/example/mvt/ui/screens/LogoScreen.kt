package com.example.mvt.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mvt.ui.components.AnimatedMvtLogo
import com.example.mvt.ui.theme.AppBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LogoScreen(onClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    val traceProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(180)
        traceProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1650,
                easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .clickable {
                scope.launch {
                    delay(200) // evita race conditions
                    onClick()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedMvtLogo(
            progress = traceProgress.value,
            modifier = Modifier
                .width(180.dp)
                .height(78.dp)
        )
    }
}
