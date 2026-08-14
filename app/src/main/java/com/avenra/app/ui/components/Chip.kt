package com.avenra.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.avenra.app.ui.theme.Theme
import com.avenra.app.ui.theme.ChipShape
import com.avenra.app.ui.theme.DarkNavy
import com.avenra.app.ui.theme.Outline
import com.avenra.app.ui.theme.Primary
import com.avenra.app.ui.theme.Spacing
import com.avenra.app.ui.theme.SurfaceVariant
import com.avenra.app.ui.theme.WhiteColor

@Composable
fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
            )
        },
        modifier = modifier,
        enabled = enabled,
        shape = ChipShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = SurfaceVariant,
            labelColor = DarkNavy,
            selectedContainerColor = Primary,
            selectedLabelColor = WhiteColor,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = Outline,
            selectedBorderColor = Primary,
            borderWidth = 1.dp,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun ChipPreview() {
    Theme {
        Chip(
            text = "Men's Fashion",
            selected = true,
            onClick = {},
            modifier = Modifier.padding(Spacing.small),
        )
    }
}
