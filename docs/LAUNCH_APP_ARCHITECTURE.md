# Launch App Architecture - Clean & SRP

## 🎯 Проблема (было)

### ❌ Нарушения в старом коде:

```kotlin
// NavGraph
composable<AppDetail> { backStackEntry ->
    val context = LocalContext.current  // ❌ Context в NavGraph
    SingleAppScreen(
        onOpenApp = { packageName ->
            // ❌ Бизнес-логика в NavGraph
            // ❌ Нет проверки на null
            context.packageManager.getLaunchIntentForPackage(packageName)?.let {
                context.startActivity(it)
            }
        }
    )
}

// AppListScreen
onAppClick = { item ->
    // ❌ Нет проверки на null для subtitle
    item.subtitle?.let { packageName ->
        navController.navigate(AppDetail(packageName))
    }
}
```

**Проблемы:**
1. ❌ **PackageManager в NavGraph** - нарушение SRP
2. ❌ **Нет обработки ошибок** - silent fail при null
3. ❌ **Нет проверки subtitle** - может быть null
4. ❌ **Нет обратной связи** - пользователь не знает, что пошло не так

---

## ✅ Решение (стало)

### Архитектура с Clean Architecture + SRP

```
UI (Screen) 
    ↓ Intent
ViewModel 
    ↓ invoke
UseCase 
    ↓ Result
ViewModel 
    ↓ Event
UI (Snackbar)
```

---

## 📁 Структура

```
domain/usecase/
└── LaunchAppUseCase.kt         // Бизнес-логика запуска приложения

presentation/singleapp/
├── SingleAppIntent.kt          // LaunchApp intent добавлен
├── SingleAppEvent.kt           // Одноразовые события (NEW)
├── SingleAppViewModel.kt       // Обработка через UseCase
└── SingleAppScreen.kt          // UI + обработка событий

navigation/
└── AppInspectorNavGraph.kt     // Только навигация, без бизнес-логики
```

---

## 🔥 Компоненты

### 1. **LaunchAppUseCase** - Бизнес-логика

```kotlin
class LaunchAppUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageManager: PackageManager,
) {
    sealed interface Result {
        data object Success : Result
        data object NoLauncherActivity : Result
        data class AppNotFound(val packageName: String) : Result
        data class Error(val message: String) : Result
    }
    
    operator fun invoke(packageName: String): Result {
        // 1. Проверка пустого packageName
        if (packageName.isBlank()) {
            return Result.Error("Package name is empty")
        }
        
        // 2. Проверка существования приложения
        try {
            packageManager.getApplicationInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            return Result.AppNotFound(packageName)
        }
        
        // 3. Проверка launcher intent
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            return Result.NoLauncherActivity
        }
        
        // 4. Запуск приложения
        try {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return Result.Success
        } catch (e: Exception) {
            return Result.Error(e.message ?: "Unknown error")
        }
    }
}
```

**Преимущества:**
- ✅ Все проверки в одном месте
- ✅ Типизированные результаты
- ✅ Легко тестировать
- ✅ Переиспользуемый

### 2. **SingleAppIntent.LaunchApp** - Намерение

```kotlin
sealed interface SingleAppIntent {
    data class Load(val packageName: String)
    data object Retry
    data object LaunchApp  // ✅ Новый intent
}
```

### 3. **SingleAppEvent** - Одноразовые события

```kotlin
sealed interface SingleAppEvent {
    data object AppLaunched : SingleAppEvent
    data class LaunchFailed(val reason: String) : SingleAppEvent
}
```

**Почему событие, а не state?**
- События происходят **один раз** (запуск приложения)
- State может быть **пропущен** при быстрой смене значений
- События **гарантированно доставляются** через Channel

### 4. **SingleAppViewModel** - Обработчик

```kotlin
@HiltViewModel
class SingleAppViewModel @Inject constructor(
    private val launchAppUseCase: LaunchAppUseCase,
    private val resourceProvider: ResourceProvider,
) : ViewModel() {
    
    private val _events = Channel<SingleAppEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    
    fun send(intent: SingleAppIntent) {
        when (intent) {
            is SingleAppIntent.LaunchApp -> launchApp()
            // ...
        }
    }
    
    private fun launchApp() {
        val packageName = _state.value.packageName
        
        when (val result = launchAppUseCase(packageName)) {
            is LaunchAppUseCase.Result.Success -> {
                viewModelScope.launch {
                    _events.send(SingleAppEvent.AppLaunched)
                }
            }
            is LaunchAppUseCase.Result.NoLauncherActivity -> {
                viewModelScope.launch {
                    _events.send(SingleAppEvent.LaunchFailed(
                        resourceProvider.getString(R.string.launch_error_no_launcher)
                    ))
                }
            }
            // ... другие случаи
        }
    }
}
```

### 5. **SingleAppScreen** - UI с обработкой событий

```kotlin
@Composable
fun SingleAppScreen(
    viewModel: SingleAppViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    showBackButton: Boolean = false,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Обработка одноразовых событий
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SingleAppEvent.AppLaunched -> {
                    // Опционально: показать успешное уведомление
                }
                is SingleAppEvent.LaunchFailed -> {
                    snackbarHostState.showSnackbar(
                        message = event.reason,
                        withDismissAction = true
                    )
                }
            }
        }
    }
    
    // ... UI код
    
    FilledTonalButton(
        onClick = { viewModel.send(SingleAppIntent.LaunchApp) }
    ) {
        Text("Открыть приложение")
    }
    
    SnackbarHost(hostState = snackbarHostState)
}
```

