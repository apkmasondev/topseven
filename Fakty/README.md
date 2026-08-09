# Fakty

Aplikacja mobilna prezentująca skondensowaną wiedzę w formie krótkich list najważniejszych faktów z różnych dziedzin (historia, biologia, geografia, fizyka itd.).

## Jak uruchomić

1. Otwórz projekt w Android Studio lub skompiluj za pomocą Gradle:
   `./gradlew assembleDebug`
2. Uruchom aplikację na emulatorze lub fizycznym urządzeniu z systemem Android.

## Jak zainstalować APK

Możesz zainstalować APK na telefonie za pomocą polecenia `adb`:
`adb install app/build/outputs/apk/debug/app-debug.apk`

## Główne funkcje

- Przeglądanie 21 kategorii wiedzy (w tym Kulinaria, Kino, Psychologia, Ekonomia).
- Łącznie 147 unikalnych i fascynujących faktów.
- Interaktywny Słowniczek wyjaśniający trudne pojęcia w oparciu o Glassmorphism UI.
- Zintegrowany lektor (Text-To-Speech) w języku polskim, czytający wybrane ciekawostki na głos.
- System "Ulubionych", pozwalający na łatwe przypinanie najlepszych faktów (w oparciu o szybkie lokalne zapisy).
- Inteligentna i natychmiastowa wyszukiwarka ulubionych faktów.
- Karty z najważniejszymi faktami z danej dziedziny.
- Wbudowany "Tryb Fiszek" (aktywna nauka z losowanymi ciekawostkami i wciągającą animacją obrotu 3D).
- Wysokiej jakości Premium UI (motyw ciemny "Glassmorphism", asynchroniczne ładowanie obrazów, Haptic Feedback).
- Płynna nawigacja w Jetpack Compose z animacjami Shared Transition.
- Optymalizacja wydajności z użyciem **Baseline Profiles** i zasad minifikacji (R8), zgodnie ze Złotym Standardem (gwarancja szybkiego startu aplikacji).

## Ograniczenia i status (w toku)

- Aplikacja obecnie używa lokalnego pliku JSON (`assets/data.json`) do ładowania danych. W przyszłości może zostać dodane pobieranie zdalne (np. z serwera).
