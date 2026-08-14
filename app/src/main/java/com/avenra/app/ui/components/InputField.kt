package com.avenra.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.avenra.app.ui.theme.DarkNavy
import com.avenra.app.ui.theme.ErrorColor
import com.avenra.app.ui.theme.InputShape
import com.avenra.app.ui.theme.Outline
import com.avenra.app.ui.theme.Primary
import com.avenra.app.ui.theme.Spacing
import com.avenra.app.ui.theme.SurfaceVariant
import com.avenra.app.ui.theme.TextSecondary
import com.avenra.app.ui.theme.Theme

@Composable
fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    errorMessage: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(text = label) },
            placeholder = placeholder?.let { { Text(text = it) } },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = !errorMessage.isNullOrBlank(),
            singleLine = singleLine,
            enabled = enabled,
            shape = InputShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceVariant,
                unfocusedContainerColor = SurfaceVariant,
                disabledContainerColor = SurfaceVariant,
                focusedBorderColor = Primary,
                unfocusedBorderColor = Outline,
                focusedLabelColor = Primary,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = DarkNavy,
                unfocusedTextColor = DarkNavy,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = ErrorColor,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(
                    start = Spacing.small,
                    top = Spacing.xSmall,
                ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InputFieldPreview() {
    Theme {
        InputField(
            value = "customer@example.com",
            onValueChange = {},
            label = "Email Address",
        )
    }
}
