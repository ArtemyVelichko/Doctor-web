# Retry Policy Architecture Guide

## 🎯 Где должен быть retry policy?

### ✅ **Repository / Data Layer** (рекомендуется)

Retry policy должен находиться в **репозитории**, потому что:

1. **Separation of Concerns** - ViewModel не должна знать о деталях retry
2. **Переиспользуемость** - retry работает для всех, кто использует репозиторий
3. **Single Responsibility** - Repository отвечает за **надежное** получение данных
4. **Чистая архитектура** - бизнес-логика не в презентационном слое

### ❌ **ViewModel** (только в исключительных случаях)

Retry в ViewModel допустим только если:
- Нужен специфичный **UI feedback** (например, "Попытка 2 из 3")
- Разные экраны требуют **разные retry policies**
- Нужна **отмена retry** при уходе с экрана

---

## 📋 Когда НЕ нужен retry?

**Локальные источники данных:**
- `PackageManager` - всегда доступен
- `SharedPreferences` - локальное хранилище
- `Room Database` - локальная БД
- Файловая система
- In-memory данные

**Причина:** Нет временных сбоев, данные всегда доступны.

---

## 🔥 Когда НУЖЕН retry?

**Сетевые запросы:**
- REST API
- GraphQL
- WebSocket
- Загрузка файлов
- Remote database

**Причина:** Возможны временные сбои сети, таймауты, перегрузка сервера.

---

## 🏗️ Правильная архитектура

### Вариант 1: Retry в Repository

```kotlin
class NetworkRepositoryImpl @Inject constructor(
    private val ioDispatcher: CoroutineDispatcher,
    private val apiService: ApiService,
) : NetworkRepository {

    private val retryPolicy = RetryPolicy(
        maxAttempts = 3,
        initialDelay = 1.seconds,
        maxDelay = 10.seconds,
        factor = 2.0
    )

    override suspend fun fetchData(): Result<Data> = withContext(ioDispatcher) {
        withRetry(policy = retryPolicy) {
            apiService.fetchData()
        }
    }
}
```

**ViewModel остается простой:**

```kotlin
class MyViewModel @Inject constructor(
    private val repository: NetworkRepository
) : ViewModel() {

    fun send(intent: Intent) {
        when (intent) {
            Load -> {
                reduce(LoadingStarted)
                viewModelScope.launch {
                    loadData()  // Retry внутри репозитория
                }
            }
        }
    }

    private suspend fun loadData() {
        try {
            val data = repository.fetchData()
            reduce(LoadingSuccess(data))
        } catch (e: Exception) {
            reduce(LoadingError(e.message))
        }
    }
}
```

### Вариант 2: Retry на уровне HTTP клиента (лучший подход)

```kotlin
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val initialDelay: Long = 1000L
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var exception: IOException? = null

        repeat(maxRetries) { attempt ->
            try {
                response = chain.proceed(request)
                if (response?.isSuccessful == true) {
                    return response!!
                }
            } catch (e: IOException) {
                exception = e
                if (attempt == maxRetries - 1) throw e
                
                // Exponential backoff
                val delay = initialDelay * (1 shl attempt)
                Thread.sleep(delay)
            }
        }
        
        return response ?: throw exception!!
    }
}

// В Hilt модуле:
@Provides
@Singleton
fun provideOkHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor(RetryInterceptor(maxRetries = 3))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
}
```

**Преимущества:**
- ✅ Retry для **всех** сетевых запросов автоматически
- ✅ Не нужно добавлять retry в каждый репозиторий
- ✅ Централизованная конфигурация
- ✅ HTTP-специфичная логика (retry только для 5xx ошибок, не для 4xx)

---

## 📊 Сравнение подходов

| Подход | Переиспользуемость | Простота | Гибкость | Рекомендация |
|--------|-------------------|----------|----------|--------------|
| **Interceptor** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | **Лучший** |
| **Repository** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | Хороший |
| **ViewModel** | ⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ | Редко |

---

## 🎯 Наша архитектура

В проекте **AppInspector**:

- `AllAppsRepository` - использует `PackageManager` (локальные данные)
  - ❌ **Retry НЕ нужен**
  - Данные всегда доступны

- Если добавим API для проверки приложений на вирусы:
  - ✅ **Retry нужен**
  - Реализация в `VirusScanRepository` или через `RetryInterceptor`

---

## 💡 Best Practices

1. **Exponential backoff** - увеличивайте задержку между попытками
2. **Max delay** - ограничивайте максимальную задержку
3. **Jitter** - добавляйте случайность, чтобы избежать "thundering herd"
4. **Idempotency** - убедитесь, что операции идемпотентны
5. **Monitoring** - логируйте retry попытки для анализа

---

## 📚 Используемые файлы

- `data/util/RetryPolicy.kt` - переиспользуемая retry логика
- `data/repository/NetworkRepositoryExample.kt` - пример использования
- `presentation/applist/AppsViewModel.kt` - чистая ViewModel без retry
