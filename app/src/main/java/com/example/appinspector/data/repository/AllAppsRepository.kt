package com.example.appinspector.data.repository

import com.example.appinspector.data.model.AppCardItem
import kotlinx.collections.immutable.ImmutableList

interface AllAppsRepository {

    suspend fun getAllApps(): ImmutableList<AppCardItem>
}
