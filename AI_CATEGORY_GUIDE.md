# 🚀 Instrukcja dla Agenta AI: Tworzenie nowej kategorii (Top Seven)
>
> Dokument stworzony na bazie `Android-guide_2026.md`.
> Cel: Standaryzacja pracy przyszłych agentów podczas poszerzania bazy danych aplikacji `Top Seven`.

---

## 1. STRUKTURA BAZY DANYCH (data.json)

Wszystkie dane trzymamy w pliku `Fakty/app/src/main/assets/data.json` (ścieżka względem korzenia repozytorium).
Każda nowa kategoria musi posiadać DOKŁADNIE 7 faktów. Treści muszą być rygorystycznie **zweryfikowane pod kątem historycznym, matematycznym i naukowym** przed wygenerowaniem.

### Standardy Jakości Treści (Content QA Standards)

Aby utrzymać spójny i premium charakter aplikacji, narzucono sztywne limity i zasady dla wszystkich tekstów w `data.json`:

- **`details` (Szczegółowy opis):** Długość musi wynosić ściśle **od 25 do 35 słów**. Tekst musi mieć styl "Edutainment" (edukacja przez rozrywkę) i **obowiązkowo zawierać "twardą wiedzę"** (konkretne liczby, daty, wymiary, pełne nazwiska lub precyzyjne mechanizmy). Złotym wzorcem jest tu kategoria *Architektura* (np. podawanie "82 kilometrowy cud inżynierii", "wysokość 26 metrów", "20 tysięcy robotników"). Kategoryczny ZAKAZ sztucznego upychania przymiotników ("keyword stuffing") i tworzenia "wodnych" opisów pełnych ogólników bez wartości dodanej.
- **`shortDescription` (Krótki opis):** Długość musi wynosić ściśle **od 4 do 7 słów**. Zamiast suchej definicji, opis powinien działać jak szybki, sensowny "hook" wzbudzający ciekawość użytkownika (bez ucinania tekstu w połowie zdania). ZAKAZ używania znaków zapytania i zagadek.
- **Zgodność ze Słowniczkiem (Glossary):** Każdy fakt musi zawierać co najmniej jedno rzeczywiście trudniejsze pojęcie. Każdy klucz w obiekcie `glossary` musi **idealnie** odpowiadać wyrazowi lub frazie użytej w tekście `details`, z zachowaniem tej samej formy gramatycznej. Wpis musi mieć niepusty `title` i samodzielną, zrozumiałą `definition`. Jakakolwiek modyfikacja tekstu wymusza ponowną kontrolę kluczy słowniczka.

### Wymagany format dla pojedynczej kategorii

```json
{
  "id": "nazwa_kategorii", // snake_case, bez polskich znaków
  "title": "Nazwa Kategorii",
  "icon": "nazwa_kategorii.webp",
  "facts": [
    {
      "id": 211, // UNIKALNE W CAŁEJ BAZIE - patrz zasada numeracji poniżej
      "title": "Tytuł faktu",
      "shortDescription": "Jedno-zdaniowy krótki opis (widoczny na fiszce i liście).",
      "details": "Szczegółowy opis, widoczny na ekranie detali.",
      "imageUrl": "nazwa_kategorii_1.webp",
      "glossary": {
        "Trudne słowo": {
          "title": "Trudne słowo",
          "definition": "Definicja trudnego słowa ułatwiająca zrozumienie faktu."
        }
      }
    }
  ]
}
```

### Zasada numeracji `id` faktów (WAŻNE)

Identyfikator musi być unikalny **w całej bazie**, a nie tylko w obrębie swojej kategorii.
Historycznie zasada ta została złamana - ta sama siódemka numerów `1-7` powtarzała się
w trzech kategoriach, a `91-97` w dwóch kolejnych. Aplikacja to znosiła, bo wszędzie
adresuje fakt parą `categoryId + factId`, ale identyfikator przestawał identyfikować
cokolwiek. Naprawione w wersji 2.1.4.

Konwencja: każda kategoria dostaje własną **dziesiątkę**, siedem kolejnych numerów.
Zajęte zakresy sprawdzisz jednym poleceniem:

```bash
python Fakty/analyze_facts.py
```

Aktualnie najwyższy użyty numer to **207** (Historia Świata), więc kolejna nowa kategoria
powinna zacząć się od **211**.

**Nazwy plików graficznych są niezależne od `id` faktu** - w bazie występują skróty
(`psy_1.webp`, `eco_1.webp`) oraz sufiksy wersji (`_v2`). Zmiana numeracji `id` nigdy
nie wymaga zmiany nazw obrazków i odwrotnie.

**UWAGA:**

- Staraj się dodawać sekcję `glossary` (Słowniczek) dla trudniejszych pojęć. Aplikacja automatycznie podświetli te słowa w UI jako interaktywne. Słowo w kluczu `glossary` musi dokładnie pokrywać się z wyrazem obecnym w polu **`details`** (uwaga: aplikacja NIE podświetla słówek obecnych wyłącznie w `shortDescription`!).
- Brakujące `glossary` = utrata ważnej funkcji edukacyjnej. Każdy fakt powinien wyjaśniać co najmniej jedno pojęcie występujące w jego polu `details`; nie dodawaj jednak sztucznie trudnych słów tylko po to, aby wypełnić słowniczek.