### 6. **AppInspectorNavGraph** - Только навигация

```kotlin
@Composable
fun AppInspectorNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = ListApps,
    ) {
        composable<ListApps> {
            AppListScreen(
                onAppClick = { item ->
                    // ✅ Проверка на null
                    val packageName = item.subtitle
                    if (!packageName.isNullOrBlank()) {
                        navController.navigate(AppDetail(packageName))
                    }
                }
            )
        }
        
        composable<AppDetail> {
            SingleAppScreen(
                onBack = { navController.popBackStack() }
                // ✅ Нет onOpenApp - логика в ViewModel
            )
        }
    }
}
```

**Принцип SRP:**
- ✅ NavGraph отвечает **только за навигацию**
- ✅ Нет PackageManager
- ✅ Нет бизнес-логики
- ✅ Простая проверка на null

---

## 📊 Сравнение

| Аспект | Было (❌) | Стало (✅) |
|--------|----------|-----------|
| **Где логика** | NavGraph | UseCase |
| **PackageManager** | В NavGraph | В UseCase |
| **Проверка null** | Частичная | Полная |
| **Обработка ошибок** | Silent fail | Snackbar с сообщением |
| **Тестируемость** | Сложно | Легко |
| **SRP** | Нарушен | Соблюден |
| **Локализация** | Хардкод | strings.xml |

---

## 🧪 Flow работы

### Успешный запуск:

```
1. User нажимает кнопку "Открыть"
   ↓
2. Screen → viewModel.send(LaunchApp)
   ↓
3. ViewModel → launchAppUseCase(packageName)
   ↓
4. UseCase → проверки → context.startActivity()
   ↓
5. UseCase → Result.Success
   ↓
6. ViewModel → _events.send(AppLaunched)
   ↓
7. Screen → LaunchedEffect собирает событие
   ↓
8. Приложение открыто ✅
```

### Ошибка (нет launcher):

```
1. User нажимает кнопку "Открыть"
   ↓
2. Screen → viewModel.send(LaunchApp)
   ↓
3. ViewModel → launchAppUseCase(packageName)
   ↓
4. UseCase → getLaunchIntentForPackage() == null
   ↓
5. UseCase → Result.NoLauncherActivity
   ↓
6. ViewModel → _events.send(LaunchFailed("У приложения нет launcher"))
   ↓
7. Screen → LaunchedEffect собирает событие
   ↓
8. Screen → snackbarHostState.showSnackbar()
   ↓
9. User видит сообщение об ошибке ✅
```

---

## ✅ Преимущества новой архитектуры

### 1. **Single Responsibility Principle**
- NavGraph → только навигация
- ViewModel → координация
- UseCase → бизнес-логика
- Screen → UI

### 2. **Полная обработка ошибок**
```kotlin
✅ packageName.isBlank() → Error
✅ App not installed → AppNotFound
✅ No launcher activity → NoLauncherActivity
✅ Exception → Error
```

### 3. **User feedback**
```kotlin
Snackbar с конкретным сообщением:
- "У приложения нет launcher activity"
- "Приложение не найдено"
- "Package name пуст"
```

### 4. **Тестируемость**
```kotlin
@Test
fun `launchApp returns NoLauncherActivity when no launcher`() {
    val packageManager = mock<PackageManager>()
    whenever(packageManager.getLaunchIntentForPackage(any())).thenReturn(null)
    
    val useCase = LaunchAppUseCase(context, packageManager)
    val result = useCase("com.example.app")
    
    assertTrue(result is LaunchAppUseCase.Result.NoLauncherActivity)
}
```

### 5. **Локализация**
```xml
<string name="launch_error_no_launcher">У приложения нет launcher activity</string>
<string name="launch_error_not_found">Приложение не найдено</string>
```

---

## 🎯 Best Practices

### 1. **Проверяй все null значения**
```kotlin
✅ if (!packageName.isNullOrBlank()) { navigate() }
❌ packageName?.let { navigate() }  // Недостаточно для isBlank
```

### 2. **Используй sealed class для результатов**
```kotlin
✅ sealed interface Result { Success, NoLauncher, NotFound, Error }
❌ Boolean  // Теряется контекст ошибки
```

### 3. **События для одноразовых действий**
```kotlin
✅ Channel<Event> для навигации, snackbar, toast
❌ State для одноразовых событий (могут быть пропущены)
```

### 4. **SRP в NavGraph**
```kotlin
✅ navController.navigate(AppDetail(packageName))
❌ context.packageManager.getLaunchIntent()  // Бизнес-логика
```

---

## 🚀 Итог

Теперь запуск приложения:
- ✅ Следует Clean Architecture
- ✅ Соблюдает SRP
- ✅ Полностью типобезопасен
- ✅ Обрабатывает все ошибки
- ✅ Дает feedback пользователю
- ✅ Легко тестируется
- ✅ Локализован

**Production-ready архитектура!** 💪
