package com.example.appinspector.data.repository

import com.example.appinspector.data.model.AppDetails

/**
 * Репозиторий деталей одного приложения: название, версия, системное/нет, контрольная сумма APK.
 */
interface AppDetailsRepository {
    suspend fun getDetails(packageName: String): AppDetails?
}
