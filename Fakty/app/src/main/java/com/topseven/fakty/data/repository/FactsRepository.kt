package com.topseven.fakty.data.repository

import android.content.Context
import com.topseven.fakty.data.models.Category
import kotlinx.coroutines.Dispatchers
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

    suspend fun getCategories(): List<Category> = withContext(Dispatchers.IO) {
        cachedCategories?.let { return@withContext it }
        try {
            val jsonString = context.assets.open("data.json").use {
                it.bufferedReader().readText()
            }
            json.decodeFromString<List<Category>>(jsonString).also {
                cachedCategories = it
            }
        } catch (e: Exception) {
            android.util.Log.e("FactsRepository", "Failed to load data.json", e)
            emptyList()
        }
    }
}
