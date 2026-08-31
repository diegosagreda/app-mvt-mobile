package com.example.mvt.trainer.catalog.data

import com.example.mvt.trainer.catalog.model.AssignmentStatus
import com.example.mvt.trainer.catalog.model.AthleteRatingProfile
import com.example.mvt.trainer.catalog.model.TrainerAssignment
import com.example.mvt.trainer.catalog.model.TrainerProfile
import com.example.mvt.trainer.catalog.model.TrainerRating
import com.example.mvt.trainer.catalog.model.TrainerRatingsState
import com.example.mvt.trainer.catalog.model.TrainerScreenState
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TrainerRepository(
    database: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val root = database.reference

    fun observeTrainer(athleteId: String): Flow<TrainerScreenState> = callbackFlow {
        if (athleteId.isBlank()) {
            trySend(TrainerScreenState.Error("No hay una sesión activa."))
            close()
            return@callbackFlow
        }

        trySend(TrainerScreenState.Loading)
        val assignmentsQuery = root.child(REQUESTS_PATH)
            .orderByChild("id_deportista")
            .equalTo(athleteId)
        val athleteRef = root.child(USERS_PATH).child(athleteId)
        var trainerListener: ValueEventListener? = null
        var trainerId = ""
        var assignment: TrainerAssignment? = null
        var trainer: TrainerProfile? = null
        var athlete = AthleteRatingProfile(id = athleteId)

        fun emitCurrent() {
            val currentAssignment = assignment
            val currentTrainer = trainer
            when {
                currentAssignment == null -> trySend(TrainerScreenState.Unassigned())
                currentAssignment.status != AssignmentStatus.APPROVED ->
                    trySend(TrainerScreenState.Unassigned(currentAssignment.status))
                currentTrainer != null -> trySend(
                    TrainerScreenState.Assigned(currentAssignment, currentTrainer, athlete)
                )
                else -> trySend(TrainerScreenState.Loading)
            }
        }

        fun changeTrainer(newTrainerId: String) {
            trainerListener?.let { listener ->
                if (trainerId.isNotBlank()) {
                    root.child(USERS_PATH).child(trainerId).removeEventListener(listener)
                }
            }
            trainerListener = null
            trainerId = newTrainerId
            trainer = null
            if (newTrainerId.isBlank()) {
                emitCurrent()
                return
            }
            val ref = root.child(USERS_PATH).child(newTrainerId)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        trySend(TrainerScreenState.Error("No fue posible encontrar el perfil del entrenador."))
                        return
                    }
                    trainer = snapshot.toTrainerProfile(newTrainerId)
                    emitCurrent()
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(TrainerScreenState.Error(error.toFriendlyMessage()))
                }
            }
            trainerListener = listener
            ref.addValueEventListener(listener)
        }

        val athleteListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                athlete = AthleteRatingProfile(
                    id = athleteId,
                    fullName = listOf(snapshot.text("nombres"), snapshot.text("apellidos"))
                        .filter(String::isNotBlank).joinToString(" ").ifBlank { "Deportista" },
                    photoUrl = snapshot.text("foto_url"),
                    lastRatingMonth = snapshot.text(LAST_RATING_MONTH_FIELD)
                )
                emitCurrent()
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(TrainerScreenState.Error(error.toFriendlyMessage()))
            }
        }

        val assignmentListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val candidates = snapshot.children.map { it.toAssignment(athleteId) }
                assignment = candidates
                    .sortedWith(
                        compareByDescending<TrainerAssignment> { it.status == AssignmentStatus.APPROVED }
                            .thenByDescending { it.createdAt }
                    )
                    .firstOrNull()
                val nextTrainerId = assignment
                    ?.takeIf { it.status == AssignmentStatus.APPROVED }
                    ?.trainerId.orEmpty()
                if (nextTrainerId != trainerId) changeTrainer(nextTrainerId) else emitCurrent()
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(TrainerScreenState.Error(error.toFriendlyMessage()))
            }
        }

        athleteRef.addValueEventListener(athleteListener)
        assignmentsQuery.addValueEventListener(assignmentListener)
        awaitClose {
            athleteRef.removeEventListener(athleteListener)
            assignmentsQuery.removeEventListener(assignmentListener)
            trainerListener?.let { listener ->
                if (trainerId.isNotBlank()) root.child(USERS_PATH).child(trainerId).removeEventListener(listener)
            }
        }
    }

    fun observeRatings(trainerId: String, athleteId: String): Flow<TrainerRatingsState> = callbackFlow {
        trySend(TrainerRatingsState(isLoading = true))
        var mine: List<TrainerRating> = emptyList()
        var others: List<TrainerRating> = emptyList()
        var mineLoaded = false
        var othersLoaded = false
        var currentError: String? = null

        fun emitCurrent() {
            trySend(
                TrainerRatingsState(
                    mine = mine,
                    others = others,
                    isLoading = !mineLoaded || !othersLoaded,
                    error = currentError
                )
            )
        }

        val mineRegistration = firestore.collection(RATINGS_COLLECTION)
            .whereEqualTo("idEntrenador", trainerId)
            .whereEqualTo("idDeportista", athleteId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    mineLoaded = true
                    currentError = ratingsErrorMessage(error)
                    emitCurrent()
                    return@addSnapshotListener
                }
                mineLoaded = true
                mine = snapshot?.documents.orEmpty()
                    .mapNotNull { it.toRating() }
                    .sortedByDescending(TrainerRating::dateMillis)
                    .take(5)
                emitCurrent()
            }

        val othersRegistration = firestore.collection(RATINGS_COLLECTION)
            .whereEqualTo("idEntrenador", trainerId)
            .whereNotEqualTo("idDeportista", athleteId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    othersLoaded = true
                    currentError = ratingsErrorMessage(error)
                    emitCurrent()
                    return@addSnapshotListener
                }
                othersLoaded = true
                others = snapshot?.documents.orEmpty()
                    .mapNotNull { it.toRating() }
                    .sortedByDescending(TrainerRating::dateMillis)
                    .take(5)
                emitCurrent()
            }

        awaitClose {
            mineRegistration.remove()
            othersRegistration.remove()
        }
    }

    suspend fun submitRating(
        assignment: TrainerAssignment,
        trainer: TrainerProfile,
        athlete: AthleteRatingProfile,
        score: Int,
        comment: String
    ) {
        require(score in 1..5) { "La calificación debe estar entre 1 y 5." }
        check(assignment.status == AssignmentStatus.APPROVED && assignment.trainerId == trainer.id) {
            "La asignación con este entrenador ya no está activa."
        }
        val now = Date()
        val month = SimpleDateFormat("yyyy-MM", Locale.US).format(now)
        if (athlete.lastRatingMonth == month) {
            error("Ya calificaste a tu entrenador durante este mes.")
        }
        val rating = hashMapOf<String, Any>(
            "score" to score,
            "comment" to comment.trim(),
            "idEntrenador" to trainer.id,
            "idDeportista" to athlete.id,
            "entrenador" to trainer.fullName,
            "deportista" to athlete.fullName,
            "imgUser" to athlete.photoUrl,
            "dateScore" to Timestamp(now),
            "dateScoreStr" to SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now)
        )

        firestore.collection(RATINGS_COLLECTION).add(rating).await()
        updateTrainerAggregate(trainer.id, score)
        root.child(USERS_PATH).child(athlete.id).child(LAST_RATING_MONTH_FIELD).setValue(month).await()
    }

    private suspend fun updateTrainerAggregate(trainerId: String, score: Int) {
        suspendCancellableCoroutine { continuation ->
            root.child(USERS_PATH).child(trainerId).runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val oldCount = currentData.child("ratingCount").value.asLong()
                    val oldSum = currentData.child("ratingSum").value.asDouble()
                        .takeIf { oldCount > 0L }
                        ?: (currentData.child("estrellas").value.asDouble() * oldCount)
                    val newCount = oldCount + 1L
                    val newSum = oldSum + score
                    currentData.child("ratingCount").value = newCount
                    currentData.child("ratingSum").value = newSum
                    currentData.child("estrellas").value = newSum / newCount
                    return Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                    if (!continuation.isActive) return
                    when {
                        error != null -> continuation.resumeWithException(error.toException())
                        !committed -> continuation.resumeWithException(IllegalStateException("No se pudo actualizar la calificación del entrenador."))
                        else -> continuation.resume(Unit)
                    }
                }
            })
        }
    }

    private fun DataSnapshot.toAssignment(athleteId: String) = TrainerAssignment(
        id = key.orEmpty(),
        athleteId = text("id_deportista").ifBlank { athleteId },
        trainerId = text("id_entrenador").ifBlank { text("idEntrenador") },
        status = AssignmentStatus.from(text("estado")),
        createdAt = child("createdAt").value.asLong().takeIf { it > 0 } ?: child("fecha_registro").value.asLong()
    )

    private fun DataSnapshot.toTrainerProfile(id: String) = TrainerProfile(
        id = id,
        nombres = text("nombres"), apellidos = text("apellidos"), fotoUrl = text("foto_url"),
        descripcion = text("descripcion"), genero = text("genero"), pais = text("pais"),
        ciudad = text("ciudad"), paisActual = text("paisActual"), ciudadActual = text("ciudadActual"),
        fechaNacimiento = text("fecha_nacimiento"), resena = text("reseña").ifBlank { text("resena") },
        hitos = text("hitos"), especialidad = text("especialidad"), experiencia = text("experiencia"),
        perfil = text("perfil"), formacionAcademica = text("formacion_academica"),
        certificaciones = text("certificaciones"), estrellas = child("estrellas").value.asDouble()
    )

    private fun DocumentSnapshot.toRating(): TrainerRating? {
        val score = (get("score") as? Number)?.toInt() ?: return null
        return TrainerRating(
            id = id, score = score, comment = getString("comment").orEmpty(),
            athleteId = getString("idDeportista").orEmpty(), athleteName = getString("deportista").orEmpty(),
            athletePhotoUrl = getString("imgUser").orEmpty(),
            dateMillis = getTimestamp("dateScore")?.toDate()?.time ?: 0L,
            dateText = getString("dateScoreStr").orEmpty()
        )
    }

    private fun DataSnapshot.text(key: String) = child(key).value?.toString()?.takeUnless { it == "null" }.orEmpty()
    private fun Any?.asLong() = when (this) { is Number -> toLong(); is String -> toLongOrNull() ?: 0L; else -> 0L }
    private fun Any?.asDouble() = when (this) { is Number -> toDouble(); is String -> toDoubleOrNull() ?: 0.0; else -> 0.0 }
    private fun DatabaseError.toFriendlyMessage() = when (code) {
        DatabaseError.PERMISSION_DENIED -> "No tienes permisos para consultar la información del entrenador."
        DatabaseError.DISCONNECTED, DatabaseError.NETWORK_ERROR -> "Revisa tu conexión e inténtalo nuevamente."
        else -> "No fue posible cargar la información del entrenador."
    }

    private fun ratingsErrorMessage(error: Exception): String {
        return if (error.message?.contains("index", ignoreCase = true) == true) {
            "La consulta de calificaciones necesita un índice de Firestore."
        } else {
            "No fue posible cargar las calificaciones."
        }
    }

    companion object {
        private const val REQUESTS_PATH = "solicitudes"
        private const val USERS_PATH = "users"
        private const val RATINGS_COLLECTION = "calificaciones"
        private const val LAST_RATING_MONTH_FIELD = "mesUltimaCalificacion"
    }
}
