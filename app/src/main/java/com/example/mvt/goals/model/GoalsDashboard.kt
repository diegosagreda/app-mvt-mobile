package com.example.mvt.goals.model

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class GoalsPeriod(val label: String, val months: Int) {
    MONTH("Este mes", 1), THREE_MONTHS("Últimos 3 meses", 3), SIX_MONTHS("Últimos 6 meses", 6), ALL("Todos", 6)
}

data class GoalsMonthProgress(val label: String, val percent: Int?)

data class GoalsDashboard(
    val active: List<SportGoal>,
    val completed: List<SportGoal>,
    val averagePercent: Int?,
    val trackedCount: Int,
    val totalCount: Int,
    val months: List<GoalsMonthProgress>
)

val SportGoal.displayProgress: Int?
    get() = if (completed) 100 else progressPercent?.coerceIn(0, 100)

val SportGoal.isCompleted: Boolean
    get() = completed || displayProgress == 100

fun buildGoalsDashboard(goals: List<SportGoal>, period: GoalsPeriod, now: Date = Date()): GoalsDashboard {
    val end = Calendar.getInstance().apply {
        time = now
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.MONTH, 1)
    }
    val start = (end.clone() as Calendar).apply { add(Calendar.MONTH, -period.months) }
    // Filter by the goal's due date; no workout history or inferred progress is used.
    val filtered = if (period == GoalsPeriod.ALL) goals else goals.filter { goal ->
        goalDueDate(goal)?.let { it >= start.time && it < end.time } == true
    }
    val tracked = filtered.mapNotNull { it.displayProgress }
    val months = (0 until period.months).map { offset ->
        val monthStart = (start.clone() as Calendar).apply { add(Calendar.MONTH, offset) }
        val monthEnd = (monthStart.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        val values = filtered.filter { goal ->
            goalDueDate(goal)?.let { it >= monthStart.time && it < monthEnd.time } == true
        }.mapNotNull { it.displayProgress }
        GoalsMonthProgress(
            SimpleDateFormat("MMM", Locale("es", "CO")).format(monthStart.time).replace(".", ""),
            values.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
        )
    }
    return GoalsDashboard(
        active = filtered.filterNot { it.isCompleted },
        completed = filtered.filter { it.isCompleted },
        averagePercent = tracked.takeIf { it.isNotEmpty() }?.average()?.roundToInt(),
        trackedCount = tracked.size,
        totalCount = filtered.size,
        months = months
    )
}

fun goalDueDate(goal: SportGoal): Date? {
    val source = goal.targetDate.ifBlank { goal.legacyDate }.substringBefore("T")
    if (!source.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return null
    val position = ParsePosition(0)
    return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }
        .parse(source, position)?.takeIf { position.index == source.length }
}
