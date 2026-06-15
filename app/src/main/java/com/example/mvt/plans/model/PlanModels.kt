package com.example.mvt.plans.model

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil

const val PLAN_DURATION_DAYS = 30
private const val DAY_MILLIS = 86_400_000L

data class AthletePlan(
    val name: String = "",
    val registrationDateMillis: Long? = null,
    val requests: Int? = null,
    val sentRequests: Int? = null,
    val chatCharacters: Int? = null,
    val wildcardRoutines: Boolean? = null,
    val videoMinutes: Int? = null,
    val photos: Int? = null
)

data class PlanFeature(
    val label: String,
    val included: Boolean?,
    val value: String? = null
)

data class SubscriptionPlan(
    val id: String,
    val name: String,
    val price: Double,
    val subtitle: String = "",
    val available: Boolean,
    val features: List<PlanFeature>,
    val chatCharacters: Int? = null,
    val requests: Int? = null,
    val photos: Int? = null,
    val videoMinutes: Int? = null,
    val wildcardRoutines: Boolean? = null
) {
    val formattedPrice: String
        get() = "${'$'}${NumberFormat.getIntegerInstance(Locale("es", "CO")).format(price)} COP"
}

enum class SubscriptionStatus { ACTIVE, EXPIRES_SOON, EXPIRED, WITHOUT_DATE }

data class SubscriptionSummary(
    val planName: String,
    val registrationDateMillis: Long?,
    val cutoffDateMillis: Long?,
    val remainingDays: Int?,
    val status: SubscriptionStatus
)

sealed interface PlanCatalogState {
    data object Loading : PlanCatalogState
    data class Ready(
        val plans: List<SubscriptionPlan>,
        val currentPlanName: String? = null
    ) : PlanCatalogState
    data object Empty : PlanCatalogState
    data class Error(val message: String) : PlanCatalogState
}

sealed interface SubscriptionScreenState {
    data object Loading : SubscriptionScreenState
    data class Ready(val summary: SubscriptionSummary) : SubscriptionScreenState
    data object Empty : SubscriptionScreenState
    data class Error(val message: String) : SubscriptionScreenState
}

fun interface SubscriptionActionHandler {
    fun onSelectPlan(planId: String)
}

fun athletePlanFromFirebase(value: Any?): AthletePlan? {
    val data = value as? Map<*, *> ?: return null
    if (data.isEmpty()) return null
    fun text(key: String) = data[key]?.toString()?.takeUnless { it == "null" }.orEmpty()
    fun int(key: String) = when (val raw = data[key]) {
        is Number -> raw.toInt()
        is String -> raw.toDoubleOrNull()?.toInt()
        else -> null
    }
    fun bool(key: String) = data[key].asBooleanOrNull()
    return AthletePlan(
        name = text("nombre"),
        registrationDateMillis = parseFirebaseDate(data["fecha_registro"]),
        requests = int("solicitudes"),
        sentRequests = int("solicitudesEnviadas"),
        chatCharacters = int("caracteres_chat"),
        wildcardRoutines = bool("rutinasComodin"),
        videoMinutes = int("video_min"),
        photos = int("fotos")
    )
}

fun subscriptionPlansFromFirebase(value: Any?): List<SubscriptionPlan> {
    val collection = value as? Map<*, *> ?: return emptyList()
    return collection.entries.mapNotNull { (key, rawPlan) ->
        val data = rawPlan as? Map<*, *> ?: return@mapNotNull null
        val id = data["id"]?.toString()?.takeIf(String::isNotBlank) ?: key?.toString().orEmpty()
        if (id.isBlank()) return@mapNotNull null
        val available = data["disponibilidad"].asBooleanOrNull() == true
        val price = data["precio"].asDouble() ?: 0.0
        val features = (data["caracteristicasCard"] as? Map<*, *>)
            ?.entries
            ?.mapNotNull { (featureKey, featureValue) ->
                val label = featureKey?.toString()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                when (featureValue) {
                    is Boolean -> PlanFeature(label, featureValue)
                    else -> PlanFeature(label, null, featureValue?.toString().orEmpty())
                }
            }
            .orEmpty()
        SubscriptionPlan(
            id = id,
            name = data["nombreCard"]?.toString()?.takeIf(String::isNotBlank) ?: "Plan $id",
            price = price,
            subtitle = data["precioNormal"]?.toString().orEmpty(),
            available = available,
            features = features,
            chatCharacters = data["caracteresChat"].asInt(),
            requests = data["solicitudes"].asInt(),
            photos = data["fotos"].asInt(),
            videoMinutes = data["videoMin"].asInt(),
            wildcardRoutines = data["rutinasComodin"].asBooleanOrNull()
        )
    }.filter(SubscriptionPlan::available).sortedBy(SubscriptionPlan::price)
}

fun subscriptionSummary(plan: AthletePlan?, nowMillis: Long = System.currentTimeMillis()): SubscriptionSummary? {
    plan ?: return null
    val registration = plan.registrationDateMillis
    if (registration == null) {
        return SubscriptionSummary(plan.name, null, null, null, SubscriptionStatus.WITHOUT_DATE)
    }
    val cutoff = registration + PLAN_DURATION_DAYS * DAY_MILLIS
    val remaining = ceil((cutoff - nowMillis) / DAY_MILLIS.toDouble()).toInt().coerceAtLeast(0)
    val status = when {
        remaining == 0 -> SubscriptionStatus.EXPIRED
        remaining <= 5 -> SubscriptionStatus.EXPIRES_SOON
        else -> SubscriptionStatus.ACTIVE
    }
    return SubscriptionSummary(plan.name, registration, cutoff, remaining, status)
}

fun formatPlanDate(millis: Long?): String {
    if (millis == null) return "Sin información"
    return SimpleDateFormat("dd/MM/yy", Locale("es", "CO")).format(Date(millis))
}

fun SubscriptionPlan.matchesPlanName(currentPlanName: String?): Boolean =
    normalizePlanName(id) == normalizePlanName(currentPlanName)

private fun normalizePlanName(value: String?): String = value.orEmpty()
    .trim()
    .lowercase(Locale.ROOT)
    .replace(Regex("\\s+"), " ")

private fun parseFirebaseDate(value: Any?): Long? {
    when (value) {
        is Number -> return value.toLong().takeIf { it > 0 }
        is String -> {
            val source = value.trim()
            source.toDoubleOrNull()?.toLong()?.takeIf { it > 0 }?.let { return it }
            runCatching { Instant.parse(source).toEpochMilli() }.getOrNull()?.let { return it }
            val patterns = listOf("yyyy-MM-dd", "dd/MM/yyyy", "yyyy-MM-dd HH:mm:ss")
            patterns.forEach { pattern ->
                runCatching {
                    SimpleDateFormat(pattern, Locale.US).apply {
                        isLenient = false
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.parse(source)?.time
                }.getOrNull()?.let { return it }
            }
        }
    }
    return null
}

private fun Any?.asInt(): Int? = when (this) {
    is Number -> toInt()
    is String -> toDoubleOrNull()?.toInt()
    else -> null
}

private fun Any?.asDouble(): Double? = when (this) {
    is Number -> toDouble()
    is String -> toDoubleOrNull()
    else -> null
}

private fun Any?.asBooleanOrNull(): Boolean? = when (this) {
    is Boolean -> this
    is Number -> toInt() != 0
    is String -> when {
        equals("true", true) || equals("si", true) || this == "1" -> true
        equals("false", true) || equals("no", true) || this == "0" -> false
        else -> null
    }
    else -> null
}
