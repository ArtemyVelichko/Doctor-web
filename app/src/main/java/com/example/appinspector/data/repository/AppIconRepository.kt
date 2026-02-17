package com.example.appinspector.data.repository

import android.graphics.Bitmap

/** Репозиторий иконок приложений по packageName. Внутри может использовать кеш — это деталь реализации. */
interface AppIconRepository {
    suspend fun getIcon(packageName: String): Bitmap?
}
