package com.example.appinspector.ui.composable

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import timber.log.Timber

private const val ICON_SIZE_PX = 128

/**
 * Рисует иконку по [packageName] через PackageManager.
 */
@Composable
fun rememberAppIconPainter(packageName: String?): Painter? {
    val context = LocalContext.current

    if (packageName.isNullOrBlank()) return null

    return remember(packageName) {
        try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            drawable.toPainter()
        } catch (e: Exception) {
            Timber.w("AppIconPainter: Failed to load icon for package=$packageName - ${e.message}")
            null
        }
    }
}

private fun Drawable.toPainter(): Painter {
    val bitmap = when (this) {
        is BitmapDrawable -> bitmap
        else -> Bitmap.createBitmap(ICON_SIZE_PX, ICON_SIZE_PX, Bitmap.Config.ARGB_8888).also { bmp ->
            setBounds(0, 0, bmp.width, bmp.height)
            draw(Canvas(bmp))
        }
    }
    return BitmapPainter(bitmap.asImageBitmap())
}
