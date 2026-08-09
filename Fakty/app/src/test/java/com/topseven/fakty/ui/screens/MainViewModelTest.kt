package com.topseven.fakty.ui.screens

import app.cash.turbine.test
import com.topseven.fakty.data.database.FavoriteFact
import com.topseven.fakty.data.models.Category
import com.topseven.fakty.data.repository.FactsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FactsRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when viewmodel is initialized with empty categories, state is Error`() = runTest {
        // Arrange
        coEvery { repository.getCategories() } returns emptyList()
        every { repository.getAllFavorites() } returns flowOf(emptyList())

        // Act
        viewModel = MainViewModel(repository)

        // Assert
        viewModel.uiState.test {
            // Because initialization happens immediately, we might miss the initial Loading state
            // but we will catch the final state.
            val item = awaitItem()
            // Depending on dispatcher timing, it might be Loading first or already Error.
            if (item is UiState.Loading) {
                val nextItem = awaitItem()
                assertTrue("Expected Error state, got $nextItem", nextItem is UiState.Error)
            } else {
                assertTrue("Expected Error state, got $item", item is UiState.Error)
            }
        }
    }

    @Test
    fun `when viewmodel is initialized with categories, state is Success and sorted`() = runTest {
        // Arrange
        val categoryB = Category("b", "B", "b.jpg", emptyList())
        val categoryA = Category("a", "A", "a.jpg", emptyList())
        coEvery { repository.getCategories() } returns listOf(categoryB, categoryA)
        every { repository.getAllFavorites() } returns flowOf(emptyList())

        // Act
        viewModel = MainViewModel(repository)

        // Assert
        viewModel.uiState.test {
            val item = awaitItem()
            if (item is UiState.Loading) {
                val nextItem = awaitItem()
                assertTrue(nextItem is UiState.Success)
                val success = nextItem as UiState.Success
                assertEquals("a", success.categories[0].id)
                assertEquals("b", success.categories[1].id)
            } else {
                assertTrue(item is UiState.Success)
                val success = item as UiState.Success
                assertEquals("a", success.categories[0].id)
                assertEquals("b", success.categories[1].id)
            }
        }
    }

    @Test
    fun `toggleFavorite adds favorite when not favorited`() = runTest {
        // Arrange
        coEvery { repository.getCategories() } returns emptyList()
        every { repository.getAllFavorites() } returns flowOf(emptyList())
        viewModel = MainViewModel(repository)

        // Act
        viewModel.toggleFavorite("kosmos", 1, isCurrentlyFavorite = false)
        
        // Let coroutines finish
        testScheduler.advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { repository.addFavorite("kosmos", 1) }
        coVerify(exactly = 0) { repository.removeFavorite(any(), any()) }
    }

    @Test
    fun `toggleFavorite removes favorite when already favorited`() = runTest {
        // Arrange
        coEvery { repository.getCategories() } returns emptyList()
        every { repository.getAllFavorites() } returns flowOf(emptyList())
        viewModel = MainViewModel(repository)

        // Act
        viewModel.toggleFavorite("kosmos", 1, isCurrentlyFavorite = true)
        
        // Let coroutines finish
        testScheduler.advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { repository.removeFavorite("kosmos", 1) }
        coVerify(exactly = 0) { repository.addFavorite(any(), any()) }
    }
}
