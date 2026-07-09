package com.example.mvt.sports.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SportsModelsTest {
    @Test
    fun `historical values are normalized without failing`() {
        val profile = sportsProfileFromFirebase(
            mapOf(
                "subjetivo" to "5",
                "pulsometro" to "si",
                "otros" to "",
                "marcaPulsometro" to "Garmin 255"
            )
        )

        assertEquals(5, profile.subjectiveLevel)
        assertTrue(profile.usesHeartRateMonitor)
        assertEquals("Garmin 255", profile.heartRateMonitorBrand)
        assertEquals(OtherSports(), profile.otherSports)
    }

    @Test
    fun `monitor and other sport validations are conditional`() {
        val errors = validateSportsProfile(
            SportsProfile(
                coachMessage = "a".repeat(50),
                sportsAge = "3 - 5 Años",
                usesHeartRateMonitor = true,
                heartRateMonitorBrand = "G",
                otherSports = OtherSports(other = true)
            )
        )

        assertNotNull(errors.heartRateMonitorBrand)
        assertNotNull(errors.otherSportName)
        assertTrue(errors.hasErrors)
    }

    @Test
    fun `normalization clears hidden conditional values`() {
        val normalized = SportsProfile(
            coachMessage = " mensaje ",
            usesHeartRateMonitor = false,
            heartRateMonitorBrand = "Garmin",
            otherSports = OtherSports(other = false),
            otherSportName = "Escalada"
        ).normalizedForSave()

        assertEquals("mensaje", normalized.coachMessage)
        assertEquals("", normalized.heartRateMonitorBrand)
        assertEquals("", normalized.otherSportName)
    }

    @Test
    fun `other sports are reported as one changed field`() {
        val old = SportsProfile(otherSports = OtherSports())
        val new = old.copy(otherSports = OtherSports(cycling = true, trail = true))
        val changes = changedSportsFields(old, new)

        assertEquals(1, changes.size)
        assertEquals("otros", changes.single().field)
        assertFalse((changes.single().previous as Map<*, *>)["ciclismo"] as Boolean)
        assertTrue((changes.single().new as Map<*, *>)["ciclismo"] as Boolean)
    }
}
