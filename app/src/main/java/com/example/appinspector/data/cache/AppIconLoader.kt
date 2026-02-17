package com.example.appinspector.data.cache

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import timber.log.Timber
import javax.inject.Inject
import androidx.core.graphics.createBitmap

private const val ICON_SIZE_PX = 128

interface AppIconLoader {
    fun loadIcon(packageName: String): Bitmap?
}

/** Загружает иконку через PackageManager и конвертирует Drawable → Bitmap. */
class AppIconLoaderImpl @Inject constructor(
    private val packageManager: android.content.pm.PackageManager,
) : AppIconLoader {

    override fun loadIcon(packageName: String): Bitmap? {
        return try {
            val drawable = packageManager.getApplicationIcon(packageName)
            drawable.toBitmap()
        } catch (e: Exception) {
            Timber.w("AppIconLoader: Failed to load icon for package=$packageName - ${e.message}")
            null
        }
    }
}

private fun Drawable.toBitmap(): Bitmap {
    return when (this) {
        is BitmapDrawable -> bitmap ?: createBitmap(ICON_SIZE_PX, ICON_SIZE_PX).also { bmp ->
            setBounds(0, 0, bmp.width, bmp.height)
            draw(Canvas(bmp))
        }
        else -> createBitmap(ICON_SIZE_PX, ICON_SIZE_PX).also { bmp ->
            setBounds(0, 0, bmp.width, bmp.height)
            draw(Canvas(bmp))
        }
    }
}
