package com.example.appinspector.data.cache

import android.graphics.Bitmap

/**
 * Кеш иконок приложений по [packageName].
 * Использует LruCache и потокобезопасный доступ (Mutex).
 */
interface AppIconCache {
    suspend fun getIcon(packageName: String): Bitmap?
}
