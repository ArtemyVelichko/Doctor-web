package com.example.appinspector.presentation.applist

import com.example.appinspector.data.model.AppCardItem
import com.example.appinspector.presentation.common.AppError
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * MVI Model — единственный источник истины для экрана списка приложений.
 */
data class AppListState(
    val isLoading: Boolean = false,
    val items: ImmutableList<AppCardItem> = persistentListOf(),
    val error: AppError? = null,
)
