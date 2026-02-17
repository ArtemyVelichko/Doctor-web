package com.example.appinspector.data.repository

import android.content.pm.PackageManager
import com.example.appinspector.data.ext.toAppCardItem
import com.example.appinspector.data.model.AppCardItem
import com.example.appinspector.di.IoDispatcher
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Список всех установленных приложений через PackageManager.
 * Иконка не подставляется здесь (нужен UI/Compose) — в UI можно подгрузить по packageName.
 * Retry не применяется: вызов локальный (PackageManager), а не транзиентный сеть/I/O сценарий.
 */
class AllAppsRepositoryImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val packageManager: PackageManager,
) : AllAppsRepository {

    override suspend fun getAllApps(): ImmutableList<AppCardItem> = withContext(ioDispatcher) {
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        apps
            .map { it.toAppCardItem(packageManager) }
            .sortedBy { it.name.lowercase() }
            .toPersistentList()
    }
}
