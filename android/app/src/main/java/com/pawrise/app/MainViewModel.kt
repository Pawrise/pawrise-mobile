package com.pawrise.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.pawrise.app.navigation.AddDogFlow
import com.pawrise.app.navigation.Home
import com.pawrise.app.navigation.Welcome
import com.pawrise.core.auth.AuthRepository
import com.pawrise.core.auth.SessionState
import com.pawrise.core.network.ApiResult
import com.pawrise.core.pets.PetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val petRepository: PetRepository,
) : ViewModel() {

    /** `null` tant que la session persistée n'est pas relue. */
    private val _startDestination = MutableStateFlow<NavKey?>(null)
    val startDestination: StateFlow<NavKey?> = _startDestination.asStateFlow()

    val session: Flow<SessionState> = authRepository.session

    init {
        viewModelScope.launch {
            _startDestination.value = when (authRepository.restoreSession()) {
                SessionState.SignedOut -> Welcome
                SessionState.SignedIn -> destinationAfterSignIn()
            }
        }
    }

    /**
     * Après connexion : la homepage, sauf compte sans chien → ajout du chien (maquette :
     * `dogs.length === 0` ouvre le parcours). Sans réseau, on ouvre la homepage.
     */
    suspend fun destinationAfterSignIn(): NavKey = when (val pets = petRepository.listPets()) {
        is ApiResult.Success -> if (pets.value.isEmpty()) AddDogFlow else Home
        is ApiResult.Failure -> Home
    }
}
