package com.example.mvt.registration.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistrationModelsTest {
    @Test
    fun `personal data accepts accents apostrophes and hyphens`() {
        val errors = validatePersonalStep(
            AthleteRegistrationForm(
                firstName = "María-José",
                lastName = "O'Connor",
                username = "maria.jose_26",
                acceptedTerms = true,
                acceptedPrivacyPolicy = true
            )
        )

        assertFalse(errors.hasPersonalErrors)
    }

    @Test
    fun `personal data rejects numbers and missing legal acceptance`() {
        val errors = validatePersonalStep(
            AthleteRegistrationForm(firstName = "Ana2", lastName = "Pérez", username = "a")
        )

        assertTrue(errors.hasPersonalErrors)
        assertTrue(errors.firstName != null)
        assertTrue(errors.username != null)
        assertTrue(errors.legal != null)
    }

    @Test
    fun `password exposes every required condition`() {
        val valid = passwordChecks("Atleta#2026")
        val invalid = passwordChecks("atleta")

        assertTrue(valid.allValid)
        assertFalse(invalid.minimumLength)
        assertFalse(invalid.uppercase)
        assertFalse(invalid.number)
        assertFalse(invalid.special)
    }

    @Test
    fun `access validates email password and confirmation`() {
        val form = AthleteRegistrationForm(
            email = "ATLETA@EXAMPLE.COM",
            password = "Atleta#2026",
            passwordConfirmation = "Atleta#2026"
        )
        val errors = validateAccessStep(form)
        val command = form.copy(firstName = " Ana ", lastName = " Pérez ", username = " atleta ").toCommand()

        assertFalse(errors.hasAccessErrors)
        assertNull(errors.email)
        assertEquals("atleta@example.com", command.email)
        assertEquals("Ana", command.firstName)
    }
}
