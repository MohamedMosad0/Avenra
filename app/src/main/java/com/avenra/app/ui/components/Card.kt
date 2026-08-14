package com.avenra.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.avenra.app.ui.theme.Theme
import com.avenra.app.ui.theme.CardShape
import com.avenra.app.ui.theme.DarkNavy
import com.avenra.app.ui.theme.Outline
import com.avenra.app.ui.theme.Spacing
import com.avenra.app.ui.theme.WhiteColor

@Composable
fun CardContainer(
    modifier: Modifier = Modifier,
    border: BorderStroke? = BorderStroke(1.dp, Outline),
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.material3.Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = WhiteColor,
            contentColor = DarkNavy,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = border,
        content = content,
    )
}

@Preview(showBackground = true)
@Composable
private fun CardContainerPreview() {
    Theme {
        CardContainer(modifier = Modifier.padding(Spacing.medium)) {
            Column(modifier = Modifier.padding(Spacing.medium)) {
                Text(
                    text = "Card Title",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "This is a content container.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
