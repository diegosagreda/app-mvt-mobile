package com.example.mvt.trainer.model

enum class AssignmentStatus(val wireValue: String) {
    PENDING("Pendiente"),
    APPROVED("Aprobado"),
    CANCELLED("Cancelado"),
    REJECTED("Rechazado"),
    LOST("Perdida"),
    UNKNOWN("");

    companion object {
        fun from(value: String): AssignmentStatus = entries.firstOrNull {
            it.wireValue.equals(value.trim(), ignoreCase = true)
        } ?: UNKNOWN
    }
}

data class TrainerAssignment(
    val id: String = "",
    val athleteId: String = "",
    val trainerId: String = "",
    val status: AssignmentStatus = AssignmentStatus.UNKNOWN,
    val createdAt: Long = 0L
)

data class TrainerProfile(
    val id: String = "",
    val nombres: String = "",
    val apellidos: String = "",
    val fotoUrl: String = "",
    val descripcion: String = "",
    val genero: String = "",
    val pais: String = "",
    val ciudad: String = "",
    val paisActual: String = "",
    val ciudadActual: String = "",
    val fechaNacimiento: String = "",
    val resena: String = "",
    val hitos: String = "",
    val especialidad: String = "",
    val experiencia: String = "",
    val perfil: String = "",
    val formacionAcademica: String = "",
    val certificaciones: String = "",
    val deporte: String = "",
    val estrellas: Double = 0.0
) {
    val fullName: String
        get() = listOf(nombres, apellidos).filter(String::isNotBlank).joinToString(" ")
            .ifBlank { "Entrenador" }
}

data class AthleteRatingProfile(
    val id: String = "",
    val fullName: String = "",
    val photoUrl: String = "",
    val lastRatingMonth: String = ""
)

data class TrainerRating(
    val id: String = "",
    val score: Int = 0,
    val comment: String = "",
    val athleteId: String = "",
    val athleteName: String = "",
    val athletePhotoUrl: String = "",
    val dateMillis: Long = 0L,
    val dateText: String = ""
)

sealed interface TrainerScreenState {
    data object Loading : TrainerScreenState
    data class Assigned(
        val assignment: TrainerAssignment,
        val trainer: TrainerProfile,
        val athlete: AthleteRatingProfile
    ) : TrainerScreenState
    data class Unassigned(val latestStatus: AssignmentStatus? = null) : TrainerScreenState
    data class Error(val message: String) : TrainerScreenState
}

data class TrainerRatingsState(
    val mine: List<TrainerRating> = emptyList(),
    val others: List<TrainerRating> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class AthletePlan(
    val name: String = "",
    val requestLimit: Int = 0,
    val sentRequests: Int = 0
)

data class TrainerRequest(
    val id: String = "",
    val athleteId: String = "",
    val trainerId: String = "",
    val athleteName: String = "",
    val athletePhoto: String = "",
    val sport: String = "",
    val status: AssignmentStatus = AssignmentStatus.UNKNOWN,
    val createdAt: Long = 0L
)

enum class TrainerRelationship { ASSIGNED, PENDING, AVAILABLE }

data class TrainerCardModel(
    val trainer: TrainerProfile,
    val relationship: TrainerRelationship,
    val pendingRequest: TrainerRequest? = null
)

sealed interface TrainersCatalogState {
    data object Loading : TrainersCatalogState
    data class Ready(
        val trainers: List<TrainerCardModel>,
        val plan: AthletePlan,
        val athlete: AthleteRatingProfile,
        val assignedTrainerId: String = ""
    ) : TrainersCatalogState
    data object Empty : TrainersCatalogState
    data class Error(val message: String) : TrainersCatalogState
}
