# Error Handling Architecture

## 🎯 Концепция

Вместо использования `String?` для ошибок, используется **sealed class `AppError`** для:

1. ✅ **Типобезопасности** - компилятор проверяет обработку всех типов ошибок
2. ✅ **Расширяемости** - легко добавлять новые типы ошибок
3. ✅ **Локализации** - сообщения для UI извлекаются из strings.xml
4. ✅ **Дополнительных данных** - каждая ошибка может содержать специфичные для нее данные
5. ✅ **Разной обработки** - UI может реагировать по-разному на разные типы ошибок

---

## 📁 Структура

```
presentation/common/
├── AppError.kt          // Sealed class с типами ошибок
└── AppErrorExt.kt       // Extension функции для конвертации в UI сообщения
```

---

## 🔥 Типы ошибок

### 1. **PermissionDenied** - нет прав доступа

```kotlin
AppError.PermissionDenied(
    permission = "READ_APPS",
    details = "Security exception details"
)
```

**Когда использовать:**
- SecurityException при чтении списка приложений
- Отсутствие необходимых permissions

**UI сообщение:** "Нет прав на чтение списка приложений"

### 2. **AppNotFound** - приложение не найдено

```kotlin
AppError.AppNotFound(
    packageName = "com.example.app"
)
```

**Когда использовать:**
- PackageManager.NameNotFoundException
- Приложение было удалено
- Неверный packageName

**UI сообщение:** "Приложение com.example.app не найдено"

### 3. **Unknown** - неизвестная ошибка

```kotlin
AppError.Unknown(
    message = "Something went wrong",
    cause = exception
)
```

**Когда использовать:**
- Любые другие Exception
- Непредвиденные ошибки

**UI сообщение:** message или "Неизвестная ошибка"

### 4. **Timeout** - превышено время ожидания

```kotlin
AppError.Timeout
```

**Когда использовать:**
- Операция выполняется слишком долго
- TimeoutException

**UI сообщение:** "Превышено время ожидания"

---

## 💻 Использование в коде

### В ViewModel:

```kotlin
private suspend fun loadApps() {
    try {
        val items = allAppsRepository.getAllApps()
        reduce(AppListEvent.LoadingSuccess(items))
    } catch (e: SecurityException) {
        // Специфичная обработка SecurityException
        reduce(AppListEvent.LoadingError(
            AppError.PermissionDenied(
                permission = "READ_APPS",
                details = e.message
            )
        ))
    } catch (e: Exception) {
        // Общая обработка всех остальных ошибок
        reduce(AppListEvent.LoadingError(
            AppError.Unknown(
                message = e.message,
                cause = e
            )
        ))
    }
}
```

### В State:

```kotlin
data class AppListState(
    val isLoading: Boolean = false,
    val items: ImmutableList<AppCardItem> = persistentListOf(),
    val error: AppError? = null,  // ✅ Типизированная ошибка
)
```

### В Event:

```kotlin
sealed interface AppListEvent {
    data object LoadingStarted
    data class LoadingSuccess(val items: ImmutableList<AppCardItem>)
    data class LoadingError(val error: AppError)  // ✅ Типизированная ошибка
}
```

### В UI (Composable):

```kotlin
@Composable
fun AppListScreen(viewModel: AppsViewModel) {
    val state by viewModel.state.collectAsState()
    val resourceProvider = remember { ResourceProviderImpl(context) }
    
    when {
        state.error != null -> {
            // Конвертируем AppError в String для UI
            val errorMessage = state.error!!.toDisplayMessage(resourceProvider)
            
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
```

---

## 🎨 Extension функции

### `toDisplayMessage()` - для UI

Конвертирует `AppError` в user-friendly сообщение:

```kotlin
fun AppError.toDisplayMessage(resourceProvider: ResourceProvider): String {
    return when (this) {
        is AppError.PermissionDenied -> 
            resourceProvider.getString(R.string.app_list_error_permission_denied)
        is AppError.AppNotFound -> 
            "Приложение $packageName не найдено"
        is AppError.Unknown -> 
            message ?: resourceProvider.getString(R.string.app_list_error_unknown)
        is AppError.Timeout -> 
            resourceProvider.getString(R.string.app_list_error_timeout)
    }
}
```

### `toLogMessage()` - для логирования

Конвертирует `AppError` в сообщение для логов:

```kotlin
fun AppError.toLogMessage(): String {
    return when (this) {
        is AppError.PermissionDenied -> "PermissionDenied: $permission - $details"
        is AppError.AppNotFound -> "AppNotFound: $packageName"
        is AppError.Unknown -> "Unknown: $message (${cause?.javaClass?.simpleName})"
        is AppError.Timeout -> "Timeout"
    }
}
```

