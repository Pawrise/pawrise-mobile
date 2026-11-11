package com.pawrise.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pawrise.core.auth.AuthRepository
import com.pawrise.core.auth.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {

    /** `null` tant que la session persistée n'est pas relue. */
    private val _initialSession = MutableStateFlow<SessionState?>(null)
    val initialSession: StateFlow<SessionState?> = _initialSession.asStateFlow()

    val session: Flow<SessionState> = authRepository.session

    init {
        viewModelScope.launch { _initialSession.value = authRepository.restoreSession() }
    }
}
