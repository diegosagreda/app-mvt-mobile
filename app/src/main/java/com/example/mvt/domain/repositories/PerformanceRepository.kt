package com.example.mvt.domain.repositories

import com.example.mvt.data.firebase.models.PerformanceRecord
import com.example.mvt.data.firebase.services.PerformanceService
import com.example.mvt.data.firebase.models.PersonalRecord
import com.example.mvt.data.firebase.models.DistanceOption

class PerformanceRepository {

    private val service = PerformanceService()

    suspend fun crearRegistroRegVamAlEliminar(uid: String) {
        service.crearRegistroRegVamAlEliminar(uid)
    }


    suspend fun saveRecord(uid: String, record: PerformanceRecord) {
        service.saveRecord(uid, record)
    }
    suspend fun saveRecordWithMarcas(
        uid: String,
        record: PerformanceRecord,
        marcas: Map<String, Map<String, String>>
    ) {
        service.saveRecordWithMarcas(uid, record, marcas)
    }

    suspend fun getRecords(uid: String): List<PerformanceRecord> {
        return service.getRecords(uid)
    }

    suspend fun getPersonalRecords(uid: String): List<PersonalRecord> {
        return service.getPersonalRecords(uid)
    }

    suspend fun savePersonalRecord(uid: String, index: Int, data: Map<String, String>) {
        service.savePersonalRecord(uid, index, data)
    }

    suspend fun updateActVamTestData(uid: String, record: PerformanceRecord) {
        service.updateActVamTestData(uid, record)
    }

    suspend fun deletePersonalRecord(uid: String, index: Int) {
        service.deletePersonalRecord(uid, index)
    }

    suspend fun getDistanceOptions(): List<DistanceOption> {
        return service.getDistanceOptions()
    }

}