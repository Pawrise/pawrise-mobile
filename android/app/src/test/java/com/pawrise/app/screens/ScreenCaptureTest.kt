package com.pawrise.app.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.pawrise.app.MainActivity
import com.pawrise.app.field
import com.pawrise.app.tap
import com.pawrise.core.auth.AuthRepository
import com.pawrise.core.auth.User
import com.pawrise.core.auth.di.AuthBindings
import com.pawrise.core.pets.Breed
import com.pawrise.core.pets.PetRepository
import com.pawrise.core.pets.di.PetBindings
import com.pawrise.core.testing.FakeAuthRepository
import com.pawrise.core.testing.FakePetRepository
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Captures des écrans de l'app réelle (Hilt + faux dépôts), nommées comme la maquette. */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
@UninstallModules(AuthBindings::class, PetBindings::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = CAPTURE_QUALIFIERS)
class ScreenCaptureTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @BindValue @JvmField val repository: AuthRepository = FakeAuthRepository()
    @BindValue @JvmField val petRepository: PetRepository = FakePetRepository(pets = emptyList())

    private val fakeAuth get() = repository as FakeAuthRepository

    @Test
    fun auth() {
        compose.snapshot("A01")
        compose.tap("Se connecter")
        compose.field("E-mail").performTextInput("alex@example.com")
        compose.field("Mot de passe").performTextInput("chien-26")
        compose.snapshot("J02")
    }

    @Test
    fun signup() {
        compose.tap("Créer un compte")
        compose.field("Prénom").performTextInput("Alex")
        compose.field("Nom").performTextInput("Martin")
        compose.field("E-mail").performTextInput("alex@example.com")
        compose.field("Mot de passe").performTextInput("chien-26")
        compose.snapshot("A02")
    }

    /** I02 Paramètres (haut), I02L (bas de page) : compte Alex Martin, Nala avec son collier. */
    @Test
    fun settings() {
        fakeAuth.account = User("u1", "Alex", "Martin", "alex@example.com", null, emailVerified = true)
        (petRepository as FakePetRepository).pets = listOf(
            FakePetRepository.NALA.copy(
                breeds = listOf(Breed("golden_retriever", "Golden Retriever")),
                birthDate = LocalDate.now().minusYears(3).minusDays(10),
                collarSerial = "PW-ABC123",
            ),
        )
        compose.tap("Se connecter")
        compose.field("E-mail").performTextInput("alex@example.com")
        compose.field("Mot de passe").performTextInput("chien-26")
        compose.tap("Se connecter")
        compose.tap("Paramètres")
        compose.snapshot("I02")
        compose.onNodeWithTag("settings-version").performScrollTo()
        compose.snapshot("I02L")
    }
}
