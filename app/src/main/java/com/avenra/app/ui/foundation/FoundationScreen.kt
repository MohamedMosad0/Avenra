package com.avenra.app.ui.foundation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.avenra.app.R
import com.avenra.app.ui.components.CardContainer
import com.avenra.app.ui.components.Chip
import com.avenra.app.ui.components.PrimaryButton
import com.avenra.app.ui.components.InputField
import com.avenra.app.ui.components.ScreenTopAppBar
import com.avenra.app.ui.theme.BackgroundColor
import com.avenra.app.ui.theme.DarkNavy
import com.avenra.app.ui.theme.Spacing

@Composable
fun FoundationScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    Scaffold(
        topBar = {
            ScreenTopAppBar(title = "Design System")
        },
        containerColor = BackgroundColor,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Spacing.medium),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_avenra_logo_horizontal),
                contentDescription = "Avenra Logo",
                modifier = Modifier.height(48.dp),
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            InputField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "Search products",
                placeholder = "e.g. Running Shoes",
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                Chip(
                    text = "All",
                    selected = selectedCategory == "All",
                    onClick = { selectedCategory = "All" },
                )
                Chip(
                    text = "Men's",
                    selected = selectedCategory == "Men's",
                    onClick = { selectedCategory = "Men's" },
                )
                Chip(
                    text = "Women's",
                    selected = selectedCategory == "Women's",
                    onClick = { selectedCategory = "Women's" },
                )
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            CardContainer {
                Column(modifier = Modifier.padding(Spacing.medium)) {
                    Text(
                        text = "Design System Foundation",
                        style = MaterialTheme.typography.titleLarge,
                        color = DarkNavy,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xSmall))
                    Text(
                        text = "Design system tokens, typography, shapes, and atomic components established.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.large))

            PrimaryButton(
                text = "Explore Catalog",
                onClick = {},
            )
        }
    }
}
