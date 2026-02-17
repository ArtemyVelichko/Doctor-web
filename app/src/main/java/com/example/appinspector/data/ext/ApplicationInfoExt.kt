package com.example.appinspector.data.ext

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.appinspector.data.model.AppCardItem

/**
 * Маппинг [ApplicationInfo] в [AppCardItem].
 * Иконка не заполняется (нужен UI/Compose) — в UI подгружать по [AppCardItem.subtitle] (packageName).
 */
fun ApplicationInfo.toAppCardItem(packageManager: PackageManager): AppCardItem {
    val label = packageManager.getApplicationLabel(this).toString()
    return AppCardItem(
        name = label,
        icon = null,
        subtitle = packageName,
    )
}
