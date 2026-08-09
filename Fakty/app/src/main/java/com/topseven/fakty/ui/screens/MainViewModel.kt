package com.topseven.fakty.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topseven.fakty.data.models.Category
import com.topseven.fakty.data.repository.FactsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import com.topseven.fakty.data.database.FavoriteFact
import com.topseven.fakty.data.models.FavoriteItem
import android.content.Context
import com.topseven.fakty.utils.TtsManager

sealed class UiState {
    object Loading : UiState()
    data class Success(val categories: List<Category>) : UiState()
    data class Error(val messageResId: Int) : UiState()
}


class MainViewModel(private val repository: FactsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Usunięto searchQuery, przeniesiono do lokalnego stanu w FavoritesScreen

    val favorites: StateFlow<List<FavoriteFact>> = repository.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFavoriteItems: StateFlow<List<FavoriteItem>> = combine(uiState, favorites) { state, favs ->
        if (state is UiState.Success) {
            favs.mapNotNull { fav ->
                val category = state.categories.find { it.id == fav.categoryId }
                val fact = category?.facts?.find { it.id == fav.factId }
                if (category != null && fact != null) {
                    FavoriteItem(category, fact)
                } else null
            }
        } else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val categories = repository.getCategories()
            if (categories.isNotEmpty()) {
                _uiState.value = UiState.Success(categories.sortedBy { it.title })
            } else {
                _uiState.value = UiState.Error(com.topseven.fakty.R.string.error_loading_data)
            }
        }
    }



    fun toggleFavorite(categoryId: String, factId: Int, isCurrentlyFavorite: Boolean) {
        viewModelScope.launch {
            if (isCurrentlyFavorite) {
                repository.removeFavorite(categoryId, factId)
            } else {
                repository.addFavorite(categoryId, factId)
            }
        }
    }

    var ttsManager: TtsManager? = null
        private set

    fun initTts(context: Context) {
        if (ttsManager == null) {
            ttsManager = TtsManager(context.applicationContext)
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager?.shutdown()
    }
}
