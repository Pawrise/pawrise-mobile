package com.pawrise.core.auth.session

import kotlinx.serialization.Serializable

@Serializable
data class Tokens(val accessToken: String, val refreshToken: String)
