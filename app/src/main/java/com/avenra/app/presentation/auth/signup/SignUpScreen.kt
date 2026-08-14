package com.avenra.app.presentation.auth.signup

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avenra.app.R
import com.avenra.app.ui.components.PrimaryButton
import com.avenra.app.ui.theme.DarkNavy
import com.avenra.app.ui.theme.ErrorColor
import com.avenra.app.ui.theme.InputShape
import com.avenra.app.ui.theme.Outline
import com.avenra.app.ui.theme.Primary
import com.avenra.app.ui.theme.Spacing
import com.avenra.app.ui.theme.SurfaceVariant
import com.avenra.app.ui.theme.TextSecondary
import com.avenra.app.ui.theme.WhiteColor

@Composable
fun SignUpScreen(
    onNavigateToSignIn: () -> Unit,
    onSignUpSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignUpViewModel = viewModel(
        factory = SignUpViewModel.provideFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onSignUpSuccess()
        }
    }

    Scaffold(
        containerColor = WhiteColor,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.large, vertical = Spacing.xLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Brand Logo Lockup
            Image(
                painter = painterResource(id = R.drawable.ic_avenra_logo_horizontal),
                contentDescription = "Avenra Logo",
                modifier = Modifier.height(36.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Greeting Headers
            Text(
                text = "Welcome To Avenra",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = DarkNavy,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Create your account to start shopping",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // General Error Banner
            if (!uiState.generalError.isNullOrBlank()) {
                Surface(
                    shape = InputShape,
                    color = ErrorColor.copy(alpha = 0.1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.medium)
                ) {
                    Text(
                        text = uiState.generalError ?: "",
                        color = ErrorColor,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(Spacing.medium),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Full Name Input Field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Full Name",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = DarkNavy,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = uiState.fullName,
                    onValueChange = { viewModel.onFullNameChange(it) },
                    placeholder = { Text("enter your full name") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = DarkNavy.copy(alpha = 0.6f)
                        )
                    },
                    isError = !uiState.fullNameError.isNullOrBlank(),
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    shape = InputShape,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceVariant,
                        unfocusedContainerColor = SurfaceVariant,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Outline,
                        focusedTextColor = DarkNavy,
                        unfocusedTextColor = DarkNavy
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (!uiState.fullNameError.isNullOrBlank()) {
                    Text(
                        text = uiState.fullNameError ?: "",
                        color = ErrorColor,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = Spacing.small, top = Spacing.xSmall)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Mobile Number Input Field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Mobile Number",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = DarkNavy,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = uiState.mobileNumber,
                    onValueChange = { viewModel.onMobileChange(it) },
                    placeholder = { Text("enter your mobile no.") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Phone,
                            contentDescription = null,
                            tint = DarkNavy.copy(alpha = 0.6f)
                        )
                    },
                    isError = !uiState.mobileError.isNullOrBlank(),
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    shape = InputShape,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceVariant,
                        unfocusedContainerColor = SurfaceVariant,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Outline,
                        focusedTextColor = DarkNavy,
                        unfocusedTextColor = DarkNavy
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (!uiState.mobileError.isNullOrBlank()) {
                    Text(
                        text = uiState.mobileError ?: "",
                        color = ErrorColor,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = Spacing.small, top = Spacing.xSmall)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Email Input Field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "E-mail address",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = DarkNavy,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = { viewModel.onEmailChange(it) },
                    placeholder = { Text("enter your email address") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = null,
                            tint = DarkNavy.copy(alpha = 0.6f)
                        )
                    },
                    isError = !uiState.emailError.isNullOrBlank(),
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    shape = InputShape,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceVariant,
                        unfocusedContainerColor = SurfaceVariant,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Outline,
                        focusedTextColor = DarkNavy,
                        unfocusedTextColor = DarkNavy
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (!uiState.emailError.isNullOrBlank()) {
                    Text(
                        text = uiState.emailError ?: "",
                        color = ErrorColor,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = Spacing.small, top = Spacing.xSmall)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Password Input Field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Password",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = DarkNavy,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = { viewModel.onPasswordChange(it) },
                    placeholder = { Text("enter your password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = DarkNavy.copy(alpha = 0.6f)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                            Icon(
                                imageVector = if (uiState.isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (uiState.isPasswordVisible) "Hide password" else "Show password",
                                tint = DarkNavy.copy(alpha = 0.6f)
                            )
                        }
                    },
                    visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = !uiState.passwordError.isNullOrBlank(),
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    shape = InputShape,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.signUp()
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceVariant,
                        unfocusedContainerColor = SurfaceVariant,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Outline,
                        focusedTextColor = DarkNavy,
                        unfocusedTextColor = DarkNavy
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (!uiState.passwordError.isNullOrBlank()) {
                    Text(
                        text = uiState.passwordError ?: "",
                        color = ErrorColor,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = Spacing.small, top = Spacing.xSmall)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Sign Up Button
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color = Primary,
                    modifier = Modifier.size(36.dp)
                )
            } else {
                PrimaryButton(
                    text = "Sign Up",
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.signUp()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(Spacing.large))

            // Link to Sign In
            Row(
                modifier = Modifier.padding(vertical = Spacing.small),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkNavy
                )
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Primary,
                    modifier = Modifier.clickable { onNavigateToSignIn() }
                )
            }
        }
    }
}
