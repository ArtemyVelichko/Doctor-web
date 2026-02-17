package com.example.appinspector.data.repository

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.appinspector.data.model.AppCardItem
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AllAppsRepositoryImplTest {

    private val packageManager: PackageManager = mockk()
    private val ioDispatcher = UnconfinedTestDispatcher()

    @Test
    fun getAllApps_sortsByNameAndMapsPackageName() = runTest {
        val appBeta = ApplicationInfo().apply { packageName = "com.example.beta" }
        val appAlpha = ApplicationInfo().apply { packageName = "com.example.alpha" }
        val appGamma = ApplicationInfo().apply { packageName = "com.example.gamma" }

        every {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        } returns listOf(appBeta, appGamma, appAlpha)

        every { packageManager.getApplicationLabel(appBeta) } returns "beta"
        every { packageManager.getApplicationLabel(appAlpha) } returns "Alpha"
        every { packageManager.getApplicationLabel(appGamma) } returns "gamma"

        val repository = AllAppsRepositoryImpl(
            ioDispatcher = ioDispatcher,
            packageManager = packageManager,
        )

        val result = repository.getAllApps()

        assertEquals(
            listOf("Alpha", "beta", "gamma"),
            result.map(AppCardItem::name),
        )
        assertEquals(
            listOf("com.example.alpha", "com.example.beta", "com.example.gamma"),
            result.map { it.subtitle },
        )
    }

    @Test
    fun getAllApps_keepsDuplicateNamesWithDifferentPackages() = runTest {
        val firstDup = ApplicationInfo().apply { packageName = "com.example.main.one" }
        val secondDup = ApplicationInfo().apply { packageName = "com.example.main.two" }

        every {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        } returns listOf(firstDup, secondDup)

        every { packageManager.getApplicationLabel(firstDup) } returns "Main components"
        every { packageManager.getApplicationLabel(secondDup) } returns "Main components"

        val repository = AllAppsRepositoryImpl(
            ioDispatcher = ioDispatcher,
            packageManager = packageManager,
        )

        val result = repository.getAllApps()
        val duplicates = result.filter { it.name == "Main components" }

        assertEquals(2, duplicates.size)
        assertTrue(duplicates.any { it.subtitle == "com.example.main.one" })
        assertTrue(duplicates.any { it.subtitle == "com.example.main.two" })
    }
}
