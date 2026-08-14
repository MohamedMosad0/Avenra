package com.avenra.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.avenra.app.ui.theme.Spacing
import com.avenra.app.ui.theme.Theme
import com.avenra.app.ui.theme.Primary
import com.avenra.app.ui.theme.TextSecondary

@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    message: String? = "Loading...",
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = Primary,
            strokeWidth = 4.dp,
        )

        if (!message.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(Spacing.medium))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingStatePreview() {
    Theme {
        LoadingState()
    }
}
