package com.diljith.android_coroutine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.diljith.android_coroutine.screen.HomeScreen
import com.diljith.android_coroutine.screen.LoginScreen
import com.diljith.android_coroutine.screen.MainScreen
import com.diljith.android_coroutine.screen.ProductDetailsScreen
import com.diljith.android_coroutine.screen.ProfileScreen
import com.diljith.android_coroutine.screen.SearchScreen
import com.diljith.android_coroutine.screen.SplashScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

@Serializable
data object Splash

@Serializable
data object Login

@Serializable
data object MainGraph

@Serializable
data object Home

@Serializable
data object Search

@Serializable
data object Profile

@Serializable
data class ProductDetails(val productId: String)

sealed class TopLevelRoute<T : Any>(val name: String, val route: T, val icon: ImageVector) {
    data object HomeRoute : TopLevelRoute<Home>( "Home", Home, Icons.Default.Home)
    data object SearchRoute : TopLevelRoute<Search>("Search", Search, Icons.Default.Search)
    data object ProfileRoute : TopLevelRoute<Profile>("Profile", Profile, Icons.Default.Person)
}

val topLevelRoutes = listOf(
    TopLevelRoute.HomeRoute,
    TopLevelRoute.SearchRoute,
    TopLevelRoute.ProfileRoute
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            AppNavHost(navController = navController)
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = "LoginScreen"
    ) {
        composable("SplashScreen") {
            SplashScreen(
                onAuthenticated = {
                    navController.navigate("SplashScreen") {
                        popUpTo(Splash) { inclusive = true }
                    }
                },
                onUnauthenticated = {
                    navController.navigate("LoginScreen") {
                        popUpTo(Splash) { inclusive = true }
                    }
                }
            )
        }

        composable("LoginScreen") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(MainGraph) {
                        popUpTo("LoginScreen") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        navigation<MainGraph>(
            startDestination = Home
        ) {
            composable<Home> {
                MainScreen(navController) {
                    HomeScreen(
                        onProductClick = { productId ->
                            navController.navigate(ProductDetails(productId))
                        }
                    )
                }
            }

            composable<Search> {
                MainScreen(navController) {
                    SearchScreen(
                        onProductClick = { productId ->
                            navController.navigate(ProductDetails(productId))
                        }
                    )
                }
            }

            composable<Profile> {
                MainScreen(navController) {
                    ProfileScreen(
                        onLogout = {
                            navController.navigate("LoginScreen") {
                                popUpTo(MainGraph) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }

            composable<ProductDetails> { entry ->
                val args = entry.toRoute<ProductDetails>()
                ProductDetailsScreen(
                    productId = args.productId,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

