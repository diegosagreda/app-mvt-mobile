package com.example.mvt.goals.model

data class SportGoal(
    val id: String = "",
    val name: String = "",
    val sport: String = "",
    val generalGoal: String = "",
    val specific: String = "",
    val specificHours: Int? = null,
    val specificMinutes: Int? = null,
    val specificSeconds: Int? = null,
    val targetDate: String = "",
    val legacyDate: String = "",
    val description: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val legacyOrder: Long = 0L
) {
    val isDuration: Boolean get() = specificHours != null || specificMinutes != null || specificSeconds != null
}

data class TrainingAvailability(
    val monday: Boolean = false,
    val tuesday: Boolean = false,
    val wednesday: Boolean = false,
    val thursday: Boolean = false,
    val friday: Boolean = false,
    val saturday: Boolean = false,
    val sunday: Boolean = false
) {
    val selectedCount: Int get() = asMap().values.count { it }

    fun asMap(): Map<String, Boolean> = linkedMapOf(
        "lunes" to monday, "martes" to tuesday, "miercoles" to wednesday,
        "jueves" to thursday, "viernes" to friday, "sabado" to saturday,
        "domingo" to sunday
    )

    fun toggle(key: String): TrainingAvailability = when (key) {
        "lunes" -> copy(monday = !monday)
        "martes" -> copy(tuesday = !tuesday)
        "miercoles" -> copy(wednesday = !wednesday)
        "jueves" -> copy(thursday = !thursday)
        "viernes" -> copy(friday = !friday)
        "sabado" -> copy(saturday = !saturday)
        "domingo" -> copy(sunday = !sunday)
        else -> this
    }

    companion object {
        fun from(values: Map<String, Boolean>) = TrainingAvailability(
            monday = values["lunes"] == true, tuesday = values["martes"] == true,
            wednesday = values["miercoles"] == true, thursday = values["jueves"] == true,
            friday = values["viernes"] == true, saturday = values["sabado"] == true,
            sunday = values["domingo"] == true
        )
    }
}

data class GoalsPlan(
    val name: String = "",
    val id: String = "",
    val planningDays: Int = 0
) {
    val isBronze: Boolean get() = name == "Bronce"
}

data class GoalsData(
    val goals: List<SportGoal> = emptyList(),
    val availability: TrainingAvailability = TrainingAvailability(),
    val sports: List<String> = emptyList(),
    val plan: GoalsPlan = GoalsPlan(),
    val trainerId: String = ""
)

sealed interface GoalsScreenState {
    data object Loading : GoalsScreenState
    data class Ready(val data: GoalsData) : GoalsScreenState
    data class Error(val message: String) : GoalsScreenState
}

data class GoalDraft(
    val name: String,
    val sport: String,
    val generalGoal: String,
    val targetDate: String,
    val description: String,
    val specificText: String = "",
    val hours: Int? = null,
    val minutes: Int? = null,
    val seconds: Int? = null
)
