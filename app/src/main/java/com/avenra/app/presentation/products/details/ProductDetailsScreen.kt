package com.avenra.app.presentation.products.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.avenra.app.domain.model.Product
import com.avenra.app.ui.components.ErrorState
import com.avenra.app.ui.components.LoadingState
import com.avenra.app.ui.components.ScreenTopAppBar
import com.avenra.app.ui.theme.Outline
import com.avenra.app.ui.theme.Primary
import com.avenra.app.ui.theme.Spacing
import com.avenra.app.ui.theme.SurfaceVariant
import com.avenra.app.ui.theme.TextSecondary
import com.avenra.app.ui.theme.WhiteColor
import kotlinx.coroutines.launch

@Composable
fun ProductDetailsScreen(
    productId: String,
    onBackClick: (() -> Unit)? = null,
    onCartClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: ProductDetailsViewModel = viewModel(
        factory = ProductDetailsViewModel.Factory(LocalContext.current, productId)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val cartCount by viewModel.cartCount.collectAsState(initial = 0)
    val wishlistProductIds by viewModel.wishlistProductIds.collectAsState(initial = emptySet())

    Scaffold(
        topBar = {
            ScreenTopAppBar(
                title = "Product Details",
                onBackClick = onBackClick,
                onCartClick = onCartClick,
                cartBadgeCount = cartCount
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
                is ProductDetailsUiState.Loading -> {
                    LoadingState(
                        message = "Loading product details...",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is ProductDetailsUiState.Error -> {
                    ErrorState(
                        title = "Product Not Found",
                        message = state.message,
                        onRetry = { viewModel.retry() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is ProductDetailsUiState.Success -> {
                    ProductDetailsContent(
                        product = state.product,
                        isWishlisted = wishlistProductIds.contains(state.product.id),
                        onWishlistToggle = { viewModel.toggleWishlist(state.product) },
                        onAddToCartClick = { qty, selectedSize, selectedColor ->
                            viewModel.addToCart(state.product, qty, selectedSize, selectedColor)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductDetailsContent(
    product: Product,
    isWishlisted: Boolean,
    onWishlistToggle: () -> Unit,
    onAddToCartClick: (quantity: Int, selectedSize: String?, selectedColor: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var quantity by remember { mutableIntStateOf(1) }
    var selectedSize by remember(product.id, product.sizes) { mutableStateOf(product.sizes.firstOrNull()) }
    var selectedColor by remember(product.id, product.colors) { mutableStateOf(product.colors.firstOrNull()) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    val images = remember(product) {
        listOf(product.imageUrl) + product.galleryImages.filter { it != product.imageUrl }
    }

    val effectiveUnitPrice = product.discountPrice ?: product.price
    val totalPrice = effectiveUnitPrice * quantity
    val formattedTotalPrice = "EGP %.2f".format(totalPrice)

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.medium)
        ) {
            Spacer(modifier = Modifier.height(Spacing.small))

            // 1. Image Carousel
            ProductImageCarousel(
                images = images,
                isWishlisted = isWishlisted,
                onWishlistToggle = onWishlistToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            // 2. Title & Price Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = Primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(Spacing.small))

                Text(
                    text = product.formattedPrice,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = Primary
                )
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            // 3. Stats & Quantity Controller Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Outline),
                        color = SurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = if (product.isAvailable) "${product.availableQuantity * 64} Sold" else "Out of Stock",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = Primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFBC02D),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "%.1f (%d)".format(product.rating, product.reviewCount * 150),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = Primary
                        )
                    }
                }

                QuantitySelectorPill(
                    quantity = quantity,
                    onQuantityChange = { newQty ->
                        if (newQty in 1..product.availableQuantity) {
                            quantity = newQty
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // 4. Description
            Text(
                text = "Description",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Primary
            )

            Spacer(modifier = Modifier.height(Spacing.xSmall))

            Text(
                text = product.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )

            if (product.description.length > 100) {
                Text(
                    text = if (isDescriptionExpanded) "Read Less" else "Read More",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Primary,
                    modifier = Modifier
                        .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                        .padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            if (product.sizes.isNotEmpty()) {
                Text(
                    text = "Size",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Primary
                )

                Spacer(modifier = Modifier.height(Spacing.small))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    product.sizes.forEach { sizeText ->
                    val isSelected = selectedSize == sizeText
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Primary else SurfaceVariant)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Primary else Outline,
                                shape = CircleShape
                            )
                            .clickable { selectedSize = sizeText }
                    ) {
                        Text(
                            text = sizeText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) WhiteColor else Primary
                        )
                    }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.medium))
            }

            if (product.colors.isNotEmpty()) {
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Primary
                )

                Spacer(modifier = Modifier.height(Spacing.small))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    product.colors.forEach { colorName ->
                    val isSelected = selectedColor == colorName
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colorName.toDisplayColor())
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) Primary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = colorName }
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected Color",
                                tint = WhiteColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.medium))
            }

            Spacer(modifier = Modifier.height(Spacing.large))
        }

        // 7. Fixed Bottom CTA Bar
        BottomCtaBar(
            totalPriceText = formattedTotalPrice,
            onAddToCartClick = {
                onAddToCartClick(quantity, selectedSize, selectedColor)
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

private fun String.toDisplayColor(): Color = when (lowercase()) {
    "black" -> Color(0xFF222222)
    "white" -> Color.White
    "red" -> Color(0xFFD32F2F)
    "blue", "navy" -> Color(0xFF1976D2)
    "green" -> Color(0xFF388E3C)
    "pink" -> Color(0xFFE91E63)
    else -> SurfaceVariant
}

@Composable
private fun ProductImageCarousel(
    images: List<String>,
    isWishlisted: Boolean,
    onWishlistToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { images.size })

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        border = BorderStroke(1.dp, Outline),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                AsyncImage(
                    model = images[page],
                    contentDescription = "Product Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            IconButton(
                onClick = onWishlistToggle,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Spacing.small)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(WhiteColor)
            ) {
                Icon(
                    imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Wishlist",
                    tint = if (isWishlisted) Color.Red else Primary
                )
            }

            if (images.size > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = Spacing.small)
                ) {
                    repeat(images.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (isSelected) 24.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Primary else Primary.copy(alpha = 0.3f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuantitySelectorPill(
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = Primary,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            IconButton(
                onClick = { onQuantityChange(quantity - 1) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease Quantity",
                    tint = WhiteColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = quantity.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = WhiteColor
            )

            IconButton(
                onClick = { onQuantityChange(quantity + 1) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase Quantity",
                    tint = WhiteColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomCtaBar(
    totalPriceText: String,
    onAddToCartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shadowElevation = 8.dp,
        color = WhiteColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium, vertical = Spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Total price",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = totalPriceText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Primary
                )
            }

            Button(
                onClick = onAddToCartClick,
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Add to Cart",
                        tint = WhiteColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Add to cart",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = WhiteColor
                    )
                }
            }
        }
    }
}
