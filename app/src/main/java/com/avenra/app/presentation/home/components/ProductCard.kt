package com.avenra.app.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.avenra.app.domain.model.Product
import com.avenra.app.ui.theme.CardShape
import com.avenra.app.ui.theme.DarkNavy
import com.avenra.app.ui.theme.ErrorColor
import com.avenra.app.ui.theme.Primary
import com.avenra.app.ui.theme.Spacing
import com.avenra.app.ui.theme.TextSecondary
import com.avenra.app.ui.theme.WhiteColor

private val ProductCardHeight = 280.dp
private const val ProductImageAspectRatio = 1.2f

@Composable
fun ProductCard(
    product: Product,
    onProductClick: () -> Unit,
    onAddToCartClick: () -> Unit,
    modifier: Modifier = Modifier,
    isWishlisted: Boolean = false,
    onWishlistClick: (() -> Unit)? = null
) {
    Card(
        onClick = onProductClick,
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(ProductCardHeight)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ProductImageAspectRatio)
                    .background(Color(0xFFF2F4F7))
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp))
                )

                // Wishlist Icon (Top Left)
                if (onWishlistClick != null) {
                    IconButton(
                        onClick = onWishlistClick,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(Spacing.xSmall)
                            .size(30.dp)
                            .background(WhiteColor.copy(alpha = 0.85f), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Wishlist",
                            tint = if (isWishlisted) ErrorColor else DarkNavy,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Rating Badge (Top Right)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.xSmall)
                        .background(Color.Black.copy(alpha = 0.65f), shape = CircleShape)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "%.1f".format(product.rating),
                        style = MaterialTheme.typography.labelSmall,
                        color = WhiteColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (product.hasDiscount) {
                    Text(
                        text = product.formattedOriginalPrice.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        textDecoration = TextDecoration.LineThrough,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(Spacing.xSmall)
                            .background(WhiteColor.copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(Spacing.small)
            ) {
                Column {
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkNavy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = product.formattedPrice,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )

                Button(
                    onClick = onAddToCartClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    contentPadding = PaddingValues(horizontal = Spacing.small),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Text(
                        text = "Add to Cart",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
