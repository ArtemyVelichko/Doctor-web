package com.example.appinspector.presentation.common

import com.example.appinspector.R
import com.example.appinspector.data.util.ResourceProvider

/**
 * Extension функции для получения user-friendly сообщений из AppError.
 */
fun AppError.toDisplayMessage(resourceProvider: ResourceProvider): String {
    return when (this) {
        is AppError.PermissionDenied -> {
            resourceProvider.getString(R.string.app_list_error_permission_denied)
        }
        is AppError.AppNotFound -> {
            resourceProvider.getString(R.string.app_error_not_found, packageName)
        }
        is AppError.Unknown -> {
            message ?: resourceProvider.getString(R.string.app_list_error_unknown)
        }
        is AppError.Timeout -> {
            resourceProvider.getString(R.string.app_list_error_timeout)
        }
    }
}

/**
 * Goog messages for developers
 */
fun AppError.toLogMessage(): String {
    return when (this) {
        is AppError.PermissionDenied -> "PermissionDenied: $permission - $details"
        is AppError.AppNotFound -> "AppNotFound: $packageName"
        is AppError.Unknown -> "Unknown: $message (${cause?.javaClass?.simpleName})"
        is AppError.Timeout -> "Timeout"
    }
}
