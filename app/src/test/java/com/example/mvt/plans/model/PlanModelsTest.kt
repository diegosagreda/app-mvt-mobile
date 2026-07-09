package com.example.mvt.plans.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanModelsTest {
    @Test
    fun `catalog is normalized filtered and sorted by price`() {
        val plans = subscriptionPlansFromFirebase(
            mapOf(
                "Oro" to mapOf(
                    "nombreCard" to "Plan Oro",
                    "precio" to 100000,
                    "disponibilidad" to true,
                    "caracteristicasCard" to mapOf("Videollamadas" to false)
                ),
                "Oculto" to mapOf("precio" to 1, "disponibilidad" to false),
                "Bronce" to mapOf(
                    "precio" to "0",
                    "disponibilidad" to "true",
                    "caracteristicasCard" to mapOf("Duración" to "30 días")
                )
            )
        )

        assertEquals(listOf("Bronce", "Oro"), plans.map { it.id })
        assertEquals(false, plans.last().features.single().included)
        assertEquals("30 días", plans.first().features.single().value)
    }

    @Test
    fun `historical athlete plan types are accepted`() {
        val plan = athletePlanFromFirebase(
            mapOf(
                "nombre" to "Plata",
                "fecha_registro" to "1781881200000",
                "solicitudes" to "3",
                "rutinasComodin" to "no"
            )
        )

        assertEquals("Plata", plan?.name)
        assertEquals(1781881200000L, plan?.registrationDateMillis)
        assertEquals(3, plan?.requests)
        assertFalse(plan?.wildcardRoutines ?: true)
    }

    @Test
    fun `summary calculates active soon and expired states`() {
        val registration = 1_000_000L
        val plan = AthletePlan(name = "Plata", registrationDateMillis = registration)

        assertEquals(SubscriptionStatus.ACTIVE, subscriptionSummary(plan, registration)?.status)
        assertEquals(30, subscriptionSummary(plan, registration)?.remainingDays)
        assertEquals(SubscriptionStatus.EXPIRES_SOON, subscriptionSummary(plan, registration + 26 * 86_400_000L)?.status)
        assertEquals(SubscriptionStatus.EXPIRED, subscriptionSummary(plan, registration + 30 * 86_400_000L)?.status)
    }

    @Test
    fun `free plan without date does not create invalid dates`() {
        val summary = subscriptionSummary(AthletePlan(name = "Bronce"))

        assertEquals(SubscriptionStatus.WITHOUT_DATE, summary?.status)
        assertNull(summary?.remainingDays)
        assertNull(summary?.cutoffDateMillis)
        assertEquals("Sin información", formatPlanDate(summary?.registrationDateMillis))
    }

    @Test
    fun `plan name comparison ignores spaces and case`() {
        val plan = SubscriptionPlan("Plata", "Plan Plata", 60000.0, available = true, features = emptyList())

        assertTrue(plan.matchesPlanName("  pLaTa "))
        assertFalse(plan.matchesPlanName("Oro"))
    }

}
