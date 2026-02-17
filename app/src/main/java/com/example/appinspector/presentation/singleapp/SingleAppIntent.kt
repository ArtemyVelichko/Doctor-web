package com.example.appinspector.presentation.singleapp

/**
 * MVI Intent — действия для экрана одного приложения.
 */
sealed interface SingleAppIntent {
    data class Load(val packageName: String) : SingleAppIntent
    data object Retry : SingleAppIntent
    data object LaunchApp : SingleAppIntent
}
