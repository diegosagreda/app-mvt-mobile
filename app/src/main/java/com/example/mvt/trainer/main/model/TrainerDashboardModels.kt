package com.example.mvt.trainer.main.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class TrainerDashboardModel(
    val profileCompletion: Int = 0,
    val plataCount: Int = 0,
    val bronceCount: Int = 0,
    val plans: Map<String, TrainerPlanDashboard> = emptyMap()
)

data class TrainerPlanDashboard(
    val completedRoutinesRate: Int = 0,
    val monitoredAthletes: Int = 0,
    val stats: List<DashboardMetric> = emptyList(),
    val athletes: List<PriorityAthlete> = emptyList()
)

data class DashboardMetric(
    val title: String,
    val value: String,
    val subValue: String,
    val icon: ImageVector,
    val accentColor: Color
)

data class PriorityAthlete(
    val id: String,
    val name: String,
    val photoUrl: String,
    val discipline: String,
    val plan: String,
    val isActive: Boolean = true
)