**Использование:**

```kotlin
state.error?.let { error ->
    Log.e("AppListScreen", "Error occurred: ${error.toLogMessage()}")
}
```

---

## 📊 Сравнение: До и После

### ❌ **Было (String?):**

```kotlin
// State
data class AppListState(
    val error: String? = null  // ❌ Не типобезопасно
)

// ViewModel
catch (e: Exception) {
    reduce(LoadingError(e.message ?: "Ошибка"))  // ❌ Теряем тип ошибки
}

// UI
Text(text = state.error ?: "")  // ❌ Нет информации о типе ошибки
```

**Проблемы:**
- Нет типобезопасности
- Потеря информации о типе ошибки
- Сложно обрабатывать по-разному
- Нет дополнительных данных

### ✅ **Стало (AppError):**

```kotlin
// State
data class AppListState(
    val error: AppError? = null  // ✅ Типобезопасно
)

// ViewModel
catch (e: SecurityException) {
    reduce(LoadingError(
        AppError.PermissionDenied(...)  // ✅ Сохраняем тип
    ))
}

// UI
when (state.error) {
    is AppError.PermissionDenied -> /* специфичная UI */
    is AppError.Unknown -> /* общая UI */
}
```

**Преимущества:**
- ✅ Типобезопасность
- ✅ Сохранение информации о типе
- ✅ Разная обработка для разных ошибок
- ✅ Дополнительные данные в каждой ошибке

---

## 🔄 Как добавить новый тип ошибки

1. **Добавь в sealed class:**

```kotlin
sealed interface AppError {
    // ...
    
    data class NetworkError(
        val statusCode: Int,
        val url: String
    ) : AppError
}
```

2. **Добавь обработку в extension:**

```kotlin
fun AppError.toDisplayMessage(resourceProvider: ResourceProvider): String {
    return when (this) {
        // ...
        is AppError.NetworkError -> 
            "Ошибка сети: код $statusCode"
    }
}
```

3. **Добавь строку в strings.xml:**

```xml
<string name="error_network">Ошибка сети</string>
```

4. **Используй в ViewModel:**

```kotlin
catch (e: IOException) {
    reduce(LoadingError(
        AppError.NetworkError(
            statusCode = 500,
            url = "..."
        )
    ))
}
```

---

## 🎯 Best Practices

1. **Используй специфичные catch блоки:**
   ```kotlin
   catch (e: SecurityException) { /* PermissionDenied */ }
   catch (e: NameNotFoundException) { /* AppNotFound */ }
   catch (e: Exception) { /* Unknown */ }
   ```

2. **Сохраняй исходное исключение в Unknown:**
   ```kotlin
   AppError.Unknown(
       message = e.message,
       cause = e  // ✅ Для отладки
   )
   ```

3. **Добавляй полезные данные:**
   ```kotlin
   AppError.AppNotFound(
       packageName = packageName  // ✅ Полезно для UI и логов
   )
   ```

4. **Локализуй сообщения:**
   ```kotlin
   // ✅ Через strings.xml
   resourceProvider.getString(R.string.error_message)
   
   // ❌ Не хардкодь
   "Ошибка при загрузке"
   ```

---

## 📝 Примеры использования

### Пример 1: Разная UI для разных ошибок

```kotlin
when (val error = state.error) {
    is AppError.PermissionDenied -> {
        ErrorScreen(
            message = error.toDisplayMessage(resourceProvider),
            action = { openAppSettings() }  // Открыть настройки
        )
    }
    is AppError.Unknown -> {
        ErrorScreen(
            message = error.toDisplayMessage(resourceProvider),
            action = { viewModel.send(Retry) }  // Повторить
        )
    }
    null -> { /* No error */ }
}
```

### Пример 2: Логирование с контекстом

```kotlin
state.error?.let { error ->
    when (error) {
        is AppError.PermissionDenied -> {
            Log.e(TAG, "Permission denied: ${error.permission}", error.cause)
            analytics.logError("permission_denied", error.permission)
        }
        is AppError.Unknown -> {
            Log.e(TAG, "Unknown error", error.cause)
            crashlytics.recordException(error.cause)
        }
    }
}
```

---

## ✅ Итог

Типизированные ошибки через sealed class:
- ✅ Типобезопасные
- ✅ Расширяемые
- ✅ Локализуемые
- ✅ С дополнительными данными
- ✅ Production-ready

**Best practice для современных Android приложений!** 🚀
