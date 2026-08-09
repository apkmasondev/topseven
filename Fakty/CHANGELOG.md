# Changelog

Wszystkie znaczące zmiany w tym projekcie będą dokumentowane w tym pliku.

## [2.1.4] - 2026-08-09 - Audyt kodu: stabilność, wydajność, dostępność

Pełny audyt kodu źródłowego (błędy logiczne, wycieki zasobów, wydajność Compose,
konfiguracja builda, dostępność). Bez zmian architektury i bez usuwania funkcji.

### Naprawione (Fixed)

**Lektor (TTS)**
- Dodano brakującą deklarację `<queries>` z akcją `TTS_SERVICE` w manifeście. Od API 30
  obowiązuje ograniczenie widoczności pakietów, więc bez niej `TextToSpeech` nie widział
  zainstalowanych silników mowy - lektor po cichu nie działał na części urządzeń.
- Naprawiono niepojawiający się przycisk lektora przy pierwszym wejściu na ekran faktu
  i fiszek. `ttsManager` był zwykłym polem tworzonym w `LaunchedEffect` (już po pierwszej
  kompozycji), więc UI nigdy nie dostawało powiadomienia o jego powstaniu. Silnik jest
  teraz wystawiony jako `StateFlow`.
- Lektor jest zatrzymywany przy opuszczeniu ekranu i przy przejściu aplikacji w tło
  (`StopTtsWhenScreenLeaves`). Wcześniej czytał dalej po zminimalizowaniu aplikacji.
- Szybkie dwukrotne kliknięcie przycisku odtwarzania uruchamiało dwie wypowiedzi zamiast
  działać jak przełącznik play/stop - stan jest teraz ustawiany optymistycznie.
- Dodano obsługę `onStop` i `onError(utteranceId, errorCode)` w `UtteranceProgressListener`
  oraz odpięcie listenera w `shutdown()`. Metoda `shutdown()` jest odporna na wielokrotne
  wywołanie i na wyjątki nakładek producenckich.
- `Locale("pl","PL")` zastąpiono `Locale.forLanguageTag("pl-PL")` (wycofane API).

**Dane i ulubione**
- `FactsRepository` przestał połykać `CancellationException`, co tłumiło anulowanie korutyn
  i mogło zapisać w cache pustą listę.
- Dodano walidację `data.json`: pusty plik, uszkodzony JSON i nieoczekiwany kształt danych
  nie wywracają aplikacji, a niekompletne kategorie i fakty są pomijane zamiast trafiać do UI.
- Nieudany odczyt nie jest już cache'owany - kolejne wejście na ekran ponawia próbę.
- Dostęp do cache objęto `Mutex` (dotąd był to niezsynchronizowany zapis z wielu korutyn).
- `FavoriteDao` czytał `SharedPreferences` w konstruktorze na wątku głównym (I/O dysku,
  ryzyko ANR). Odczyt i zapis przeniesiono na `Dispatchers.IO`, zapis używa `commit()`.
- Kolejność listy ulubionych jest deterministyczna. Wcześniej wynikała z kolejności iteracji
  zbioru i przestawiała się po każdej zmianie.
- Uszkodzone klucze w zapisanych ulubionych są pomijane zamiast psuć całą listę.

**Nawigacja i crashe**
- Ekran szczegółów faktu otwierany z Ulubionych czytał `allFavoriteItems.value` zamiast
  subskrybować strumień. Przy `SharingStarted.WhileSubscribed` zwracało to pustą listę
  i pokazywało "Nie znaleziono faktu".
- Zabezpieczono `items[pagerState.currentPage]` przed `IndexOutOfBoundsException`, gdy lista
  skróci się w trakcie oglądania (usunięcie ulubionego).
- Wejście na ekran kategorii lub faktu w trakcie wczytywania danych pokazywało pusty ekran
  (`return@composable`) - teraz wyświetla wskaźnik ładowania.

**Fiszki**
- Szybkie tapanie "Następna fiszka" przy odwróconej karcie uruchamiało wiele korutyn
  z opóźnieniem i licznik przeskakiwał o kilka kart naraz.
- Długie treści faktów były ucinane na tyle karty na mniejszych ekranach - dodano
  przewijanie z zachowaniem dotychczasowego wyrównania do dołu.

### Wydajność (Performance)
- `GlossaryText` budował adnotowany tekst (pełny skan wyrażeniami regularnymi po całej
  treści) przy **każdej** rekompozycji. Na ekranie fiszek oznaczało to pełny przebieg
  regeksów w każdej klatce animacji obrotu 3D. Wynik jest teraz pamiętany dla pary
  `text`/`glossary`.
- `Flashcard` odczytywał wartość animacji obrotu wprost w ciele kompozycji, przez co cała
  karta rekomponowała się ~36 razy na obrót. Zastąpiono to `derivedStateOf`, dzięki czemu
  animacja zostaje w fazie rysowania.
- Granice wyrazów w słowniczku oparto na klasach Unicode zamiast ASCII-owego `\p{Punct}`,
  co poprawia wykrywanie pojęć otoczonych polskimi znakami interpunkcyjnymi.

