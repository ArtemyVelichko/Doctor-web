package com.example.appinspector.data.util

import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Retry policy для повторных попыток при ошибках.
 */
data class RetryPolicy(
    val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    val initialDelay: Duration = DEFAULT_INITIAL_DELAY,
    val maxDelay: Duration = DEFAULT_MAX_DELAY,
    val factor: Double = DEFAULT_BACKOFF_FACTOR,
) {
    companion object {
        const val DEFAULT_MAX_ATTEMPTS = 3
        val DEFAULT_INITIAL_DELAY: Duration = 1.seconds
        val DEFAULT_MAX_DELAY: Duration = 10.seconds
        const val DEFAULT_BACKOFF_FACTOR = 2.0
    }

    /**
     * Вычисляет задержку для текущей попытки (exponential backoff).
     * attempt = 0 -> initialDelay
     * attempt = 1 -> initialDelay * factor
     * attempt = 2 -> initialDelay * factor^2
     */
    fun delayForAttempt(attempt: Int): Duration {
        if (attempt == 0) return Duration.ZERO
        val delay = initialDelay * factor.pow(attempt - 1)
        return minOf(delay, maxDelay)
    }

    private fun Double.pow(n: Int): Double = 
        (1..n).fold(1.0) { acc, _ -> acc * this }
}

/**
 * Выполняет блок с retry policy.
 * 
 * @param policy Политика повторных попыток
 * @param onRetry Callback при каждой попытке (передается номер попытки)
 * @param block Блок кода для выполнения
 * @return Результат выполнения блока
 * @throws Exception Последнее исключение, если все попытки исчерпаны
 */
suspend fun <T> withRetry(
    policy: RetryPolicy,
    onRetry: (attempt: Int, maxAttempts: Int) -> Unit = { _, _ -> },
    block: suspend () -> T,
): T {
    var attempt = 0

    while (true) {
        try {
            onRetry(attempt + 1, policy.maxAttempts)
            return block()
        } catch (e: Exception) {
            attempt++

            // Если это последняя попытка, выбрасываем исходное исключение
            if (attempt >= policy.maxAttempts) {
                throw e
            }

            // Ждем перед следующей попыткой
            val delayDuration = policy.delayForAttempt(attempt)
            if (delayDuration > Duration.ZERO) {
                delay(delayDuration)
            }
        }
    }
}
