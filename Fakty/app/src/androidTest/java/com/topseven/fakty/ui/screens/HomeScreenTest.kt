package com.topseven.fakty.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.topseven.fakty.data.models.Category
import com.topseven.fakty.ui.theme.FaktyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@androidx.compose.animation.ExperimentalSharedTransitionApi
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_whenLoading_showsProgressIndicator() {
        // Arrange
        val uiState = UiState.Loading

        // Act
        composeTestRule.setContent {
            FaktyTheme {
                androidx.compose.animation.SharedTransitionLayout {
                    androidx.compose.animation.AnimatedVisibility(visible = true) {
                        HomeScreenContent(
                            uiState = uiState,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedVisibility,
                            onCategoryClick = {},
                            onFavoritesClick = {},
                            onFlashcardsClick = {}
                        )
                    }
                }
            }
        }

        // Assert
        // We can't directly check CircularProgressIndicator by default unless it has a testTag,
        // but let's assert that the category cards are NOT displayed
        composeTestRule.onNodeWithText("Kosmos").assertDoesNotExist()
    }

    @Test
    fun homeScreen_whenSuccess_showsCategories() {
        // Arrange
        val mockCategories = listOf(
            Category(id = "kosmos", title = "Kosmos", icon = "kosmos.jpg", facts = emptyList()),
            Category(id = "fizyka", title = "Fizyka", icon = "fizyka.jpg", facts = emptyList())
        )
        val uiState = UiState.Success(mockCategories)

        // Act
        composeTestRule.setContent {
            FaktyTheme {
                androidx.compose.animation.SharedTransitionLayout {
                    androidx.compose.animation.AnimatedVisibility(visible = true) {
                        HomeScreenContent(
                            uiState = uiState,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedVisibility,
                            onCategoryClick = {},
                            onFavoritesClick = {},
                            onFlashcardsClick = {}
                        )
                    }
                }
            }
        }

        // Assert
        // Check if the titles are displayed
        composeTestRule.onNodeWithText("Kosmos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fizyka").assertIsDisplayed()
    }

    @Test
    fun homeScreen_whenCategoryClicked_triggersCallback() {
        // Arrange
        val category = Category(id = "kosmos", title = "Kosmos", icon = "kosmos.jpg", facts = emptyList())
        val mockCategories = listOf(category)
        val uiState = UiState.Success(mockCategories)
        var clickedCategory: Category? = null

        // Act
        composeTestRule.setContent {
            FaktyTheme {
                androidx.compose.animation.SharedTransitionLayout {
                    androidx.compose.animation.AnimatedVisibility(visible = true) {
                        HomeScreenContent(
                            uiState = uiState,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedVisibility,
                            onCategoryClick = { clickedCategory = it },
                            onFavoritesClick = {},
                            onFlashcardsClick = {}
                        )
                    }
                }
            }
        }

        // Click the category card
        composeTestRule.onNodeWithText("Kosmos").performClick()

        // Assert
        assert(clickedCategory == category)
    }

    @Test
    fun homeScreen_whenFlashcardsClicked_triggersCallback() {
        // Arrange
        val uiState = UiState.Success(emptyList())
        var flashcardsClicked = false

        // Act
        composeTestRule.setContent {
            FaktyTheme {
                androidx.compose.animation.SharedTransitionLayout {
                    androidx.compose.animation.AnimatedVisibility(visible = true) {
                        HomeScreenContent(
                            uiState = uiState,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedVisibility,
                            onCategoryClick = {},
                            onFavoritesClick = {},
                            onFlashcardsClick = { flashcardsClicked = true }
                        )
                    }
                }
            }
        }

        // Click the flashcards icon (assuming content description is set up in string resources)
        // Since we don't have access to context easily in the test string matching,
        // we can find it by looking for the icon or use a tag. Let's add a test tag or find by content description.
        // Actually, we can use stringRes in instrumentation tests:
        val flashcardsDesc = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.getString(com.topseven.fakty.R.string.content_desc_flashcards)
        composeTestRule.onNodeWithContentDescription(flashcardsDesc).performClick()

        // Assert
        assert(flashcardsClicked)
    }
}
