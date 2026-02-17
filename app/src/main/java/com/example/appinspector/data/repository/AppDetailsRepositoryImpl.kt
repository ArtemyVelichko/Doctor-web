package com.example.appinspector.data.repository

import android.content.Intent
import android.os.Build
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.appinspector.data.model.AppDetails
import com.example.appinspector.data.util.RetryPolicy
import com.example.appinspector.data.util.withRetry
import com.example.appinspector.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class AppDetailsRepositoryImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val packageManager: PackageManager,
) : AppDetailsRepository {
    companion object {
        private const val PACKAGE_MANAGER_FLAGS_NONE = 0
        private const val SHA_RETRY_MAX_ATTEMPTS = 2
        private const val SHA_RETRY_INITIAL_DELAY_MS = 200L
        private const val SHA_RETRY_MAX_DELAY_MS = 500L
        private const val SHA_BUFFER_SIZE_BYTES = 8192
        private const val SHA_ALGORITHM = "SHA-256"
    }

    private val shaRetryPolicy = RetryPolicy(
        maxAttempts = SHA_RETRY_MAX_ATTEMPTS,
        initialDelay = SHA_RETRY_INITIAL_DELAY_MS.milliseconds,
        maxDelay = SHA_RETRY_MAX_DELAY_MS.milliseconds,
    )

    override suspend fun getDetails(packageName: String): AppDetails? = withContext(ioDispatcher) {
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, PACKAGE_MANAGER_FLAGS_NONE)
            val packageInfo = packageManager.getPackageInfo(packageName, PACKAGE_MANAGER_FLAGS_NONE)
            val label = packageManager.getApplicationLabel(appInfo).toString()
            val isSystemApp =
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != PACKAGE_MANAGER_FLAGS_NONE
            val sha256 = appInfo.sourceDir?.let { path ->
                withRetry(
                    policy = shaRetryPolicy,
                    onRetry = { attempt, maxAttempts ->
                        Timber.w(
                            "AppDetailsRepositoryImpl: retry SHA-256 for %s (%d/%d)",
                            packageName,
                            attempt,
                            maxAttempts,
                        )
                    },
                ) {
                    computeSha256(File(path))
                }
            }
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            val hasLauncherActivity = checkHasLauncherActivity(packageName)
            
            AppDetails(
                packageName = packageName,
                label = label,
                versionName = packageInfo.versionName,
                versionCode = versionCode,
                isSystemApp = isSystemApp,
                apkChecksumSha256 = sha256,
                hasLauncherActivity = hasLauncherActivity,
            )
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e, "AppDetailsRepositoryImpl: App not found - package=$packageName")
            null
        } catch (e: Exception) {
            Timber.e(e, "AppDetailsRepositoryImpl: Failed to load app details for package=$packageName")
            null
        }
    }
    
    /**
     * Проверяет, можно ли запустить приложение
     */
    private fun checkHasLauncherActivity(packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(packageName)
        }
        val activities = packageManager.queryIntentActivities(intent, PACKAGE_MANAGER_FLAGS_NONE)
        return activities.isNotEmpty()
    }

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance(SHA_ALGORITHM)
        file.inputStream().use { input ->
            val buffer = ByteArray(SHA_BUFFER_SIZE_BYTES)
            var read: Int
            while (input.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
