package com.example.statepattensample.di


import com.example.statepattensample.data.UserRepository
import com.example.statepattensample.data.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
internal interface Module {

    @Binds
    fun bindUserRepository(repo: UserRepositoryImpl): UserRepository
}
