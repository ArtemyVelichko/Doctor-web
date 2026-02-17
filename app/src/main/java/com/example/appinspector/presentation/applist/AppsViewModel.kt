package com.example.appinspector.presentation.applist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appinspector.R
import com.example.appinspector.data.repository.AllAppsRepository
import com.example.appinspector.data.util.ResourceProvider
import com.example.appinspector.presentation.common.AppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для экрана со списком — отображение всех приложений.
 */
@HiltViewModel
class AppsViewModel @Inject constructor(
    private val allAppsRepository: AllAppsRepository,
    private val resourceProvider: ResourceProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(AppListState())
    val state: StateFlow<AppListState> = _state.asStateFlow()

    init {
        send(AppListIntent.Load)
    }

    fun send(intent: AppListIntent) {
        when (intent) {
            is AppListIntent.Load,
            is AppListIntent.Refresh,
            is AppListIntent.Retry -> {
                viewModelScope.launch {
                    reduce(AppListEvent.LoadingStarted)
                    loadApps()
                }
            }
        }
    }

    /** Reducer — атомарно изменяет state через MutableStateFlow.update. */
    private  fun reduce(event: AppListEvent) {
        _state.update { currentState ->
            when (event) {
                is AppListEvent.LoadingStarted -> currentState.copy(
                    isLoading = true,
                    error = null
                )
                is AppListEvent.LoadingSuccess -> currentState.copy(
                    isLoading = false,
                    items = event.items,
                    error = null
                )
                is AppListEvent.LoadingError -> currentState.copy(
                    isLoading = false,
                    error = event.error
                )
            }
        }
    }

    private suspend fun loadApps() {
        try {
            val items = allAppsRepository.getAllApps()
            reduce(AppListEvent.LoadingSuccess(items))
        } catch (e: SecurityException) {
            reduce(AppListEvent.LoadingError(
                AppError.PermissionDenied(
                    permission = "READ_APPS",
                    details = e.message
                )
            ))
        } catch (e: Exception) {
            reduce(AppListEvent.LoadingError(
                AppError.Unknown(
                    message = e.message ?: resourceProvider.getString(R.string.app_list_error_loading),
                    cause = e
                )
            ))
        }
    }
}
