package com.topseven.fakty.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Zatrzymuje lektora, gdy ekran znika z kompozycji (nawigacja wstecz / dalej) albo gdy
 * aplikacja trafia w tło.
 *
 * Bez tego czytanie leciało dalej po opuszczeniu ekranu i po zminimalizowaniu aplikacji.
 * Świadomie nie wołamy tu `shutdown()` - silnik należy do ViewModelu i ma przeżyć
 * zmiany konfiguracji; zwalniany jest dopiero w `onCleared()`.
 */
@Composable
fun StopTtsWhenScreenLeaves(ttsManager: TtsManager?) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(ttsManager, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                ttsManager?.stop()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            ttsManager?.stop()
        }
    }
}
