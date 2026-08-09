package com.topseven.fakty.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generator Baseline Profile. Wynik trafia do `app/src/release/generated/baselineProfiles/`
 * i jest pakowany do APK release przez plugin `androidx.baselineprofile`, a wczytywany
 * w czasie działania przez `androidx.profileinstaller`.
 *
 * Uruchomienie: `./gradlew :app:generateReleaseBaselineProfile` przy podłączonym urządzeniu
 * lub emulatorze (API 28+).
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    /**
     * Profil startowy - klasy potrzebne do pierwszej klatki. To on najmocniej skraca
     * czas zimnego startu, a wcześniej nie powstawał w ogóle.
     */
    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = PACKAGE_NAME,
        includeInStartupProfile = true,
        profileBlock = {
            pressHome()
            startActivityAndWait()
            device.waitForIdle()
        }
    )

    /** Pełna ścieżka: lista kategorii -> lista faktów -> szczegóły -> fiszki. */
    @Test
    fun userJourney() = baselineProfileRule.collect(
        packageName = PACKAGE_NAME,
        profileBlock = {
            pressHome()
            startActivityAndWait()
            device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), TIMEOUT_MS)

            scrollCategoryList()
            openFirstCategoryAndFact()
            openFlashcards()
        }
    )

    private fun androidx.benchmark.macro.MacrobenchmarkScope.scrollCategoryList() {
        val list = device.findObject(By.scrollable(true)) ?: return
        list.setGestureMargin(device.displayWidth / 5)
        list.fling(Direction.DOWN)
        device.waitForIdle()
        list.fling(Direction.UP)
        device.waitForIdle()
    }

    private fun androidx.benchmark.macro.MacrobenchmarkScope.openFirstCategoryAndFact() {
        // Pierwsza karta kategorii na liście.
        val category = device.findObject(By.clickable(true).depth(0)) ?: return
        category.click()
        device.waitForIdle()

        // Pierwszy fakt z listy - otwiera ekran szczegółów wraz z animacją shared element.
        device.findObject(By.clickable(true))?.click()
        device.waitForIdle()

        device.pressBack()
        device.waitForIdle()
        device.pressBack()
        device.waitForIdle()
    }

    private fun androidx.benchmark.macro.MacrobenchmarkScope.openFlashcards() {
        val flashcards = device.findObject(By.desc(FLASHCARDS_DESC)) ?: return
        flashcards.click()
        device.waitForIdle()
        // Obrót fiszki 3D - najcięższa animacja w aplikacji.
        device.findObject(By.clickable(true))?.click()
        device.waitForIdle()
        device.pressBack()
        device.waitForIdle()
    }

    private companion object {
        const val PACKAGE_NAME = "com.topseven.fakty"
        const val TIMEOUT_MS = 5_000L
        const val FLASHCARDS_DESC = "Fiszki"
    }
}
