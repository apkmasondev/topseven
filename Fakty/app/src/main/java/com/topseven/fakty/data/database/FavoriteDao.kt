package com.topseven.fakty.data.database

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Trwały zapis ulubionych faktów w [SharedPreferences] pod jednym kluczem typu `Set<String>`
 * w formacie `categoryId:factId`.
 *
 * Odczyt i zapis dysku odbywają się na [Dispatchers.IO], a każda modyfikacja przechodzi przez
 * [mutex], więc szybkie przełączanie serduszka nie gubi zmian (read-modify-write jest atomowy).
 */
class FavoriteDao(context: Context) {

    private val appContext = context.applicationContext

    // `lazy` + IO: pierwsze sięgnięcie do SharedPreferences czyta cały plik z dysku,
    // więc nie może się wydarzyć w konstruktorze na wątku głównym.
    private val prefs: SharedPreferences by lazy {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** `null` oznacza "jeszcze nie wczytano z dysku". */
    private val _favoritesFlow = MutableStateFlow<Set<String>?>(null)
    private val mutex = Mutex()

    fun getAllFavorites(): Flow<List<FavoriteFact>> {
        return _favoritesFlow
            .onStart { ensureLoaded() }
            .filterNotNull()
            // Sortowanie jest istotne: kolejność iteracji zbioru jest nieokreślona, więc bez
            // niego lista ulubionych przestawiała się po każdym dodaniu/usunięciu wpisu.
            .map { keys ->
                keys.mapNotNull { it.toFavoriteFactOrNull() }
                    .sortedWith(compareBy({ it.categoryId }, { it.factId }))
            }
    }

    suspend fun insertFavorite(favoriteFact: FavoriteFact) {
        mutate { it + favoriteFact.toKey() }
    }

    suspend fun deleteFavorite(favoriteFact: FavoriteFact) {
        mutate { it - favoriteFact.toKey() }
    }

    private suspend fun ensureLoaded() {
        if (_favoritesFlow.value != null) return
        mutex.withLock {
            if (_favoritesFlow.value != null) return
            _favoritesFlow.value = withContext(Dispatchers.IO) { readFromDisk() }
        }
    }

    private suspend fun mutate(transform: (Set<String>) -> Set<String>) {
        mutex.withLock {
            // Nie wołamy ensureLoaded() - Mutex nie jest reentrantny.
            val current = _favoritesFlow.value ?: withContext(Dispatchers.IO) { readFromDisk() }
            val updated = transform(current)
            if (updated == current) {
                _favoritesFlow.value = current
                return
            }
            withContext(Dispatchers.IO) { writeToDisk(updated) }
            _favoritesFlow.value = updated
        }
    }

    private fun readFromDisk(): Set<String> =
        // Kopiujemy wynik: SharedPreferences zwraca referencję do własnej, współdzielonej
        // instancji zbioru, której zgodnie z dokumentacją nie wolno modyfikować.
        prefs.getStringSet(KEY_FAVORITES, null)?.toSet() ?: emptySet()

    private fun writeToDisk(favorites: Set<String>) {
        // commit() zamiast apply(): jesteśmy już na IO, a synchroniczny zapis daje pewność,
        // że kolejne odczyty (także po ubiciu procesu) widzą aktualny stan.
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).commit()
    }

    private fun FavoriteFact.toKey(): String = "$categoryId$SEPARATOR$factId"

    private fun String.toFavoriteFactOrNull(): FavoriteFact? {
        val separatorIndex = lastIndexOf(SEPARATOR)
        if (separatorIndex <= 0 || separatorIndex == length - 1) return null
        val factId = substring(separatorIndex + 1).toIntOrNull() ?: return null
        return FavoriteFact(substring(0, separatorIndex), factId)
    }

    private companion object {
        const val PREFS_NAME = "favorites_prefs"
        const val KEY_FAVORITES = "favorite_keys"
        const val SEPARATOR = ':'
    }
}
