package com.pawrise.core.auth.di

import com.pawrise.core.auth.AuthRepository
import com.pawrise.core.auth.DefaultAuthRepository
import com.pawrise.core.auth.data.AuthApi
import com.pawrise.core.auth.data.RefreshApi
import com.pawrise.core.auth.session.BearerInterceptor
import com.pawrise.core.auth.session.EncryptedTokenStore
import com.pawrise.core.auth.session.TokenAuthenticator
import com.pawrise.core.auth.session.TokenStore
import com.pawrise.core.network.AppInterceptors
import com.pawrise.core.network.AuthService
import com.pawrise.core.network.BareAuthService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton
import okhttp3.Authenticator
import okhttp3.Interceptor
import retrofit2.Retrofit
import retrofit2.create

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthBindings {
    @Binds
    abstract fun authRepository(impl: DefaultAuthRepository): AuthRepository

    @Binds
    abstract fun tokenStore(impl: EncryptedTokenStore): TokenStore

    @Binds
    @IntoSet
    @AppInterceptors
    abstract fun bearerInterceptor(impl: BearerInterceptor): Interceptor

    @Binds
    abstract fun authenticator(impl: TokenAuthenticator): Authenticator
}

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides
    @Singleton
    fun authApi(@AuthService retrofit: Retrofit): AuthApi = retrofit.create()

    @Provides
    @Singleton
    fun refreshApi(@BareAuthService retrofit: Retrofit): RefreshApi = retrofit.create()
}
