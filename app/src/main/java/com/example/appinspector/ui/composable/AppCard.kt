package com.example.appinspector.ui.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import com.example.appinspector.R
import com.example.appinspector.data.model.AppCardItem

/**
 * Карточка приложения: иконка (или дефолтная) + имя (+ опционально subtitle).
 */
@Composable
fun AppCard(
    item: AppCardItem,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val iconPadding = dimensionResource(R.dimen.app_card_inner_padding)
    val contentSpacing = dimensionResource(R.dimen.app_card_content_spacing)
    val iconSize = dimensionResource(R.dimen.app_card_icon_size)
    val appIcon = rememberAppIconPainter(item.subtitle)
    val painter = item.icon ?: appIcon ?: painterResource(R.drawable.ic_default_app_icon)

    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(item.subtitle ?: item.name),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E9),
            contentColor = Color(0xFF212121),
        ),
    ) {
        Row(
            modifier = Modifier.padding(iconPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(contentSpacing),
        ) {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                contentScale = ContentScale.Fit,
            )
            Column {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                item.subtitle?.let { sub ->
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF212121),
                    )
                }
            }
        }
    }
}
