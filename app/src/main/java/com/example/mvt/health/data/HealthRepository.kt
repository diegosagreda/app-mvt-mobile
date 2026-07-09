package com.example.mvt.health.data

import com.example.mvt.health.model.HealthDisease
import com.example.mvt.health.model.HealthInjury
import com.example.mvt.health.model.HealthLoadData
import com.example.mvt.health.model.HealthProfile
import com.example.mvt.health.model.HealthRepositoryState
import com.google.firebase.Timestamp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class HealthRepository(
    database: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val root = database.reference

    fun observe(athleteId: String): Flow<HealthRepositoryState> = callbackFlow {
        if (athleteId.isBlank()) {
            trySend(HealthRepositoryState.Error("No hay una sesión activa."))
            close()
            return@callbackFlow
        }
        trySend(HealthRepositoryState.Loading)
        val catalogRef = root.child(PARAMETERS).child(NUTRITION_STATUS)
        var health: HealthProfile? = null
        var trainerId = ""
        var options: List<String>? = null
        var catalogError: String? = null

        fun emitReady() {
            val currentHealth = health ?: return
            val currentOptions = options ?: return
            trySend(
                HealthRepositoryState.Ready(
                    HealthLoadData(currentHealth, currentOptions, trainerId, catalogError)
                )
            )
        }

        val catalogListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                options = snapshot.children.mapNotNull { it.key }.sorted()
                catalogError = null
                emitReady()
            }

            override fun onCancelled(error: DatabaseError) {
                options = emptyList()
                catalogError = "No fue posible actualizar el catálogo nutricional."
                emitReady()
            }
        }
        catalogRef.addValueEventListener(catalogListener)

        val loadJob = launch {
            runCatching {
                coroutineScope {
                    val healthTask = async { root.child(SPORTS).child(athleteId).get().await() }
                    val requestsTask = async {
                        root.child(REQUESTS).orderByChild("id_deportista").equalTo(athleteId).get().await()
                    }
                    health = healthTask.await().toHealthProfile()
                    trainerId = requestsTask.await().children.firstOrNull {
                        it.text("estado").equals("Aprobado", ignoreCase = true)
                    }?.text("id_entrenador").orEmpty()
                }
            }.onSuccess { emitReady() }
                .onFailure { trySend(HealthRepositoryState.Error(it.friendlyMessage())) }
        }

        awaitClose {
            loadJob.cancel()
            catalogRef.removeEventListener(catalogListener)
        }
    }

    suspend fun save(
        athleteId: String,
        profile: HealthProfile,
        trainerId: String,
        changedFields: List<String>
    ): HealthProfile {
        require(athleteId.isNotBlank()) { "No hay una sesión activa." }
        val saved = profile.copy(updatedAt = System.currentTimeMillis())
        val updates = mapOf<String, Any>(
            "esfuerzo_trabajo" to saved.workPhysicalEffort?.toString().orEmpty(),
            "sueño" to saved.sleepHours?.toString().orEmpty(),
            "estadoNutricion" to saved.nutritionalStatus,
            "condicion_salud" to saved.healthCondition.trim(),
            "lesiones" to saved.injuries.map { it.toFirebaseMap() },
            "enfermedades" to saved.diseases.map { it.toFirebaseMap() },
            "updatedAt" to saved.updatedAt
        )
        root.child(SPORTS).child(athleteId).updateChildren(updates).await()

        if (changedFields.isNotEmpty() && trainerId.isNotBlank()) {
            runCatching { notifyTrainer(athleteId, trainerId, changedFields) }
        }
        return saved
    }

    suspend fun saveInjuries(
        athleteId: String,
        injuries: List<HealthInjury>,
        trainerId: String
    ) {
        require(athleteId.isNotBlank()) { "No hay una sesión activa." }
        root.child(SPORTS).child(athleteId).child("lesiones")
            .setValue(injuries.map { it.toFirebaseMap() })
            .await()
        if (trainerId.isNotBlank()) {
            runCatching { notifyTrainer(athleteId, trainerId, listOf("lesiones")) }
        }
    }

    private suspend fun notifyTrainer(athleteId: String, trainerId: String, changedFields: List<String>) {
        val now = Date()
        firestore.collection("notificaciones").add(
            mapOf(
                "tipo" to "salud",
                "remitente" to athleteId,
                "destinatario" to trainerId,
                "txt" to "Actualizó su información de salud",
                "fechaStr" to isoDate(now),
                "fecha" to Timestamp(now),
                "estado" to "no_leido",
                "rutina" to "",
                "fields" to changedFields
            )
        ).await()
        root.child(USERS).child(trainerId).child("notificaciones").setValue(true).await()
    }

    private fun DataSnapshot.toHealthProfile() = HealthProfile(
        workPhysicalEffort = text("esfuerzo_trabajo").toIntOrNull()?.takeIf { it in 1..10 },
        sleepHours = text("sueño").toIntOrNull()?.takeIf { it in 1..10 },
        nutritionalStatus = text("estadoNutricion").ifBlank { text("nutricion") },
        healthCondition = text("condicion_salud"),
        injuries = child("lesiones").children.mapIndexedNotNull { index, snapshot -> snapshot.toInjury(index) },
        diseases = child("enfermedades").children.mapIndexedNotNull { index, snapshot -> snapshot.toDisease(index) },
        updatedAt = child("updatedAt").value.asLong()
    )

    private fun DataSnapshot.toInjury(index: Int): HealthInjury? {
        val map = (value as? Map<*, *>)?.stringMap() ?: return null
        val location = map.text("zona").ifBlank { map.text("location") }
        val treatment = map.text("tratamiento")
        if (location.isBlank() && treatment.isBlank()) return null
        val signature = "$location|$treatment|${map.text("actual")}|${map.text("fecha_inicio")}|$index"
        return HealthInjury(
            id = map.text("id").ifBlank { "legacy-${signature.hashCode().toUInt().toString(16)}" },
            location = location,
            treatment = treatment,
            currentlyActive = map.text("actual").equals("Si", ignoreCase = true),
            startDate = map.text("fecha_inicio"),
            duration = map.text("duracion"),
            createdAt = map["createdAt"].asLong(),
            updatedAt = map["updatedAt"].asLong()
        )
    }

    private fun DataSnapshot.toDisease(index: Int): HealthDisease? {
        val map = (value as? Map<*, *>)?.stringMap() ?: return null
        if (map.isEmpty()) return null
        val signature = map.entries.sortedBy { it.key }.joinToString("|") { "${it.key}:${it.value}" } + index
        return HealthDisease(
            id = map.text("id").ifBlank { "legacy-${signature.hashCode().toUInt().toString(16)}" },
            name = map.text("nombre"), affectedArea = map.text("zona"),
            treatment = map.text("tratamiento"), startDate = map.text("fecha_inicio"),
            duration = map.text("duracion"),
            currentlyActive = map.text("actual").equals("Si", ignoreCase = true)
        )
    }

    private fun HealthInjury.toFirebaseMap(): Map<String, Any> = mapOf(
        "id" to id, "zona" to location, "tratamiento" to treatment,
        "actual" to if (currentlyActive) "Si" else "No",
        "fecha_inicio" to startDate, "duracion" to duration,
        "createdAt" to createdAt, "updatedAt" to updatedAt
    )

    private fun HealthDisease.toFirebaseMap(): Map<String, Any> = mapOf(
        "id" to id, "nombre" to name, "zona" to affectedArea,
        "tratamiento" to treatment, "fecha_inicio" to startDate,
        "duracion" to duration, "actual" to if (currentlyActive) "Si" else "No"
    )

    private fun Map<*, *>.stringMap() = entries.associate { it.key.toString() to it.value }
    private fun Map<String, Any?>.text(key: String) = this[key]?.toString()?.takeUnless { it == "null" }.orEmpty()
    private fun DataSnapshot.text(key: String) = child(key).value?.toString()?.takeUnless { it == "null" }.orEmpty()
    private fun Any?.asLong() = when (this) { is Number -> toLong(); is String -> toLongOrNull() ?: 0L; else -> 0L }
    private fun Throwable.friendlyMessage() = message?.takeIf(String::isNotBlank) ?: "No fue posible cargar la información de salud."
    private fun isoDate(date: Date) = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(date)

    companion object {
        private const val SPORTS = "Deportes"
        private const val PARAMETERS = "Parametrización"
        private const val NUTRITION_STATUS = "Estado nutricional"
        private const val REQUESTS = "solicitudes"
        private const val USERS = "users"
    }
}
