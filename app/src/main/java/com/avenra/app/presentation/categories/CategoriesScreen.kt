package com.avenra.app.presentation.categories

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.avenra.app.R
import com.avenra.app.domain.model.Category
import com.avenra.app.presentation.categories.components.SubcategoryCard
import com.avenra.app.ui.components.EmptyState
import com.avenra.app.ui.components.ErrorState
import com.avenra.app.ui.components.LoadingState
import com.avenra.app.domain.model.Subcategory
import com.avenra.app.ui.theme.DarkNavy
import com.avenra.app.ui.theme.Spacing
import com.avenra.app.ui.theme.WhiteColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    modifier: Modifier = Modifier,
    onSubcategoryClick: (Subcategory) -> Unit = {},
    onCartClick: () -> Unit = {},
    viewModel: CategoriesViewModel = viewModel(factory = CategoriesViewModel.Factory(LocalContext.current))
) {
    val uiState by viewModel.uiState.collectAsState()
    val cartBadgeCount by viewModel.cartCount.collectAsState(initial = 0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_avenra_logo_mark),
                        contentDescription = "Avenra",
                        modifier = Modifier.requiredSize(28.dp)
                    )
                },
                actions = {
                    IconButton(onClick = onCartClick) {
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
                is CategoriesUiState.Loading -> {
                    LoadingState(
                        message = "Loading categories...",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is CategoriesUiState.Empty -> {
                    EmptyState(
                        title = "No Categories Available",
                        message = "We couldn't find any categories at this time.",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is CategoriesUiState.Error -> {
                    ErrorState(
                        title = "Service Connection Error",
                        message = state.message,
                        onRetry = { viewModel.retry() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is CategoriesUiState.Success -> {
                    CategoriesContent(
                        categories = state.categories,
                        selectedCategory = state.selectedCategory,
                        onCategorySelect = { viewModel.selectCategory(it) },
                        onSubcategoryClick = onSubcategoryClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoriesContent(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelect: (String) -> Unit,
    onSubcategoryClick: (Subcategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxSize()) {
        // Left Category Sidebar (110.dp width)
        LazyColumn(
            contentPadding = PaddingValues(vertical = Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier
                .width(110.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            items(categories, key = { it.id }) { category ->
                val isSelected = selectedCategory?.id == category.id
                CategorySidebarItem(
                    category = category,
                    isSelected = isSelected,
                    onClick = { onCategorySelect(category.id) }
                )
            }
        }

        // Right Subcategory Content Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(Spacing.medium)
        ) {
            if (selectedCategory != null) {
                // Category Header Banner Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = selectedCategory.imageUrl,
                            contentDescription = selectedCategory.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                        )
                        Text(
                            text = selectedCategory.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(Spacing.small)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.large))

                Text(
                    text = "Subcategories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(Spacing.small))

                if (selectedCategory.subcategories.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        verticalArrangement = Arrangement.spacedBy(Spacing.small),
                        contentPadding = PaddingValues(bottom = Spacing.large),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(selectedCategory.subcategories, key = { it.id }) { subcategory ->
                            SubcategoryCard(
                                subcategory = subcategory,
                                onClick = { onSubcategoryClick(subcategory) }
                            )
                        }
                    }
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "No subcategories available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySidebarItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    } else {
        Color.Transparent
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(backgroundColor)
            .padding(vertical = Spacing.medium, horizontal = Spacing.small)
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(Spacing.xSmall))
        }

        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            textAlign = TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
