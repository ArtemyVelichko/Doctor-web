package com.example.appinspector.presentation.applist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.appinspector.R
import com.example.appinspector.data.model.AppCardItem
import com.example.appinspector.data.util.ResourceProvider
import com.example.appinspector.data.util.ResourceProviderImpl
import com.example.appinspector.presentation.common.toDisplayMessage
import com.example.appinspector.ui.composable.CategorizedAppCardList
import com.example.appinspector.ui.theme.AppInspectorTheme
import kotlinx.collections.immutable.persistentListOf

/**
 * Экран со списком приложений (MVI presentation слой).
 * Подписывается на ViewModel, показывает загрузку/ошибки/данные.
 */
@Composable
fun AppListScreen(
    viewModel: AppsViewModel,
    onAppClick: (AppCardItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val resourceProvider: ResourceProvider = remember { ResourceProviderImpl(context) }
    val listBackground = colorResource(R.color.app_list_background)
    val listContent = colorResource(R.color.app_list_content)

    Surface(
        modifier = modifier,
        color = listBackground,
        contentColor = listContent,
    ) {
        when {
            state.isLoading && state.items.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.padding(dimensionResource(R.dimen.app_list_error_padding)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.app_list_error_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.app_list_error_title_spacing)))
                    Text(
                        text = state.error!!.toDisplayMessage(resourceProvider),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.app_list_error_button_spacing)))
                    FilledTonalButton(
                        onClick = { viewModel.send(AppListIntent.Retry) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.padding(end = dimensionResource(R.dimen.app_list_error_icon_end_padding)),
                        )
                        Text(stringResource(R.string.app_list_retry_button))
                    }
                }
            }
            else -> CategorizedAppCardList(
                items = state.items,
                onItemClick = onAppClick,
            )
        }
    }
}

/** Для превью без ViewModel. С захардкоженными данными */
private fun previewSampleList(): kotlinx.collections.immutable.ImmutableList<AppCardItem> =
    persistentListOf(
        AppCardItem(name = "Chrome", subtitle = "com.android.chrome"),
        AppCardItem(name = "Gmail", subtitle = "com.google.android.gm"),
        AppCardItem(name = "Maps", subtitle = "com.google.android.apps.maps"),
    )

@Preview(showBackground = true)
@Composable
private fun AppListScreenPreview() {
    AppInspectorTheme {
        Surface {
            CategorizedAppCardList(items = remember { previewSampleList() })
        }
    }
}
