package com.avenra.app.presentation.cart

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.avenra.app.domain.model.CartItem
import com.avenra.app.ui.components.EmptyState
import com.avenra.app.ui.components.ErrorState
import com.avenra.app.ui.components.LoadingState
import com.avenra.app.ui.components.ScreenTopAppBar
import com.avenra.app.ui.theme.Outline
import com.avenra.app.ui.theme.Primary
import com.avenra.app.ui.theme.Spacing
import com.avenra.app.ui.theme.SurfaceVariant
import com.avenra.app.ui.theme.TextSecondary
import com.avenra.app.ui.theme.WhiteColor

@Composable
fun CartScreen(
    onBackClick: (() -> Unit)? = null,
    onCheckoutClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CartViewModel = viewModel(
        factory = CartViewModel.Factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            ScreenTopAppBar(
                title = "Cart",
                onBackClick = onBackClick
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
                is CartUiState.Loading -> {
                    LoadingState(
                        message = "Loading cart...",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is CartUiState.Empty -> {
                    EmptyState(
                        title = "Your Cart is Empty",
                        message = "Add items to your cart to start shopping.",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is CartUiState.Error -> {
                    ErrorState(
                        title = "Failed to Load Cart",
                        message = state.message,
                        onRetry = { },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is CartUiState.Success -> {
                    CartContent(
                        items = state.items,
                        formattedSubtotal = state.formattedSubtotal,
                        onQuantityChange = { id, qty -> viewModel.updateQuantity(id, qty) },
                        onRemoveItem = { id -> viewModel.removeItem(id) },
                        onCheckoutClick = onCheckoutClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun CartContent(
    items: List<CartItem>,
    formattedSubtotal: String,
    onQuantityChange: (String, Int) -> Unit,
    onRemoveItem: (String) -> Unit,
    onCheckoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(
                horizontal = Spacing.medium,
                vertical = Spacing.small
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp) // Leave room for fixed bottom bar
        ) {
            items(items, key = { it.id }) { item ->
                CartItemCard(
                    item = item,
                    onQuantityChange = { newQty -> onQuantityChange(item.id, newQty) },
                    onRemoveItem = { onRemoveItem(item.id) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Fixed Bottom Checkout CTA Bar
        CartBottomCtaBar(
            subtotalText = formattedSubtotal,
            onCheckoutClick = onCheckoutClick,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun CartItemCard(
    item: CartItem,
    onQuantityChange: (Int) -> Unit,
    onRemoveItem: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        border = BorderStroke(1.dp, Outline),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image Thumbnail
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(WhiteColor)
            )

            Spacer(modifier = Modifier.width(Spacing.medium))

            // Item Details (Title, Variant, Price & Stepper)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onRemoveItem,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Item",
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (item.variantInfo.isNotBlank()) {
                    Text(
                        text = item.variantInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Price and Quantity Stepper Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.formattedItemSubtotal,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Primary
                    )

                    CartQuantityStepper(
                        quantity = item.quantity,
                        onQuantityChange = onQuantityChange
                    )
                }
            }
        }
    }
}

@Composable
private fun CartQuantityStepper(
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
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease Quantity",
                    tint = WhiteColor,
                    modifier = Modifier.size(14.dp)
                )
            }

            Text(
                text = quantity.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = WhiteColor,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            IconButton(
                onClick = { onQuantityChange(quantity + 1) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase Quantity",
                    tint = WhiteColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun CartBottomCtaBar(
    subtotalText: String,
    onCheckoutClick: () -> Unit,
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
                    text = subtotalText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Primary
                )
            }

            Button(
                onClick = onCheckoutClick,
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    Text(
                        text = "Check Out",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = WhiteColor
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Checkout",
                        tint = WhiteColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
