# Type-Safe Navigation в AppInspector

## 🎯 Концепция

Приложение использует **type-safe navigation** из Jetpack Compose Navigation 2.8.0+ с `kotlinx.serialization`.

Вместо строковых маршрутов:
```kotlin
❌ navController.navigate("app_detail/${packageName}")  // Строка, не типобезопасно
```

Используются типизированные классы:
```kotlin
✅ navController.navigate(AppDetail(packageName = packageName))  // Типобезопасно!
```

---

## 📁 Структура

```
navigation/
├── AppInspectorRoutes.kt    // Определение маршрутов (sealed classes)
└── AppInspectorNavGraph.kt  // Навигационный граф
```

---

## 🗺️ Маршруты (Routes)

### `object ListApps` - Экран списка приложений

**Почему object?**
- Экран **не имеет параметров**
- Все экземпляры идентичны
- Singleton pattern

**Использование:**
```kotlin
// Навигация
navController.navigate(ListApps)

// Определение destination
composable<ListApps> {
    AppListScreen(...)
}
```

### `data class AppDetail(val packageName: String)` - Экран деталей

**Почему data class?**
- Экран **требует параметр** (packageName)
- Каждый экземпляр содержит данные конкретного приложения
- Поддержка equals/hashCode/copy

**Использование:**
```kotlin
// Навигация с параметром
navController.navigate(AppDetail(packageName = "com.android.chrome"))

// Определение destination
composable<AppDetail> { backStackEntry ->
    val args = backStackEntry.toRoute<AppDetail>()
    AppDetailScreen(packageName = args.packageName)
}
```

---

## 🔄 Как это работает

### 1. Определение маршрутов

```kotlin
// AppInspectorRoutes.kt

@Serializable
object ListApps  // Без параметров

@Serializable
data class AppDetail(val packageName: String)  // С параметрами
```

`@Serializable` нужен для:
- Сериализации параметров в URL
- Восстановления state после process death
- Type-safe передачи данных

### 2. Навигационный граф

```kotlin
// AppInspectorNavGraph.kt

NavHost(
    navController = navController,
    startDestination = ListApps,  // ✅ Тип, не строка
) {
    // Экран БЕЗ параметров
    composable<ListApps> {
        AppListScreen(
            onAppClick = { item ->
                // Навигация С параметром
                navController.navigate(AppDetail(packageName = item.packageName))
            }
        )
    }
    
    // Экран С параметрами
    composable<AppDetail> { backStackEntry ->
        // Извлечение параметров
        val args = backStackEntry.toRoute<AppDetail>()
        
        AppDetailScreen(packageName = args.packageName)
    }
}
```

### 3. Навигация в коде

```kotlin
// В AppListScreen при клике на приложение
onAppClick = { item ->
    navController.navigate(
        AppDetail(packageName = item.subtitle ?: "")
    )
}

// Назад
navController.popBackStack()

// На главную
navController.navigate(ListApps) {
    popUpTo(ListApps) { inclusive = true }
}
```

---

## ✅ Преимущества Type-Safe Navigation

### 1. **Compile-time safety**

```kotlin
❌ Было (String-based):
navController.navigate("app_detail/$packageNam")  // Опечатка! Runtime ошибка

✅ Стало (Type-safe):
navController.navigate(AppDetail(packageNam = "..."))  // Не скомпилируется!
```

### 2. **Auto-completion в IDE**

```kotlin
navController.navigate(AppDetail(
    packageName = "..."  // ✅ IDE подсказывает параметры
))
```

### 3. **Refactoring-friendly**

```kotlin
// Переименовали параметр? IDE обновит все использования!
data class AppDetail(val appPackage: String)  // Было: packageName
```

### 4. **Нет магических строк**

```kotlin
❌ Было:
const val ARG_PACKAGE_NAME = "packageName"
navController.navigate("app_detail/$packageName")
val packageName = backStackEntry.arguments?.getString(ARG_PACKAGE_NAME)

✅ Стало:
navController.navigate(AppDetail(packageName))
val args = backStackEntry.toRoute<AppDetail>()
val packageName = args.packageName
```

---

## 📊 Сравнение подходов

### ❌ **String-based navigation (старый подход)**

