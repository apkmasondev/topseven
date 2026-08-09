package com.topseven.fakty

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.topseven.fakty.data.database.FavoriteDao
import com.topseven.fakty.data.repository.FactsRepository

class TopSevenApplication : Application(), SingletonImageLoader.Factory {
    
    val favoriteDao by lazy { FavoriteDao(this) }
    val repository by lazy { FactsRepository(this, favoriteDao) }

    override fun newImageLoader(context: android.content.Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.20)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
