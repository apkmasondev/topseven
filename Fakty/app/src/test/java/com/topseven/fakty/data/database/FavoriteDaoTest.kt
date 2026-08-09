package com.topseven.fakty.data.database

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Ulubione są przechowywane w [SharedPreferences] jako zbiór kluczy `categoryId:factId`.
 * Testy pilnują przede wszystkim atomowości cyklu odczyt-modyfikacja-zapis, bo to on
 * decyduje o tym, czy szybkie przełączanie serduszka nie gubi wpisów.
 */
class FavoriteDaoTest {

    private val store = mutableMapOf<String, Set<String>>()
    private lateinit var context: Context

    @Before
    fun setUp() {
        store.clear()

        val editor = mockk<SharedPreferences.Editor>()
        val valueSlot = slot<Set<String>>()
        every { editor.putStringSet(any(), capture(valueSlot)) } answers {
            store["favorite_keys"] = valueSlot.captured.toSet()
            editor
        }
        every { editor.commit() } returns true

        val prefs = mockk<SharedPreferences>()
        every { prefs.getStringSet(any(), any()) } answers { store["favorite_keys"] }
        every { prefs.edit() } returns editor

        context = mockk()
        every { context.applicationContext } returns context
        every { context.getSharedPreferences(any(), any()) } returns prefs
    }

    @Test
    fun `dodanie i usuniecie ulubionego jest widoczne w strumieniu`() = runTest {
        val dao = FavoriteDao(context)

        dao.insertFavorite(FavoriteFact("kosmos", 31))
        assertEquals(listOf(FavoriteFact("kosmos", 31)), dao.getAllFavorites().first())

        dao.deleteFavorite(FavoriteFact("kosmos", 31))
        assertTrue(dao.getAllFavorites().first().isEmpty())
    }

    @Test
    fun `stan jest wczytywany z dysku przy pierwszej subskrypcji`() = runTest {
        store["favorite_keys"] = setOf("kosmos:31", "fizyka:61")

        val dao = FavoriteDao(context)

        assertEquals(
            listOf(FavoriteFact("fizyka", 61), FavoriteFact("kosmos", 31)),
            dao.getAllFavorites().first()
        )
    }

    @Test
    fun `rownolegle dodawanie nie gubi zadnego wpisu`() = runTest {
        val dao = FavoriteDao(context)

        // Read-modify-write bez blokady gubił tu większość zapisów.
        (1..50).map { id ->
            async { dao.insertFavorite(FavoriteFact("kategoria", id)) }
        }.awaitAll()

        assertEquals(50, dao.getAllFavorites().first().size)
    }

    @Test
    fun `szybkie przelaczanie tego samego faktu konczy sie spojnym stanem`() = runTest {
        val dao = FavoriteDao(context)
        val fact = FavoriteFact("kosmos", 31)

        repeat(20) {
            dao.insertFavorite(fact)
            dao.deleteFavorite(fact)
        }
        dao.insertFavorite(fact)

        assertEquals(listOf(fact), dao.getAllFavorites().first())
    }

    @Test
    fun `uszkodzone klucze sa ignorowane zamiast wywracac liste`() = runTest {
        store["favorite_keys"] = setOf(
            "kosmos:31",
            "bez-separatora",
            "kosmos:nie-liczba",
            ":42",
            "kosmos:"
        )

        val dao = FavoriteDao(context)

        assertEquals(listOf(FavoriteFact("kosmos", 31)), dao.getAllFavorites().first())
    }

    @Test
    fun `kolejnosc listy jest deterministyczna`() = runTest {
        store["favorite_keys"] = setOf("zwierzeta:111", "anatomia:91", "kosmos:31", "anatomia:92")

        val dao = FavoriteDao(context)

        assertEquals(
            listOf(
                FavoriteFact("anatomia", 91),
                FavoriteFact("anatomia", 92),
                FavoriteFact("kosmos", 31),
                FavoriteFact("zwierzeta", 111)
            ),
            dao.getAllFavorites().first()
        )
    }

    @Test
    fun `ponowne dodanie tego samego faktu nie tworzy duplikatu`() = runTest {
        val dao = FavoriteDao(context)

        dao.insertFavorite(FavoriteFact("kosmos", 31))
        dao.insertFavorite(FavoriteFact("kosmos", 31))

        assertEquals(1, dao.getAllFavorites().first().size)
    }
}
