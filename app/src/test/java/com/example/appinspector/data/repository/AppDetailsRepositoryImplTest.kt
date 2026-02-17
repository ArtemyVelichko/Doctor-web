package com.example.appinspector.data.repository

import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.security.MessageDigest

@OptIn(ExperimentalCoroutinesApi::class)
class AppDetailsRepositoryImplTest {

    private val packageManager: PackageManager = mockk(relaxed = true)
    private val ioDispatcher = UnconfinedTestDispatcher()

    @Test
    fun computeSha256_returnsExpectedHashForKnownContent() = runTest {
        val apkFile = File.createTempFile("appinspector-test", ".apk")
        apkFile.writeText("doctor-web-test-apk-content")

        val repository = createRepository()
        val actualHash = invokeComputeSha256(repository, apkFile)
        val expectedHash = sha256(apkFile)

        assertEquals(expectedHash, actualHash)
        apkFile.delete()
    }

    @Test
    fun getDetails_returnsNullWhenAppNotFound() = runTest {
        val packageName = "com.example.missing"
        every { packageManager.getApplicationInfo(packageName, 0) } throws
            PackageManager.NameNotFoundException("missing")

        val repository = AppDetailsRepositoryImpl(
            ioDispatcher = ioDispatcher,
            packageManager = packageManager,
        )

        val details = repository.getDetails(packageName)
        assertEquals(null, details)
    }

    @Test
    fun computeSha256_returnsExpectedHashForLargeFile_moreThanOneBuffer() = runTest {
        val apkFile = File.createTempFile("appinspector-large", ".apk")
        val largeContent = buildString {
            repeat(20_000) { append('a' + (it % 26)) }
        }
        apkFile.writeText(largeContent)

        val repository = createRepository()
        val actualHash = invokeComputeSha256(repository, apkFile)
        val expectedHash = sha256(apkFile)

        assertEquals(expectedHash, actualHash)
        apkFile.delete()
    }

    @Test
    fun computeSha256_returnsKnownValueForEmptyFile() = runTest {
        val apkFile = File.createTempFile("appinspector-empty", ".apk")
        apkFile.writeBytes(byteArrayOf())

        val repository = createRepository()
        val actualHash = invokeComputeSha256(repository, apkFile)
        val expectedHash = sha256(apkFile)

        assertEquals(expectedHash, actualHash)
        apkFile.delete()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun createRepository(): AppDetailsRepositoryImpl {
        return AppDetailsRepositoryImpl(
            ioDispatcher = ioDispatcher,
            packageManager = packageManager,
        )
    }

    private fun invokeComputeSha256(repository: AppDetailsRepositoryImpl, file: File): String {
        val method = AppDetailsRepositoryImpl::class.java.getDeclaredMethod(
            "computeSha256",
            File::class.java,
        )
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(repository, file) as String
    }
}
