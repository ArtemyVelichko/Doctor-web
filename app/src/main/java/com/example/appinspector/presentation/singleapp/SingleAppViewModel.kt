package com.example.appinspector.presentation.singleapp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appinspector.R
import com.example.appinspector.data.repository.AppDetailsRepository
import com.example.appinspector.data.util.ResourceProvider
import com.example.appinspector.domain.usecase.LaunchAppUseCase
import com.example.appinspector.presentation.common.AppError
import com.example.appinspector.presentation.common.toDisplayMessage
import com.example.appinspector.presentation.common.toLogMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel экрана деталей приложения: название, версия, пакет, контрольная сумма APK, системное/нет.
 */
@HiltViewModel
class SingleAppViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val appDetailsRepository: AppDetailsRepository,
    private val launchAppUseCase: LaunchAppUseCase,
    private val resourceProvider: ResourceProvider,
) : ViewModel() {
    companion object {
        private const val SAVED_PACKAGE_NAME_KEY = "packageName"
    }

    private val _state = MutableStateFlow(SingleAppState())
    val state: StateFlow<SingleAppState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SingleAppEvent>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val events: SharedFlow<SingleAppEvent> = _events.asSharedFlow()

    private fun sendEvent(event: SingleAppEvent) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }

    private suspend fun emitEvent(event: SingleAppEvent) {
        _events.emit(event)
    }

    init {
        savedStateHandle.get<String>(SAVED_PACKAGE_NAME_KEY)?.let {
            send(SingleAppIntent.Load(it))
        }
    }

    fun send(intent: SingleAppIntent) {
        when (intent) {
            is SingleAppIntent.Load -> {
                viewModelScope.launch {
                    reduce(SingleAppStateEvent.LoadingStarted(intent.packageName))
                    loadApp(intent.packageName)
                }
            }
            is SingleAppIntent.Retry -> {
                viewModelScope.launch {
                    val packageName = _state.value.packageName
                    reduce(SingleAppStateEvent.LoadingStarted(packageName))
                    loadApp(packageName)
                }
            }
            is SingleAppIntent.LaunchApp -> launchApp()
        }
    }

    /** Reducer — атомарно изменяет state через MutableStateFlow.update. */
    private fun reduce(event: SingleAppStateEvent) {
        if (event is SingleAppStateEvent.LoadingStarted) {
            savedStateHandle[SAVED_PACKAGE_NAME_KEY] = event.packageName
        }
        _state.update { currentState ->
            when (event) {
                is SingleAppStateEvent.LoadingStarted -> currentState.copy(
                    isLoading = true,
                    error = null,
                    packageName = event.packageName
                )
                is SingleAppStateEvent.LoadingSuccess -> currentState.copy(
                    isLoading = false,
                    label = event.details.label,
                    versionName = event.details.versionName,
                    versionCode = event.details.versionCode,
                    apkChecksumSha256 = event.details.apkChecksumSha256,
                    isSystemApp = event.details.isSystemApp,
                    hasLauncherActivity = event.details.hasLauncherActivity,
                    error = null
                )
                is SingleAppStateEvent.LoadingError -> currentState.copy(
                    isLoading = false,
                    error = event.error
                )
            }
        }
    }
    
    /**
     * Side-effect — выполняет асинхронную загрузку деталей приложения.
     * Все изменения state происходят через reducer.
     */
    private suspend fun loadApp(packageName: String) {
        Timber.d("SingleAppViewModel: Loading app details for package=$packageName")
        
        try {
            val details = appDetailsRepository.getDetails(packageName)
            if (details != null) {
                Timber.d("SingleAppViewModel: Successfully loaded app details for $packageName")
                reduce(SingleAppStateEvent.LoadingSuccess(details))
            } else {
                val error = AppError.AppNotFound(packageName)
                Timber.w("SingleAppViewModel: App not found - ${error.toLogMessage()}")
                reduce(SingleAppStateEvent.LoadingError(error))
                emitEvent(
                    SingleAppEvent.LoadingError(
                        error.toDisplayMessage(resourceProvider)
                    )
                )
            }
        } catch (e: SecurityException) {
            val error = AppError.PermissionDenied(
                permission = "READ_APP_DETAILS",
                details = e.message
            )
            Timber.e("SingleAppViewModel: Permission denied - ${error.toLogMessage()}")
            reduce(SingleAppStateEvent.LoadingError(error))
            emitEvent(
                SingleAppEvent.LoadingError(
                    error.toDisplayMessage(resourceProvider)
                )
            )
        } catch (e: Exception) {
            val error = AppError.Unknown(
                message = e.message ?: resourceProvider.getString(R.string.single_app_screen_error_unknown),
                cause = e
            )
            Timber.e(e, "SingleAppViewModel: Failed to load app details - ${error.toLogMessage()}")
            reduce(SingleAppStateEvent.LoadingError(error))
            emitEvent(
                SingleAppEvent.LoadingError(
                    error.toDisplayMessage(resourceProvider)
                )
            )
        }
    }
    
    /**
     * Side-effect — запускает приложение через UseCase.
     * Результат отправляется как одноразовое событие (не state).
     */
    private fun launchApp() {
        val packageName = _state.value.packageName
        if (packageName.isBlank()) {
            sendEvent(
                SingleAppEvent.LaunchFailed(
                    resourceProvider.getString(R.string.single_app_screen_launch_error_empty_package)
                )
            )
            return
        }
        
        when (val result = launchAppUseCase(packageName)) {
            is LaunchAppUseCase.Result.Success -> {
                sendEvent(SingleAppEvent.AppLaunched)
            }
            is LaunchAppUseCase.Result.NoLauncherActivity -> {
                sendEvent(
                    SingleAppEvent.LaunchFailed(
                        resourceProvider.getString(R.string.single_app_screen_launch_error_no_launcher)
                    )
                )
            }
            is LaunchAppUseCase.Result.AppNotFound -> {
                sendEvent(
                    SingleAppEvent.LaunchFailed(
                        resourceProvider.getString(R.string.single_app_screen_launch_error_not_found)
                    )
                )
            }
            is LaunchAppUseCase.Result.Error -> {
                sendEvent(SingleAppEvent.LaunchFailed(result.message))
            }
        }
    }
}
