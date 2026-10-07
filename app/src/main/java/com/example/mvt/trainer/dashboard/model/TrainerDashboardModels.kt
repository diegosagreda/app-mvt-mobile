package com.example.mvt.trainer.dashboard.model

import com.google.firebase.database.PropertyName

/**
 * Modelos para mapeo directo con Firebase Realtime Database y Firestore
 */
data class SolicitudMatch(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("deporte") @set:PropertyName("deporte") var deporte: String = "",
    @get:PropertyName("estado") @set:PropertyName("estado") var estado: String = "",
    @get:PropertyName("fecha_registro") @set:PropertyName("fecha_registro") var fechaRegistro: Any? = null,
    @get:PropertyName("foto") @set:PropertyName("foto") var foto: String = "",
    @get:PropertyName("id_deportista") @set:PropertyName("id_deportista") var idDeportista: String = "",
    @get:PropertyName("id_entrenador") @set:PropertyName("id_entrenador") var idEntrenador: String = "",
    @get:PropertyName("nombres") @set:PropertyName("nombres") var nombres: String = ""
)

data class RutinaPersonalizada(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("titulo") @set:PropertyName("titulo") var titulo: String = "",
    @get:PropertyName("id_entrenador") @set:PropertyName("id_entrenador") var idEntrenador: String = "",
    @get:PropertyName("id_deportista") @set:PropertyName("id_deportista") var idDeportista: String = "",
    @get:PropertyName("descripcion") @set:PropertyName("descripcion") var descripcion: String = "",
    @get:PropertyName("objetivos") @set:PropertyName("objetivos") var objetivos: String = "",
    @get:PropertyName("fecha") @set:PropertyName("fecha") var fecha: Any? = null,
    @get:PropertyName("tipo_esfuerzo") @set:PropertyName("tipo_esfuerzo") var tipoEsfuerzo: String = "",
    @get:PropertyName("tipo_medicion") @set:PropertyName("tipo_medicion") var tipoMedicion: String = "",
    @get:PropertyName("estado") @set:PropertyName("estado") var estado: String = "",
    @get:PropertyName("completa") @set:PropertyName("completa") var completa: Boolean = false
)

/**
 * Información principal del Perfil del Entrenador.
 */
data class TrainerProfileInfo(
    val photoUrl: String? = null,
    val name: String = "",
    val discipline: String = "",
    val location: String? = null,
    val currentPlan: String = "",
    val availablePlans: List<String> = listOf("Plata", "Bronce"),
    val selectedPlan: String = "Plata",
    val profileCompletedPercentage: Int = 0,
    val routinesCompletedPercentage: Int = 0,
    val monitoredAthletesCount: Int = 0
)

/**
 * Indicadores principales / Tarjetas de métricas del Dashboard.
 */
data class DashboardMetrics(
    val linkedAthletesCount: Int = 0,
    val activeAthletesCount: Int = 0,
    val createdRoutinesCount: Int = 0,
    val monthlyCreatedRoutinesCount: Int = 0,
    val pendingRoutinesCount: Int = 0,
    val completedRoutinesCount: Int = 0,
    val notDoneRoutinesCount: Int = 0,
    val draftRoutinesCount: Int = 0,
    val routinesToCloseCount: Int = 0,
    val availableFreePlansCount: Int = 0,
    val pendingRequestsCount: Int = 0
)

/**
 * Resumen de deportista en Seguimiento Prioritario.
 */
data class PriorityAthlete(
    val id: String = "",
    val photoUrl: String? = null,
    val name: String = "",
    val discipline: String = "",
    val plan: String = "",
    val status: String = "Activo"
)

/**
 * Producción Deportiva: Rutinas creadas y avance por deportista.
 */
data class AthleteSportsProduction(
    val athleteId: String = "",
    val athleteName: String = "",
    val athletePhotoUrl: String? = null,
    val totalRoutines: Int = 0,
    val completedRoutines: Int = 0,
    val pendingRoutines: Int = 0,
    val notDoneRoutines: Int = 0,
    val draftRoutines: Int = 0,
    val monthlyRoutines: Int = 0,
    val progressPercentage: Float = 0f
)

/**
 * Resumen del Estado General de Rutinas.
 */
data class RoutineFlowSummary(
    val completedCount: Int = 0,
    val pendingCount: Int = 0,
    val notDoneCount: Int = 0,
    val draftCount: Int = 0
)

/**
 * Registro de actividades o rutinas recientes.
 */
data class RecentRoutineActivity(
    val id: String = "",
    val athleteId: String = "",
    val title: String = "",
    val formattedDate: String = "",
    val status: String = "Pendiente"
)

/**
 * Planes gratuitos disponibles.
 */
data class AvailablePlan(
    val id: String = "",
    val name: String = "",
    val description: String = ""
)

/**
 * Tipos de acción recomendada.
 */
enum class ActionType {
    PENDING_REQUEST,
    PENDING_ROUTINE,
    COMPLETE_PROFILE,
    GENERAL
}

/**
 * Tarjetas de acciones urgentes o recomendadas.
 */
data class RecommendedAction(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val actionType: ActionType = ActionType.GENERAL,
    val targetId: String? = null
)

/**
 * Contenedor principal de datos del Dashboard.
 */
data class TrainerDashboardData(
    val profile: TrainerProfileInfo = TrainerProfileInfo(),
    val metrics: DashboardMetrics = DashboardMetrics(),
    val priorityAthletes: List<PriorityAthlete> = emptyList(),
    val sportsProduction: List<AthleteSportsProduction> = emptyList(),
    val routineFlow: RoutineFlowSummary = RoutineFlowSummary(),
    val recentActivities: List<RecentRoutineActivity> = emptyList(),
    val availablePlans: List<AvailablePlan> = emptyList(),
    val recommendedActions: List<RecommendedAction> = emptyList()
)