package com.topseven.fakty.data.repository

import android.content.Context
import android.util.Log
import com.topseven.fakty.data.models.Category
import com.topseven.fakty.data.models.Fact
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

import com.topseven.fakty.data.database.FavoriteDao
import com.topseven.fakty.data.database.FavoriteFact
import kotlinx.coroutines.flow.Flow

class FactsRepository(
    private val context: Context,
    private val favoriteDao: FavoriteDao
) {

    private val json = Json { ignoreUnknownKeys = true }

    // Favorites
    suspend fun addFavorite(categoryId: String, factId: Int) {
        favoriteDao.insertFavorite(FavoriteFact(categoryId, factId))
    }

    suspend fun removeFavorite(categoryId: String, factId: Int) {
        favoriteDao.deleteFavorite(FavoriteFact(categoryId, factId))
    }

    fun getAllFavorites(): Flow<List<FavoriteFact>> {
        return favoriteDao.getAllFavorites()
    }

    private var cachedCategories: List<Category>? = null
    private val cacheMutex = Mutex()

    /**
     * Wczytuje i waliduje `assets/data.json`.
     *
     * Zwraca pustą listę, gdy plik nie istnieje, jest pusty, uszkodzony albo nie zawiera
     * ani jednej poprawnej kategorii - warstwa UI mapuje to na komunikat błędu.
     * Rekordy niekompletne są pomijane, żeby jeden zepsuty wpis nie wywracał całej aplikacji.
     */
    suspend fun getCategories(): List<Category> = withContext(Dispatchers.IO) {
        cachedCategories?.let { return@withContext it }

        cacheMutex.withLock {
            // Ponowne sprawdzenie: inna korutyna mogła wypełnić cache, czekając na blokadę.
            cachedCategories?.let { return@withLock it }

            val parsed = try {
                val jsonString = context.assets.open(DATA_FILE).use { stream ->
                    stream.bufferedReader().readText()
                }

                if (jsonString.isBlank()) {
                    Log.e(TAG, "$DATA_FILE jest pusty.")
                    emptyList()
                } else {
                    json.decodeFromString<List<Category>>(jsonString)
                }
            } catch (e: CancellationException) {
                // Anulowanie korutyny nie jest błędem wczytywania - musi polecieć dalej,
                // inaczej cache zapisałby pustą listę i ekran zostałby w stanie błędu.
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Nie udało się wczytać $DATA_FILE", e)
                emptyList()
            }

            val valid = parsed.mapNotNull { it.sanitizedOrNull() }
            if (valid.size != parsed.size) {
                Log.w(TAG, "Pominięto ${parsed.size - valid.size} niepoprawnych kategorii w $DATA_FILE")
            }

            // Pustego wyniku nie cache'ujemy - przy błędzie odczytu kolejne wejście
            // na ekran ma prawo spróbować ponownie.
            if (valid.isNotEmpty()) {
                cachedCategories = valid
            }
            valid
        }
    }

    /**
     * Odrzuca kategorie bez identyfikatora, tytułu lub bez ani jednego poprawnego faktu
     * i usuwa z nich uszkodzone fakty.
     */
    private fun Category.sanitizedOrNull(): Category? {
        if (id.isBlank() || title.isBlank()) {
            Log.w(TAG, "Kategoria bez id/tytułu została pominięta (id='$id').")
            return null
        }

        val validFacts = facts.filter { it.isValid() }
        if (validFacts.isEmpty()) {
            Log.w(TAG, "Kategoria '$id' nie zawiera poprawnych faktów - pominięta.")
            return null
        }

        // Duplikaty id w obrębie kategorii rozbiłyby klucze list w LazyColumn.
        val deduplicated = validFacts.distinctBy { it.id }
        if (deduplicated.size != validFacts.size) {
            Log.w(TAG, "Kategoria '$id' zawierała zduplikowane id faktów - usunięto powtórzenia.")
        }

        return if (deduplicated == facts) this else copy(facts = deduplicated)
    }

    private fun Fact.isValid(): Boolean =
        title.isNotBlank() && shortDescription.isNotBlank() && details.isNotBlank()

    private companion object {
        const val TAG = "FactsRepository"
        const val DATA_FILE = "data.json"
    }
}
