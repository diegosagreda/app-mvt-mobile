package com.example.mvt.registration.data

import com.example.mvt.data.firebase.GoogleAccountProfile
import com.example.mvt.registration.model.AthleteRegistrationCommand
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AthleteRegistrationService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    private val root = database.reference

    suspend fun createAuthUser(command: AthleteRegistrationCommand): FirebaseUser {
        return auth.createUserWithEmailAndPassword(command.email, command.password).await().user
            ?: error("Firebase no devolvió el usuario creado.")
    }

    suspend fun reserveNumericId(): Long = suspendCancellableCoroutine { continuation ->
        root.child("UsersID").child("IDactual").runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val next = (currentData.getValue(Long::class.java) ?: 0L) + 1L
                currentData.value = next
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: com.google.firebase.database.DataSnapshot?) {
                when {
                    error != null -> if (continuation.isActive) continuation.resumeWithException(error.toException())
                    !committed -> if (continuation.isActive) continuation.resumeWithException(IllegalStateException("No fue posible reservar el identificador."))
                    else -> if (continuation.isActive) continuation.resume(snapshot?.getValue(Long::class.java) ?: 0L)
                }
            }
        })
    }

    suspend fun initializeAthlete(uid: String, numericId: Long, command: AthleteRegistrationCommand) {
        initializeAthlete(uid, numericId, command, googleProfile = null)
    }

    suspend fun initializeGoogleAthlete(
        user: FirebaseUser,
        googleProfile: GoogleAccountProfile = user.toFallbackGoogleProfile()
    ) {
        val numericId = reserveNumericId()
        initializeAthlete(
            uid = user.uid,
            numericId = numericId,
            command = googleProfile.toGoogleRegistrationCommand(user.uid),
            googleProfile = googleProfile
        )
    }

    private suspend fun initializeAthlete(
        uid: String,
        numericId: Long,
        command: AthleteRegistrationCommand,
        googleProfile: GoogleAccountProfile? = null
    ) {
        val now = System.currentTimeMillis()
        val profile = mapOf<String, Any>(
            "UserID" to numericId,
            "estado" to "Pendiente",
            "bienvenida" to 7,
            "onBoarding" to false,
            "pagoRecurrente" to false,
            "rol" to "Deportista",
            "showStrava" to false,
            "email" to command.email,
            "fecha_nacimiento" to "",
            "fecha_registro" to now,
            "foto_url" to googleProfile?.photoUrl.orEmpty(),
            "estrellas" to 0,
            "descripcion" to "",
            "telefono" to googleProfile?.phone.orEmpty(),
            "prefijo" to googleProfile?.phonePrefix.orEmpty(),
            "direccion" to "",
            "identificacion" to "",
            "nombres" to command.firstName,
            "apellidos" to command.lastName,
            "nameUser" to command.nameUser,
            "genero" to googleProfile?.gender.orEmpty(),
            "ciudadActual" to "",
            "pais" to "",
            "paisActual" to "",
            "plan" to mapOf(
                "nombre" to "Bronce", "solicitudes" to 3, "solicitudesEnviadas" to 0,
                "caracteres_chat" to 0, "rutinasComodin" to false, "video_min" to 0, "fotos" to 0
            ),
            "isLoggedIn" to false,
            "legalAcceptance" to mapOf(
                "termsAccepted" to true, "termsVersion" to "2026-01",
                "privacyAccepted" to true, "privacyVersion" to "2026-01",
                "acceptedAt" to ServerValue.TIMESTAMP, "platform" to "mobile"
            )
        )
        val morphology = mutableMapOf<String, Any>(
            "estatura" to "", "IMC" to "", "peso" to "", "grasa" to "",
            "FCmin" to "60", "FCmax" to "190", "somatipo" to "", "distancia" to "",
            "tiempo_marca" to "", "fecha_marca" to "", "ritmo" to "", "FCprom" to ""
        ).apply {
            listOf("cintura", "brazo", "muslo", "pantorrilla", "pecho", "hombros", "gluteos").forEach {
                put("fecha_$it", ""); put("medida_$it", "")
            }
        }
        val sports = mapOf<String, Any>(
            "edad_deportiva" to "", "subjetivo" to "",
            "otros" to mapOf("ciclismo" to false, "natacion" to false, "triatlon" to false, "gimnasio" to false, "trail" to false, "otro" to false),
            "otro" to "", "pulsometro" to "", "marcaPulsometro" to "", "resenia" to "", "mensaje" to "",
            "esfuerzo_trabajo" to "", "sueño" to "", "lesiones" to emptyList<Any>(),
            "enfermedades" to emptyList<Any>(), "estadoNutricion" to ""
        )
        val days = mapOf("lunes" to false, "martes" to false, "miercoles" to false, "jueves" to false, "viernes" to false, "sabado" to false, "domingo" to false)
        val goals = mapOf<String, Any>("meta_deportiva" to emptyList<Any>(), "dias_entrenamiento" to days)
        val semicooper = mapOf<String, Any>("semicooper" to "1000", "VAM_decimal" to "", "VAM" to "", "VO2Max" to "", "marcas" to emptyList<Any>())

        root.updateChildren(
            mapOf(
                "users/$uid" to profile,
                "Morfologias/$uid" to morphology,
                "Deportes/$uid" to sports,
                "Objetivos/$uid" to goals,
                "Semicooper/$uid" to semicooper
            )
        ).await()
    }

    private fun FirebaseUser.toFallbackGoogleProfile(): GoogleAccountProfile {
        return GoogleAccountProfile(
            email = email.orEmpty(),
            displayName = displayName.orEmpty(),
            givenName = "",
            familyName = "",
            photoUrl = photoUrl?.toString().orEmpty()
        )
    }

    private fun GoogleAccountProfile.toGoogleRegistrationCommand(uid: String): AthleteRegistrationCommand {
        val cleanEmail = email.trim().lowercase()
        val displayParts = displayName
            .trim()
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)
        val firstName = givenName.trim()
            .ifBlank { displayParts.firstOrNull().orEmpty() }
            .ifBlank { cleanEmail.substringBefore("@") }
        val lastName = familyName.trim()
            .ifBlank { displayParts.drop(1).joinToString(" ") }
        val username = cleanEmail.substringBefore("@")
            .replace(Regex("[^a-zA-Z0-9._-]"), "")
            .take(30)
            .ifBlank { uid.take(12) }
        return AthleteRegistrationCommand(
            firstName = firstName,
            lastName = lastName,
            nameUser = username,
            email = cleanEmail,
            password = ""
        )
    }

    suspend fun sendVerification(user: FirebaseUser) { user.sendEmailVerification().await() }
    fun signOut() = auth.signOut()

    suspend fun resendVerification(email: String, password: String) {
        val user = auth.signInWithEmailAndPassword(email, password).await().user
            ?: error("No fue posible autenticar la cuenta.")
        if (!user.isEmailVerified) user.sendEmailVerification().await()
        auth.signOut()
    }

    suspend fun requestEmailChange(currentEmail: String, password: String, newEmail: String) {
        val user = auth.signInWithEmailAndPassword(currentEmail, password).await().user
            ?: error("No fue posible autenticar la cuenta.")
        user.verifyBeforeUpdateEmail(newEmail.trim().lowercase()).await()
        auth.signOut()
    }
}
