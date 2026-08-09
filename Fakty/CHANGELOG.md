# Changelog

Wszystkie znaczące zmiany w tym projekcie będą dokumentowane w tym pliku.

## [2.1.3] - Architektura Stanu i Audyt Kodu (Etap 3)

### Zmienione (Changed)
- Przeprowadzono gruntowną redakcję merytoryczną pliku `data.json` we wszystkich 21 kategoriach. Zgodnie z wytycznymi dotyczącymi edutainmentu usunięto z krótkich opisów ("shortDescription") pompatyczne przymiotniki i pytania, ujednolicono ich długość (ściśle 4-6 słów) oraz przeredagowano je tak, by ukryć główny fakt i stworzyć formę intrygującej zagadki w trybie fiszek.
- Usunięto globalny stan wyszukiwania (`searchQuery`) z `MainViewModel`, odciążając główny kontroler aplikacji.
- Przebudowano ekran Ulubionych (`FavoritesScreen`), wprowadzając lokalne zarządzanie pamięcią podręczną wyszukiwania z bezpośrednim filtrowaniem wyeksponowanej listy (`allFavoriteItems`), co eliminuje błędy związane z utrzymywaniem martwego stanu wyszukiwania między ekranami.
- Podniesiono `versionCode` do 7 oraz `versionName` do 2.1.3 przed przygotowaniami do publikacji w Google Play.
- Zabezpieczono `FavoriteDao` wprowadzając struktury `Mutex` (Mutual Exclusion), całkowicie eliminując potencjalne wyścigi zapisów (race conditions) przy szybkim dodawaniu i usuwaniu ulubionych faktów.
- Skonfigurowano jasne polityki `backup_rules` oraz `data_extraction_rules` w Manifeście aplikacji.
- Poprawiono dostępność (accessibility) w liście ulubionych, dodając pełną semantykę komponentu (`mergeDescendants`, `Role.Button`).
- Dodano adnotację `@Immutable` do modelu `FavoriteItem`, zapobiegając nadmiarowym rekompozycjom list.

### Naprawione (Fixed)
- Rozwiązano problem gubienia "kontekstu źródłowego" na ekranie detali faktu otwartym z poziomu Ulubionych. Pager (`FactDetailScreen`) obsługuje od teraz dynamiczne listy (zależne od nowo dodanego parametru `source` w Routingu), zachowując pełną zgodność z zasadą jednokierunkowego przepływu danych (UDF) bez przeciążania NavControllera.
- Załatano problem z cichymi, niepotrzebnymi alokacjami pamięci powodującymi wycieki subskrypcji dla awaryjnych przepływów (fallback `MutableStateFlow`) w silniku TTS.
- Wyeliminowano ryzyko błędu wymuszonego odpakowania (force unwrap `!!`) w oknach detali fiszek używając bezpiecznych rzutowań (safe calls).
- Oczyszczono zmartwy kod (dead code) i wyeliminowano nieużywane zmienne kontekstowe dla lepszej przejrzystości klas interfejsu.

## [2.1.2] - Optymalizacja Silnika i Architektury UI (Etap 2)

### Zmienione (Changed)
- Zrefaktoryzowano obsługę silnika TTS (`ttsManager`) przenosząc logikę do bloku `LaunchedEffect`, usuwając tym samym negatywne efekty uboczne przy rekompozycjach Compose w oknach `FactDetailScreen` oraz `FlashcardsScreen`.
- Wymieniono na sztywno zakodowany rozmiar wirtualnego wypełnienia (spacer `320.dp`) na dynamiczną, procentową wartość wysokości w widoku szczegółów faktu, wprowadzając pełną zgodność z mniejszymi ekranami i tabletami.
- Zoptymalizowano system pamięci na potrzeby modeli nawigacyjnych dodając rygorystyczne oznaczenia stabilności `@Immutable` na kluczowe warstwy danych (`Category` oraz `Fact`), zapobiegając nadmiernym rekompozycjom.
- Podniesiono wydajność ekranu z Fiszki wyciągając często używane reguły walidacyjne (`Regex`) bezpośrednio do pamięci klasy, zamiast przeliczania ich od zera za każdym razem.

## [2.1.1] - Hotfix & Optymalizacja (Etap 1)

### Zmienione (Changed)
- Zaktualizowano `README.md` o najnowsze informacje (21 kategorii, 147 faktów, lektor TTS, słowniczek).

