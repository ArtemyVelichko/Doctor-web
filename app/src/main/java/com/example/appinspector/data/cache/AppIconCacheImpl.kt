package com.example.appinspector.data.cache

import android.graphics.Bitmap
import android.util.LruCache
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * Кеш по ключу (packageName): LruCache + Mutex.
 * При промахе грузит через [iconLoader] и кладёт в кеш — сама загрузка (PM, drawable→bitmap) в другой логике.
 */
class AppIconCacheImpl @Inject constructor(
    private val iconLoader: AppIconLoader,
) : AppIconCache {

    val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    val cacheSize = maxMemory / 8

    private val cacheMutex = Mutex()

    val imageCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int = bitmap.allocationByteCount / 1024
    }

    override suspend fun getIcon(packageName: String): Bitmap? {
        imageCache.get(packageName)?.let { return it }
        return cacheMutex.withLock {
            imageCache.get(packageName)?.let { return it }
            val bitmap = iconLoader.loadIcon(packageName) ?: return null
            imageCache.put(packageName, bitmap)
            bitmap
        }
    }
}
