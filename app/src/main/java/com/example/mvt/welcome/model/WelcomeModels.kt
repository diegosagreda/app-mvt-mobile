package com.example.mvt.welcome.model

import com.example.mvt.goals.model.SportGoal
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil

enum class WelcomeStep(val remoteValue: Int, val position: Int) {
    PERSONAL_DATA(7, 1), PLAN(6, 2), OBJECTIVES(5, 3), FINAL_SELECTION(1, 4), COMPLETED(0, 5);

    companion object {
        fun from(value: Int?) = entries.firstOrNull { it.remoteValue == value }
            ?: if ((value ?: 0) <= 0) COMPLETED else PERSONAL_DATA
    }
}

data class WelcomePersonalForm(
    val firstName: String = "", val lastName: String = "", val birthDate: String = "",
    val gender: String = "", val phonePrefix: String = "+57", val phone: String = "",
    val dialCountryCode: String = "CO",
    val photoUrl: String = "",
    val height: String = "", val weight: String = "",
    val minHeartRate: String = "", val maxHeartRate: String = "",
    val subjectiveLevel: Int? = null,
    val heartRateMonitorAnswer: Boolean? = null
)

data class WelcomeValidation(val errors: Map<String, String> = emptyMap()) {
    val isValid get() = errors.isEmpty()
}

fun validatePersonal(form: WelcomePersonalForm): WelcomeValidation {
    val errors = buildMap {
        if (form.firstName.trim().length < 2) put("firstName", "Ingresa tu nombre")
        if (form.lastName.trim().length < 2) put("lastName", "Ingresa tus apellidos")
        if (form.birthDate.isBlank()) put("birthDate", "Selecciona tu fecha de nacimiento")
        if (form.gender.isBlank()) put("gender", "Selecciona tu género")
        if (!Regex("^\\+[0-9]{1,4}$").matches(form.phonePrefix.trim())) {
            put("phonePrefix", "Revisa el prefijo internacional")
        } else if (!isPhoneValidForDialCountry(form.phonePrefix, form.phone, form.dialCountryCode)) {
            put("phone", "El teléfono no es válido para el indicativo seleccionado")
        }
        val height = form.height.toDoubleOrNull()
        if (height == null || height !in 80.0..220.0) put("height", "La estatura debe estar entre 80 y 220 cm")
        val weight = form.weight.toDoubleOrNull()
        if (weight == null || weight <= 30.0) put("weight", "El peso debe ser mayor a 30 kg")
        val minHr = form.minHeartRate.toIntOrNull()
        if (minHr == null || minHr !in 35..80) put("minHeartRate", "La FC mínima debe estar entre 35 y 80")
        val maxHr = form.maxHeartRate.toIntOrNull()
        if (maxHr == null || maxHr !in 150..220) put("maxHeartRate", "La FC máxima debe estar entre 150 y 220")
        if (minHr != null && maxHr != null && minHr >= maxHr) put("maxHeartRate", "La FC máxima debe superar la mínima")
        if (form.subjectiveLevel !in 1..10) put("subjectiveLevel", "Selecciona tu nivel deportivo")
        if (form.heartRateMonitorAnswer == null) put("heartRateMonitor", "Indica si usas pulsómetro")
    }
    return WelcomeValidation(errors)
}

data class DialCountryOption(val value: String, val label: String, val dialCode: String)

