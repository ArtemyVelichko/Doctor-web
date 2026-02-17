package com.example.appinspector.presentation.applist

/**
 * MVI Intent — действия пользователя или системы для экрана списка приложений.
 */
sealed interface AppListIntent {
    data object Load : AppListIntent
    data object Refresh : AppListIntent
    data object Retry : AppListIntent
}
