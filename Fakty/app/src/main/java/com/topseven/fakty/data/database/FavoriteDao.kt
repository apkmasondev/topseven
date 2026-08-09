package com.topseven.fakty.data.database

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FavoriteDao(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)

    // Zestaw ulubionych "categoryId:factId"
    private val _favoritesFlow = MutableStateFlow(loadFavorites())
    private val mutex = Mutex()

    private fun loadFavorites(): Set<String> {
        return prefs.getStringSet("favorite_keys", emptySet())?.toSet() ?: emptySet()
    }

    private fun saveFavorites(favorites: Set<String>) {
        prefs.edit().putStringSet("favorite_keys", favorites).apply()
        _favoritesFlow.value = favorites
    }

    suspend fun insertFavorite(favoriteFact: FavoriteFact) {
        mutex.withLock {
            val current = loadFavorites().toMutableSet()
            current.add("${favoriteFact.categoryId}:${favoriteFact.factId}")
            saveFavorites(current)
        }
    }

    suspend fun deleteFavorite(favoriteFact: FavoriteFact) {
        mutex.withLock {
            val current = loadFavorites().toMutableSet()
            current.remove("${favoriteFact.categoryId}:${favoriteFact.factId}")
            saveFavorites(current)
        }
    }

    fun getAllFavorites(): Flow<List<FavoriteFact>> {
        return _favoritesFlow.map { set ->
            set.mapNotNull {
                val parts = it.split(":")
                if (parts.size == 2) {
                    val catId = parts[0]
                    val factId = parts[1].toIntOrNull()
                    if (factId != null) FavoriteFact(catId, factId) else null
                } else null
            }
        }
    }

    fun isFavorite(categoryId: String, factId: Int): Flow<Boolean> {
        return _favoritesFlow.map { set ->
            set.contains("$categoryId:$factId")
        }
    }
}
