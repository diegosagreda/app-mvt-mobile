package com.example.mvt.welcome.data

import android.net.Uri
import com.example.mvt.goals.model.SportGoal
import com.example.mvt.welcome.model.*
import com.google.firebase.Timestamp
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private const val VIRTUAL_TRAINER_ID = "QKDHvESUFrb4nPkCGAFuTdk74bo2"

class WelcomeRepository(
    database: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private val root = database.reference

    fun observe(uid: String): Flow<WelcomeSnapshot> = callbackFlow {
        val userRef = root.child("users").child(uid)
        val freePlansRef = root.child("planes_gratuitos")
        var latestUser = WelcomeSnapshot()
        var latestFreePlans = emptyList<FreeTrainingPlan>()
        var freePlansListener: ValueEventListener? = null

        fun emitCurrent() {
            val isBronzeFinal = latestUser.step == WelcomeStep.FINAL_SELECTION &&
                latestUser.planName.lowercase(Locale.getDefault()).contains("bronce")
            trySend(if (isBronzeFinal) latestUser.copy(freePlans = latestFreePlans) else latestUser)
        }

        fun ensureFreePlansListener() {
            if (freePlansListener != null) return
            freePlansListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    latestFreePlans = snapshot.children.mapNotNull { it.toFreeTrainingPlan() }
                    emitCurrent()
                }

                override fun onCancelled(error: DatabaseError) { close(error.toException()) }
            }.also { freePlansRef.addValueEventListener(it) }
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val step = WelcomeStep.from(snapshot.child("bienvenida").asInt())
                val planName = listOf("plan/nombre", "plan/name", "plan/plan")
                    .firstNotNullOfOrNull { path -> snapshot.child(path).text().takeIf(String::isNotBlank) }
                    .orEmpty()
                latestUser = WelcomeSnapshot(step = step, planName = planName)
                if (step == WelcomeStep.FINAL_SELECTION && planName.lowercase(Locale.getDefault()).contains("bronce")) {
                    ensureFreePlansListener()
                }
                emitCurrent()
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        userRef.addValueEventListener(listener)
        awaitClose {
            userRef.removeEventListener(listener)
            freePlansListener?.let { freePlansRef.removeEventListener(it) }
        }
    }

    suspend fun loadStep(uid: String, snapshot: WelcomeSnapshot): WelcomeSnapshot = when (snapshot.step) {
        WelcomeStep.PERSONAL_DATA -> snapshot.copy(personal = loadPersonal(uid))
        WelcomeStep.PLAN -> snapshot.copy(plans = loadPlans())
        WelcomeStep.OBJECTIVES -> snapshot.copy(
            goalOptions = loadGoalOptions(),
            goals = loadGoals(uid),
            availableDays = loadAvailableDays(uid)
        )
        WelcomeStep.FINAL_SELECTION -> if (snapshot.planName.lowercase(Locale.getDefault()).contains("bronce")) {
            snapshot
        } else snapshot.copy(trainers = loadTrainers())
        else -> snapshot
    }

    private suspend fun loadGoalOptions(): Map<String, List<String>> =
        root.child("Categorias").get().await().children
            .mapNotNull { sport ->
                val name = sport.key?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                name to sport.children.mapNotNull { option ->
                    option.value?.toString()?.takeIf(String::isNotBlank)
                }
            }
            .sortedBy { it.first }
            .toMap(linkedMapOf())

    private suspend fun loadGoals(uid: String): List<SportGoal> =
        root.child("Objetivos/$uid/meta_deportiva").get().await().children.mapNotNull { item ->
            val name = item.child("nombre").text()
            if (name.isBlank()) return@mapNotNull null
            SportGoal(
                id = item.child("id").text().ifBlank { item.key.orEmpty() },
                name = name,
                sport = item.child("deporte").text(),
                generalGoal = item.child("general").text(),
                specific = item.child("especifico").text(),
                specificHours = item.child("especificoH").text().toIntOrNull(),
                specificMinutes = item.child("especificoM").text().toIntOrNull(),
                specificSeconds = item.child("especificoS").text().toIntOrNull(),
                targetDate = item.child("targetDate").text().ifBlank { item.child("fecha").text().take(10) },
                legacyDate = item.child("fecha").text(),
                description = item.child("descripcion").text(),
                createdAt = item.child("createdAt").value.asLong(),
                updatedAt = item.child("updatedAt").value.asLong()
            )
        }.sortedByDescending { it.createdAt }

    private suspend fun loadAvailableDays(uid: String): Set<String> =
        root.child("Objetivos/$uid/dias_entrenamiento").get().await().children
            .mapNotNull { day -> day.key?.takeIf { day.getValue(Boolean::class.java) == true } }
            .toSet()

    private suspend fun loadPersonal(uid: String): WelcomePersonalForm {
        val user = root.child("users/$uid").get().await()
        val morphology = root.child("Morfologias/$uid").get().await()
        val sports = root.child("Deportes/$uid").get().await()
        val phonePrefix = user.child("prefijo").text().ifBlank { "+57" }
        val dialCountryCode = dialCountryOptions
            .firstOrNull { it.value != "OTHER" && it.dialCode == phonePrefix }
            ?.value ?: "OTHER"
        return WelcomePersonalForm(
            firstName = user.child("nombres").text(), lastName = user.child("apellidos").text(),
            birthDate = user.child("fecha_nacimiento").text(), gender = user.child("genero").text(),
            phonePrefix = phonePrefix, phone = user.child("telefono").text(), dialCountryCode = dialCountryCode,
            photoUrl = user.child("foto_url").text(),
            height = morphology.child("estatura").text(),
            weight = morphology.child("peso").text(),
            minHeartRate = morphology.child("FCmin").text(), maxHeartRate = morphology.child("FCmax").text(),
            subjectiveLevel = sports.child("subjetivo").asInt().takeIf { it > 0 },
            heartRateMonitorAnswer = sports.child("pulsometro").text().takeIf(String::isNotBlank)?.equals("Si", true)
        )
    }

    suspend fun savePersonal(uid: String, form: WelcomePersonalForm) {
        val minHr = requireNotNull(form.minHeartRate.toIntOrNull())
        val maxHr = requireNotNull(form.maxHeartRate.toIntOrNull())
        val height = requireNotNull(form.height.toDoubleOrNull())
        val weight = requireNotNull(form.weight.toDoubleOrNull())
        val reserve = maxHr - minHr
        fun target(percent: Double) = (percent * reserve + minHr).roundToInt()
        val zones = mapOf(
            "z0min" to 80, "z0max" to target(.60), "z1min" to target(.60), "z1max" to target(.70),
            "z2min" to target(.70), "z2max" to target(.80), "z3min" to target(.80), "z3max" to target(.90),
            "z4min" to target(.90), "z4max" to target(.95), "z5min" to target(.95), "z5max" to maxHr
        )
        val imc = weight / ((height / 100) * (height / 100))
        val phone = form.phone.filter(Char::isDigit)
        val customPrefix = form.phonePrefix.takeUnless { prefix ->
            dialCountryOptions.any { it.value != "OTHER" && it.dialCode == prefix }
        }.orEmpty()
        val updates = mutableMapOf<String, Any?>(
            "users/$uid/nombres" to form.firstName.trim(), "users/$uid/apellidos" to form.lastName.trim(),
            "users/$uid/fecha_nacimiento" to form.birthDate, "users/$uid/genero" to form.gender,
            "users/$uid/prefijo" to form.phonePrefix.trim(), "users/$uid/telefono" to phone,
            "users/$uid/prefijoPersonalizado" to customPrefix,
            "Morfologias/$uid/estatura" to form.height, "Morfologias/$uid/peso" to form.weight,
            "Morfologias/$uid/IMC" to String.format("%.2f", imc),
            "Morfologias/$uid/FCmin" to form.minHeartRate, "Morfologias/$uid/FCmax" to form.maxHeartRate,
            "Deportes/$uid/subjetivo" to form.subjectiveLevel,
            "Deportes/$uid/pulsometro" to if (form.heartRateMonitorAnswer == true) "Si" else "No",
            "users/$uid/zonas" to zones, "users/$uid/bienvenida" to WelcomeStep.PLAN.remoteValue
        )
        root.updateChildren(updates).await()
    }

    suspend fun uploadProfilePhoto(uid: String, uri: Uri) {
        val reference = storage.reference.child("profilePictures/$uid.jpg")
        reference.putFile(uri).await()
        val url = reference.downloadUrl.await().toString()
        root.child("users/$uid/foto_url").setValue(url).await()
    }

    private suspend fun loadPlans(): List<WelcomePlan> = root.child("Planes").get().await().children.mapNotNull { item ->
        val available = item.child("disponibilidad").asBoolean()
        if (!available) null else WelcomePlan(
            id = item.key.orEmpty(), name = item.child("nombreCard").text().ifBlank { item.key.orEmpty() },
            price = item.child("precio").asDouble(), available = true,
            features = item.child("caracteristicasCard").children.mapNotNull { feature ->
                val label = feature.key?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                when (val value = feature.value) {
                    is Boolean -> WelcomePlanFeature(label, value)
                    null -> null
                    else -> WelcomePlanFeature(label, null, value.toString())
                }
            }
        )
    }.sortedBy { it.price }

    suspend fun selectFreePlan(uid: String) {
        val plan = mapOf(
            "caracteres_chat" to 0, "fotos" to 0, "nombre" to "Bronce", "rutinasComodin" to false,
            "solicitudes" to 3, "solicitudesEnviadas" to 0, "video_min" to 0,
            "fecha_registro" to ServerValue.TIMESTAMP
        )
        root.updateChildren(mapOf("users/$uid/plan" to plan, "users/$uid/bienvenida" to WelcomeStep.OBJECTIVES.remoteValue)).await()
    }

    suspend fun goBack(uid: String, currentStep: WelcomeStep) {
        val previous = when (currentStep) {
            WelcomeStep.PLAN -> WelcomeStep.PERSONAL_DATA
            WelcomeStep.OBJECTIVES -> WelcomeStep.PLAN
            WelcomeStep.FINAL_SELECTION -> WelcomeStep.OBJECTIVES
            else -> return
        }
        root.child("users/$uid/bienvenida").setValue(previous.remoteValue).await()
    }

    suspend fun addGoal(uid: String, form: WelcomeGoalForm) {
        val goalRef = root.child("Objetivos/$uid/meta_deportiva").push()
        val id = goalRef.key ?: error("No fue posible crear el objetivo.")
        goalRef.setValue(goalMap(id, form)).await()
    }

    suspend fun updateGoal(uid: String, id: String, form: WelcomeGoalForm) {
        root.child("Objetivos/$uid/meta_deportiva/$id").setValue(goalMap(id, form)).await()
    }

    suspend fun deleteGoal(uid: String, id: String) {
        root.child("Objetivos/$uid/meta_deportiva/$id").removeValue().await()
    }

    private fun goalMap(id: String, form: WelcomeGoalForm): Map<String, Any?> {
        val now = System.currentTimeMillis()
        val specificHours = form.hours.toIntOrNull()
        val specificMinutes = form.minutes.toIntOrNull()
        val specificSeconds = form.seconds.toIntOrNull()
        val specific = if (isDurationGoalSport(form.sport)) {
            "%02d:%02d:%02d".format(Locale.US, specificHours, specificMinutes, specificSeconds)
        } else form.specificText.trim()
        return mapOf(
            "id" to id, "nombre" to form.name.trim(), "descripcion" to form.description.trim(),
            "deporte" to form.sport.trim(), "general" to form.generalGoal, "especifico" to specific,
            "especificoH" to specificHours?.toString(), "especificoM" to specificMinutes?.toString(),
            "especificoS" to specificSeconds?.toString(), "fecha" to "${form.targetDate}T00:00:00.000Z",
            "targetDate" to form.targetDate, "createdAt" to now, "updatedAt" to now
        )
    }

    suspend fun completeObjectives(uid: String, selectedDays: Set<String>) {
        val sports = root.child("Deportes/$uid/subjetivo").get().await().asInt().coerceAtLeast(1)
        val distance = if (sports <= 3) 800 else if (sports <= 6) 1000 else 1200
        val vo2 = (22.4 * distance / 1000.0) - 11.3
        val vam = 6000.0 / distance
        val days = listOf("lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo")
            .associateWith { it in selectedDays }
        val test = mapOf("distancia" to distance, "VO2Max" to vo2, "VAM" to vam, "fecha" to ServerValue.TIMESTAMP)
        val testKey = root.child("Semicooper/$uid/regVAM").push().key ?: error("No fue posible crear el test físico.")
        root.updateChildren(mapOf(
            "Objetivos/$uid/dias_entrenamiento" to days,
            "Semicooper/$uid/actVAM/$uid" to test, "Semicooper/$uid/regVAM/$testKey" to test,
            "users/$uid/ritmos" to mapOf("VAM" to vam, "distancia" to distance),
            "users/$uid/bienvenida" to WelcomeStep.FINAL_SELECTION.remoteValue
        )).await()
    }

    private suspend fun loadTrainers(): List<WelcomeTrainer> = root.child("users").get().await().children
        .filter {
            it.child("rol").text().equals("Entrenador", ignoreCase = true) &&
                it.child("estado").text().equals("Aprobado", ignoreCase = true)
        }
        .map {
            WelcomeTrainer(
                it.key.orEmpty(),
                listOf(it.child("nombres").text(), it.child("apellidos").text())
                    .filter(String::isNotBlank)
                    .joinToString(" "),
                it.child("deporte").text(),
                it.child("foto_url").text(),
                specialty = it.child("especialidad").text(),
                rating = it.child("estrellas").asDouble(),
                description = it.child("descripcion").text(),
                gender = it.child("genero").text(),
                birthDate = it.child("fecha_nacimiento").text(),
                country = it.child("pais").text(),
                city = it.child("ciudad").text(),
                currentCountry = it.child("paisActual").text(),
                currentCity = it.child("ciudadActual").text(),
                review = it.child("reseña").text().ifBlank { it.child("resena").text() },
                milestones = it.child("hitos").text(),
                experience = it.child("experiencia").text(),
                professionalProfile = it.child("perfil").text(),
                academicBackground = it.child("formacion_academica").text(),
                certifications = it.child("certificaciones").text()
            )
        }

    suspend fun enrollFreePlan(uid: String, plan: FreeTrainingPlan) {
        val batch = firestore.batch()
        val subjective = root.child("Deportes/$uid/subjetivo").get().await().asInt()
        val nextMonday = nextMonday(LocalDate.now())
        val availableDays = datesBeforePlanStart(nextMonday)
        if (availableDays.isNotEmpty()) {
            val wildcardRoutines = loadWildcardRoutines(subjective, availableDays.size)
            wildcardRoutines.forEachIndexed { index, routine ->
                batch.set(
                    firestore.collection("rutinas").document(),
                    routine.toAssignedRoutine(uid, availableDays[index])
                )
            }
        }

        var weekMonday = nextMonday
        plan.weeks.forEach { week ->
            week.forEachIndexed { dayIndex, routine ->
                if (!routine.isAssigned) return@forEachIndexed
                batch.set(
                    firestore.collection("rutinas").document(),
                    routine.data.toAssignedRoutine(uid, weekMonday.plusDays(dayIndex.toLong()))
                )
            }
            weekMonday = weekMonday.plusWeeks(1)
        }
        batch.commit().await()
        root.updateChildren(mapOf("users/$uid/planGratuito" to plan.id, "users/$uid/bienvenida" to 0)).await()
    }

    private suspend fun loadWildcardRoutines(subjective: Int, limit: Int): List<Map<String, Any?>> {
        val range = when (subjective) {
            in 1..3 -> "bajo"
            in 4..6 -> "medio"
            else -> "alto"
        }
        return firestore.collection("rutinas_comodin")
            .whereEqualTo("rango", range)
            .get()
            .await()
            .documents
            .map { it.data.orEmpty() }
            .sortedBy { it["numero"].asSortableInt() }
            .take(limit)
    }

    private fun Map<String, Any?>.toAssignedRoutine(uid: String, date: LocalDate): Map<String, Any?> {
        val routine = toMutableMap()
        routine["id_deportista"] = uid
        routine["id_entrenador"] = VIRTUAL_TRAINER_ID
        routine["fecha"] = Timestamp(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()))
        routine.putIfAbsent("titulo", routineText("titulo", "nombre", "tipo_esfuerzo", "tipo_medicion").ifBlank { "Rutina programada" })
        routine.putIfAbsent("descripcion", "")
        routine.putIfAbsent("objetivos", "")
        routine.putIfAbsent("tipo_esfuerzo", "")
        routine.putIfAbsent("tipo_medicion", "")
        routine.putIfAbsent("tipo_terreno", "")
        routine.putIfAbsent("sesiones_calentamiento", emptyList<Map<String, Any?>>())
        routine.putIfAbsent("sesiones_central", mapOf("numero_series" to 0, "series" to emptyList<Map<String, Any?>>()))
        routine.putIfAbsent("sesiones_calma", emptyList<Map<String, Any?>>())
        routine.putIfAbsent("comentarios_fase_calentamiento", "")
        routine.putIfAbsent("comentarios_fase_central", "")
        routine.putIfAbsent("comentarios_fase_calma", "")
        routine.putIfAbsent("comentarios", listOf(
            mapOf(
                "rol" to "Entrenador",
                "id" to 0,
                "comentario" to "Bienvenid@",
                "autor" to "Entrenador Virtual",
                "fecha" to System.currentTimeMillis()
            )
        ))
        routine.putIfAbsent("estado", "Pendiente")
        routine.putIfAbsent("completa", true)
        return routine
    }

    private fun Map<String, Any?>.routineText(vararg keys: String): String =
        keys.firstNotNullOfOrNull { key -> this[key]?.toString()?.takeUnless { it == "null" || it.isBlank() } }.orEmpty()

    suspend fun requestTrainer(uid: String, trainer: WelcomeTrainer) {
        val user = root.child("users/$uid").get().await()
        val sent = user.child("plan/solicitudesEnviadas").asInt()
        val limit = user.child("plan/solicitudes").asInt()
        check(sent < limit) { "Alcanzaste el límite de solicitudes de tu plan." }
        val key = root.child("solicitudes").push().key ?: error("No fue posible crear la solicitud.")
        val request = mapOf(
            "id_deportista" to uid, "id_entrenador" to trainer.id,
            "nombres" to listOf(user.child("nombres").text(), user.child("apellidos").text()).filter(String::isNotBlank).joinToString(" "),
            "foto" to user.child("foto_url").text(), "deporte" to trainer.sport,
            "estado" to "Pendiente", "fecha_registro" to ServerValue.TIMESTAMP
        )
        root.updateChildren(mapOf(
            "solicitudes/$key" to request, "users/$uid/plan/solicitudesEnviadas" to sent + 1,
            "users/${trainer.id}/notificaciones" to true, "users/$uid/bienvenida" to 0
        )).await()
        val now = Date()
        firestore.collection("notificaciones").add(
            mapOf(
                "tipo" to "solicitud", "remitente" to uid, "destinatario" to trainer.id,
                "txt" to "Te ha enviado una solicitud para que sea su entrenador. Revísala en el menú de solicitudes pendientes.",
                "fecha" to Timestamp(now), "fechaStr" to java.time.Instant.ofEpochMilli(now.time).toString(),
                "estado" to "no_leido"
            )
        ).await()
    }
}

