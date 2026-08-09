package com.topseven.fakty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.topseven.fakty.data.database.FavoriteDao
import com.topseven.fakty.data.repository.FactsRepository
import com.topseven.fakty.ui.screens.*
import com.topseven.fakty.ui.theme.FaktyTheme
import com.topseven.fakty.ui.navigation.*
import androidx.navigation.toRoute

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FaktyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FaktyApp()
                }
            }
        }
    }
}

class MainViewModelFactory(
    private val repository: FactsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(repository) as T
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FaktyApp() {
    val context = LocalContext.current
    val app = context.applicationContext as TopSevenApplication
    val factory = remember { MainViewModelFactory(app.repository) }
    
    val viewModel: MainViewModel = viewModel(factory = factory)

    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    
    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            enterTransition = { slideInHorizontally(animationSpec = tween(400)) { it / 4 } + fadeIn(animationSpec = tween(400)) },
            exitTransition = { slideOutHorizontally(animationSpec = tween(400)) { -it / 4 } + fadeOut(animationSpec = tween(400)) },
            popEnterTransition = { slideInHorizontally(animationSpec = tween(400)) { -it / 4 } + fadeIn(animationSpec = tween(400)) },
            popExitTransition = { slideOutHorizontally(animationSpec = tween(400)) { it / 4 } + fadeOut(animationSpec = tween(400)) }
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    viewModel = viewModel,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    onCategoryClick = { category ->
                        navController.navigate(CategoryRoute(category.id))
                    },
                    onFavoritesClick = {
                        navController.navigate(FavoritesRoute)
                    },
                    onFlashcardsClick = {
                        navController.navigate(FlashcardsRoute)
                    }
                )
            }
            
            composable<FlashcardsRoute> {
                FlashcardsScreen(
                    viewModel = viewModel,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable<FavoritesRoute> {
                FavoritesScreen(
                    viewModel = viewModel,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onFactClick = { categoryId, factId ->
                        navController.navigate(FactDetailRoute(categoryId, factId, source = "favorites"))
                    }
                )
            }
                
            composable<CategoryRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<CategoryRoute>()
                val categoryId = route.categoryId
                
                val successState = uiState as? UiState.Success ?: return@composable
                val category = successState.categories.find { it.id == categoryId }
                if (category != null) {
                    CategoryListScreen(
                        category = category,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable,
                        onBackClick = { navController.popBackStack() },
                        onFactClick = { factId ->
                            navController.navigate(FactDetailRoute(category.id, factId))
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(id = R.string.error_category_not_found), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            composable<FactDetailRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<FactDetailRoute>()
                val categoryId = route.categoryId
                val factId = route.factId
                val source = route.source

                val successState = uiState as? UiState.Success ?: return@composable

                val items = if (source == "favorites") {
                    viewModel.allFavoriteItems.value
                } else {
                    val category = successState.categories.find { it.id == categoryId }
                    category?.facts?.map { com.topseven.fakty.data.models.FavoriteItem(category, it) } ?: emptyList()
                }

                if (items.isNotEmpty()) {
                    FactDetailScreen(
                        viewModel = viewModel,
                        items = items,
                        initialFactId = factId,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable,
                        onBackClick = { navController.popBackStack() }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(id = R.string.error_fact_not_found), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
