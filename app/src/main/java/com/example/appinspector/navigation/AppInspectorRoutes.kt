package com.example.appinspector.navigation

import kotlinx.serialization.Serializable
/**
 * Маршрут: Экран списка всех приложений.
 *
 */
@Serializable
object ListApps

/**
 * Маршрут: Экран деталей одного приложения.
 */
@Serializable
data class AppDetail(val packageName: String)
