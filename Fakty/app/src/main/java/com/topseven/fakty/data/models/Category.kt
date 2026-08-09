package com.topseven.fakty.data.models

import kotlinx.serialization.Serializable
import androidx.compose.runtime.Immutable

@Immutable
@Serializable
data class Category(
    val id: String,
    val title: String,
    val icon: String,
    val facts: List<Fact>
)
