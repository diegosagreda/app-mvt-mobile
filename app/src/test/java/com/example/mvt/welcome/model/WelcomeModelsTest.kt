package com.example.mvt.welcome.model

import org.junit.Assert.*
import org.junit.Test

class WelcomeModelsTest {
    private val validPersonal = WelcomePersonalForm(
        firstName = "Ana", lastName = "Pérez", birthDate = "1995-05-10",
        gender = "Femenino", phonePrefix = "+57", phone = "3001234567",
        height = "165", weight = "58", minHeartRate = "55", maxHeartRate = "185",
        subjectiveLevel = 5, heartRateMonitorAnswer = false
    )

    @Test fun `personal form accepts valid required data`() {
        assertTrue(validatePersonal(validPersonal).isValid)
    }

    @Test fun `personal form rejects unsafe heart rate values`() {
        val result = validatePersonal(validPersonal.copy(minHeartRate = "90", maxHeartRate = "140"))
        assertFalse(result.isValid)
        assertTrue("minHeartRate" in result.errors)
        assertTrue("maxHeartRate" in result.errors)
    }

    @Test fun `paid plan goal requires at least one available day`() {
        val goal = validGoal()
        val invalid = validateGoal(goal, requireAvailableDays = true)
        assertEquals("Selecciona exactamente 4 días disponibles", invalid.errors["days"])
        assertTrue(validateGoal(goal.copy(selectedDays = setOf("lunes", "martes", "jueves", "sabado")), requireAvailableDays = true).isValid)
    }

    @Test fun `bronze plan goal does not require available days`() {
        assertTrue(validateGoal(validGoal(), requireAvailableDays = false).isValid)
    }

    private fun validGoal() = WelcomeGoalForm(
        name = "10K",
        sport = "Fútbol",
        targetDate = java.time.LocalDate.now().plusMonths(3).toString(),
        generalGoal = "Mejorar rendimiento",
        specificText = "Aumentar mi resistencia"
    )

    @Test fun `legacy welcome values use safe personal fallback`() {
        assertEquals(WelcomeStep.PERSONAL_DATA, WelcomeStep.from(4))
        assertEquals(WelcomeStep.COMPLETED, WelcomeStep.from(0))
    }

    @Test fun `dial countries preserve web order and first plus one match`() {
        assertEquals("AR", dialCountryOptions.first().value)
        assertEquals("OTHER", dialCountryOptions.last().value)
        assertEquals("CA", dialCountryOptions.first { it.dialCode == "+1" }.value)
    }

    @Test fun `country flags use unicode regional symbols`() {
        assertEquals("🇨🇴", countryCodeToFlagEmoji("CO"))
        assertEquals("🌐", countryCodeToFlagEmoji("OTHER"))
    }

    @Test fun `custom phone prefix keeps plus and at most four digits`() {
        assertEquals("+999", normalizeCustomPhonePrefix("999"))
        assertEquals("+1234", normalizeCustomPhonePrefix("++12a345"))
        assertEquals("", normalizeCustomPhonePrefix("abc"))
    }

    @Test fun `phone validation uses selected dial country`() {
        assertTrue(isPhoneValidForDialCountry("+57", "3001234567", "CO"))
        assertTrue(isPhoneValidForDialCountry("+1", "2025550123", "US"))
        assertFalse(isPhoneValidForDialCountry("+1", "2025550123", "CA"))
        assertFalse(isPhoneValidForDialCountry("+57", "2025550123", "CO"))
    }
}
