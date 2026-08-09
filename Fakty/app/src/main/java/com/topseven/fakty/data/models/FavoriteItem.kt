package com.topseven.fakty.data.models

import androidx.compose.runtime.Immutable

@Immutable
data class FavoriteItem(
    val category: Category,
    val fact: Fact
)
