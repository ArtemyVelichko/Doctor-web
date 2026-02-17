package com.example.appinspector.presentation.applist

import com.example.appinspector.data.model.AppCardItem
import com.example.appinspector.presentation.common.AppError
import kotlinx.collections.immutable.ImmutableList

/**
 * События (результаты side-effects), которые изменяют state через reducer.
 * Используются внутри ViewModel для синхронного изменения состояния.
 */
sealed interface AppListEvent {
    data object LoadingStarted : AppListEvent
    data class LoadingSuccess(val items: ImmutableList<AppCardItem>) : AppListEvent
    data class LoadingError(val error: AppError) : AppListEvent
}