### Naprawione (Fixed)
- Usunięto krytyczny błąd `NullPointerException` (crash) w `ttsManager`, który występował na urządzeniach pozbawionych wsparcia dla systemowego Text-To-Speech.
- Zlikwidowano problem białego przebłysku podczas uruchamiania (splash screen) modyfikując motyw systemowy z `Light` na `Dark` w konfiguracji XML.
- Skorygowano zachowanie paska nawigacji (TopAppBar) w oknie faktu poprzez dodanie bezpiecznego marginesu (`statusBarsPadding`), zapewniającego zgodność z wyświetlaniem "od krawędzi do krawędzi" (edge-to-edge).

### Usunięte (Removed)
- Repozytorium zostało dogłębnie wyczyszczone z lokalnych plików weryfikacji i tymczasowych skryptów testowych (np. `fizyka_check.txt`, `temp_cat.json`).

## [2.1.0] - Złota Edycja: Optymalizacja UI i Perfekcyjna Treść

### Dodane (Added)
- **Text-To-Speech (Lektor):** Zintegrowano natywny silnik `TextToSpeech` Androida (klasa `TtsManager`). Ikona odtwarzania pojawia się na górnym pasku detali faktu oraz z tyłu fiszki tylko jeśli telefon wspiera język polski (zgodnie z życzeniem użytkownika).
- **Haptic Feedback (Wibracje UI):** Dodano subtelne wibracje podczas nawigacji (CategoryListScreen), odwracania kart (FlashcardsScreen) i klikania w ulubione/lektor.
- Dodano nowoczesną, trójwymiarową ikonę aplikacji 3D z neonowymi akcentami (wsparcie dla wszystkich rozdzielczości mipmap i Adaptive Icons).
- **Interaktywny Słowniczek (Glossary):** Trudne pojęcia naukowe i historyczne we wszystkich 147 faktach są dynamicznie podświetlane na złoto na ekranach detali. Dotknięcie słowa otwiera elegancki panel (Glassmorphism `ModalBottomSheet`) z definicją.
- **Tryb Fiszek (Flashcards Mode):** Wdrożono trójwymiarową animację obrotu (3D flip), umożliwiającą interaktywną naukę wylosowanych faktów.
- **Ekran Ulubionych (`FavoritesScreen`):** Wyświetla w postaci kafelków zapisane ciekawostki.
- Wdrożono inteligentną, asynchroniczną wyszukiwarkę w zakładce Ulubione opartą na `StateFlow` i estetyce Glassmorphism, pozwalającą na natychmiastowe filtrowanie zapisanych ciekawostek po tytule i opisie.
- **Moduł Macrobenchmark (`:baselineprofile`):** Z wbudowanym testem UI Automator. Służy do generowania plików Baseline Profiles zapewniających szybsze włączanie aplikacji i płynniejsze przewijanie list (kompilacja AOT).
- Pełna internacjonalizacja (i18n): wydzielono wszystkie twarde ciągi znaków (hardcoded strings) z kodu UI do pliku `res/values/strings.xml` i zastosowano funkcję `stringResource`.
- Infrastruktura testowa: zaimplementowano biblioteki `MockK` i `Turbine` oraz napisano testy jednostkowe dla `MainViewModel`.
- **Nowoczesny projekt UI (Premium UI):**
  - Estetyka Glassmorphism wdrożona na wielu ekranach.
  - Tytuł główny "TOP SEVEN" otrzymał nowoczesny gradient poziomy (`Brush.linearGradient`).
  - Elementy interfejsu rysowane od krawędzi do krawędzi (Edge-to-Edge).
  - Płynne animacje przejść nawigacyjnych (Slide i Fade).
  - Fizyczne, animowane efekty odbijania (bouncing scale) dla przycisków i kart kategorii.
  - Asynchroniczne ładowanie obrazów przy wykorzystaniu biblioteki Coil ze zoptymalizowanym zanikaniem (crossfade).
- Pełna integracja narzędzia R8 ("Złoty Standard") z obsługą ProGuard, która usuwa niepotrzebny kod i obfuskuje aplikację.
- 5 nowych, edukacyjnych kategorii w bazie JSON ("Kulinaria i Jedzenie", "Film i Kino", "Psychologia", "Ekonomia i Finanse", "Historia Świata"), dociągając projekt do finałowych 21 kategorii, liczących łącznie 147 unikalnych faktów.
- 35 nowych ilustracji (WebP) dopasowanych do najnowszych kategorii.

