package com.example.appinspector.presentation.applist

import com.example.appinspector.R
import com.example.appinspector.data.model.AppCardItem
import com.example.appinspector.data.repository.AllAppsRepository
import com.example.appinspector.data.util.ResourceProvider
import com.example.appinspector.presentation.common.AppError
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class AppsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val allAppsRepository: AllAppsRepository = mockk()
    private val resourceProvider: ResourceProvider = FakeAppListResourceProvider()

    @Test
    fun init_whenRepositoryThrowsSecurityException_setsPermissionDeniedError() = runTest {
        coEvery { allAppsRepository.getAllApps() } throws SecurityException("denied")
        val viewModel = createViewModel()

        advanceUntilIdle()

        assertTrue(viewModel.state.value.error is AppError.PermissionDenied)
        val error = viewModel.state.value.error as AppError.PermissionDenied
        assertEquals("READ_APPS", error.permission)
        assertEquals("denied", error.details)
    }

    @Test
    fun load_whenRepositoryReturnsDuplicateNames_keepsAllDuplicatesInState() = runTest {
        val duplicateName = "Main components"
        coEvery { allAppsRepository.getAllApps() } returns persistentListOf(
            AppCardItem(name = duplicateName, subtitle = "com.example.dup.one"),
            AppCardItem(name = duplicateName, subtitle = "com.example.dup.two"),
            AppCardItem(name = "Alpha", subtitle = "com.example.alpha"),
        )
        val viewModel = createViewModel()

        advanceUntilIdle()

        val items = viewModel.state.value.items
        val duplicates = items.filter { it.name == duplicateName }

        assertEquals(3, items.size)
        assertEquals(2, duplicates.size)
        assertTrue(duplicates.any { it.subtitle == "com.example.dup.one" })
        assertTrue(duplicates.any { it.subtitle == "com.example.dup.two" })
        assertEquals(null, viewModel.state.value.error)
    }

    private fun createViewModel(): AppsViewModel {
        return AppsViewModel(
            allAppsRepository = allAppsRepository,
            resourceProvider = resourceProvider,
        )
    }
}

private class FakeAppListResourceProvider : ResourceProvider {
    override fun getString(resId: Int): String = when (resId) {
        R.string.app_list_error_loading -> "Ошибка загрузки приложений"
        else -> "unknown"
    }

    override fun getString(resId: Int, vararg formatArgs: Any): String = getString(resId)
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
