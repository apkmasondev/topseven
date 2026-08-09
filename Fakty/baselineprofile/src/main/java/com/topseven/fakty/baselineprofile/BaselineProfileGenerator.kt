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
 * Generator profilów Baseline Profile, który symuluje typowe zachowanie użytkownika
 * po uruchomieniu aplikacji. To pozwala Ahead-Of-Time kompilatorowi Androida
 * na wstępne skompilowanie ścieżek kodu, co znacząco przyspiesza uruchamianie.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = "com.topseven.fakty",
        maxIterations = 1,
        profileBlock = {
            // Start aplikacji
            pressHome()
            startActivityAndWait()

            // Poczekaj na pełne wyrenderowanie aplikacji (brak aktywności)
            device.wait(Until.hasObject(By.pkg("com.topseven.fakty").depth(0)), 5000)

            // Wykonaj przewijanie w dół (skrolowanie), żeby obudzić widoki (LazyColumn)
            val lazyColumn = device.findObject(By.scrollable(true))
            if (lazyColumn != null) {
                lazyColumn.setGestureMargin(device.displayWidth / 5)
                lazyColumn.fling(Direction.DOWN)
            }
        }
    )
}
