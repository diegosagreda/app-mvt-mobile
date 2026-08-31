package com.example.mvt.trainer.main.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import com.example.mvt.trainer.main.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

class TrainerDashboardRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getDashboardData(): TrainerDashboardModel {
        val uid = auth.currentUser?.uid ?: return TrainerDashboardModel()

        val snapshots = coroutineScope {
            val trainer = async { db.getReference("users").child(uid).get().await() }
            val requests = async { db.getReference("solicitudes").get().await() }
            val users = async { db.getReference("users").get().await() }
            val freePlans = async { db.getReference("planes_gratuitos").get().await() }
            DashboardSnapshots(
                trainer = trainer.await(),
                requests = requests.await(),
                users = users.await(),
                freePlans = freePlans.await()
            )
        }
        val trainerSnapshot = snapshots.trainer
        val requestsSnapshot = snapshots.requests
        val usersSnapshot = snapshots.users
        val freePlansSnapshot = snapshots.freePlans

        val athletesById = usersSnapshot.children.associateBy { it.key.orEmpty() }
        val approvedRequests = requestsSnapshot.children
            .filter { it.trainerId() == uid && it.isApproved() && it.athleteId().isNotBlank() }
            .groupBy { it.athleteId() }
            .mapValues { (_, requests) -> requests.maxByOrNull { it.requestDate() }!! }

        val activeAthletes = approvedRequests.values
            .mapNotNull { request ->
                val athlete = athletesById[request.athleteId()] ?: return@mapNotNull null
                athlete.toPriorityAthlete(request.athleteId())
            }

        val pendingRequestsCount = requestsSnapshot.children.count {
            it.trainerId() == uid && it.isPending()
        }
        val closedRequestsCount = requestsSnapshot.children.count {
            it.trainerId() == uid && it.isClosed()
        }
        val athletesByPlan = activeAthletes
            .filter { it.plan.equals("Plata", true) || it.plan.equals("Bronce", true) }
            .groupBy { it.plan.normalizedPlan() }
        val plataAthletes = athletesByPlan["Plata"].orEmpty()
        val bronceAthletes = athletesByPlan["Bronce"].orEmpty()

        val routinesByAthlete = coroutineScope {
            activeAthletes.map { athlete ->
                async {
                    firestore.collection("rutinas")
                        .whereEqualTo("id_deportista", athlete.id)
                        .get()
                        .await()
                        .documents
                        .filter { it.getString("id_entrenador") == uid }
                        .let { athlete.id to it }
                }
            }.awaitAll().toMap()
        }
        val freePlansCount = freePlansSnapshot.children.count { it.trainerId() == uid }

