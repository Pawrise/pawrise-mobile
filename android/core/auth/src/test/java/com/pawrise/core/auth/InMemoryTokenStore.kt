package com.pawrise.core.auth

import com.pawrise.core.auth.session.TokenStore
import com.pawrise.core.auth.session.Tokens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class InMemoryTokenStore(initial: Tokens? = null) : TokenStore {
    private val state = MutableStateFlow(initial)
    override val tokens: StateFlow<Tokens?> = state
    override suspend fun load() = Unit
    override suspend fun save(tokens: Tokens) { state.value = tokens }
    override suspend fun clear() { state.value = null }
}
