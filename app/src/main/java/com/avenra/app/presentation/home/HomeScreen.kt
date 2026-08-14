package com.avenra.app.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avenra.app.R
import com.avenra.app.domain.model.HomeData
import com.avenra.app.domain.model.Product
import com.avenra.app.presentation.home.components.BannerCard
import com.avenra.app.presentation.home.components.CategoryCard
import com.avenra.app.presentation.home.components.ProductCard
import com.avenra.app.ui.components.ErrorState
import com.avenra.app.ui.components.LoadingState
import com.avenra.app.ui.theme.DarkNavy
import com.avenra.app.ui.theme.Primary
import com.avenra.app.ui.theme.Spacing
import com.avenra.app.ui.theme.TextSecondary
import com.avenra.app.ui.theme.WhiteColor

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onCategoryClick: (String) -> Unit = {},
    onProductClick: (String) -> Unit = {},
    onSearchSubmit: (String) -> Unit = {},
    onCartClick: (() -> Unit)? = null,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(LocalContext.current))
) {
    val uiState by viewModel.uiState.collectAsState()
    val cartBadgeCount by viewModel.cartCount.collectAsState(initial = 0)
    val wishlistProductIds by viewModel.wishlistProductIds.collectAsState(initial = emptySet())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_avenra_logo_mark),
                        contentDescription = "Avenra Logo",
                        modifier = Modifier.requiredSize(28.dp)
                    )
                },
                actions = {
                    IconButton(onClick = { onCartClick?.invoke() }) {
                        BadgedBox(
                            badge = {
                                if (cartBadgeCount > 0) {
                                    Badge { Text(if (cartBadgeCount > 99) "99+" else cartBadgeCount.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ShoppingCart,
                                contentDescription = "Shopping Cart",
                                tint = DarkNavy
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WhiteColor
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    LoadingState(
                        message = "Loading catalog...",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is HomeUiState.Error -> {
                    ErrorState(
                        title = "Service Connection Error",
                        message = state.message,
                        onRetry = { viewModel.retry() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is HomeUiState.Success -> {
                    HomeContent(
                        homeData = state.homeData,
                        wishlistProductIds = wishlistProductIds,
                        onBannerClick = { categoryId -> onCategoryClick(categoryId) },
                        onCategoryClick = onCategoryClick,
                        onProductClick = onProductClick,
                        onSearchSubmit = onSearchSubmit,
                        onAddToCartClick = { product ->
                            viewModel.addToCart(product)
                        },
                        onWishlistToggle = { product ->
                            viewModel.toggleWishlist(product)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeContent(
    homeData: HomeData,
    wishlistProductIds: Set<String>,
    onBannerClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onProductClick: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onAddToCartClick: (Product) -> Unit,
    onWishlistToggle: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(bottom = Spacing.xLarge),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = modifier.fillMaxSize()
    ) {
        // Search Input
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "what do you search for?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = Primary
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (searchQuery.isNotBlank()) {
                            onSearchSubmit(searchQuery.trim())
                        }
                    }
                ),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium)
            )
        }

        // Promotional Banners Carousel
        if (homeData.banners.isNotEmpty()) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(homeData.banners, key = { it.id }) { banner ->
                        BannerCard(
                            banner = banner,
                            onClick = { banner.targetCategoryId?.let { onBannerClick(it) } },
                            modifier = Modifier.width(300.dp)
                        )
                    }
                }
            }
        }

        // Categories Header & Row
        if (homeData.categories.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.medium)
                    ) {
                        Text(
                            text = "Categories",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = DarkNavy
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.small))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Spacing.medium),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(homeData.categories, key = { it.id }) { category ->
                            CategoryCard(
                                category = category,
                                onClick = { onCategoryClick(category.id) }
                            )
                        }
                    }
                }
            }
        }

        // Featured Products Grid
        if (homeData.featuredProducts.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.medium)
                ) {
                    Text(
                        text = "Featured Products",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = DarkNavy
                    )

                    Spacer(modifier = Modifier.height(Spacing.small))

                    val chunkedProducts = homeData.featuredProducts.chunked(2)
                    chunkedProducts.forEach { rowProducts ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.xSmall)
                        ) {
                            rowProducts.forEach { product ->
                                ProductCard(
                                    product = product,
                                    onProductClick = { onProductClick(product.id) },
                                    onAddToCartClick = { onAddToCartClick(product) },
                                    isWishlisted = wishlistProductIds.contains(product.id),
                                    onWishlistClick = { onWishlistToggle(product) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowProducts.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
