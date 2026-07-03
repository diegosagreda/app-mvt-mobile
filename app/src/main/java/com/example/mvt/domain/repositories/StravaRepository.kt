package com.example.mvt.domain.repositories

import com.example.mvt.data.firebase.models.StravaActivitiesResult
import com.example.mvt.data.firebase.models.StravaConnection
import com.example.mvt.data.firebase.models.StravaConnectionSnapshot
import com.example.mvt.data.firebase.models.StravaRoutineActionResult
import com.example.mvt.data.firebase.services.StravaService

class StravaRepository {

    private val service = StravaService()

    suspend fun getConnectionSnapshot(): StravaConnectionSnapshot = service.getConnectionSnapshot()

    suspend fun setSyncEnabled(enabled: Boolean) = service.setSyncEnabled(enabled)

    suspend fun disconnect() = service.disconnect()

    suspend fun exchangeToken(code: String, scope: String?): StravaConnection =
        service.exchangeToken(code, scope)

    suspend fun listActivities(
        idDeportista: String,
        fecha: String? = null,
        limit: Int = 20
    ): StravaActivitiesResult = service.listActivities(idDeportista, fecha, limit)

    suspend fun linkActivityToRoutine(
        idDeportista: String,
        routineId: String,
        activityId: String
    ): StravaRoutineActionResult =
        service.linkActivityToRoutine(idDeportista, routineId, activityId)

    suspend fun unlinkActivityFromRoutine(
        idDeportista: String,
        routineId: String
    ): StravaRoutineActionResult =
        service.unlinkActivityFromRoutine(idDeportista, routineId)

    fun buildAuthorizationUrl(state: String): String = service.buildAuthorizationUrl(state)

    fun getRedirectUri(): String = service.getRedirectUri()
}