val dialCountryOptions = listOf(
    DialCountryOption("AR", "Argentina (+54)", "+54"),
    DialCountryOption("AU", "Australia (+61)", "+61"),
    DialCountryOption("BO", "Bolivia (+591)", "+591"),
    DialCountryOption("BR", "Brasil (+55)", "+55"),
    DialCountryOption("CA", "Canadá (+1)", "+1"),
    DialCountryOption("CL", "Chile (+56)", "+56"),
    DialCountryOption("CO", "Colombia (+57)", "+57"),
    DialCountryOption("CR", "Costa Rica (+506)", "+506"),
    DialCountryOption("CU", "Cuba (+53)", "+53"),
    DialCountryOption("DO", "República Dominicana (+1)", "+1"),
    DialCountryOption("EC", "Ecuador (+593)", "+593"),
    DialCountryOption("ES", "España (+34)", "+34"),
    DialCountryOption("FR", "Francia (+33)", "+33"),
    DialCountryOption("GB", "Reino Unido (+44)", "+44"),
    DialCountryOption("DE", "Alemania (+49)", "+49"),
    DialCountryOption("GT", "Guatemala (+502)", "+502"),
    DialCountryOption("HN", "Honduras (+504)", "+504"),
    DialCountryOption("IE", "Irlanda (+353)", "+353"),
    DialCountryOption("IT", "Italia (+39)", "+39"),
    DialCountryOption("MX", "México (+52)", "+52"),
    DialCountryOption("NI", "Nicaragua (+505)", "+505"),
    DialCountryOption("PA", "Panamá (+507)", "+507"),
    DialCountryOption("PE", "Perú (+51)", "+51"),
    DialCountryOption("PR", "Puerto Rico (+1)", "+1"),
    DialCountryOption("PT", "Portugal (+351)", "+351"),
    DialCountryOption("PY", "Paraguay (+595)", "+595"),
    DialCountryOption("UY", "Uruguay (+598)", "+598"),
    DialCountryOption("US", "Estados Unidos (+1)", "+1"),
    DialCountryOption("VE", "Venezuela (+58)", "+58"),
    DialCountryOption("OTHER", "Otro indicativo (personalizado)", "")
)

fun countryCodeToFlagEmoji(countryCode: String): String {
    if (countryCode == "OTHER") return "🌐"
    return countryCode.uppercase().map { char ->
        String(Character.toChars(char.code + 127397))
    }.joinToString("")
}

fun normalizeCustomPhonePrefix(value: String): String {
    val digits = value.filter(Char::isDigit).take(4)
    return when {
        digits.isNotEmpty() -> "+$digits"
        value.contains('+') -> "+"
        else -> ""
    }
}

fun isPhoneValidForDialCountry(prefix: String, phone: String, countryCode: String): Boolean {
    val normalizedPhone = phone.filter(Char::isDigit)
    if (normalizedPhone.isBlank()) return false

    return try {
        val phoneUtil = PhoneNumberUtil.getInstance()
        val normalizedPrefix = prefix.filter(Char::isDigit)
        val parsedNumber = if (countryCode == "OTHER") {
            phoneUtil.parse("+$normalizedPrefix$normalizedPhone", null)
        } else {
            phoneUtil.parse(normalizedPhone, countryCode)
        }
        val prefixMatches = parsedNumber.countryCode.toString() == normalizedPrefix
        val regionMatches = countryCode == "OTHER" || phoneUtil.isValidNumberForRegion(parsedNumber, countryCode)
        prefixMatches && regionMatches && phoneUtil.isValidNumber(parsedNumber)
    } catch (_: NumberParseException) {
        false
    }
}

data class WelcomeGoalForm(
    val name: String = "", val sport: String = "", val description: String = "",
    val targetDate: String = "", val generalGoal: String = "", val specificText: String = "",
    val hours: String = "", val minutes: String = "", val seconds: String = "",
    val selectedDays: Set<String> = emptySet()
)

fun validateGoal(form: WelcomeGoalForm, requireAvailableDays: Boolean): WelcomeValidation = WelcomeValidation(buildMap {
    if (form.name.trim().length < 3) put("name", "Escribe un objetivo concreto")
    if (form.sport.isBlank()) put("sport", "Indica el deporte")
    if (form.generalGoal.isBlank()) put("generalGoal", "Selecciona un objetivo general")
    if (form.targetDate.isBlank()) {
        put("targetDate", "Define una fecha objetivo")
    } else if (form.targetDate < java.time.LocalDate.now().toString()) {
        put("targetDate", "La fecha objetivo no puede estar en el pasado")
    }
    if (isDurationGoalSport(form.sport)) {
        val hours = form.hours.toIntOrNull() ?: -1
        val minutes = form.minutes.toIntOrNull() ?: -1
        val seconds = form.seconds.toIntOrNull() ?: -1
        if (hours < 0 || minutes !in 0..59 || seconds !in 0..59 || hours + minutes + seconds <= 0) {
            put("specific", "Ingresa una duración válida")
        }
    } else if (form.sport.isNotBlank() && form.specificText.trim().length < 3) {
        put("specific", "Describe el objetivo específico")
    }
    if (requireAvailableDays && form.selectedDays.size != 4) {
        put("days", "Selecciona exactamente 4 días disponibles")
    }
})

