package com.example.mvt.plans.data

import com.example.mvt.plans.model.AthletePlan
import com.example.mvt.plans.model.SubscriptionPlan
import com.example.mvt.plans.model.athletePlanFromFirebase
import com.example.mvt.plans.model.subscriptionPlansFromFirebase
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class PlansRepository(database: FirebaseDatabase = FirebaseDatabase.getInstance()) {
    private val root = database.reference

    suspend fun loadCurrentPlan(athleteId: String): AthletePlan? {
        require(athleteId.isNotBlank()) { "No hay una sesión activa." }
        return root.child(USERS).child(athleteId).child(PLAN).get().await().value
            .let(::athletePlanFromFirebase)
    }

    suspend fun loadCatalog(): List<SubscriptionPlan> {
        return root.child(PLANS).get().await().value
            .let(::subscriptionPlansFromFirebase)
    }

    private companion object {
        const val USERS = "users"
        const val PLAN = "plan"
        const val PLANS = "Planes"
    }
}
