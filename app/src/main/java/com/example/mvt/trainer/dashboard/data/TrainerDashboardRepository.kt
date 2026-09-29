package com.example.mvt.trainer.dashboard.data

import android.util.Log
import com.example.mvt.trainer.dashboard.model.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


private enum class RoutineStatus {
    REALIZADA,
    PENDIENTE,
    NO_REALIZADA,
    BORRADOR,
    DESCONOCIDO
}

private fun parseRoutineStatus(rawStatus: String?): RoutineStatus {
    val normalizedStatus = rawStatus?.lowercase()?.trim()
    if (normalizedStatus.isNullOrEmpty()) {
        return RoutineStatus.DESCONOCIDO
    }

    return when (normalizedStatus) {
        "realizada" -> RoutineStatus.REALIZADA
        "pendiente" -> RoutineStatus.PENDIENTE
        "no_realizada" -> RoutineStatus.NO_REALIZADA
        "borrador" -> RoutineStatus.BORRADOR
        else -> RoutineStatus.DESCONOCIDO
    }
}

private fun routineTimestamp(value: Any?): Long = when (value) {
    is Timestamp -> value.toDate().time
    is Number -> value.toLong()
    is String -> value.toLongOrNull() ?: 0L
    else -> 0L
}

private fun isRecent(timestamp: Long?): Boolean {
    if (timestamp == null || timestamp <= 0L) return false
    val thirtyDays = 30L * 24L * 60L * 60L * 1000L
    return System.currentTimeMillis() - timestamp <= thirtyDays
}

private fun String?.firstNonBlank(vararg alternatives: String?): String =
    sequenceOf(this, *alternatives)
        .map { it?.trim().orEmpty() }
        .firstOrNull { it.isNotEmpty() }
        .orEmpty()

private fun DataSnapshot.firstText(vararg paths: String): String =
    paths.asSequence()
        .map { child(it).value?.toString().orEmpty().trim() }
        .firstOrNull { it.isNotEmpty() }
        .orEmpty()


class TrainerDashboardRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val realtimeDb: FirebaseDatabase = FirebaseDatabase.getInstance()
) {

    suspend fun fetchDashboardData(): Result<TrainerDashboardData> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val authUid = currentUser.uid
            Log.d("MVT_DEBUG", "Cargando Dashboard para Auth UID: $authUid")

            // 1. Obtener Perfil del Entrenador en Realtime DB ('users/{uid}')
            val profileSnapshot = realtimeDb.getReference("users")
                .child(authUid)
                .get()
                .await()

            val photoUrl = profileSnapshot.child("foto_url").value?.toString()
                .firstNonBlank(
                    profileSnapshot.child("foto").value?.toString(),
                    profileSnapshot.child("fotoUrl").value?.toString()
                )
            val effectivePhotoUrl = photoUrl.firstNonBlank(currentUser.photoUrl?.toString())

            val nombres = profileSnapshot.child("nombres").value?.toString()
                .firstNonBlank(profileSnapshot.child("nombre").value?.toString())
            val apellidos = profileSnapshot.child("apellidos").value?.toString().orEmpty()
            val fullName = "$nombres $apellidos".trim().ifEmpty { currentUser.displayName ?: "Entrenador" }

            val telefono = profileSnapshot.child("telefono").value?.toString().orEmpty()
            val ciudad = profileSnapshot.child("ciudad").value?.toString()
                .firstNonBlank(profileSnapshot.child("ciudadActual").value?.toString())
            val pais = profileSnapshot.child("pais").value?.toString()
                .firstNonBlank(profileSnapshot.child("paisActual").value?.toString())
            val descripcion = profileSnapshot.child("descripcion").value?.toString()
                .firstNonBlank(profileSnapshot.child("perfil").value?.toString())
            val especialidad = profileSnapshot.child("especialidad").value?.toString()
                .firstNonBlank(
                    profileSnapshot.child("carrera").value?.toString(),
                    profileSnapshot.child("disciplina").value?.toString(),
                    profileSnapshot.child("perfil").value?.toString()
                )

            val trainerIdKey = authUid

            val profileFields = listOf(
                nombres,
                apellidos,
                effectivePhotoUrl,
                telefono,
                ciudad,
                pais,
                descripcion,
                especialidad
            )
            val truthyCount = profileFields.count { it?.trim()?.isNotEmpty() == true }
            val calculatedProfilePercentage = Math.round((truthyCount.toFloat() / 8f) * 100f)
            Log.d(
                "MVT_DASHBOARD",
                "Perfil completado: $calculatedProfilePercentage% " +
                    "(nombres=${nombres.isNotBlank()}, apellidos=${apellidos.isNotBlank()}, " +
                    "foto=${!photoUrl.isNullOrBlank()}, telefono=${telefono.isNotBlank()}, " +
                    "ciudad=${ciudad.isNotBlank()}, pais=${pais.isNotBlank()}, " +
                    "descripcion=${descripcion.isNotBlank()}, especialidad=${especialidad.isNotBlank()})"
            )

            val solicitudesSnapshot = realtimeDb.getReference("solicitudes")
                .get()
                .await()

            val priorityAthletesList = mutableListOf<PriorityAthlete>()
            var pendingRequestsCount = 0

            val athleteRoutinesMap = mutableMapOf<String, Int>()
            val athleteCompletedMap = mutableMapOf<String, Int>()
            val athletePendingMap = mutableMapOf<String, Int>()
            val athleteNotDoneMap = mutableMapOf<String, Int>()
            val athleteMonthlyRoutinesMap = mutableMapOf<String, Int>()
            val now = Calendar.getInstance()
            val currentMonth = now.get(Calendar.MONTH)
            val currentYear = now.get(Calendar.YEAR)

            var globalCompletedCount = 0
            var globalPendingCount = 0
            var globalNoRealizadaCount = 0
            var globalDraftCount = 0
            val allCompletedRoutinesList = mutableListOf<Map<String, Any?>>()
            val latestRoutineByAthlete = mutableMapOf<String, Long>()

            for (child in solicitudesSnapshot.children) {
                val entrenadorIdSol = child.child("id_entrenador").value?.toString() ?: ""
                val estadoSol = child.child("estado").value?.toString() ?: ""

                if (entrenadorIdSol == trainerIdKey) {
                    if (estadoSol == "Pendiente") {
                        pendingRequestsCount++
                    } else if (estadoSol == "Aprobado") {
                        val athleteId = child.child("id_deportista").value?.toString() ?: ""
                        val athleteName = child.child("nombres").value?.toString()
                            ?: child.child("nombre").value?.toString() ?: "Atleta"
                        val deporte = child.child("deporte").value?.toString() ?: "Multideporte"

                        var realAthletePhoto: String? = null
                        var planName = "Sin plan"

                        if (athleteId.isNotEmpty()) {
                            val athleteUserSnapshot = realtimeDb.getReference("users")
                                .child(athleteId)
                                .get()
                                .await()

                            realAthletePhoto = athleteUserSnapshot.child("foto_url").value?.toString()
                                ?: athleteUserSnapshot.child("foto").value?.toString()
                                        ?: child.child("foto").value?.toString()

                            val rawPlan = athleteUserSnapshot.child("plan").value
                            planName = when (rawPlan) {
                                is String -> rawPlan
                                is Map<*, *> -> rawPlan["nombre"]?.toString()
                                    ?: rawPlan["name"]?.toString()
                                    ?: rawPlan["plan"]?.toString() ?: "Sin plan"
                                else -> athleteUserSnapshot.child("plan_suscrito").value?.toString() ?: "Sin plan"
                            }

                            val rutinasAtletaQuery = firestore.collection("rutinas")
                                .whereEqualTo("id_deportista", athleteId)
                                .whereEqualTo("id_entrenador", trainerIdKey)
                                .get()
                                .await()

                            val docsList = if (rutinasAtletaQuery.isEmpty && trainerIdKey != authUid) {
                                firestore.collection("rutinas")
                                    .whereEqualTo("id_deportista", athleteId)
                                    .whereEqualTo("id_entrenador", authUid)
                                    .get()
                                    .await()
                                    .documents
                            } else {
                                rutinasAtletaQuery.documents
                            }

                            val athleteTotal = docsList.size
                            var athleteCompleted = 0
                            var athletePending = 0
                            var athleteNotDone = 0
                            var athleteMonthlyRoutines = 0

                            for (doc in docsList) {
                                val rawEstado = doc.getString("estado")
                                val status = parseRoutineStatus(rawEstado)

                                val dataMap = doc.data ?: emptyMap()
                                allCompletedRoutinesList.add(dataMap + ("doc_id" to doc.id))
                                val routineDate = routineTimestamp(doc.get("fecha"))
                                val routineCalendar = Calendar.getInstance().apply {
                                    timeInMillis = routineDate
                                }
                                if (
                                    routineCalendar.get(Calendar.MONTH) == currentMonth &&
                                    routineCalendar.get(Calendar.YEAR) == currentYear
                                ) {
                                    athleteMonthlyRoutines++
                                }
                                latestRoutineByAthlete[athleteId] = maxOf(
                                    latestRoutineByAthlete[athleteId] ?: 0L,
                                    routineDate
                                )

                                when (status) {
                                    RoutineStatus.REALIZADA -> {
                                        athleteCompleted++
                                        globalCompletedCount++
                                    }
                                    RoutineStatus.NO_REALIZADA -> {
                                        athleteNotDone++
                                        globalNoRealizadaCount++
                                    }
                                    RoutineStatus.BORRADOR -> {
                                        globalDraftCount++
                                    }
                                    RoutineStatus.PENDIENTE -> {
                                        athletePending++
                                        globalPendingCount++
                                    }
                                    RoutineStatus.DESCONOCIDO -> {
                                        Log.w(
                                            "MVT_DASHBOARD",
                                            "Rutina ${doc.id} sin estado; se excluye de métricas"
                                        )
                                    }
                                }
                            }

                            val parcialesAtletaQuery = firestore.collection("rutinas_parciales")
                                .whereEqualTo("id_deportista", athleteId)
                                .whereEqualTo("id_entrenador", trainerIdKey)
                                .get()
                                .await()

                            val draftDocs = if (parcialesAtletaQuery.isEmpty && trainerIdKey != authUid) {
                                firestore.collection("rutinas_parciales")
                                    .whereEqualTo("id_deportista", athleteId)
                                    .whereEqualTo("id_entrenador", authUid)
                                    .get()
                                    .await()
                                    .documents
                            } else {
                                parcialesAtletaQuery.documents
                            }

                            val athleteDrafts = draftDocs.size
                            globalDraftCount += athleteDrafts

                            athleteRoutinesMap[athleteId] = athleteTotal
                            athleteCompletedMap[athleteId] = athleteCompleted
                            athletePendingMap[athleteId] = athletePending
                            athleteNotDoneMap[athleteId] = athleteNotDone
                            athleteMonthlyRoutinesMap[athleteId] = athleteMonthlyRoutines
                        }

                        priorityAthletesList.add(
                            PriorityAthlete(
                                id = athleteId,
                                photoUrl = realAthletePhoto?.ifEmpty { null },
                                name = athleteName,
                                discipline = deporte,
                                plan = planName,
                                status = if (
                                    !planName.contains("bronce", ignoreCase = true) ||
                                    isRecent(latestRoutineByAthlete[athleteId])
                                ) "Activo" else "Revisar"
                            )
                        )
                    }
                }
            }

            val linkedAthletesCount = priorityAthletesList.size
            val totalFullRoutines = allCompletedRoutinesList.size
            val monthlyCreatedRoutines = athleteMonthlyRoutinesMap.values.sum()
            val totalPendingWork = globalPendingCount + globalDraftCount

            val maxRoutinesAmongAthletes = priorityAthletesList.maxOfOrNull { athleteRoutinesMap[it.id] ?: 0 } ?: 1
            val maxDenominator = if (maxRoutinesAmongAthletes > 0) maxRoutinesAmongAthletes else 1

            val sortedAthletesByRoutines = priorityAthletesList.sortedByDescending { athleteRoutinesMap[it.id] ?: 0 }
            val sportsProductionList = sortedAthletesByRoutines.take(6).map { athlete ->

                val totalRoutinesCount = athleteRoutinesMap[athlete.id] ?: 0
                val countRealizadas = athleteCompletedMap[athlete.id] ?: 0
                val countPendientes = athletePendingMap[athlete.id] ?: 0
                val countNoRealizadas = athleteNotDoneMap[athlete.id] ?: 0
                val countMonthlyRoutines = athleteMonthlyRoutinesMap[athlete.id] ?: 0

                val progress = if (maxDenominator > 0) (totalRoutinesCount.toFloat() / maxDenominator.toFloat()) else 0f

                AthleteSportsProduction(
                    athleteId = athlete.id,
                    athleteName = athlete.name,
                    athletePhotoUrl = athlete.photoUrl,
                    totalRoutines = totalRoutinesCount,
                    completedRoutines = countRealizadas,
                    pendingRoutines = countPendientes,
                    notDoneRoutines = countNoRealizadas,
                    monthlyRoutines = countMonthlyRoutines,
                    progressPercentage = progress
                )
            }

            val dateFormat = SimpleDateFormat("dd 'de' MMM 'de' yyyy", Locale("es", "CO"))

            val sortedRoutines = allCompletedRoutinesList.sortedByDescending { doc ->
                when (val rawFecha = doc["fecha"]) {
                    is Timestamp -> rawFecha.toDate().time
                    is Long -> rawFecha
                    is Number -> rawFecha.toLong()
                    else -> 0L
                }
            }

            val recentActivitiesList = sortedRoutines.take(5).map { doc ->
                val idDoc = doc["doc_id"]?.toString() ?: ""
                val titulo = doc["titulo"]?.toString()
                    ?: doc["nombre_rutina"]?.toString()
                    ?: "Rutina personalizada"

                val dateStr = when (val rawFecha = doc["fecha"]) {
                    is Timestamp -> dateFormat.format(rawFecha.toDate())
                    is Long -> dateFormat.format(rawFecha)
                    is Number -> dateFormat.format(rawFecha.toLong())
                    is String -> rawFecha
                    else -> "Fecha no definida"
                }

                val estado = doc["estado"]?.toString()
                    ?.takeIf { it.isNotBlank() }
                    ?: "Pendiente"

                RecentRoutineActivity(
                    id = idDoc,
                    athleteId = doc["id_deportista"]?.toString().orEmpty(),
                    title = titulo,
                    formattedDate = dateStr,
                    status = estado
                )
            }

            val globalCompletionRate = if (totalFullRoutines > 0) {
                Math.round((globalCompletedCount.toFloat() / totalFullRoutines.toFloat()) * 100f)
            } else {
                0
            }

            // Para el Estado General / Flujo de rutinas
            val freePlans = realtimeDb.getReference("planes_gratuitos")
                .get()
                .await()
                .children
                .filter {
                    it.firstText("id_entrenador", "idEntrenador", "entrenador_id", "trainerId") == trainerIdKey
                }
                .map { plan ->
                    AvailablePlan(
                        id = plan.key.orEmpty(),
                        name = plan.firstText("nombre_plan", "nombre", "name", "titulo"),
                        description = plan.firstText("descripcion", "description")
                    )
                }

            val metrics = DashboardMetrics(
                linkedAthletesCount = linkedAthletesCount,
                activeAthletesCount = priorityAthletesList.count { it.status == "Activo" },
                createdRoutinesCount = totalFullRoutines,
                monthlyCreatedRoutinesCount = monthlyCreatedRoutines,
                pendingRoutinesCount = totalPendingWork,
                completedRoutinesCount = globalCompletedCount,
                notDoneRoutinesCount = globalNoRealizadaCount,
                draftRoutinesCount = globalDraftCount,
                routinesToCloseCount = totalPendingWork,
                availableFreePlansCount = freePlans.size,
                pendingRequestsCount = pendingRequestsCount
            )

            val routineFlow = RoutineFlowSummary(
                completedCount = globalCompletedCount,
                pendingCount = globalPendingCount,
                notDoneCount = globalNoRealizadaCount,
                draftCount = globalDraftCount
            )

            val recommendedActions = mutableListOf<RecommendedAction>().apply {
                add(
                    RecommendedAction(
                        id = "act_req",
                        title = "$pendingRequestsCount solicitudes pendientes",
                        description = "Responde invitaciones de atletas.",
                        actionType = ActionType.PENDING_REQUEST
                    )
                )
                add(
                    RecommendedAction(
                        id = "act_pending",
                        title = "$totalPendingWork rutinas por cerrar",
                        description = "Incluye pendientes y borradores.",
                        actionType = ActionType.PENDING_ROUTINE
                    )
                )
                add(
                    RecommendedAction(
                        id = "act_profile",
                        title = "$calculatedProfilePercentage% de perfil completado",
                        description = "Completa tu vitrina profesional.",
                        actionType = ActionType.COMPLETE_PROFILE
                    )
                )
            }

            val profileInfo = TrainerProfileInfo(
                photoUrl = effectivePhotoUrl,
                name = fullName,
                discipline = especialidad.ifEmpty { "Entrenador MVT" },
                location = if (ciudad.isNotEmpty()) ciudad else pais,
                currentPlan = profileSnapshot.child("plan").child("nombre").value?.toString()
                    ?: profileSnapshot.child("plan").value?.toString().orEmpty(),
                availablePlans = listOf("Plata", "Bronce") + priorityAthletesList
                    .map { it.plan }
                    .filter { it.isNotBlank() && it !in listOf("Plata", "Bronce") }
                    .distinct()
                    .sorted(),
                profileCompletedPercentage = calculatedProfilePercentage,
                routinesCompletedPercentage = globalCompletionRate,
                monitoredAthletesCount = linkedAthletesCount
            )

            Result.success(
                TrainerDashboardData(
                    profile = profileInfo,
                    metrics = metrics,
                    priorityAthletes = priorityAthletesList.take(5),
                    sportsProduction = sportsProductionList,
                    routineFlow = routineFlow,
                    recentActivities = recentActivitiesList,
                    availablePlans = freePlans,
                    recommendedActions = recommendedActions
                )
            )
        } catch (e: Exception) {
            Log.e("MVT_DEBUG", "Error consultando datos del Dashboard: ${e.message}", e)
            Result.failure(e)
        }
    }
}