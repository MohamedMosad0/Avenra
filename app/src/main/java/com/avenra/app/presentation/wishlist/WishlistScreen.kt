package com.avenra.app.presentation.wishlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.avenra.app.R
import com.avenra.app.domain.model.WishlistItem
import com.avenra.app.ui.components.ErrorState
import com.avenra.app.ui.components.LoadingState
import com.avenra.app.ui.theme.DarkNavy
import com.avenra.app.ui.theme.Primary
import com.avenra.app.ui.theme.Spacing
import com.avenra.app.ui.theme.SurfaceVariant
import com.avenra.app.ui.theme.TextSecondary
import com.avenra.app.ui.theme.WhiteColor

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    onProductClick: (String) -> Unit = {},
    onCartClick: () -> Unit = {},
    onSearchSubmit: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: WishlistViewModel = viewModel(
        factory = WishlistViewModel.Factory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val cartCount by viewModel.cartCount.collectAsState(initial = 0)
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_avenra_logo_mark),
                            contentDescription = "Avenra Logo",
                            modifier = Modifier.requiredSize(28.dp)
                        )

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = "what do you search for?",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "Search",
                                    tint = Primary,
                                    modifier = Modifier.size(18.dp)
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
                                .weight(1f)
                                .height(42.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onCartClick) {
                        BadgedBox(
                            badge = {
                                if (cartCount > 0) {
                                    Badge { Text(if (cartCount > 99) "99+" else cartCount.toString()) }
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WhiteColor)
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
                is WishlistUiState.Loading -> {
                    LoadingState(
                        message = "Loading wishlist...",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is WishlistUiState.Empty -> {
                    EmptyWishlistContent(modifier = Modifier.fillMaxSize())
                }
                is WishlistUiState.Error -> {
                    ErrorState(
                        title = "Failed to load wishlist",
                        message = state.message,
                        onRetry = { },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is WishlistUiState.Success -> {
                    val filteredProducts = remember(state.products, searchQuery) {
                        if (searchQuery.isBlank()) {
                            state.products
                        } else {
                            state.products.filter { it.title.contains(searchQuery, ignoreCase = true) }
                        }
                    }

                    if (filteredProducts.isEmpty()) {
                        EmptyWishlistContent(modifier = Modifier.fillMaxSize())
                    } else {
                        WishlistListContent(
                            products = filteredProducts,
                            onProductClick = onProductClick,
                            onRemoveFromWishlist = { productId -> viewModel.removeFromWishlist(productId) },
                            onAddToCart = { product -> viewModel.addToCart(product) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WishlistListContent(
    products: List<WishlistItem>,
    onProductClick: (String) -> Unit,
    onRemoveFromWishlist: (String) -> Unit,
    onAddToCart: (WishlistItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = modifier
    ) {
        items(products, key = { it.id }) { product ->
            WishlistItemCard(
                product = product,
                onProductClick = { onProductClick(product.id) },
                onRemoveFromWishlist = { onRemoveFromWishlist(product.id) },
                onAddToCart = { onAddToCart(product) }
            )
        }
    }
}

@Composable
private fun WishlistItemCard(
    product: WishlistItem,
    onProductClick: () -> Unit,
    onRemoveFromWishlist: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onProductClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image Thumbnail
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(WhiteColor)
            )

            Spacer(modifier = Modifier.width(Spacing.medium))

            // Right Product Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title and Heart Icon Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onRemoveFromWishlist,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Remove from Wishlist",
                            tint = Primary
                        )
                    }
                }

                // Price and Add to Cart Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = product.formattedPrice,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = Primary
                        )

                        if (product.discountPrice != null && product.discountPrice < product.price) {
                            Text(
                                text = "EGP %.0f".format(product.price),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    textDecoration = TextDecoration.LineThrough
                                ),
                                color = TextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = onAddToCart,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .width(124.dp)
                            .height(34.dp)
                    ) {
                        Text(
                            text = "Add to Cart",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = WhiteColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyWishlistContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(Spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = SurfaceVariant,
            modifier = Modifier.size(90.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = "Empty Wishlist",
                    tint = Primary,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.medium))

        Text(
            text = "Your Wishlist is Empty",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Primary
        )

        Spacer(modifier = Modifier.height(Spacing.small))

        Text(
            text = "Save items you love by tapping the heart icon on any product card.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}
