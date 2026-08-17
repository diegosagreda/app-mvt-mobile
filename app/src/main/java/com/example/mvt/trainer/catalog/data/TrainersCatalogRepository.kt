package com.example.mvt.trainer.catalog.data

import com.example.mvt.trainer.catalog.model.AssignmentStatus
import com.example.mvt.trainer.catalog.model.AthletePlan
import com.example.mvt.trainer.catalog.model.AthleteRatingProfile
import com.example.mvt.trainer.catalog.model.TrainerCardModel
import com.example.mvt.trainer.catalog.model.TrainerProfile
import com.example.mvt.trainer.catalog.model.TrainerRelationship
import com.example.mvt.trainer.catalog.model.TrainerRequest
import com.example.mvt.trainer.catalog.model.TrainersCatalogState
import com.google.firebase.Timestamp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TrainersCatalogRepository(
    database: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val root = database.reference

    fun observeCatalog(athleteId: String): Flow<TrainersCatalogState> = callbackFlow {
        if (athleteId.isBlank()) {
            trySend(TrainersCatalogState.Error("No hay una sesión activa."))
            close()
            return@callbackFlow
        }

        trySend(TrainersCatalogState.Loading)
        val trainersQuery = root.child(USERS).orderByChild("rol").equalTo("Entrenador")
        val requestsQuery = root.child(REQUESTS).orderByChild("id_deportista").equalTo(athleteId)
        val athleteRef = root.child(USERS).child(athleteId)
        var trainers = emptyList<TrainerProfile>()
        var requests = emptyList<TrainerRequest>()
        var athlete = AthleteRatingProfile(id = athleteId)
        var plan = AthletePlan()
        var trainersLoaded = false
        var requestsLoaded = false
        var athleteLoaded = false

        fun emitCurrent() {
            if (!trainersLoaded || !requestsLoaded || !athleteLoaded) return
            if (trainers.isEmpty()) {
                trySend(TrainersCatalogState.Empty)
                return
            }
            val assigned = requests
                .filter { it.status == AssignmentStatus.APPROVED }
                .maxByOrNull(TrainerRequest::createdAt)
            val cards = trainers.map { trainer ->
                val pending = requests
                    .filter { it.trainerId == trainer.id && it.status == AssignmentStatus.PENDING }
                    .maxByOrNull(TrainerRequest::createdAt)
                TrainerCardModel(
                    trainer = trainer,
                    relationship = when {
                        trainer.id == assigned?.trainerId -> TrainerRelationship.ASSIGNED
                        pending != null -> TrainerRelationship.PENDING
                        else -> TrainerRelationship.AVAILABLE
                    },
                    pendingRequest = pending
                )
            }.sortedWith(
                compareByDescending<TrainerCardModel> { it.relationship == TrainerRelationship.ASSIGNED }
                    .thenBy { it.trainer.fullName.lowercase() }
            )
            trySend(TrainersCatalogState.Ready(cards, plan, athlete, assigned?.trainerId.orEmpty()))
        }

        val trainersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trainersLoaded = true
                trainers = snapshot.children
                    .filter { it.text("rol") == "Entrenador" && it.text("estado") == "Aprobado" }
                    .map { it.toTrainerProfile() }
                emitCurrent()
            }
            override fun onCancelled(error: DatabaseError) = sendError(error)
            private fun sendError(error: DatabaseError) { trySend(TrainersCatalogState.Error(error.friendlyMessage())) }
        }
        val requestsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                requestsLoaded = true
                requests = snapshot.children.map { it.toTrainerRequest(athleteId) }
                emitCurrent()
            }
            override fun onCancelled(error: DatabaseError) { trySend(TrainersCatalogState.Error(error.friendlyMessage())) }
        }
        val athleteListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                athleteLoaded = true
                athlete = AthleteRatingProfile(
                    id = athleteId,
                    fullName = listOf(snapshot.text("nombres"), snapshot.text("apellidos"))
                        .filter(String::isNotBlank).joinToString(" ").ifBlank { "Deportista" },
                    photoUrl = snapshot.text("foto_url")
                )
                val planSnapshot = snapshot.child("plan")
                plan = AthletePlan(
                    name = planSnapshot.text("nombre"),
                    requestLimit = planSnapshot.child("solicitudes").value.asInt(),
                    sentRequests = planSnapshot.child("solicitudesEnviadas").value.asInt()
                )
                emitCurrent()
            }
            override fun onCancelled(error: DatabaseError) { trySend(TrainersCatalogState.Error(error.friendlyMessage())) }
        }

        trainersQuery.addValueEventListener(trainersListener)
        requestsQuery.addValueEventListener(requestsListener)
        athleteRef.addValueEventListener(athleteListener)
        awaitClose {
            trainersQuery.removeEventListener(trainersListener)
            requestsQuery.removeEventListener(requestsListener)
            athleteRef.removeEventListener(athleteListener)
        }
    }

    suspend fun sendRequest(athlete: AthleteRatingProfile, trainer: TrainerProfile) {
        val approved = root.child(REQUESTS).orderByChild("id_deportista").equalTo(athlete.id).get().await()
            .children.any { AssignmentStatus.from(it.text("estado")) == AssignmentStatus.APPROVED }
        check(!approved) { "Ya tienes un entrenador asignado." }

        changePlanCount(athlete.id, increment = true)
        val requestRef = root.child(REQUESTS).push()
        try {
            requestRef.setValue(
                mapOf(
                    "id_deportista" to athlete.id,
                    "id_entrenador" to trainer.id,
                    "nombres" to athlete.fullName,
                    "foto" to athlete.photoUrl,
                    "deporte" to trainer.deporte,
                    "estado" to AssignmentStatus.PENDING.wireValue,
                    "fecha_registro" to System.currentTimeMillis()
                )
            ).await()
        } catch (error: Throwable) {
            runCatching { changePlanCount(athlete.id, increment = false) }
            throw error
        }

        val now = Date()
        runCatching {
            firestore.collection("notificaciones").add(
                mapOf(
                    "tipo" to "solicitud",
                    "remitente" to athlete.id,
                    "destinatario" to trainer.id,
                    "txt" to "Te ha enviado una solicitud para que sea su entrenador. Revísala en el menú de solicitudes pendientes.",
                    "fecha" to Timestamp(now),
                    "fechaStr" to isoDate(now),
                    "estado" to "no_leido"
                )
            ).await()
            root.child(USERS).child(trainer.id).child("notificaciones").setValue(true).await()
        }
    }

    suspend fun cancelRequest(athleteId: String, request: TrainerRequest) {
        check(request.athleteId == athleteId && request.status == AssignmentStatus.PENDING) {
            "La solicitud ya no está disponible para cancelar."
        }
        changePlanCount(athleteId, increment = false)
        try {
            root.child(REQUESTS).child(request.id).removeValue().await()
        } catch (error: Throwable) {
            runCatching { changePlanCount(athleteId, increment = true, skipValidation = true) }
            throw error
        }
    }

    private suspend fun changePlanCount(athleteId: String, increment: Boolean, skipValidation: Boolean = false) {
        suspendCancellableCoroutine { continuation ->
            var failure: String? = null
            root.child(USERS).child(athleteId).child("plan").runTransaction(object : Transaction.Handler {
                override fun doTransaction(data: MutableData): Transaction.Result {
                    val name = data.child("nombre").value?.toString().orEmpty()
                    val limit = data.child("solicitudes").value.asInt()
                    val sent = data.child("solicitudesEnviadas").value.asInt()
                    if (increment && !skipValidation) {
                        failure = when {
                            name == "Bronce" -> "Tu plan Bronce no permite enviar solicitudes."
                            limit <= 0 || sent >= limit -> "Alcanzaste el límite de solicitudes de tu plan."
                            else -> null
                        }
                        if (failure != null) return Transaction.abort()
                    }
                    data.child("solicitudesEnviadas").value = if (increment) sent + 1 else (sent - 1).coerceAtLeast(0)
                    return Transaction.success(data)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                    if (!continuation.isActive) return
                    when {
                        error != null -> continuation.resumeWithException(error.toException())
                        !committed -> continuation.resumeWithException(IllegalStateException(failure ?: "No fue posible actualizar el cupo de solicitudes."))
                        else -> continuation.resume(Unit)
                    }
                }
            })
        }
    }

    private fun DataSnapshot.toTrainerProfile() = TrainerProfile(
        id = key.orEmpty(), nombres = text("nombres"), apellidos = text("apellidos"),
        fotoUrl = text("foto_url"), descripcion = text("descripcion"), genero = text("genero"),
        pais = text("pais"), ciudad = text("ciudad"), paisActual = text("paisActual"),
        ciudadActual = text("ciudadActual"), fechaNacimiento = text("fecha_nacimiento"),
        resena = text("reseña").ifBlank { text("resena") }, hitos = text("hitos"),
        especialidad = text("especialidad"), experiencia = text("experiencia"), perfil = text("perfil"),
        formacionAcademica = text("formacion_academica"), certificaciones = text("certificaciones"),
        deporte = text("deporte"), estrellas = child("estrellas").value.asDouble()
    )

    private fun DataSnapshot.toTrainerRequest(athleteId: String) = TrainerRequest(
        id = key.orEmpty(), athleteId = text("id_deportista").ifBlank { athleteId },
        trainerId = text("id_entrenador"), athleteName = text("nombres"), athletePhoto = text("foto"),
        sport = text("deporte"), status = AssignmentStatus.from(text("estado")),
        createdAt = child("fecha_registro").value.asLong()
    )

    private fun DataSnapshot.text(key: String) = child(key).value?.toString()?.takeUnless { it == "null" }.orEmpty()
    private fun Any?.asInt() = when (this) { is Number -> toInt(); is String -> toIntOrNull() ?: 0; else -> 0 }
    private fun Any?.asLong() = when (this) { is Number -> toLong(); is String -> toLongOrNull() ?: 0L; else -> 0L }
    private fun Any?.asDouble() = when (this) { is Number -> toDouble(); is String -> toDoubleOrNull() ?: 0.0; else -> 0.0 }
    private fun DatabaseError.friendlyMessage() = when (code) {
        DatabaseError.PERMISSION_DENIED -> "No tienes permisos para consultar el catálogo."
        DatabaseError.DISCONNECTED, DatabaseError.NETWORK_ERROR -> "Revisa tu conexión e inténtalo nuevamente."
        else -> "No fue posible cargar los entrenadores."
    }

    private fun isoDate(date: Date): String = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        Locale.US
    ).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(date)

    companion object { private const val USERS = "users"; private const val REQUESTS = "solicitudes" }
}
