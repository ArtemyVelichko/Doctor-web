package com.example.appinspector.presentation.singleapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.appinspector.R
import com.example.appinspector.data.util.ResourceProvider
import com.example.appinspector.data.util.ResourceProviderImpl
import com.example.appinspector.presentation.common.AppError
import com.example.appinspector.presentation.common.toDisplayMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/**
 * Экран с подробной информацией о приложении: название, версия, пакет, контрольная сумма APK,
 * признак системного приложения, кнопка «Открыть приложение».
 */
@Composable
fun SingleAppScreen(
    viewModel: SingleAppViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    showBackButton: Boolean = false,
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    HandleSingleAppEvents(
        events = viewModel.events,
        snackbarHostState = snackbarHostState
    )

    Scaffold(
        topBar = {
            if (showBackButton) {
                SingleAppTopBar(onBack = onBack)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        SingleAppContent(
            state = state,
            onLaunchApp = { viewModel.send(SingleAppIntent.LaunchApp) },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

/**
 * Обработка одноразовых событий (launch app, ошибки).
 */
@Composable
private fun HandleSingleAppEvents(
    events: Flow<SingleAppEvent>,
    snackbarHostState: SnackbarHostState
) {
    LaunchedEffect(Unit) {
        events.collectLatest { event ->
            when (event) {
                is SingleAppEvent.AppLaunched -> {
                    // Опционально: можно показать snackbar об успешном запуске
                }
                is SingleAppEvent.LaunchFailed -> {
                    snackbarHostState.showSnackbar(
                        message = event.reason,
                        withDismissAction = true
                    )
                }
                is SingleAppEvent.LoadingError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        withDismissAction = true
                    )
                }
            }
        }
    }
}

/**
 * TopBar с кнопкой "Назад".
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SingleAppTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.single_app_screen_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.single_app_screen_back),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

/**
 * Основной контент с состояниями: Loading, Error, Content.
 */
@Composable
private fun SingleAppContent(
    state: SingleAppState,
    onLaunchApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resourceProvider: ResourceProvider = remember { ResourceProviderImpl(context) }
    
    when {
        state.isLoading && state.label.isEmpty() -> {
            LoadingState(modifier = modifier)
        }
        state.error != null -> {
            ErrorState(
                error = state.error,
                resourceProvider = resourceProvider,
                modifier = modifier
            )
        }
        else -> {
            AppDetailsContent(
                state = state,
                onLaunchApp = onLaunchApp,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(
    error: AppError,
    resourceProvider: ResourceProvider,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = error.toDisplayMessage(resourceProvider),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * Детальный контент приложения: заголовок, карточка с информацией, кнопка запуска.
 */
@Composable
private fun AppDetailsContent(
    state: SingleAppState,
    onLaunchApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppHeader(
            label = state.label,
            isSystemApp = state.isSystemApp
        )
        AppInfoCard(state = state)
        if (state.hasLauncherActivity) {
            LaunchAppButton(onClick = onLaunchApp)
        }
    }
}

/**
 * Заголовок приложения: название + чип (системное/пользовательское).
 */
@Composable
private fun AppHeader(
    label: String,
    isSystemApp: Boolean
) {
    Text(
        text = label,
        style = MaterialTheme.typography.headlineMedium
    )
    SuggestionChip(
        onClick = {},
        label = {
            Text(
                stringResource(
                    if (isSystemApp) R.string.single_app_screen_system_app
                    else R.string.single_app_screen_user_app
                )
            )
        }
    )
}

/**
 * Карточка с информацией о приложении: версия, пакет, контрольная сумма.
 */
@Composable
private fun AppInfoCard(state: SingleAppState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoRow(
                label = stringResource(R.string.single_app_screen_version_label),
                value = state.versionName ?: state.versionCode.toString()
            )
            InfoRow(
                label = stringResource(R.string.single_app_screen_package_label),
                value = state.packageName
            )
            state.apkChecksumSha256?.let { sha ->
                InfoRow(
                    label = stringResource(R.string.single_app_screen_checksum_label),
                    value = sha,
                    mono = true
                )
            }
        }
    }
}

/**
 * Кнопка запуска приложения.
 */
@Composable
private fun LaunchAppButton(onClick: () -> Unit) {
    Spacer(modifier = Modifier.height(8.dp))
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.single_app_screen_open_app_button))
    }
}

/**
 * Строка информации: label + value.
 * 
 * @param label Метка (например, "Версия")
 * @param value Значение (например, "1.0.0")
 * @param mono Использовать ли моноширинный шрифт для value
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    mono: Boolean = false,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (mono) {
                MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            } else {
                MaterialTheme.typography.bodyMedium
            }
        )
    }
}