        return TrainerDashboardModel(
            profileCompletion = calculateProfileCompletion(trainerSnapshot),
            plataCount = plataAthletes.size,
            bronceCount = bronceAthletes.size,
            plans = mapOf(
                "Plata" to buildPlanDashboard(
                    athletes = plataAthletes,
                    routines = plataAthletes.flatMap { routinesByAthlete[it.id].orEmpty() },
                    freePlansCount = freePlansCount,
                    pendingRequestsCount = pendingRequestsCount,
                    closedRequestsCount = closedRequestsCount
                ),
                "Bronce" to buildPlanDashboard(
                    athletes = bronceAthletes,
                    routines = bronceAthletes.flatMap { routinesByAthlete[it.id].orEmpty() },
                    freePlansCount = freePlansCount,
                    pendingRequestsCount = pendingRequestsCount,
                    closedRequestsCount = closedRequestsCount
                )
            ),
        )
    }

    private fun buildPlanDashboard(
        athletes: List<PriorityAthlete>,
        routines: List<DocumentSnapshot>,
        freePlansCount: Int,
        pendingRequestsCount: Int,
        closedRequestsCount: Int
    ): TrainerPlanDashboard {
        val totalCreatedRoutines = routines.size
        val createdThisMonth = routines.count { it.routineDate().isCurrentMonth() }
        val pendingRoutines = routines.count { it.routineStatus() == "pendiente" }
        val draftRoutines = routines.count { it.routineStatus() == "borrador" }
        val completedRoutines = routines.count { it.routineStatus() in completedRoutineStatuses }
        val activeAthletes = athletes.count { athlete ->
            routines.any { it.getString("id_deportista") == athlete.id && it.routineDate().isCurrentMonth() }
        }
        val completedRoutinesRate = if (totalCreatedRoutines > 0) {
            (completedRoutines * 100) / totalCreatedRoutines
        } else {
            0
        }

        return TrainerPlanDashboard(
            completedRoutinesRate = completedRoutinesRate,
            monitoredAthletes = athletes.size,
            stats = listOf(
                DashboardMetric("Atletas vinculados", athletes.size.toString(), "${athletes.size} activos", Icons.Default.DirectionsRun, Color(0xFF63C7FF)),
                DashboardMetric("Atletas activos", activeAthletes.toString(), "Plan vigente o actividad reciente", Icons.Default.Favorite, Color(0xFF62D9A8)),
                DashboardMetric("Rutinas creadas", totalCreatedRoutines.toString(), "$createdThisMonth este mes", Icons.Default.EventAvailable, Color(0xFF63C7FF)),
                DashboardMetric("Rutinas pendientes", pendingRoutines.toString(), "$draftRoutines borradores", Icons.Default.Timer, Color(0xFFFFC857)),
                DashboardMetric("Planes gratuitos", freePlansCount.toString(), "Plantillas disponibles", Icons.Default.Layers, Color(0xFF4DD0E1)),
                DashboardMetric("Solicitudes pendientes", pendingRequestsCount.toString(), "$closedRequestsCount cerradas", Icons.Default.GroupAdd, Color(0xFFFF72A7))
            ),
            athletes = athletes
        )
    }

    private fun calculateProfileCompletion(snapshot: DataSnapshot): Int {
        if (!snapshot.exists()) return 0
        val fields = listOf(
            "nombres", "apellidos", "deporte", "pais",
            "ciudad", "especialidad", "identificacion", "telefono",
            "genero", "fecha_nacimiento", "descripcion", "hitos"
        )
        var filled = 0
        fields.forEach { field ->
            val value = when (field) {
                "ciudad" -> snapshot.text("ciudadActual").ifBlank { snapshot.text("ciudad") }
                else -> snapshot.text(field)
            }
            if (!value.isNullOrBlank() && value != "null") filled++
        }
        return (filled * 100) / fields.size
    }

    private fun DataSnapshot.toPriorityAthlete(id: String): PriorityAthlete {
        val plan = child("plan").child("nombre").text().trim()
        val name = listOf(text("nombres"), text("apellidos"))
            .filter(String::isNotBlank)
            .joinToString(" ")
            .ifBlank { "Atleta" }
        return PriorityAthlete(
            id = id,
            name = name,
            photoUrl = text("foto_url"),
            discipline = listOf(text("deporte"), plan).filter(String::isNotBlank).joinToString(" · "),
            plan = plan,
            isActive = plan.equals("Plata", ignoreCase = true)
        )
    }

    private fun DataSnapshot.trainerId(): String = text("id_entrenador").ifBlank { text("idEntrenador") }
    private fun DataSnapshot.athleteId(): String = text("id_deportista").ifBlank { text("idDeportista") }
    private fun DataSnapshot.requestDate(): Long = child("fecha_registro").value.asLong().takeIf { it > 0 } ?: child("createdAt").value.asLong()
    private fun DataSnapshot.isApproved(): Boolean = text("estado").equals("Aprobado", true) || text("estado").equals("Aceptada", true)
    private fun DataSnapshot.isPending(): Boolean = text("estado").equals("Pendiente", true)
    private fun DataSnapshot.isClosed(): Boolean = text("estado").equals("Cerrada", true) || text("estado").equals("Cerrado", true)
    private fun DataSnapshot.text(key: String): String = child(key).value?.toString()?.takeUnless { it == "null" }.orEmpty()
    private fun DataSnapshot.text(): String = value?.toString()?.takeUnless { it == "null" }.orEmpty()
    private fun String.normalizedPlan(): String = if (equals("Plata", true)) "Plata" else "Bronce"
    private fun Any?.asLong(): Long = when (this) {
        is Number -> toLong()
        is String -> toLongOrNull() ?: 0L
        else -> 0L
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.routineStatus(): String =
        getString("estado").orEmpty().trim().lowercase()

    private fun com.google.firebase.firestore.DocumentSnapshot.routineDate(): Date? = when (val value = get("fecha")) {
        is Timestamp -> value.toDate()
        is Date -> value
        is Number -> Date(value.toLong())
        is Map<*, *> -> {
            val seconds = (value["_seconds"] as? Number)?.toLong() ?: return null
            val nanos = (value["_nanoseconds"] as? Number)?.toLong() ?: 0L
            Date(seconds * 1_000L + nanos / 1_000_000L)
        }
        else -> null
    }

    private fun Date?.isCurrentMonth(): Boolean {
        if (this == null) return false
        val now = Calendar.getInstance()
        val date = Calendar.getInstance().apply { time = this@isCurrentMonth }
        return now.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
            now.get(Calendar.MONTH) == date.get(Calendar.MONTH)
    }

    private data class DashboardSnapshots(
        val trainer: DataSnapshot,
        val requests: DataSnapshot,
        val users: DataSnapshot,
        val freePlans: DataSnapshot
    )

    private companion object {
        val completedRoutineStatuses = setOf("realizada", "completada")
    }
}
