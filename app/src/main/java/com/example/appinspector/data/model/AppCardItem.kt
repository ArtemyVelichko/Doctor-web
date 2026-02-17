package com.example.appinspector.data.model

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.Painter

/**
 * Данные для карточки приложения.
 */
@Stable
data class AppCardItem(
    val name: String,
    val icon: Painter? = null,
    val subtitle: String? = null,
)
