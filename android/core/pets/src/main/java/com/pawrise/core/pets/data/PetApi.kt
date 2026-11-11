package com.pawrise.core.pets.data

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface PetApi {
    @GET("pets")
    suspend fun listPets(): PetListDto

    @POST("pets")
    suspend fun createPet(@Body body: CreatePetRequest): PetDto

    @POST("pets/{petId}/photo")
    suspend fun setSamplePhoto(@Path("petId") petId: String, @Body body: SamplePhotoRequest): PetDto

    @Multipart
    @POST("pets/{petId}/photo")
    suspend fun uploadPhoto(@Path("petId") petId: String, @Part file: MultipartBody.Part): PetDto

    @GET("breeds")
    suspend fun listBreeds(@Query("q") query: String?, @Query("limit") limit: Int = 50): BreedListDto

    @PUT("pets/{petId}/collar")
    suspend fun setCollar(@Path("petId") petId: String, @Body body: SetCollarRequest): CollarDto
}
