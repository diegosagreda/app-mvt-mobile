package com.example.mvt.trainer.routines.model

import com.google.firebase.Timestamp

data class RoutineClosureItem(
    val id: String,
    val title: String,
    val status: String,
    val athleteName: String,
    val date: Timestamp?,
    val isDraft: Boolean
)
