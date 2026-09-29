package com.example.mvt.trainer.dashboard.ui

import com.example.mvt.trainer.dashboard.model.TrainerDashboardData

/**
 * Sealed interface que representa los estados visuales del Dashboard.
 * Cumple con los criterios de aceptación: Loading, Success, Empty y Error.
 */
sealed interface TrainerDashboardUiState {
    object Loading : TrainerDashboardUiState

    data class Success(
        val data: TrainerDashboardData
    ) : TrainerDashboardUiState

    object Empty : TrainerDashboardUiState

    data class Error(
        val message: String
    ) : TrainerDashboardUiState
}