package com.pawrise.core.network

import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import java.util.Optional
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Client sans authentification : sert au refresh des jetons (sinon boucle sur l'Authenticator). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BareClient

/** Intercepteurs ajoutés au client principal par d'autres modules (ex. `core:auth` → en-tête Bearer). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppInterceptors

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindings {
    @Multibinds
    @AppInterceptors
    abstract fun appInterceptors(): Set<Interceptor>

    /** Fourni par `core:auth` (refresh sur 401). Absent → pas de refresh. */
    @BindsOptionalOf
    abstract fun authenticator(): Authenticator
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = PawriseJson

    @Provides
    @Singleton
    @BareClient
    fun provideBareOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
                redactHeader("Authorization")
            },
        )
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @BareClient bare: OkHttpClient,
        @AppInterceptors interceptors: Set<@JvmSuppressWildcards Interceptor>,
        authenticator: Optional<Authenticator>,
    ): OkHttpClient = bare.newBuilder()
        // Dispatcher distinct du client nu : sinon les requêtes en 401 occupent toutes les places
        // (5 par hôte) en attendant le refresh, qui ne peut plus partir → interblocage.
        .dispatcher(Dispatcher())
        .apply {
            // Les intercepteurs applicatifs passent avant le logging hérité de `bare`.
            interceptors().addAll(0, interceptors)
            authenticator.ifPresent { authenticator(it) }
        }
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = retrofit(client, json)

    @Provides
    @Singleton
    @BareClient
    fun provideBareRetrofit(@BareClient client: OkHttpClient, json: Json): Retrofit = retrofit(client, json)

    private fun retrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}

/** Configuration JSON commune (aussi utilisée telle quelle dans les tests). */
val PawriseJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}
