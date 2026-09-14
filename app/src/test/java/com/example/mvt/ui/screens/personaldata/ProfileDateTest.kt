package com.example.mvt.ui.screens.personaldata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileDateTest {
    @Test
    fun displaysStoredDatesWithoutMovingTheBirthday() {
        assertEquals("29/02/2000", displayBirthDate("2000-02-29T00:00:00Z"))
        assertEquals("15/07/1990", displayBirthDate("1990-07-15"))
        assertEquals("15/07/1990", displayBirthDate("15/07/1990"))
    }

    @Test
    fun rejectsImpossibleAndIncompleteDates() {
        listOf("2023-02-29", "31/04/2000", "2000-13-01", "1990-07", "", "null/null/null")
            .forEach { assertNull(it, parseBirthDate(it)) }
    }

    @Test
    fun missingDateStaysEmpty() {
        assertEquals("", displayBirthDate(""))
    }
}
