package com.example.appinspector.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.appinspector.presentation.applist.AppListScreen
import com.example.appinspector.presentation.applist.AppsViewModel
import com.example.appinspector.presentation.singleapp.SingleAppScreen
import com.example.appinspector.presentation.singleapp.SingleAppViewModel
import timber.log.Timber

/**
 * Навигационный граф приложения.
 */
@Composable
fun AppInspectorNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = ListApps,
    ) {
        composable<ListApps> {
            val appsViewModel: AppsViewModel = hiltViewModel()
            AppListScreen(
                viewModel = appsViewModel,
                onAppClick = { item ->
                    val packageName = item.subtitle
                    if (!packageName.isNullOrBlank()) {
                        Timber.d("User action: Navigate to AppDetail with package=$packageName")
                        navController.navigate(AppDetail(packageName))
                    } else {
                        Timber.e("Navigation error: packageName is null or blank for item=${item}")
                    }
                },
            )
        }

        composable<AppDetail> { backStackEntry ->
            val singleAppViewModel: SingleAppViewModel = hiltViewModel(backStackEntry)
            SingleAppScreen(
                viewModel = singleAppViewModel,
                onBack = {
                    Timber.d("User action: Navigate back from AppDetail")
                    navController.popBackStack()
                },
                showBackButton = true,
            )
        }
    }
}
