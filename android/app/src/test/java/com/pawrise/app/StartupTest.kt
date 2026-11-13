package com.pawrise.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.pawrise.core.auth.AuthRepository
import com.pawrise.core.auth.SessionState
import com.pawrise.core.auth.di.AuthBindings
import com.pawrise.core.pets.PetRepository
import com.pawrise.core.pets.di.PetBindings
import com.pawrise.core.testing.FakeAuthRepository
import com.pawrise.core.testing.FakePetRepository
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Relance de l'app avec une session déjà ouverte, sans chien : reprise sur l'ajout du chien. */
@HiltAndroidTest
@UninstallModules(AuthBindings::class, PetBindings::class)
@RunWith(RobolectricTestRunner::class)
class StartupWithoutDogTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @BindValue @JvmField val repository: AuthRepository = FakeAuthRepository(SessionState.SignedIn)
    @BindValue @JvmField val petRepository: PetRepository = FakePetRepository(pets = emptyList())

    @Test
    fun `reprise sur l'ajout du chien`() {
        compose.onNodeWithText("Ajoutez une photo de votre chien").assertIsDisplayed()
    }
}

/** Relance avec session ouverte et un chien : homepage directement. */
@HiltAndroidTest
@UninstallModules(AuthBindings::class, PetBindings::class)
@RunWith(RobolectricTestRunner::class)
class StartupWithDogTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @BindValue @JvmField val repository: AuthRepository = FakeAuthRepository(SessionState.SignedIn)
    @BindValue @JvmField val petRepository: PetRepository = FakePetRepository()

    @Test
    fun `ouverture sur la homepage`() {
        compose.onNodeWithText("INDICATEURS").assertIsDisplayed()
    }
}