### Zmienione (Changed)
- **Manualne QA i redakcja JSON:** Oczyszczono i zweryfikowano bazę z dawnych błędów. Ograniczono długość shortDescription dokładnie do max 6 słów, a details ściśle do max 30 słów dla wszystkich kategorii. Rozwiązało to problem z ucinanym UI we FlashcardsScreen bez ruszania linijki kodu Androida. 
- Wyeliminowano sztuczną "popelinę", powtórzenia i urwane w pół zdania dla wszystkich (21) kategorii: Język Polski, Geografia, Ekonomia i Finanse, Ciało Człowieka, Biologia, Kosmos, Kino, Kulinaria, Architektura, Inżynieria, Chemia, Muzyka, Historia Świata, Matematyka, Fizyka, Sport, Technologia, Sztuka, Zwierzęta, Psychologia, Historia Polski. Wszystkie teksty i słowniczki brzmią teraz naturalnie.
- Zmieniono nagłówek na froncie fiszek z "CZY WIESZ, ŻE..." na "O CZYM MOWA?", uwypuklając zagadkowy charakter trybu.
- Zoptymalizowano silnik list (LazyVerticalGrid, LazyColumn) dla systemu Jetpack Compose przy wykorzystaniu wbudowanych unikalnych kluczy (`key`).
- Zmigrowano wycofywany `ClickableText` do naturalnego elementu `Text` wykorzystując wprowadzony w najnowszych wersjach tag `LinkAnnotation` – polepsza to znacznie obsługę słowniczka i wykrywania kliknięć.
- Uproszczono silnik bazy danych zmieniając rozbudowaną bibliotekę Room/KSP na szybsze i stabilniejsze `SharedPreferences`.
- Usprawniono `UiState` przez usunięcie skomplikowanego smart-castingu przy nawigacji po głównych modułach.
- Wdrożono tymczasową pamięć podręczną (in-memory cache) w `FactsRepository` zapobiegającą redundatnemu parsowaniu JSON.
- Zablokowano automatyczne obracanie interfejsu (tylko `portrait`).
- Zmodyfikowano ikonę przycisku nawigacji wstecznej na automatycznie odpowiadającą bieżącemu trybowi kierunku czytania systemu (RTL-aware).
- Optymalizacja pamięciowania `TtsManager` dla zapobiegania wyciekom w Activity.
- Gradient w `HomeScreen` przeliczany bezpośrednio w fazie rysowania (`drawBehind`) w celu optymalizacji.
- Fraza wyszukiwania w ulubionych jest od teraz automatycznie czyszczona przy wejściu.
- Słowniczek (`GlossaryText`) kompiluje pamięć podręczną wyrażeń regularnych podczas budowania siatki tekstu.
- Klawisz czytania na głos korzysta z globalnych definicji z pliku `strings.xml`.
- Porządek w strukturze UI: `LazyVerticalGrid(columns=1)` zamieniono na poprawną semantycznie listę pionową `LazyColumn`.

### Naprawione (Fixed)
- Naprawiono problem z tzw. "Z-Index Popping" w animacji SharedTransition (tekst łagodnie nakłada się na zdjęcie w overlay'u).
- Zsynchronizowano animację gradientu na zdjęciu w oknie faktu, wyeliminowało to błąd nagłego ściemniania zdjęcia na sam koniec przejścia.
- Rozwiązano problem przedwczesnego ujawniania odpowiedzi na fiszkach ("spojlerów") podczas przechodzenia do następnej karty - zastosowano precyzyjne opóźnienie synchronizując zmianę danych z kątem 90-stopni obrotu 3D.
- Naprawiono crash `PatternSyntaxException` w Słowniczku przy specjalnych znakach (rozwiązano rzutowaniem `Regex.escape`).
- Załatano poważny błąd braku spójności identyfikatora granicy wyrazu w Słowniczku przy wykorzystywaniu polskich znaków (`ą`, `ć`, `ź`). Użyto nowej reguły opartej na analizie trybu Unicode `(?iu)`.
- Wyeliminowano wyciek zasobów w `MainViewModel` wywołany przez nieregulowane instancje strumieni stanu dla `isFavorite()`.
- Usunięto lukę bezpieczeństwa przy odczytywaniu zasobów asynchronicznych (assets) stosując zabezpieczenie blokowe `use {}` w `FactsRepository`.
- Usunięto problemy braku kompilacji związane z porzuconymi generatorami starych warstw w Kotlin i Android Gradle Pluginu.
- Naprawiono "sztywne" hardcodowanie tekstu błędu powiadomień.

### Usunięte (Removed)
- Przeprowadzono gigantyczne oczyszczanie (cleanup) plików projektu z dziesiątek tymczasowych, śmieciowych skryptów pythonowych wykorzystywanych w procesach QA bazy danych i plików `batch_source.json`.
- Usunięto fragmenty ślepych plików źródłowych, niepotrzebnych instrukcji oraz dawnych repozytoriów m.in. `DataRepository.kt` i starych przejść z `Color.kt`.
- Uporządkowano rejestry zewnętrznych zależności Mavena, usuwając odwołania do wycofanych pakietów Navigation 3 oraz zbędnych modułów i bibliotek bazy danych Room.