```kotlin
// Определение routes
object Routes {
    const val LIST = "list"
    const val DETAIL = "detail/{packageName}"
}

// Навигация
navController.navigate("detail/$packageName")  // Строки!

// Получение аргументов
composable(
    route = Routes.DETAIL,
    arguments = listOf(navArgument("packageName") { type = NavType.StringType })
) { backStackEntry ->
    val packageName = backStackEntry.arguments?.getString("packageName")  // Строки!
    AppDetailScreen(packageName = packageName ?: "")
}
```

**Проблемы:**
- ❌ Опечатки находятся только в runtime
- ❌ Нет автодополнения
- ❌ Сложно рефакторить
- ❌ Магические строки везде

### ✅ **Type-safe navigation (современный подход)**

```kotlin
// Определение routes
@Serializable
data class AppDetail(val packageName: String)

// Навигация
navController.navigate(AppDetail(packageName = packageName))  // Типобезопасно!

// Получение аргументов
composable<AppDetail> { backStackEntry ->
    val args = backStackEntry.toRoute<AppDetail>()  // Типобезопасно!
    AppDetailScreen(packageName = args.packageName)
}
```

**Преимущества:**
- ✅ Ошибки на этапе компиляции
- ✅ Автодополнение в IDE
- ✅ Легкий рефакторинг
- ✅ Нет магических строк

---

## 🎓 Когда использовать object vs data class

### **object** - для экранов БЕЗ параметров

```kotlin
@Serializable
object Home

@Serializable
object Settings

@Serializable
object Profile
```

**Признаки:**
- Экран не зависит от входных данных
- Все пользователи видят одно и то же
- Singleton

### **data class** - для экранов С параметрами

```kotlin
@Serializable
data class UserProfile(val userId: String)

@Serializable
data class ProductDetail(val productId: Int, val variantId: String? = null)

@Serializable
data class SearchResults(val query: String, val category: String? = null)
```

**Признаки:**
- Экран зависит от входных данных
- Разные пользователи/контексты видят разное
- Нужны параметры для загрузки данных

---

## 🔧 Добавление нового экрана

### Шаг 1: Определите маршрут

```kotlin
// AppInspectorRoutes.kt

@Serializable
data class PermissionDetails(
    val permission: String,
    val appPackageName: String
)
```

### Шаг 2: Добавьте в NavGraph

```kotlin
// AppInspectorNavGraph.kt

composable<PermissionDetails> { backStackEntry ->
    val args = backStackEntry.toRoute<PermissionDetails>()
    
    PermissionDetailsScreen(
        permission = args.permission,
        appPackageName = args.appPackageName,
        onBack = { navController.popBackStack() }
    )
}
```

### Шаг 3: Используйте навигацию

```kotlin
// В любом экране
Button(onClick = {
    navController.navigate(
        PermissionDetails(
            permission = "CAMERA",
            appPackageName = "com.example.app"
        )
    )
}) {
    Text("Подробности")
}
```

---

## 📚 Зависимости

```kotlin
// build.gradle.kts (app)

dependencies {
    // Navigation Compose с type-safe support
    implementation("androidx.navigation:navigation-compose:2.8.0")
    
    // Kotlinx Serialization для type-safe routes
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
```

```kotlin
// build.gradle.kts (project/module)

plugins {
    kotlin("plugin.serialization") version "1.9.0"
}
```

---

## 🎯 Best Practices

### 1. **Всегда используйте @Serializable**

```kotlin
✅ @Serializable
   data class UserProfile(val userId: String)

❌ data class UserProfile(val userId: String)  // Не будет работать
```

### 2. **Используйте значения по умолчанию для опциональных параметров**

```kotlin
✅ @Serializable
   data class SearchResults(
       val query: String,
       val category: String? = null  // Опциональный
   )
```

### 3. **Именуйте маршруты понятно**

```kotlin
✅ AppDetail, UserProfile, SearchResults
❌ Screen1, Page2, View3
```

### 4. **Группируйте связанные маршруты**

```kotlin
sealed interface AuthRoutes {
    @Serializable object Login : AuthRoutes
    @Serializable object Register : AuthRoutes
    @Serializable data class ResetPassword(val email: String) : AuthRoutes
}
```

---

## 🚀 Итог

Type-safe navigation в AppInspector:
- ✅ Безопасный (compile-time проверки)
- ✅ Удобный (автодополнение)
- ✅ Современный (best practices 2024+)
- ✅ Масштабируемый (легко добавлять экраны)

**Используется во всем приложении!** 💪
