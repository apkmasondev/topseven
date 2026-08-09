package com.topseven.fakty.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
object FlashcardsRoute

@Serializable
object FavoritesRoute

@Serializable
data class CategoryRoute(val categoryId: String)

@Serializable
data class FactDetailRoute(val categoryId: String, val factId: Int, val source: String = "category")
