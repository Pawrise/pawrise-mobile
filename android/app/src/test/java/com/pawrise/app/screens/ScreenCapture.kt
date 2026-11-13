package com.pawrise.app.screens

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onRoot
import java.io.File

/**
 * Captures écran par écran, nommées comme les cadres du Figma « Pawrise-App » (A01 = US-A écran 01…).
 * Écran 390 × 844 dp en ×3 (mêmes dimensions que la maquette et que les captures iOS).
 * Dossier de sortie : propriété système `pawrise.captureDir` (sinon build/captures).
 * Lancer : ./gradlew :app:testDebugUnitTest --tests '*ScreenCaptureTest*' -Ppawrise.captureDir=/chemin
 */
const val CAPTURE_QUALIFIERS = "w390dp-h844dp-port-xxhdpi"

fun AndroidComposeTestRule<*, *>.snapshot(label: String) {
    waitForIdle()
    mainClock.advanceTimeBy(1_000)
    waitForIdle()
    val dir = File(System.getProperty("pawrise.captureDir") ?: "build/captures").apply { mkdirs() }
    val bitmap = onRoot().captureToImage().asAndroidBitmap()
    File(dir, "$label.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
}
