package com.avenra.app.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.avenra.app.data.repository.AuthRepository
import com.avenra.app.domain.model.NetworkResult
import com.avenra.app.presentation.auth.signin.SignInScreen
import com.avenra.app.presentation.auth.signup.SignUpScreen
import com.avenra.app.presentation.account.AccountScreen
import com.avenra.app.presentation.cart.CartScreen
import com.avenra.app.presentation.checkout.CheckoutScreen
import com.avenra.app.presentation.categories.CategoriesScreen
import com.avenra.app.presentation.home.HomeScreen
import com.avenra.app.presentation.products.ProductListScreen
import com.avenra.app.presentation.products.details.ProductDetailsScreen
import com.avenra.app.presentation.wishlist.WishlistScreen

private const val HomeRoute = "home"
private const val CategoriesRoute = "categories"
private const val WishlistRoute = "wishlist"
private const val AccountRoute = "account"
private const val ProductsRoute = "products"
private const val ProductDetailsRoute = "products/details"
private const val CartRoute = "cart"
const val SignInRoute = "auth/signin"
const val SignUpRoute = "auth/signup"

@Composable
fun RootNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: HomeRoute

    val context = LocalContext.current
    // Validate saved auth token on app startup (non-blocking)
    val authRepository = remember { AuthRepository.getInstance(context) }
    LaunchedEffect(Unit) {
        authRepository.fetchCurrentProfile().collect { /* result handled internally by AuthRepository */ }
    }

    val isBottomBarVisible = currentRoute in listOf(HomeRoute, CategoriesRoute, WishlistRoute, AccountRoute)

    Scaffold(
        contentWindowInsets = if (isBottomBarVisible) {
            WindowInsets(0, 0, 0, 0)
        } else {
            ScaffoldDefaults.contentWindowInsets
        },
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == HomeRoute,
                        onClick = {
                            if (currentRoute != HomeRoute) {
                                navController.navigate(HomeRoute) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == HomeRoute) Icons.Default.Home else Icons.Outlined.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = { Text("Home") }
                    )

                    NavigationBarItem(
                        selected = currentRoute == CategoriesRoute,
                        onClick = {
                            if (currentRoute != CategoriesRoute) {
                                navController.navigate(CategoriesRoute) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == CategoriesRoute) Icons.Default.Category else Icons.Outlined.Category,
                                contentDescription = "Categories"
                            )
                        },
                        label = { Text("Categories") }
                    )

                    NavigationBarItem(
                        selected = currentRoute == WishlistRoute,
                        onClick = {
                            if (currentRoute != WishlistRoute) {
                                navController.navigate(WishlistRoute) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == WishlistRoute) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Wishlist"
                            )
                        },
                        label = { Text("Wishlist") }
                    )

                    NavigationBarItem(
                        selected = currentRoute == AccountRoute,
                        onClick = {
                            if (currentRoute != AccountRoute) {
                                navController.navigate(AccountRoute) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == AccountRoute) Icons.Default.Person else Icons.Outlined.PersonOutline,
                                contentDescription = "Account"
                            )
                        },
                        label = { Text("Account") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(HomeRoute) {
                HomeScreen(
                    onCategoryClick = { categoryId ->
                        navController.navigate("$ProductsRoute?categoryId=$categoryId")
                    },
                    onProductClick = { productId ->
                        navController.navigate("$ProductDetailsRoute/$productId")
                    },
                    onSearchSubmit = { query ->
                        navController.navigate(productSearchRoute(query))
                    },
                    onCartClick = {
                        navController.navigate(CartRoute)
                    }
                )
            }
            composable(CategoriesRoute) {
                CategoriesScreen(
                    onSubcategoryClick = { subcategory ->
                        navController.navigate("$ProductsRoute?categoryId=${subcategory.categoryId}")
                    },
                    onCartClick = {
                        navController.navigate(CartRoute)
                    }
                )
            }
            composable(WishlistRoute) {
                WishlistScreen(
                    onProductClick = { productId ->
                        navController.navigate("$ProductDetailsRoute/$productId")
                    },
                    onCartClick = {
                        navController.navigate(CartRoute)
                    },
                    onSearchSubmit = { query ->
                        navController.navigate(productSearchRoute(query))
                    }
                )
            }
            composable(AccountRoute) {
                AccountScreen(
                    onNavigateToSignIn = { navController.navigate(SignInRoute) },
                    onNavigateToSignUp = { navController.navigate(SignUpRoute) }
                )
            }
            composable(SignInRoute) {
                SignInScreen(
                    onNavigateToSignUp = {
                        navController.navigate(SignUpRoute) {
                            popUpTo(SignInRoute) { inclusive = true }
                        }
                    },
                    onSignInSuccess = {
                        navController.popBackStack()
                    }
                )
            }
            composable(SignUpRoute) {
                SignUpScreen(
                    onNavigateToSignIn = {
                        navController.navigate(SignInRoute) {
                            popUpTo(SignUpRoute) { inclusive = true }
                        }
                    },
                    onSignUpSuccess = {
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = "$ProductsRoute?categoryId={categoryId}&q={q}",
                arguments = listOf(
                    navArgument("categoryId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("q") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getString("categoryId")
                val query = backStackEntry.arguments?.getString("q")
                ProductListScreen(
                    categoryId = categoryId,
                    initialQuery = query,
                    onBackClick = if (navController.previousBackStackEntry != null) {
                        { navController.navigateUp() }
                    } else null,
                    onProductClick = { productId ->
                        navController.navigate("$ProductDetailsRoute/$productId")
                    },
                    onCartClick = { navController.navigate(CartRoute) }
                )
            }
            composable(
                route = "$ProductDetailsRoute/{productId}",
                arguments = listOf(
                    navArgument("productId") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: ""
                ProductDetailsScreen(
                    productId = productId,
                    onBackClick = { navController.navigateUp() },
                    onCartClick = { navController.navigate(CartRoute) }
                )
            }
            composable(CartRoute) {
                CartScreen(
                    onBackClick = { navController.navigateUp() },
                    onCheckoutClick = { navController.navigate(CheckoutRoute) }
                )
            }
            composable(CheckoutRoute) {
                CheckoutScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onOrderSuccess = {
                        navController.popBackStack(HomeRoute, inclusive = false)
                    }
                )
            }
        }
    }
}

const val CheckoutRoute = "checkout"

private fun productSearchRoute(query: String): String = "$ProductsRoute?q=${Uri.encode(query)}"
