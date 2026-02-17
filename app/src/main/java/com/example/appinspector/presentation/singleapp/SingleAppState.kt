package com.example.appinspector.presentation.singleapp

import com.example.appinspector.presentation.common.AppError

/**
 * MVI Model — состояние экрана с подробной информацией об одном приложении.
 *
 * @param packageName Имя пакета приложения (например, `com.android.chrome`).
 * @param label Название приложения, отображаемое пользователю (из PackageManager).
 * @param versionName Версия приложения в виде строки (например, `"120.0.6099.109"`), может быть null.
 * @param versionCode Числовой код версии (версия для сравнения), 0 если неизвестна.
 * @param apkChecksumSha256 Контрольная сумма APK-файла (SHA-256) в hex, null пока не вычислена или при ошибке.
 * @param isSystemApp Единственный параметр, определяющий тип приложения: `true` — системное (входит в прошивку / системный образ), `false` — пользовательское (установлено пользователем).
 * @param hasLauncherActivity Есть ли у приложения launcher activity (можно ли его открыть).
 * @param isLoading Идёт ли загрузка данных (пока true, контент экрана обычно не показывают).
 * @param error Информация об ошибке (типизированная); при не null экран показывает ошибку вместо данных.
 */
data class SingleAppState(
    val packageName: String = "",
    val label: String = "",
    val versionName: String? = null,
    val versionCode: Long = 0L,
    val apkChecksumSha256: String? = null,
    val isSystemApp: Boolean = false,
    val hasLauncherActivity: Boolean = false,
    val isLoading: Boolean = false,
    val error: AppError? = null,
)
