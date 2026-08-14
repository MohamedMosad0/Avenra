package com.avenra.app.presentation.checkout

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avenra.app.domain.model.CheckoutQuote
import com.avenra.app.domain.model.OrderResult
import com.avenra.app.domain.model.ShippingAddress
import com.avenra.app.ui.components.ErrorState
import com.avenra.app.ui.components.LoadingState
import com.avenra.app.ui.components.ScreenTopAppBar
import com.avenra.app.ui.theme.DarkNavy
import com.avenra.app.ui.theme.Outline
import com.avenra.app.ui.theme.Primary
import com.avenra.app.ui.theme.Spacing
import com.avenra.app.ui.theme.SurfaceVariant
import com.avenra.app.ui.theme.TextSecondary
import com.avenra.app.ui.theme.WhiteColor
import java.util.Locale

@Composable
fun CheckoutScreen(
    onNavigateBack: () -> Unit,
    onOrderSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: CheckoutViewModel = viewModel(
        factory = CheckoutViewModel.provideFactory(application)
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            ScreenTopAppBar(
                title = "Checkout",
                onBackClick = onNavigateBack
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
                is CheckoutUiState.AddressForm -> {
                    AddressFormContent(
                        initialAddress = state.shippingAddress,
                        validationError = state.validationError,
                        onSubmit = { address -> viewModel.requestQuote(address) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is CheckoutUiState.QuoteLoading -> {
                    LoadingState(
                        message = "Calculating checkout quote...",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is CheckoutUiState.QuoteSuccess -> {
                    QuoteSummaryContent(
                        quote = state.quote,
                        address = state.shippingAddress,
                        onConfirmOrder = { viewModel.confirmOrder() },
                        onChangeAddress = { viewModel.backToAddress(state.shippingAddress) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is CheckoutUiState.OrderSubmitting -> {
                    LoadingState(
                        message = "Creating your order...",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is CheckoutUiState.OrderSuccess -> {
                    OrderSuccessContent(
                        orderResult = state.orderResult,
                        onContinueShopping = onOrderSuccess,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is CheckoutUiState.Error -> {
                    ErrorState(
                        title = "Checkout Error",
                        message = state.message,
                        onRetry = { viewModel.backToAddress(state.shippingAddress) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun AddressFormContent(
    initialAddress: ShippingAddress,
    validationError: String?,
    onSubmit: (ShippingAddress) -> Unit,
    modifier: Modifier = Modifier
) {
    var fullName by remember { mutableStateOf(initialAddress.fullName) }
    var phone by remember { mutableStateOf(initialAddress.phone) }
    var city by remember { mutableStateOf(initialAddress.city) }
    var addressLine by remember { mutableStateOf(initialAddress.addressLine) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(Spacing.medium)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.LocalShipping,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.padding(start = 8.dp))
            Text(
                text = "Shipping Address",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = Primary
            )
        }

        Spacer(modifier = Modifier.height(Spacing.medium))

        if (validationError != null) {
            Text(
                text = validationError,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = Spacing.small)
            )
        }

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Outline
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone Number") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Outline
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("City") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Outline
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        OutlinedTextField(
            value = addressLine,
            onValueChange = { addressLine = it },
            label = { Text("Address Line") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Outline
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Spacing.large))

        Button(
            onClick = {
                onSubmit(
                    ShippingAddress(
                        fullName = fullName.trim(),
                        phone = phone.trim(),
                        city = city.trim(),
                        addressLine = addressLine.trim()
                    )
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Proceed to Summary →",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = WhiteColor
            )
        }
    }
}

@Composable
private fun QuoteSummaryContent(
    quote: CheckoutQuote,
    address: ShippingAddress,
    onConfirmOrder: () -> Unit,
    onChangeAddress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(Spacing.medium)
    ) {
        // Address Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(Spacing.medium)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ship To",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Primary
                    )
                    Button(
                        onClick = onChangeAddress,
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariant),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${address.fullName} (${address.phone})",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = DarkNavy
                )
                Text(
                    text = "${address.addressLine}, ${address.city}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.medium))

        // Order Items Summary
        Text(
            text = "Order Items",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Primary
        )

        Spacer(modifier = Modifier.height(Spacing.small))

        quote.items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${item.quantity}x ${item.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkNavy,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = String.format(Locale.US, "%.2f %s", item.totalPrice, quote.currency),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Primary
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.medium))
        Divider(color = Outline.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(Spacing.medium))

        // Payment Method Card
        Text(
            text = "Payment Method",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Primary
        )

        Spacer(modifier = Modifier.height(Spacing.small))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = true,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = Primary)
                )
                Icon(
                    imageVector = Icons.Outlined.Payments,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(start = 4.dp)
                )
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Column {
                    Text(
                        text = "Cash on Delivery",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Primary
                    )
                    Text(
                        text = "Pay upon delivery receipt",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.medium))
        Divider(color = Outline.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(Spacing.medium))

        // Authoritative Price Breakdown
        Text(
            text = "Order Summary",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Primary
        )

        Spacer(modifier = Modifier.height(Spacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Subtotal", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text(
                String.format(Locale.US, "%.2f %s", quote.itemSubtotal, quote.currency),
                style = MaterialTheme.typography.bodyMedium,
                color = DarkNavy
            )
        }

        if (quote.discountTotal > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Discount", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(
                    String.format(Locale.US, "-%.2f %s", quote.discountTotal, quote.currency),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Primary
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Shipping Fee", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text(
                String.format(Locale.US, "%.2f %s", quote.deliveryFee, quote.currency),
                style = MaterialTheme.typography.bodyMedium,
                color = DarkNavy
            )
        }

        Spacer(modifier = Modifier.height(Spacing.small))
        Divider(color = Outline.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(Spacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Total",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Primary
            )
            Text(
                text = String.format(Locale.US, "%.2f %s", quote.finalTotal, quote.currency),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Primary
            )
        }

        Spacer(modifier = Modifier.height(Spacing.large))

        Button(
            onClick = onConfirmOrder,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = String.format(Locale.US, "Confirm Order (%.2f %s)", quote.finalTotal, quote.currency),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = WhiteColor
            )
        }
    }
}

@Composable
private fun OrderSuccessContent(
    orderResult: OrderResult,
    onContinueShopping: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(Spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Primary.copy(alpha = 0.1f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.large))

        Text(
            text = "Order Confirmed!",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Primary
        )

        Spacer(modifier = Modifier.height(Spacing.small))

        Text(
            text = "Order Reference: ${orderResult.orderReference}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = DarkNavy
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        Text(
            text = "Thank you for your purchase. Your Cash on Delivery order has been successfully placed.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = Spacing.medium)
        )

        Spacer(modifier = Modifier.height(Spacing.xLarge))

        Button(
            onClick = onContinueShopping,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Continue Shopping",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = WhiteColor
            )
        }
    }
}
