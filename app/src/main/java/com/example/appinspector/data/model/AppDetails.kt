package com.example.appinspector.data.model

/**
 * Детальная информация о приложении для экрана инфо.
 */
data class AppDetails(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val apkChecksumSha256: String?,
    val hasLauncherActivity: Boolean,
)
