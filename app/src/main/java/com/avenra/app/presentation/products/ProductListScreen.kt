package com.avenra.app.presentation.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avenra.app.domain.model.Product
import com.avenra.app.presentation.home.components.ProductCard
import com.avenra.app.ui.components.EmptyState
import com.avenra.app.ui.components.ErrorState
import com.avenra.app.ui.components.LoadingState
import com.avenra.app.ui.components.ScreenTopAppBar
import com.avenra.app.ui.theme.Outline
import com.avenra.app.ui.theme.Primary
import com.avenra.app.ui.theme.Spacing
import com.avenra.app.ui.theme.SurfaceVariant
import com.avenra.app.ui.theme.TextSecondary

import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton

@Composable
fun ProductListScreen(
    title: String = "Catalog",
    categoryId: String? = null,
    initialQuery: String? = null,
    onBackClick: (() -> Unit)? = null,
    onProductClick: (String) -> Unit = {},
    onCartClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: ProductListViewModel = viewModel(
        factory = ProductListViewModel.Factory(
            context = LocalContext.current,
            categoryId = categoryId,
            initialQuery = initialQuery
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val effectiveCartBadgeCount by viewModel.cartCount.collectAsState(initial = 0)
    val wishlistProductIds by viewModel.wishlistProductIds.collectAsState(initial = emptySet())

    var searchInput by remember { mutableStateOf(initialQuery ?: "") }

    val displayTitle = when {
        !searchInput.isBlank() -> "Search Results"
        title != "Catalog" -> title
        else -> "Catalog"
    }

    Scaffold(
        topBar = {
            ScreenTopAppBar(
                title = displayTitle,
                onBackClick = onBackClick,
                onCartClick = onCartClick,
                cartBadgeCount = effectiveCartBadgeCount
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.medium)
        ) {
            Spacer(modifier = Modifier.height(Spacing.small))

            // Persistent Search Bar
            OutlinedTextField(
                value = searchInput,
                onValueChange = {
                    searchInput = it
                    viewModel.onSearchQueryChanged(it)
                },
                placeholder = {
                    Text(
                        text = "what do you search for?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Primary
                    )
                },
                trailingIcon = {
                    if (searchInput.isNotEmpty()) {
                        IconButton(onClick = {
                            searchInput = ""
                            viewModel.onSearchQueryChanged(null)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Search",
                                tint = Primary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(30.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceVariant,
                    unfocusedContainerColor = SurfaceVariant,
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Outline
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is ProductListUiState.Loading -> {
                        LoadingState(
                            message = "Searching products...",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is ProductListUiState.Empty -> {
                        EmptyState(
                            title = "No Products Found",
                            message = state.message,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is ProductListUiState.Error -> {
                        ErrorState(
                            title = "Failed to Load Products",
                            message = state.message,
                            onRetry = { viewModel.retry() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is ProductListUiState.Success -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                            contentPadding = PaddingValues(bottom = Spacing.large),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.products, key = { it.id }) { product ->
                                ProductCard(
                                    product = product,
                                    onProductClick = { onProductClick(product.id) },
                                    onAddToCartClick = {
                                        viewModel.addToCart(product)
                                    },
                                    isWishlisted = wishlistProductIds.contains(product.id),
                                    onWishlistClick = {
                                        viewModel.toggleWishlist(product)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
