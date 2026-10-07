package com.example.mvt.goals.model

import java.text.SimpleDateFormat
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class GoalsDashboardTest {
    private val now = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse("2026-09-13")!!
    private fun goal(date: String, progress: Int? = null, completed: Boolean = false) =
        SportGoal(id = date, targetDate = date, progressPercent = progress, completed = completed)

    @Test fun monthIncludesFutureDeadlinesInCurrentMonthOnly() {
        val result = buildGoalsDashboard(listOf(goal("2026-08-31"), goal("2026-09-01"), goal("2026-09-30"), goal("2026-10-01")), GoalsPeriod.MONTH, now)
        assertEquals(listOf("2026-09-01", "2026-09-30"), result.active.map { it.targetDate })
    }

    @Test fun absentTrackingDoesNotBecomeZeroOrCompleteWhenOverdue() {
        val result = buildGoalsDashboard(listOf(goal("2026-09-01")), GoalsPeriod.MONTH, now)
        assertNull(result.averagePercent)
        assertEquals(1, result.active.size)
        assertTrue(result.completed.isEmpty())
        assertNull(result.months.single().percent)
    }

    @Test fun averagesOnlyRecordedProgressAndSeparatesCompletedGoals() {
        val result = buildGoalsDashboard(listOf(goal("2026-09-01", 40), goal("2026-09-02"), goal("2026-09-03", completed = true)), GoalsPeriod.MONTH, now)
        assertEquals(70, result.averagePercent)
        assertEquals(2, result.trackedCount)
        assertEquals(3, result.totalCount)
        assertEquals(1, result.completed.size)
        assertEquals(70, result.months.single().percent)
    }

    @Test fun threeMonthsCrossesYearBoundaryAndBoundsProgress() {
        val january = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse("2026-01-15")!!
        val result = buildGoalsDashboard(listOf(goal("2025-10-31"), goal("2025-11-01", -5), goal("2026-01-20", 110)), GoalsPeriod.THREE_MONTHS, january)
        assertEquals(2, result.totalCount)
        assertEquals(50, result.averagePercent)
        assertEquals(3, result.months.size)
        assertEquals(1, result.completed.size)
    }

    @Test fun allKeepsUndatedAndOlderGoalsAccessible() {
        val goals = listOf(goal(""), goal("2020-01-01"), goal("2027-01-01"))
        assertEquals(3, buildGoalsDashboard(goals, GoalsPeriod.ALL, now).active.size)
        assertNull(goalDueDate(goal("2026-02-30")))
    }
}
