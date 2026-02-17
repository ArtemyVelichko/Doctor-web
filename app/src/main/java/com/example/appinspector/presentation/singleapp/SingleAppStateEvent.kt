package com.example.appinspector.presentation.singleapp

import com.example.appinspector.data.model.AppDetails
import com.example.appinspector.presentation.common.AppError

/**
 * События (результаты side-effects), которые изменяют state через reducer.
 * Используются внутри ViewModel для синхронного изменения состояния.
 */
sealed interface SingleAppStateEvent {
    data class LoadingStarted(val packageName: String) : SingleAppStateEvent
    data class LoadingSuccess(val details: AppDetails) : SingleAppStateEvent
    data class LoadingError(val error: AppError) : SingleAppStateEvent
}
