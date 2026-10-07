package com.example.mvt.trainer.navigation

object TrainerDestination {
    const val HOME = "trainer_home"
    const val PROFILE = "trainer_profile"
    const val PERSONAL_INFO = "trainer_personal_info"
    const val SETTINGS = "trainer_settings"

    //rutas para las acciones del dashboard
    const val ATHLETES = "trainer_athletes"
    const val PENDING_REQUESTS = "trainer_pending_requests"
    const val ROUTINE_LIBRARY = "trainer_routine_library"
    const val CALENDAR = "trainer_calendar"
    const val CALENDAR_ROUTE = "$CALENDAR/{athleteId}"
}