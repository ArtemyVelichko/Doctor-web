package com.example.appinspector.domain.usecase

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.appinspector.R
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

/**
 * UseCase для запуска приложения по packageName.
 * 
 * Инкапсулирует логику проверки возможности запуска и сам запуск.
 */
class LaunchAppUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageManager: PackageManager,
) {
    
    /**
     * Результат попытки запуска приложения.
     */
    sealed interface Result {
        /** Приложение успешно запущено */
        data object Success : Result
        
        /** У приложения нет launcher activity */
        data object NoLauncherActivity : Result
        
        /** Приложение не найдено */
        data class AppNotFound(val packageName: String) : Result
        
        /** Ошибка при запуске */
        data class Error(val message: String) : Result
    }
    
    /**
     * Пытается запустить приложение.
     *
     */
    operator fun invoke(packageName: String): Result {
        Timber.d("LaunchAppUseCase: Attempting to launch app with package=$packageName")
        
        if (packageName.isBlank()) {
            Timber.e("LaunchAppUseCase: Package name is empty")
            return Result.Error(
                context.getString(R.string.single_app_screen_launch_error_empty_package)
            )
        }
        
        return try {
            // Проверяем, установлено ли приложение
            packageManager.getApplicationInfo(packageName, 0)
            Timber.d("LaunchAppUseCase: App $packageName is installed")
            
            // Пытаемся получить launcher intent
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            
            if (launchIntent == null) {
                Timber.w("LaunchAppUseCase: No launcher activity found for $packageName")
                return Result.NoLauncherActivity
            }

            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            
            Timber.i("LaunchAppUseCase: Successfully launched app $packageName")
            Result.Success
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e, "LaunchAppUseCase: App not found: $packageName")
            Result.AppNotFound(packageName)
        } catch (e: Exception) {
            Timber.e(e, "LaunchAppUseCase: Failed to launch app $packageName")
            Result.Error(
                e.message ?: context.getString(R.string.single_app_screen_launch_error_unknown)
            )
        }
    }
}
