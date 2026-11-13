package com.pawrise.feature.chat

import org.junit.Assert.assertEquals
import org.junit.Test

/** Message d'accueil du Chat IA : prénom du propriétaire quand il est connu. */
class ChatGreetingTest {
    @Test
    fun `salue le proprietaire par son prenom`() {
        assertEquals(
            "Bonjour Alex 👋 Je suis Pawrise, l'assistant de Nala. Que souhaitez-vous consulter ?",
            chatGreeting("Alex", "Nala"),
        )
        assertEquals(
            "Bonjour Alex 👋 Je suis Pawrise, l'assistant de Nala. Que souhaitez-vous consulter ?",
            chatGreeting("  Alex ", "Nala"),
        )
    }

    @Test
    fun `sans prenom, garde la phrase de la maquette`() {
        val expected = "Bonjour 👋 Je suis Pawrise, l'assistant de Nala. Que souhaitez-vous consulter ?"
        assertEquals(expected, chatGreeting(null, "Nala"))
        assertEquals(expected, chatGreeting("  ", "Nala"))
    }

    @Test
    fun `l'emoji est un vrai caractere unicode (U+1F44B), pas un caractere de remplacement`() {
        val greeting = chatGreeting("Alex", "Nala")
        assertEquals(0x1F44B, greeting.codePointAt(greeting.indexOf("Alex") + 5))
    }
}
