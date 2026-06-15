package com.example.mvt.health.model

data class HealthInjury(
    val id: String = "",
    val location: String = "",
    val treatment: String = "",
    val currentlyActive: Boolean = false,
    val startDate: String = "",
    val duration: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class HealthDisease(
    val id: String = "",
    val name: String = "",
    val affectedArea: String = "",
    val treatment: String = "",
    val startDate: String = "",
    val duration: String = "",
    val currentlyActive: Boolean = false
)

data class HealthProfile(
    val workPhysicalEffort: Int? = null,
    val sleepHours: Int? = null,
    val nutritionalStatus: String = "",
    val healthCondition: String = "",
    val injuries: List<HealthInjury> = emptyList(),
    val diseases: List<HealthDisease> = emptyList(),
    val updatedAt: Long = 0L
)

data class HealthLoadData(
    val health: HealthProfile,
    val nutritionalOptions: List<String>,
    val trainerId: String,
    val catalogError: String? = null
)

sealed interface HealthRepositoryState {
    data object Loading : HealthRepositoryState
    data class Ready(val data: HealthLoadData) : HealthRepositoryState
    data class Error(val message: String) : HealthRepositoryState
}

