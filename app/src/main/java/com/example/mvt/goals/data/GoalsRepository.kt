package com.example.mvt.goals.data

import com.example.mvt.goals.model.GoalDraft
import com.example.mvt.goals.model.GoalsData
import com.example.mvt.goals.model.GoalsPlan
import com.example.mvt.goals.model.SportGoal
import com.example.mvt.goals.model.TrainingAvailability
import com.google.firebase.Timestamp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GoalsRepository(
    database: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val root = database.reference

    suspend fun load(athleteId: String): GoalsData = coroutineScope {
        require(athleteId.isNotBlank()) { "No hay una sesión activa." }
        val goalsTask = async { root.child(GOALS).child(athleteId).get().await() }
        val categoriesTask = async { root.child(CATEGORIES).get().await() }
        val athleteTask = async { root.child(USERS).child(athleteId).get().await() }
        val requestsTask = async {
            root.child(REQUESTS).orderByChild("id_deportista").equalTo(athleteId).get().await()
        }

        val goalsSnapshot = goalsTask.await()
        val categoriesSnapshot = categoriesTask.await()
        val athleteSnapshot = athleteTask.await()
        val planName = athleteSnapshot.child("plan").text("nombre")
        val planSnapshot = if (planName.isNotBlank()) root.child(PLANS).child(planName).get().await() else null
        val trainerId = requestsTask.await().children.firstOrNull {
            it.text("estado").equals("Aprobado", ignoreCase = true)
        }?.text("id_entrenador").orEmpty()

        GoalsData(
            goals = goalsSnapshot.child("meta_deportiva").children
                .mapIndexedNotNull { index, snapshot -> snapshot.toGoal(index.toLong()) }
                .sortedByDescending { it.createdAt.takeIf { value -> value > 0L } ?: it.legacyOrder },
            availability = goalsSnapshot.child("dias_entrenamiento").toAvailability(),
            sports = categoriesSnapshot.children.mapNotNull { it.key }.sorted(),
            plan = GoalsPlan(
                name = planName,
                id = planSnapshot?.text("id").orEmpty(),
                planningDays = planSnapshot?.child("planificacion")?.value.asInt()
            ),
            trainerId = trainerId
        )
    }

    suspend fun loadGeneralGoals(sport: String): List<String> {
        if (sport.isBlank()) return emptyList()
        return root.child(CATEGORIES).child(sport).get().await().children
            .mapNotNull { it.value?.toString()?.takeIf(String::isNotBlank) }
    }

    suspend fun addGoal(athleteId: String, draft: GoalDraft, trainerId: String): SportGoal {
        validateDraft(draft)
        val now = System.currentTimeMillis()
        val goal = SportGoal(
            id = root.child(GOALS).child(athleteId).child("meta_deportiva").push().key.orEmpty(),
            name = draft.name.trim(), sport = draft.sport, generalGoal = draft.generalGoal,
            specific = if (isDurationSport(draft.sport)) formatDuration(draft.hours!!, draft.minutes!!, draft.seconds!!) else draft.specificText.trim(),
            specificHours = draft.hours, specificMinutes = draft.minutes, specificSeconds = draft.seconds,
            targetDate = draft.targetDate,
            legacyDate = "${draft.targetDate}T00:00:00.000Z",
            description = draft.description.trim(), createdAt = now, updatedAt = now
        )
        transactGoals(athleteId) { current -> current + goal.toFirebaseMap() }
        runCatching {
            notifyTrainer(
                athleteId, trainerId, "Agrego un nuevo objetivo",
                listOf(mapOf("campo" to "meta_deportiva", "nuevo" to goal.toFirebaseMap()))
            )
        }
        return goal
    }

    suspend fun deleteGoal(athleteId: String, goal: SportGoal) {
        transactGoals(athleteId) { current ->
            current.filterNot { map -> goalId(map) == goal.id }
        }
    }

    suspend fun updateGoal(athleteId: String, original: SportGoal, draft: GoalDraft, trainerId: String): SportGoal {
        validateDraft(draft)
        val updated = original.copy(
            name = draft.name.trim(),
            sport = draft.sport,
            generalGoal = draft.generalGoal,
            specific = if (isDurationSport(draft.sport)) {
                formatDuration(draft.hours!!, draft.minutes!!, draft.seconds!!)
            } else {
                draft.specificText.trim()
            },
            specificHours = draft.hours,
            specificMinutes = draft.minutes,
            specificSeconds = draft.seconds,
            targetDate = draft.targetDate,
            legacyDate = "${draft.targetDate}T00:00:00.000Z",
            description = draft.description.trim(),
            updatedAt = System.currentTimeMillis()
        )
        transactGoals(athleteId) { current ->
            current.map { map -> if (goalId(map) == original.id) updated.toFirebaseMap() else map }
        }
        runCatching {
            notifyTrainer(
                athleteId, trainerId, "Actualizó un objetivo",
                listOf(
                    mapOf(
                        "campo" to "meta_deportiva",
                        "anterior" to original.toFirebaseMap(),
                        "nuevo" to updated.toFirebaseMap()
                    )
                )
            )
        }
        return updated
    }

    suspend fun saveAvailability(
        athleteId: String,
        previous: TrainingAvailability,
        availability: TrainingAvailability,
        plan: GoalsPlan,
        trainerId: String
    ) {
        if (!plan.isBronze) {
            require(plan.planningDays > 0) { "No fue posible determinar los días permitidos por tu plan." }
            require(availability.selectedCount == plan.planningDays) {
                "Tu plan requiere seleccionar exactamente ${plan.planningDays} días."
            }
        }
        root.child(GOALS).child(athleteId).child("dias_entrenamiento").setValue(availability.asMap()).await()
        runCatching {
            notifyTrainer(
                athleteId, trainerId, "Actualizó su disponibilidad",
                listOf(
                    mapOf(
                        "campo" to "dias_entrenamiento",
                        "anterior" to previous.asMap(),
                        "nuevo" to availability.asMap()
                    )
                )
            )
        }
    }

    private suspend fun transactGoals(athleteId: String, change: (List<Map<String, Any?>>) -> List<Map<String, Any?>>) {
        suspendCancellableCoroutine { continuation ->
            root.child(GOALS).child(athleteId).child("meta_deportiva")
                .runTransaction(object : Transaction.Handler {
                    override fun doTransaction(data: MutableData): Transaction.Result {
                        val current = normalizeGoalCollection(data.value)
                        data.value = change(current)
                        return Transaction.success(data)
                    }

                    override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                        if (!continuation.isActive) return
                        when {
                            error != null -> continuation.resumeWithException(error.toException())
                            !committed -> continuation.resumeWithException(IllegalStateException("No fue posible actualizar los objetivos."))
                            else -> continuation.resume(Unit)
                        }
                    }
                })
        }
    }

    private suspend fun notifyTrainer(athleteId: String, trainerId: String, text: String, fields: List<Map<String, Any?>>) {
        if (trainerId.isBlank()) return
        val now = Date()
        firestore.collection("notificaciones").add(
            mapOf(
                "tipo" to "nuevoObjetivo", "remitente" to athleteId, "destinatario" to trainerId,
                "txt" to text, "fechaStr" to isoDate(now), "fecha" to Timestamp(now),
                "estado" to "no_leido", "rutina" to "", "fields" to fields
            )
        ).await()
        root.child(USERS).child(trainerId).child("notificaciones").setValue(true).await()
    }

    private fun validateDraft(draft: GoalDraft) {
        require(draft.name.trim().length >= 3) { "El nombre debe tener al menos 3 caracteres." }
        require(draft.sport.isNotBlank()) { "Selecciona un deporte." }
        require(draft.generalGoal.isNotBlank()) { "Selecciona un objetivo general." }
        require(draft.targetDate >= today()) { "La fecha objetivo no puede estar en el pasado." }
        if (isDurationSport(draft.sport)) {
            val h = draft.hours ?: -1; val m = draft.minutes ?: -1; val s = draft.seconds ?: -1
            require(h >= 0 && m in 0..59 && s in 0..59 && h + m + s > 0) { "Ingresa una duración válida." }
        } else {
            require(draft.specificText.trim().length >= 3) { "Describe el objetivo específico." }
        }
    }

    private fun DataSnapshot.toGoal(fallbackOrder: Long): SportGoal? {
        val raw = value as? Map<*, *> ?: return null
        val map = raw.entries.associate { it.key.toString() to it.value }
        val name = map.text("nombre")
        if (name.isBlank()) return null
        val legacyDate = map.text("fecha")
        val h = map.nullableInt("especificoH")
        val m = map.nullableInt("especificoM")
        val s = map.nullableInt("especificoS")
        return SportGoal(
            id = map.text("id").ifBlank { goalId(map) }, name = name,
            sport = map.text("deporte"), generalGoal = map.text("general"), specific = map.text("especifico"),
            specificHours = h, specificMinutes = m, specificSeconds = s,
            targetDate = map.text("targetDate").ifBlank { normalizeDate(legacyDate) },
            legacyDate = legacyDate, description = map.text("descripcion"),
            createdAt = map["createdAt"].asLong(), updatedAt = map["updatedAt"].asLong(),
            legacyOrder = key?.toLongOrNull() ?: fallbackOrder
        )
    }

    private fun SportGoal.toFirebaseMap(): Map<String, Any?> = linkedMapOf(
        "id" to id, "nombre" to name, "deporte" to sport, "general" to generalGoal,
        "especifico" to specific, "especificoH" to specificHours?.toString(),
        "especificoM" to specificMinutes?.toString(), "especificoS" to specificSeconds?.toString(),
        "fecha" to legacyDate, "targetDate" to targetDate, "descripcion" to description,
        "createdAt" to createdAt, "updatedAt" to updatedAt
    )

    private fun normalizeGoalCollection(value: Any?): List<Map<String, Any?>> = when (value) {
        is List<*> -> value.mapNotNull { item -> (item as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value } }
        is Map<*, *> -> value.values.mapNotNull { item -> (item as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value } }
        else -> emptyList()
    }

    private fun goalId(map: Map<String, Any?>): String = map.text("id").ifBlank {
        val signature = listOf("nombre", "deporte", "general", "fecha", "especifico").joinToString("|") { map.text(it) }
        "legacy-${signature.hashCode().toUInt().toString(16)}"
    }

    private fun DataSnapshot.toAvailability() = TrainingAvailability.from(
        listOf("lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo")
            .associateWith { child(it).getValue(Boolean::class.java) == true }
    )

    private fun normalizeDate(raw: String): String {
        if (raw.matches(Regex("\\d{4}-\\d{2}-\\d{2}.*"))) return raw.take(10)
        val source = raw.substringBefore(" (").trim()
        val patterns = listOf("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", "EEE MMM dd yyyy HH:mm:ss z", "MMM dd, yyyy")
        for (pattern in patterns) {
            runCatching { SimpleDateFormat(pattern, Locale.US).parse(source) }.getOrNull()?.let {
                return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it)
            }
        }
        return ""
    }

    private fun formatDuration(h: Int, m: Int, s: Int) = "%02d:%02d:%02d".format(Locale.US, h, m, s)
    private fun isDurationSport(sport: String) = sport in setOf("Atletismo", "Ciclismo", "Natacion", "Natación", "Triatlon", "Triatlón")
    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    private fun isoDate(date: Date) = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(date)
    private fun DataSnapshot.text(key: String) = child(key).value?.toString()?.takeUnless { it == "null" }.orEmpty()
    private fun Map<String, Any?>.text(key: String) = this[key]?.toString()?.takeUnless { it == "null" }.orEmpty()
    private fun Map<String, Any?>.nullableInt(key: String): Int? = when (val value = this[key]) {
        is Number -> value.toInt(); is String -> value.toIntOrNull(); else -> null
    }
    private fun Any?.asInt() = when (this) { is Number -> toInt(); is String -> toIntOrNull() ?: 0; else -> 0 }
    private fun Any?.asLong() = when (this) { is Number -> toLong(); is String -> toLongOrNull() ?: 0L; else -> 0L }

    companion object {
        private const val GOALS = "Objetivos"
        private const val CATEGORIES = "Categorias"
        private const val USERS = "users"
        private const val PLANS = "Planes"
        private const val REQUESTS = "solicitudes"
    }
}
