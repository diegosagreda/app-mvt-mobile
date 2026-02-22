package com.example.mvt.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.example.mvt.ui.theme.PrimaryBlue

@Composable
fun DashboardScreen() { CenterText("Inicio deportista") }

@Composable
fun TrainerScreen() { CenterText("Tu entrenador") }

@Composable
fun CoachesScreen() { CenterText("Entrenadores") }


@Composable
fun ExploreScreen() { CenterText("Explorar") }

@Composable
fun SuggestionsScreen() { CenterText("Sugerencias") }

@Composable
fun CenterText(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = PrimaryBlue, fontSize = 22.sp)
    }
}
