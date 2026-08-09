# Plan aplikacji: "Fakty" – kategorie wiedzy na Androida

## 1. Opis koncepcji

Aplikacja mobilna prezentująca skondensowaną wiedzę w formie krótkich list najważniejszych faktów z różnych dziedzin (historia, biologia, geografia, fizyka itd.). Użytkownik wybiera kategorię, a następnie przegląda np. 7 kluczowych faktów w przystępnej, kartowej formie.

**Grupa docelowa:** uczniowie, studenci, osoby chcące szybko odświeżyć/poszerzyć wiedzę ogólną.

---

## 2. Struktura nawigacji

```
Ekran główny (siatka kategorii)
  └── Historia
        └── Historia Polski
              └── Lista 7 dat/faktów
                    └── Szczegóły faktu (karta)
  └── Biologia
        └── ...
  └── Geografia
        └── ...
```

### Ekrany
1. **Home** – grid kafelków głównych dziedzin (ikona + nazwa + kolor)
2. **Podkategorie** (opcjonalnie, jeśli dziedzina jest szeroka, np. Historia → Polski / Świata / Starożytna)
3. **Lista faktów** – lista 7 pozycji z numeracją (1/7 itd.)
4. **Szczegóły faktu** – opis, ewentualna grafika, ciekawostka, przyciski "wstecz/dalej"

---

## 3. Funkcjonalności

### MVP (pierwsza wersja)
- [ ] Ekran główny z kategoriami
- [ ] Lista faktów w kategorii
- [ ] Ekran szczegółów faktu
- [ ] Nawigacja swipe między faktami
- [ ] Dane statyczne (JSON w assets)

### Wersja rozszerzona
- [ ] Tryb quizu / fiszek (nauka aktywna)
- [ ] Zapisywanie ulubionych faktów
- [ ] Wyszukiwarka faktów po słowach kluczowych
- [ ] Tryb ciemny
- [ ] Udostępnianie faktu (share intent)
- [ ] Powiadomienia "fakt dnia"
- [ ] Aktualizacja treści zdalnie (Firebase Remote Config / backend), bez publikacji nowej wersji apki
- [ ] Statystyki nauki (ile faktów przejrzano, streak dni)

---

## 4. Stos technologiczny

| Warstwa | Technologia |
|---|---|
| Język | Kotlin |
| UI | Jetpack Compose |
| Nawigacja | Navigation Compose |
| Dane lokalne | JSON w `assets/` (MVP) → Room DB (rozszerzenie) |
| Zdalne dane (opcjonalnie) | Firebase Firestore / Remote Config |
| Obrazy | Coil (ładowanie i cache grafik) |
| Persystencja ulubionych | DataStore / Room |
| Architektura | MVVM (ViewModel + StateFlow) |

---

## 5. Model danych (przykład JSON)

```json
{
  "category": "historia_polski",
  "title": "Historia Polski",
  "facts": [
    {
      "id": 1,
      "title": "966 – Chrzest Polski",
      "shortDescription": "Mieszko I przyjmuje chrzest, Polska wchodzi do zachodniego kręgu kulturowego.",
      "details": "Pełny opis wydarzenia...",
      "imageUrl": "chrzest_polski.jpg"
    }
  ]
}
```

Model Kotlin (data class):
```kotlin
data class Fact(
    val id: Int,
    val title: String,
    val shortDescription: String,
    val details: String,
    val imageUrl: String? = null
)

data class Category(
    val id: String,
    val title: String,
    val icon: String,
    val facts: List<Fact>
)
```

---

## 6. Przykładowa zawartość — Historia Polski (7 kluczowych dat)

1. **966** – Chrzest Polski (Mieszko I)
2. **1025** – Koronacja Bolesława Chrobrego
3. **1385/1386** – Unia w Krewie
4. **1569** – Unia lubelska (Rzeczpospolita Obojga Narodów)
5. **1791** – Konstytucja 3 maja
6. **1795** – III rozbiór Polski
7. **1918** – Odzyskanie niepodległości

*(Do rozważenia: opcja "pokaż więcej" rozszerzająca listę do 12–15 dat dla zaawansowanych użytkowników.)*

---

## 7. Design / UX

- Prosty, czytelny styl kart (Material 3)
- Kolory przypisane do dziedzin (np. historia – brąz/złoto, biologia – zieleń, geografia – niebieski)
- Duże, czytelne typografie dla dat/tytułów
- Progres w formie "3/7" na górze ekranu szczegółów
- Płynne animacje przejść między kartami (swipe)

---

## 8. Etapy realizacji (roadmapa)

1. **Tydzień 1** – szkielet projektu, nawigacja, ekran główny z kategoriami
2. **Tydzień 2** – ekran listy faktów + szczegółów, wczytywanie danych z JSON
3. **Tydzień 3** – przygotowanie treści dla min. 3–4 dziedzin (research + redakcja tekstów)
4. **Tydzień 4** – UI polish, ciemny motyw, testy na różnych urządzeniach
5. **Tydzień 5** – funkcje rozszerzone (ulubione, quiz) — opcjonalnie
6. **Tydzień 6** – testy, poprawki, publikacja w Google Play (wersja beta)

---

## 9. Kwestie do decyzji

- Czy liczba faktów w kategorii ma być stała (zawsze 7), czy zmienna?
- Czy treść ma być w 100% statyczna, czy aktualizowana zdalnie?
- Czy aplikacja ma mieć jeden język, czy od razu wsparcie wielojęzyczne?
- Model monetyzacji: darmowa z reklamami / jednorazowy zakup / freemium z dodatkowymi kategoriami?

---

## 10. Źródła treści

- Weryfikacja faktów z rzetelnych źródeł (podręczniki, encyklopedie, publikacje naukowe)
- Unikanie treści chronionych prawem autorskim — teksty pisane własnymi słowami
- Możliwość dodania sekcji "źródła" przy każdym fakcie dla wiarygodności
