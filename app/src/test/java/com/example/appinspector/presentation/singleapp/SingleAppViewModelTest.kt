package com.example.appinspector.presentation.singleapp

import androidx.lifecycle.SavedStateHandle
import com.example.appinspector.R
import com.example.appinspector.data.repository.AppDetailsRepository
import com.example.appinspector.data.util.ResourceProvider
import com.example.appinspector.domain.usecase.LaunchAppUseCase
import com.example.appinspector.presentation.common.AppError
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class SingleAppViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val appDetailsRepository: AppDetailsRepository = mockk()
    private val launchAppUseCase: LaunchAppUseCase = mockk()
    private val resourceProvider: ResourceProvider = FakeResourceProvider()

    @Test
    fun load_whenRepositoryReturnsNull_setsAppNotFoundAndEmitsLoadingErrorEvent() = runTest {
        coEvery { appDetailsRepository.getDetails("com.test.app") } returns null

        val viewModel = createViewModel()
        val events = mutableListOf<SingleAppEvent>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events += it }
        }
        runCurrent()

        viewModel.send(SingleAppIntent.Load("com.test.app"))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.error is AppError.AppNotFound)
        val loadingError = events.filterIsInstance<SingleAppEvent.LoadingError>().firstOrNull()
        assertEquals("Приложение com.test.app не найдено", loadingError?.message)
        job.cancel()
    }

    @Test
    fun launchApp_whenPackageNameIsBlank_emitsLaunchFailedForInvalidAction() = runTest {
        val viewModel = createViewModel()
        val events = mutableListOf<SingleAppEvent>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events += it }
        }
        runCurrent()

        viewModel.send(SingleAppIntent.LaunchApp)
        advanceUntilIdle()

        val launchFailed = events.filterIsInstance<SingleAppEvent.LaunchFailed>().firstOrNull()
        assertEquals("Package name пуст", launchFailed?.reason)
        job.cancel()
    }

    @Test
    fun launchApp_afterLoadingError_emitsLaunchFailedWhenUseCaseReturnsError() = runTest {
        coEvery { appDetailsRepository.getDetails("com.test.app") } returns null
        every { launchAppUseCase.invoke("com.test.app") } returns LaunchAppUseCase.Result.Error("invalid action")

        val viewModel = createViewModel()
        val events = mutableListOf<SingleAppEvent>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events += it }
        }
        runCurrent()

        viewModel.send(SingleAppIntent.Load("com.test.app"))
        advanceUntilIdle()
        viewModel.send(SingleAppIntent.LaunchApp)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.error is AppError.AppNotFound)
        val launchFailed = events.filterIsInstance<SingleAppEvent.LaunchFailed>().lastOrNull()
        assertEquals("invalid action", launchFailed?.reason)
        job.cancel()
    }

    @Test
    fun retry_afterInitialError_usesLastPackageNameAndEmitsLoadingErrorAgain() = runTest {
        coEvery { appDetailsRepository.getDetails("com.retry.app") } returns null

        val viewModel = createViewModel()
        val events = mutableListOf<SingleAppEvent>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events += it }
        }
        runCurrent()

        viewModel.send(SingleAppIntent.Load("com.retry.app"))
        advanceUntilIdle()
        viewModel.send(SingleAppIntent.Retry)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.error is AppError.AppNotFound)
        assertEquals("com.retry.app", viewModel.state.value.packageName)
        assertEquals(2, events.filterIsInstance<SingleAppEvent.LoadingError>().size)
        job.cancel()
    }

    @Test
    fun load_savesPackageNameToSavedStateHandle() = runTest {
        val savedStateHandle = SavedStateHandle()
        coEvery { appDetailsRepository.getDetails("com.saved.state") } returns null
        val viewModel = createViewModel(savedStateHandle = savedStateHandle)

        viewModel.send(SingleAppIntent.Load("com.saved.state"))
        advanceUntilIdle()

        assertEquals("com.saved.state", savedStateHandle.get<String>("packageName"))
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): SingleAppViewModel {
        return SingleAppViewModel(
            savedStateHandle = savedStateHandle,
            appDetailsRepository = appDetailsRepository,
            launchAppUseCase = launchAppUseCase,
            resourceProvider = resourceProvider,
        )
    }
}

private class FakeResourceProvider : ResourceProvider {
    override fun getString(resId: Int): String = when (resId) {
        R.string.single_app_screen_launch_error_empty_package -> "Package name пуст"
        R.string.single_app_screen_error_unknown -> "Неизвестная ошибка"
        R.string.single_app_screen_launch_error_no_launcher -> "У приложения нет launcher activity"
        R.string.single_app_screen_launch_error_not_found -> "Приложение не найдено"
        R.string.app_error_not_found -> "Приложение %1\$s не найдено"
        else -> "unknown"
    }

    override fun getString(resId: Int, vararg formatArgs: Any): String {
        return when (resId) {
            R.string.app_error_not_found -> "Приложение ${formatArgs.firstOrNull()} не найдено"
            else -> getString(resId)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
