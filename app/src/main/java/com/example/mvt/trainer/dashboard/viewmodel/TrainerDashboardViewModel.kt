package com.example.mvt.trainer.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvt.trainer.dashboard.data.TrainerDashboardRepository
import com.example.mvt.trainer.dashboard.ui.TrainerDashboardUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.mvt.trainer.dashboard.model.ActionType
import com.example.mvt.trainer.dashboard.model.RoutineFlowSummary
import com.example.mvt.trainer.dashboard.model.TrainerDashboardData

class TrainerDashboardViewModel(
    private val repository: TrainerDashboardRepository = TrainerDashboardRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<TrainerDashboardUiState>(TrainerDashboardUiState.Loading)
    val uiState: StateFlow<TrainerDashboardUiState> = _uiState.asStateFlow()

    private val _selectedPlan = MutableStateFlow("Plata")
    val selectedPlan: StateFlow<String> = _selectedPlan.asStateFlow()

    private val _planCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val planCounts: StateFlow<Map<String, Int>> = _planCounts.asStateFlow()

    private var completeDashboardData: TrainerDashboardData? = null

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = TrainerDashboardUiState.Loading

            repository.fetchDashboardData()
                .onSuccess { data ->
                    completeDashboardData = data
                    _selectedPlan.value = data.profile.selectedPlan
                    _planCounts.value = data.priorityAthletes.groupingBy { it.plan }.eachCount()
                    _uiState.value = if (
                        data.profile.name.isBlank() &&
                        data.metrics.linkedAthletesCount == 0 &&
                        data.metrics.createdRoutinesCount == 0 &&
                        data.availablePlans.isEmpty()
                    ) {
                        TrainerDashboardUiState.Empty
                    } else {
                        TrainerDashboardUiState.Success(data)
                    }
                }
                .onFailure { exception ->
                    _uiState.value = TrainerDashboardUiState.Error(
                        message = exception.localizedMessage ?: "Error al cargar los datos del dashboard"
                    )
                }
        }
    }

    fun selectPlan(plan: String) {
        val data = completeDashboardData ?: return
        val normalizedPlan = plan.trim().ifEmpty { "Plata" }
        _selectedPlan.value = normalizedPlan

        val filteredPriorityAthletes = data.priorityAthletes.filter { athlete ->
            athlete.plan.contains(normalizedPlan, ignoreCase = true)
        }
        val filteredAthleteIds = filteredPriorityAthletes.mapTo(mutableSetOf()) { it.id }
        val filteredProduction = data.sportsProduction.filter { it.athleteId in filteredAthleteIds }
        val filteredTotalRoutines = filteredProduction.sumOf { it.totalRoutines }
        val filteredMonthlyRoutines = filteredProduction.sumOf { it.monthlyRoutines }
        val filteredCompletedRoutines = filteredProduction.sumOf { it.completedRoutines }
        val filteredPendingRoutines = filteredProduction.sumOf { it.pendingRoutines }
        val filteredNotDoneRoutines = filteredProduction.sumOf { it.notDoneRoutines }
        val filteredDraftRoutines = filteredProduction.sumOf { it.draftRoutines }
        val filteredCompletionRate = if (filteredTotalRoutines > 0) {
            kotlin.math.round((filteredCompletedRoutines.toFloat() / filteredTotalRoutines) * 100).toInt()
        } else {
            0
        }
        val filteredRecentActivities = if (filteredAthleteIds.isEmpty()) {
            emptyList()
        } else {
            data.recentActivities.filter {
                it.athleteId.isBlank() || it.athleteId in filteredAthleteIds
            }
        }
        val filteredRecommendedActions = data.recommendedActions.map { action ->
            when (action.actionType) {
                ActionType.PENDING_REQUEST -> action.copy(
                    title = "${if (filteredAthleteIds.isEmpty()) 0 else data.metrics.pendingRequestsCount} solicitudes pendientes"
                )
                ActionType.PENDING_ROUTINE -> action.copy(
                    title = "${filteredPendingRoutines + filteredDraftRoutines} rutinas por cerrar"
                )
                else -> action
            }
        }

        _uiState.value = TrainerDashboardUiState.Success(
            data.copy(
                profile = data.profile.copy(
                    selectedPlan = normalizedPlan,
                    routinesCompletedPercentage = filteredCompletionRate,
                    monitoredAthletesCount = filteredPriorityAthletes.size
                ),
                metrics = data.metrics.copy(
                    linkedAthletesCount = filteredPriorityAthletes.size,
                    activeAthletesCount = filteredPriorityAthletes.count { it.status == "Activo" },
                    createdRoutinesCount = filteredTotalRoutines,
                    monthlyCreatedRoutinesCount = filteredMonthlyRoutines,
                    pendingRoutinesCount = filteredPendingRoutines + filteredDraftRoutines,
                    completedRoutinesCount = filteredCompletedRoutines,
                    notDoneRoutinesCount = filteredNotDoneRoutines,
                    draftRoutinesCount = filteredDraftRoutines,
                    routinesToCloseCount = filteredPendingRoutines + filteredDraftRoutines,
                    pendingRequestsCount = if (filteredAthleteIds.isEmpty()) {
                        0
                    } else {
                        data.metrics.pendingRequestsCount
                    }
                ),
                priorityAthletes = filteredPriorityAthletes,
                sportsProduction = filteredProduction,
                routineFlow = RoutineFlowSummary(
                    completedCount = filteredCompletedRoutines,
                    pendingCount = filteredPendingRoutines,
                    notDoneCount = filteredNotDoneRoutines,
                    draftCount = filteredDraftRoutines
                ),
                recentActivities = filteredRecentActivities,
                recommendedActions = filteredRecommendedActions
            )
        )
    }
}