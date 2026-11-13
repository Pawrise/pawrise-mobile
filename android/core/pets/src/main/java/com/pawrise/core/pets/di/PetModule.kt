package com.pawrise.core.pets.di

import com.pawrise.core.pets.DefaultPetRepository
import com.pawrise.core.pets.PetRepository
import com.pawrise.core.pets.data.PetApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit
import retrofit2.create

@Module
@InstallIn(SingletonComponent::class)
abstract class PetBindings {
    @Binds
    abstract fun petRepository(impl: DefaultPetRepository): PetRepository
}

@Module
@InstallIn(SingletonComponent::class)
object PetModule {
    @Provides
    @Singleton
    fun petApi(retrofit: Retrofit): PetApi = retrofit.create()
}
