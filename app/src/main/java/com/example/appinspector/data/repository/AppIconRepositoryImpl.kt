package com.example.appinspector.data.repository

import com.example.appinspector.data.cache.AppIconCache
import javax.inject.Inject

class AppIconRepositoryImpl @Inject constructor(
    private val appIconCache: AppIconCache,
) : AppIconRepository {

    override suspend fun getIcon(packageName: String) = appIconCache.getIcon(packageName)
}
