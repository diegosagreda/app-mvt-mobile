package com.example.mvt.domain.repositories

import com.example.mvt.data.firebase.models.Morphology
import com.example.mvt.data.firebase.services.MorphologyService

class MorphologyRepository {

    private val service = MorphologyService()

    suspend fun getMorphology(uid: String): Morphology? {
        return service.getMorphology(uid)
    }

    suspend fun saveMorphology(uid: String, morphology: Morphology) {
        service.saveMorphology(uid, morphology)
    }
}