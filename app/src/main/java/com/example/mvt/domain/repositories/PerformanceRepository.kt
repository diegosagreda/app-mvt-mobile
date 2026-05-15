package com.example.mvt.domain.repositories

import com.example.mvt.data.firebase.models.PerformanceRecord
import com.example.mvt.data.firebase.services.PerformanceService

class PerformanceRepository {

    private val service = PerformanceService()

    suspend fun saveRecord(uid: String, record: PerformanceRecord) {
        service.saveRecord(uid, record)
    }

    suspend fun getRecords(uid: String): List<PerformanceRecord> {
        return service.getRecords(uid)
    }
}