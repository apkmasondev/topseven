package com.topseven.fakty.data.repository

import android.content.Context
import android.content.res.AssetManager
import com.topseven.fakty.data.database.FavoriteDao
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * Testy odporności wczytywania `assets/data.json` - uszkodzony lub niekompletny plik
 * nie może wywracać aplikacji ani przepuszczać niepoprawnych rekordów do UI.
 */
class FactsRepositoryTest {

    private val favoriteDao: FavoriteDao = mockk(relaxed = true)

    private fun repositoryWith(json: String): FactsRepository {
        val assets = mockk<AssetManager>()
        every { assets.open("data.json") } answers { ByteArrayInputStream(json.toByteArray()) }
        val context = mockk<Context>()
        every { context.assets } returns assets
        return FactsRepository(context, favoriteDao)
    }

    private fun repositoryThatFailsToOpen(): Pair<FactsRepository, AssetManager> {
        val assets = mockk<AssetManager>()
        every { assets.open("data.json") } throws IOException("brak pliku")
        val context = mockk<Context>()
        every { context.assets } returns assets
        return FactsRepository(context, favoriteDao) to assets
    }

    @Test
    fun `poprawny json zwraca kategorie z faktami`() = runTest {
        val repository = repositoryWith(
            """
            [
              {
                "id": "kosmos",
                "title": "Kosmos",
                "icon": "kosmos.webp",
                "facts": [
                  {"id": 1, "title": "Mars", "shortDescription": "Czerwona planeta", "details": "Czwarta planeta."}
                ]
              }
            ]
            """.trimIndent()
        )

        val categories = repository.getCategories()

        assertEquals(1, categories.size)
        assertEquals("kosmos", categories[0].id)
        assertEquals(1, categories[0].facts.size)
    }

    @Test
    fun `pusty plik nie powoduje crashu i zwraca pusta liste`() = runTest {
        assertTrue(repositoryWith("").getCategories().isEmpty())
    }

    @Test
    fun `uszkodzony json nie powoduje crashu i zwraca pusta liste`() = runTest {
        assertTrue(repositoryWith("{ to nie jest json").getCategories().isEmpty())
    }

    @Test
    fun `nieoczekiwany ksztalt json nie powoduje crashu`() = runTest {
        // Obiekt zamiast oczekiwanej tablicy kategorii.
        assertTrue(repositoryWith("""{"categories": []}""").getCategories().isEmpty())
    }

    @Test
    fun `brak pliku w assets nie powoduje crashu`() = runTest {
        val (repository, _) = repositoryThatFailsToOpen()
        assertTrue(repository.getCategories().isEmpty())
    }

    @Test
    fun `kategoria bez tytulu jest pomijana`() = runTest {
        val repository = repositoryWith(
            """
            [
              {"id": "a", "title": "", "icon": "a.webp",
               "facts": [{"id": 1, "title": "T", "shortDescription": "S", "details": "D"}]},
              {"id": "b", "title": "B", "icon": "b.webp",
               "facts": [{"id": 2, "title": "T", "shortDescription": "S", "details": "D"}]}
            ]
            """.trimIndent()
        )

        val categories = repository.getCategories()

        assertEquals(1, categories.size)
        assertEquals("b", categories[0].id)
    }

    @Test
    fun `kategoria bez poprawnych faktow jest pomijana`() = runTest {
        val repository = repositoryWith(
            """
            [
              {"id": "a", "title": "A", "icon": "a.webp",
               "facts": [{"id": 1, "title": "", "shortDescription": "", "details": ""}]}
            ]
            """.trimIndent()
        )

        assertTrue(repository.getCategories().isEmpty())
    }

    @Test
    fun `zduplikowane id faktow w kategorii sa usuwane`() = runTest {
        val repository = repositoryWith(
            """
            [
              {"id": "a", "title": "A", "icon": "a.webp", "facts": [
                {"id": 1, "title": "Pierwszy", "shortDescription": "S", "details": "D"},
                {"id": 1, "title": "Duplikat", "shortDescription": "S", "details": "D"},
                {"id": 2, "title": "Drugi", "shortDescription": "S", "details": "D"}
              ]}
            ]
            """.trimIndent()
        )

        val facts = repository.getCategories().single().facts

        assertEquals(listOf(1, 2), facts.map { it.id })
        assertEquals("Pierwszy", facts[0].title)
    }

    @Test
    fun `nieznane pola w json sa ignorowane`() = runTest {
        val repository = repositoryWith(
            """
            [
              {"id": "a", "title": "A", "icon": "a.webp", "nieznanePole": 42, "facts": [
                {"id": 1, "title": "T", "shortDescription": "S", "details": "D", "cosNowego": true}
              ]}
            ]
            """.trimIndent()
        )

        assertEquals(1, repository.getCategories().size)
    }

    @Test
    fun `drugie wywolanie korzysta z cache i nie czyta pliku ponownie`() = runTest {
        val assets = mockk<AssetManager>()
        every { assets.open("data.json") } answers {
            ByteArrayInputStream(
                """
                [{"id": "a", "title": "A", "icon": "a.webp",
                  "facts": [{"id": 1, "title": "T", "shortDescription": "S", "details": "D"}]}]
                """.trimIndent().toByteArray()
            )
        }
        val context = mockk<Context>()
        every { context.assets } returns assets
        val repository = FactsRepository(context, favoriteDao)

        repository.getCategories()
        repository.getCategories()

        verify(exactly = 1) { assets.open("data.json") }
    }

    @Test
    fun `pusty wynik nie jest cachowany i pozwala na ponowna probe`() = runTest {
        val assets = mockk<AssetManager>()
        var attempt = 0
        every { assets.open("data.json") } answers {
            attempt++
            if (attempt == 1) {
                throw IOException("chwilowy blad")
            }
            ByteArrayInputStream(
                """
                [{"id": "a", "title": "A", "icon": "a.webp",
                  "facts": [{"id": 1, "title": "T", "shortDescription": "S", "details": "D"}]}]
                """.trimIndent().toByteArray()
            )
        }
        val context = mockk<Context>()
        every { context.assets } returns assets
        val repository = FactsRepository(context, favoriteDao)

        assertTrue(repository.getCategories().isEmpty())
        assertEquals(1, repository.getCategories().size)
    }
}
