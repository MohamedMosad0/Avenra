package com.avenra.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.avenra.app.R
import com.avenra.app.ui.theme.Spacing
import com.avenra.app.ui.theme.Theme
import com.avenra.app.ui.theme.DarkNavy
import com.avenra.app.ui.theme.Primary
import com.avenra.app.ui.theme.TextSecondary

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.large),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_avenra_logo_mark),
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(64.dp),
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = DarkNavy,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(Spacing.small))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )

        if (!actionButtonText.isNullOrBlank() && onActionClick != null) {
            Spacer(modifier = Modifier.height(Spacing.large))
            PrimaryButton(
                text = actionButtonText,
                onClick = onActionClick,
                modifier = Modifier.width(200.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    Theme {
        EmptyState(
            title = "Your Cart is Empty",
            message = "Explore our collection and discover amazing products to add to your cart.",
            actionButtonText = "Start Shopping",
            onActionClick = {},
        )
    }
}