---

## 2. STANDARDY GRAFICZNE (Obrazy)

To najważniejsza techniczna blokada! Nie marnuj limitów generowania obrazów (Quota Exhausted). Upewnij się, że prompty wykluczają halucynacje. Wymuszone parametry graficzne:

1. **Ikona kategorii główna (1 sztuka)**
   - Wymiary rygorystyczne: **512x512 px**
   - Nazwa: `<id_kategorii>.webp`
   - Zapisywane w: `assets/images/` (bezpośrednio w folderze images)
   - **UWAGA:** Ta grafika jest wykorzystywana jako potężne, animowane tło w nagłówku (LargeTopAppBar) na ekranie kategorii. Zadbaj o to, by była oszałamiającej jakości i nadawała aplikacji charakter "Premium" (efekt WOW).

2. **Obrazy do poszczególnych faktów (7 sztuk)**
   - Wymiary rygorystyczne: **512x512 px**
   - Nazwa: `<id_kategorii>_<numer_faktu>.webp` (bez żadnych innych prefiksów np. "images/facts/...")
   - Zapisywane w: `assets/images/facts/`

**Format ZAPISU:** WYŁĄCZNIE `.webp` (wymagane w celu optymalizacji wielkości paczki APK).
Prompty graficzne muszą ZAWSZE posiadać tag: `no text`, by AI nie tworzyło "śmieciowych" liter na zdjęciach.

---

## 3. CHECKLISTA WDROŻENIOWA (Nie dotykaj kodu, zanim tego nie sprawdzisz)

1. **Weryfikacja numeracji IDs:** Nowe `id` nie mogą kolidować z żadnym istniejącym w bazie.
2. **Generowanie obrazów:** Wygeneruj, skaluj (`LANCZOS`), skompresuj (`WEBP`), zapisz.
3. **Plik JSON:** Dodaj obiekt do `data.json`. Ścieżka w `imageUrl` to SAMA nazwa pliku (np. `matematyka_41.webp`), ponieważ aplikacja sama dokleja prefiks `file:///android_asset/images/facts/`.
4. **Weryfikacja słownika:** Upewnij się, że klucze słownika mają sens i nie krzaczą się w wyrażeniach regularnych (bez zbędnych znaków specjalnych).
5. **URUCHOM WALIDATOR** - to jedyny krok, którego nie wolno pominąć:

   ```bash
   python Fakty/analyze_facts.py
   ```

   Automatycznie sprawdza: globalną i lokalną unikalność `id`, dokładnie 7 faktów
   w kategorii, obecność wszystkich plików graficznych, limity długości
   `shortDescription` (4-7 słów) i `details` (25-35 słów), obecność słowniczka oraz to,
   czy każdy jego klucz faktycznie występuje w tekście `details`. Kod wyjścia 0 = w porządku.

6. **Zbuduj i sprawdź na urządzeniu:** `cd Fakty && ./gradlew :app:assembleDebug`, a następnie
   otwórz nową kategorię i jeden jej fakt. Testy jednostkowe warstwy danych:
   `./gradlew :app:testDebugUnitTest`.

---

## 4. CO DODAĆ DLA AGENTA AI W PRZYSZŁOŚCI? (Roadmapa)

Gdy nadejdą nowe funkcje, pamiętaj uaktualnić tę instrukcję o:

- **Tłumaczenia (i18n):** Jeśli wprowadzimy j. angielski, pamiętaj, że klucz główny (np. polskie słowa na fiszkach) ma pozostać zgodny z UI, by zachować zamierzony efekt zagadek.
- **Audio TTS (Lektor):** Jeśli generujemy pre-kompilowane pliki mp3 pod teksty zamiast natywnego silnika urządzenia.
- **Parametry gamifikacji:** Każda kategoria może kiedyś potrzebować pola `"xp_reward"`, jeśli wdrożymy system punktacji za przeczytane ciekawostki. Zwracaj na to uwagę.

## 5. ZESTAWIENIE KATEGORII (21/21 UKOŃCZONE)

Aktualnie aplikacja posiada pełen zaplanowany zasób 21 unikalnych kategorii edukacyjnych:

1. Historia Polski
2. Biologia
3. Geografia
4. Kosmos
5. Matematyka
6. Język Polski
7. Fizyka
8. Sport
9. Technologia
10. Ciało Człowieka (Anatomia)
11. Sztuka
12. Zwierzęta
13. Psychologia
14. Ekonomia i Finanse
15. Historia Świata
16. Architektura
17. Inżynieria
18. Chemia
19. Muzyka
20. Kino i film
21. Kulinaria i jedzenie

**Baza Danych (data.json) została ostatecznie sfinalizowana, wszystkie opisy mieszczą się w wyznaczonych limitach, a słowniczki (glossary) są bezwzględnie zintegrowane z tekstem. Cel osiągnięty!**
