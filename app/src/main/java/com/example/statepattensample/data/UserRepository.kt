package com.example.statepattensample.data

interface UserRepository {

    suspend fun getUser(): Result<User>
}
