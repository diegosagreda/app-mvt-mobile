package com.example.mvt.sports.model

data class OtherSports(
    val cycling: Boolean = false,
    val swimming: Boolean = false,
    val triathlon: Boolean = false,
    val gym: Boolean = false,
    val trail: Boolean = false,
    val other: Boolean = false
) {
    fun toFirebaseMap(): Map<String, Boolean> = mapOf(
        "ciclismo" to cycling,
        "natacion" to swimming,
        "triatlon" to triathlon,
        "gimnasio" to gym,
        "trail" to trail,
        "otro" to other
    )
}

data class SportsProfile(
    val review: String = "",
    val coachMessage: String = "",
    val sportsAge: String = "",
    val subjectiveLevel: Int? = null,
    val usesHeartRateMonitor: Boolean = false,
    val heartRateMonitorBrand: String = "",
    val otherSports: OtherSports = OtherSports(),
    val otherSportName: String = ""
) {
    fun normalizedForSave() = copy(
        review = review.trim(),
        coachMessage = coachMessage.trim(),
        heartRateMonitorBrand = if (usesHeartRateMonitor) heartRateMonitorBrand.trim() else "",
        otherSportName = if (otherSports.other) otherSportName.trim() else ""
    )

    fun toFirebaseUpdates(): Map<String, Any> = mapOf(
        "resenia" to review,
        "mensaje" to coachMessage,
        "edad_deportiva" to sportsAge,
        "subjetivo" to (subjectiveLevel ?: ""),
        "pulsometro" to if (usesHeartRateMonitor) "Si" else "No",
        "marcaPulsometro" to heartRateMonitorBrand,
        "otros" to otherSports.toFirebaseMap(),
        "otro" to otherSportName
    )
}

data class SportsValidationErrors(
    val coachMessage: String? = null,
    val sportsAge: String? = null,
    val heartRateMonitorBrand: String? = null,
    val otherSportName: String? = null
) {
    val hasErrors: Boolean
        get() = coachMessage != null || sportsAge != null ||
            heartRateMonitorBrand != null || otherSportName != null
}

data class ChangedSportsField(
    val field: String,
    val previous: Any,
    val new: Any
) {
    fun toFirebaseMap(): Map<String, Any> = mapOf(
        "campo" to field,
        "anterior" to previous,
        "nuevo" to new
    )
}

fun validateSportsProfile(profile: SportsProfile): SportsValidationErrors {
    val messageLength = profile.coachMessage.trim().length
    val validBrand = Regex("^[\\p{L}\\p{N} ._-]{2,40}$")
    return SportsValidationErrors(
        coachMessage = if (messageLength !in 50..1000) "Debe contener entre 50 y 1000 caracteres" else null,
        sportsAge = if (profile.sportsAge.isBlank()) "Selecciona tu experiencia deportiva" else null,
        heartRateMonitorBrand = if (
            profile.usesHeartRateMonitor && !validBrand.matches(profile.heartRateMonitorBrand.trim())
        ) "Ingresa una marca o modelo válido" else null,
        otherSportName = if (profile.otherSports.other && profile.otherSportName.isBlank()) {
            "Especifica el otro deporte"
        } else null
    )
}

fun sportsProfileFromFirebase(value: Any?): SportsProfile {
    val data = value as? Map<*, *> ?: emptyMap<Any, Any>()
    val others = data["otros"] as? Map<*, *> ?: emptyMap<Any, Any>()
    fun text(key: String) = data[key]?.toString()?.takeUnless { it == "null" }.orEmpty()
    fun flag(key: String) = when (val raw = others[key]) {
        is Boolean -> raw
        is Number -> raw.toInt() != 0
        is String -> raw.equals("true", ignoreCase = true) || raw == "1"
        else -> false
    }

    return SportsProfile(
        review = text("resenia"),
        coachMessage = text("mensaje"),
        sportsAge = text("edad_deportiva"),
        subjectiveLevel = when (val level = data["subjetivo"]) {
            is Number -> level.toInt()
            is String -> level.toDoubleOrNull()?.toInt()
            else -> null
        },
        usesHeartRateMonitor = text("pulsometro").equals("si", ignoreCase = true),
        heartRateMonitorBrand = text("marcaPulsometro"),
        otherSports = OtherSports(
            cycling = flag("ciclismo"),
            swimming = flag("natacion"),
            triathlon = flag("triatlon"),
            gym = flag("gimnasio"),
            trail = flag("trail"),
            other = flag("otro")
        ),
        otherSportName = text("otro")
    )
}

fun changedSportsFields(old: SportsProfile, new: SportsProfile): List<ChangedSportsField> = buildList {
    fun addIfChanged(field: String, previous: Any, current: Any) {
        if (previous != current) add(ChangedSportsField(field, previous, current))
    }
    addIfChanged("resenia", old.review, new.review)
    addIfChanged("mensaje", old.coachMessage, new.coachMessage)
    addIfChanged("edad_deportiva", old.sportsAge, new.sportsAge)
    addIfChanged("subjetivo", old.subjectiveLevel ?: "", new.subjectiveLevel ?: "")
    addIfChanged("pulsometro", if (old.usesHeartRateMonitor) "Si" else "No", if (new.usesHeartRateMonitor) "Si" else "No")
    addIfChanged("marcaPulsometro", old.heartRateMonitorBrand, new.heartRateMonitorBrand)
    addIfChanged("otros", old.otherSports.toFirebaseMap(), new.otherSports.toFirebaseMap())
    addIfChanged("otro", old.otherSportName, new.otherSportName)
}