### Konfiguracja builda (Build)
- **Baseline Profiles faktycznie działają.** Moduł `:baselineprofile` generował profil,
  którego nikt nie konsumował: brakowało pluginu `androidx.baselineprofile` i zależności
  `baselineProfile(project(":baselineprofile"))`, więc `profileinstaller` nie miał czego
  wczytać, a w APK nie było żadnego profilu. Po aktualizacji `androidx.benchmark` z 1.3.3
  do 1.5.0-beta01 (1.3.3 i 1.4.1 nie obsługują AGP 9.x) profil jest generowany i pakowany
  do APK jako `assets/dexopt/baseline.prof`. Generator pokrywa teraz realną ścieżkę
  użytkownika i tworzy dodatkowo **profil startowy**, którego wcześniej nie było.
- Dodano `signingConfig` dla wariantu release, czytany z `keystore.properties` spoza
  repozytorium. Brak pliku nie psuje builda - powstaje wtedy APK niepodpisany.
- Dodano brakujący `testInstrumentationRunner`. Testy z `androidTest/` istniały, ale nie
  dało się ich uruchomić.
- Zawężono reguły ProGuard. `-keep class kotlinx.serialization.** { *; }` oraz
  `-keepclassmembers class * { *** Companion; }` blokowały obfuskację wszystkich obiektów
  towarzyszących w aplikacji i jej zależnościach.
- Rozszerzono `.gitignore` o klucze podpisujące, artefakty builda i katalogi IDE.

### Dostępność (Accessibility)
- Fiszka używała `contentDescription` na węźle scalającym potomków, co **zastępowało**
  ich teksty - czytnik ekranu w ogóle nie odczytywał treści fiszki. Zmieniono na
  `stateDescription`.
- Opisy przycisków lektora i ulubionych odzwierciedlają stan ("Czytaj fakt na głos" /
  "Zatrzymaj czytanie", "Dodaj do ulubionych" / "Usuń z ulubionych").
- Licznik "1 / 7" ma opis "Fakt 1 z 7".
- Obrazy tła oznaczono jako dekoracyjne (`contentDescription = null`), żeby czytnik ekranu
  nie powtarzał nazwy kategorii i tytułu faktu dwa razy.

### Interfejs (UI)
- Podniesiono krycie dolnej krawędzi szklanej karty na ekranie faktu (0.5 -> 0.82).
  Przy jasnych zdjęciach biały tekst tracił kontrast.
- Treść ekranu faktu nie chowa się już pod paskiem nawigacji systemowej.

### Testy
- Liczba testów jednostkowych wzrosła z 4 do 22. Dodano `FactsRepositoryTest` (walidacja
  i odporność parsowania `data.json`) oraz `FavoriteDaoTest` (atomowość zapisu, równoległe
  modyfikacje, uszkodzone dane, determinizm kolejności).

### Usunięte (Removed)
- Martwy kod: nieużywana metoda `FavoriteDao.isFavorite()`, nieodczytywane parametry
  `FaktyTheme(darkTheme, dynamicColor)`, nieużywane importy i zmienne, zbędny `Row`
  wokół przycisku na ekranie fiszek.
- Zahardkodowane ciągi `"favorites"` i separator `" • "` przeniesiono odpowiednio do stałej
  `FactDetailSource` i do `strings.xml`.

### Dane (Data)
- Usunięto zduplikowane identyfikatory faktów między kategoriami. Ta sama siódemka numerów
  powtarzała się w trzech kategoriach (1-7 w `historia_polski`, `psychologia`, `ekonomia`)
  oraz w dwóch kolejnych (91-97 w `anatomia`, `historia_swiata`). Przenumerowano
  `psychologia` na 181-187, `ekonomia` na 191-197 i `historia_swiata` na 201-207;
  `historia_polski` i `anatomia` zachowały pierwotne numery. Nazwy plików graficznych są
  niezależne od identyfikatorów, więc nie wymagały zmian.
- Klucze `sharedElement` zawierają teraz identyfikator kategorii
  (`fact-image-<categoryId>-<factId>`), więc pozostają jednoznaczne niezależnie od danych.
- `analyze_facts.py` sprawdzał unikalność identyfikatorów wyłącznie w obrębie kategorii -
  dlatego kolizje globalne przechodziły niezauważone. Dodano kontrolę globalną.

### Znane ograniczenia
- APK 2.1.4 jest podpisany **nowym** certyfikatem wydawniczym (poprzedni plik ze strony był
  buildem debug). Instalacja na urządzeniu ze starszą wersją wymaga jej odinstalowania.
- Przenumerowanie identyfikatorów unieważnia ulubione zapisane w wersjach do 2.1.3 włącznie
  dla kategorii Psychologia, Ekonomia i Historia Świata. Aplikacja nie była jeszcze
  dystrybuowana, więc realnie nie dotyczy to nikogo; osierocone wpisy i tak są pomijane
  przy wczytywaniu listy.

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