private fun DataSnapshot.text() = value?.toString()?.takeUnless { it == "null" }.orEmpty()
private fun DataSnapshot.asInt() = when (val raw = value) { is Number -> raw.toInt(); is String -> raw.toDoubleOrNull()?.toInt() ?: 0; else -> 0 }
private fun DataSnapshot.asDouble() = when (val raw = value) { is Number -> raw.toDouble(); is String -> raw.toDoubleOrNull() ?: 0.0; else -> 0.0 }
private fun DataSnapshot.asBoolean() = when (val raw = value) { is Boolean -> raw; is Number -> raw.toInt() != 0; is String -> raw.equals("true", true) || raw == "1"; else -> false }
private fun Any?.asLong() = when (this) { is Number -> toLong(); is String -> toLongOrNull() ?: 0L; else -> 0L }
private fun Any?.asSortableInt() = when (this) { is Number -> toInt(); is String -> toIntOrNull() ?: Int.MAX_VALUE; else -> Int.MAX_VALUE }

private fun DataSnapshot.toFreeTrainingPlan(): FreeTrainingPlan? {
    val id = key.orEmpty()
    if (id.isBlank()) return null
    return FreeTrainingPlan(
        id = id,
        trainerId = child("id_entrenador").text(),
        trainerName = child("nombre_entrenador").text(),
        name = child("nombre_plan").text(),
        sport = child("deporte").text(),
        level = child("nivel").text(),
        description = child("descripcion").text(),
        goals = child("objetivos").text(),
        weeks = child("plan").toRoutineMatrix()
    ).takeIf { it.name.isNotBlank() }
}

