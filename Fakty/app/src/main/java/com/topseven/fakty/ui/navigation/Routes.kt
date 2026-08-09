package com.topseven.fakty.ui.navigation

import kotlinx.serialization.Serializable

/** Skąd otwarto ekran szczegółów - decyduje o tym, po jakiej liście przewija pager. */
object FactDetailSource {
    const val CATEGORY = "category"
    const val FAVORITES = "favorites"
}

@Serializable
object HomeRoute

@Serializable
object FlashcardsRoute

@Serializable
object FavoritesRoute

@Serializable
data class CategoryRoute(val categoryId: String)

@Serializable
data class FactDetailRoute(
    val categoryId: String,
    val factId: Int,
    val source: String = FactDetailSource.CATEGORY
)
