package com.example.mvt.domain.usecases

import com.example.mvt.data.firebase.models.User
import com.example.mvt.domain.repositories.UserRepository

class GetUserInfoUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(): User? = repository.getCurrentUser()
}