fun isDurationGoalSport(sport: String) = sport in setOf(
    "Atletismo", "Ciclismo", "Natacion", "Natación", "Triatlon", "Triatlón"
)

data class WelcomePlanFeature(val label: String, val included: Boolean?, val value: String? = null)
data class WelcomePlan(
    val id: String,
    val name: String,
    val price: Double,
    val available: Boolean,
    val features: List<WelcomePlanFeature> = emptyList()
)
data class FreeTrainingRoutine(
    val data: Map<String, Any?> = emptyMap()
) {
    val isAssigned: Boolean get() = data.isNotEmpty()
    val title: String get() = firstText("titulo", "nombre", "tipo_esfuerzo", "tipo_medicion")
        .ifBlank { "Rutina programada" }
    val metadata: String get() = listOf(
        text("tipo_esfuerzo"),
        text("tipo_medicion"),
        text("tipo_terreno")
    ).filter(String::isNotBlank).joinToString(" · ")

    fun text(key: String): String = data[key]?.toString()?.takeUnless { it == "null" }.orEmpty()
    private fun firstText(vararg keys: String): String = keys.firstNotNullOfOrNull { key ->
        text(key).takeIf(String::isNotBlank)
    }.orEmpty()
}

data class FreeTrainingPlan(
    val id: String,
    val trainerId: String,
    val trainerName: String,
    val name: String,
    val sport: String,
    val level: String,
    val description: String,
    val goals: String,
    val weeks: List<List<FreeTrainingRoutine>>
) {
    val activeWeeks: Int get() = weeks.count { week -> week.any { it.isAssigned } }
    val sessionCount: Int get() = weeks.sumOf { week -> week.count { it.isAssigned } }
    val weeklyAverage: Double get() = if (activeWeeks == 0) 0.0 else sessionCount.toDouble() / activeWeeks
    val readyRoutineCount: Int get() = sessionCount
    val searchableText: String get() = listOf(name, sport, level, description, goals, trainerName)
        .joinToString(" ")
        .lowercase()
}
data class WelcomeTrainer(
    val id: String,
    val name: String,
    val sport: String,
    val photoUrl: String,
    val specialty: String = "",
    val rating: Double = 0.0,
    val description: String = "",
    val gender: String = "",
    val birthDate: String = "",
    val country: String = "",
    val city: String = "",
    val currentCountry: String = "",
    val currentCity: String = "",
    val review: String = "",
    val milestones: String = "",
    val experience: String = "",
    val professionalProfile: String = "",
    val academicBackground: String = "",
    val certifications: String = ""
)

data class WelcomeSnapshot(
    val step: WelcomeStep = WelcomeStep.PERSONAL_DATA,
    val planName: String = "",
    val personal: WelcomePersonalForm = WelcomePersonalForm(),
    val plans: List<WelcomePlan> = emptyList(),
    val goalOptions: Map<String, List<String>> = emptyMap(),
    val goals: List<SportGoal> = emptyList(),
    val availableDays: Set<String> = emptySet(),
    val freePlans: List<FreeTrainingPlan> = emptyList(),
    val trainers: List<WelcomeTrainer> = emptyList()
)

sealed interface WelcomeUiState {
    data object Loading : WelcomeUiState
    data class Ready(
        val snapshot: WelcomeSnapshot,
        val personal: WelcomePersonalForm = snapshot.personal,
        val goal: WelcomeGoalForm = WelcomeGoalForm(),
        val selectedDays: Set<String> = snapshot.availableDays,
        val selectedPlanId: String? = null,
        val selectedFreePlanId: String? = null,
        val previewFreePlanId: String? = null,
        val selectedTrainerId: String? = null,
        val validation: WelcomeValidation = WelcomeValidation(),
        val isSaving: Boolean = false,
        val message: String? = null
    ) : WelcomeUiState
    data class Error(val message: String) : WelcomeUiState
}
