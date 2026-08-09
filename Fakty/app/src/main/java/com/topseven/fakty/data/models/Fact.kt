package com.topseven.fakty.data.models

import kotlinx.serialization.Serializable
import androidx.compose.runtime.Immutable

@Immutable
@Serializable
data class Fact(
    val id: Int,
    val title: String,
    val shortDescription: String,
    val details: String,
    val imageUrl: String? = null,
    val glossary: Map<String, GlossaryEntry>? = null
)

@Immutable
@Serializable
data class GlossaryEntry(
    val title: String,
    val definition: String
)
