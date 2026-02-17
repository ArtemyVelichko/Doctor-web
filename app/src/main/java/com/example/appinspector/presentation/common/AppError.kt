package com.example.appinspector.presentation.common

/**
 * Sealed class для типизированных ошибок в приложении.
 */
sealed interface AppError {
    
    /**
     * Нет прав доступа для выполнения операции.
     * Например, нет прав на чтение списка приложений или детальной информации.
     */
    data class PermissionDenied(
        val permission: String? = null,
        val details: String? = null
    ) : AppError
    
    /**
     * Приложение не найдено (для SingleAppScreen).
     * Возможно, приложение было удалено или передан неверный packageName.
     */
    data class AppNotFound(
        val packageName: String
    ) : AppError
    
    /**
     * Неизвестная ошибка - что-то пошло не так.
     */
    data class Unknown(
        val message: String? = null,
        val cause: Throwable? = null
    ) : AppError
    
    /**
     * Операция заняла слишком много времени (опционально, для будущего).
     */
    data object Timeout : AppError
}
