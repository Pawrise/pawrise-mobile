package com.pawrise.core.pets

import com.pawrise.core.network.PawriseJson
import com.pawrise.core.pets.data.PetApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

/** Pile réseau réelle avec un Bearer fixe (les routes Animaux exigent une session). */
fun petRepository(baseUrl: String): DefaultPetRepository {
    val client = OkHttpClient.Builder()
        .addInterceptor { chain -> chain.proceed(chain.request().newBuilder().header("Authorization", "Bearer test").build()) }
        .build()
    val api = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(PawriseJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create<PetApi>()
    return DefaultPetRepository(api, PawriseJson)
}