private fun DataSnapshot.toRoutineMatrix(): List<List<FreeTrainingRoutine>> =
    (0 until 4).map { weekIndex ->
        val week = child("semana$weekIndex")
        (0 until 7).map { dayIndex ->
            val raw = week.child("dia$dayIndex").value
            FreeTrainingRoutine(raw.toStringKeyMap())
        }
    }

private fun Any?.toStringKeyMap(): Map<String, Any?> = when (this) {
    is Map<*, *> -> entries.associate { (key, value) -> key.toString() to value.normalizeFirebaseValue() }
    else -> emptyMap()
}

private fun Any?.normalizeFirebaseValue(): Any? = when (this) {
    is Map<*, *> -> entries.associate { (key, value) -> key.toString() to value.normalizeFirebaseValue() }
    is List<*> -> map { it.normalizeFirebaseValue() }
    else -> this
}

private fun nextMonday(from: LocalDate): LocalDate {
    var date = from.plusDays(1)
    while (date.dayOfWeek != DayOfWeek.MONDAY) date = date.plusDays(1)
    return date
}

private fun datesBeforePlanStart(nextMonday: LocalDate): List<LocalDate> {
    val dates = mutableListOf<LocalDate>()
    var date = LocalDate.now().plusDays(1)
    while (date.isBefore(nextMonday)) {
        dates += date
        date = date.plusDays(1)
    }
    return dates
}
