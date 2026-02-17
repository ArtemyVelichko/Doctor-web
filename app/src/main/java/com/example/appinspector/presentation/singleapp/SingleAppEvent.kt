package com.example.appinspector.presentation.singleapp

/**
 * Одноразовые события (side-effects) для SingleAppScreen.
 */
sealed interface SingleAppEvent {
    /**
     * Приложение успешно запущено.
     */
    data object AppLaunched : SingleAppEvent
    
    /**
     * Не удалось запустить приложение.
     */
    data class LaunchFailed(val reason: String) : SingleAppEvent
    
    /**
     * Ошибка при загрузке информации о приложении.
     * Показывается как Toast/Snackbar пользователю.
     */
    data class LoadingError(val message: String) : SingleAppEvent
}
