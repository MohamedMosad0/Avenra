package com.avenra.app.presentation.account

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avenra.app.R
import com.avenra.app.domain.model.UserProfile
import com.avenra.app.ui.components.ErrorState
import com.avenra.app.ui.components.LoadingState
import com.avenra.app.ui.components.PrimaryButton
import com.avenra.app.ui.theme.DarkNavy
import com.avenra.app.ui.theme.ErrorColor
import com.avenra.app.ui.theme.Outline
import com.avenra.app.ui.theme.Primary
import com.avenra.app.ui.theme.Spacing
import com.avenra.app.ui.theme.SurfaceVariant
import com.avenra.app.ui.theme.TextSecondary
import com.avenra.app.ui.theme.WhiteColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    modifier: Modifier = Modifier,
    onNavigateToSignIn: () -> Unit = {},
    onNavigateToSignUp: () -> Unit = {},
    viewModel: AccountViewModel = viewModel(
        factory = AccountViewModel.provideFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_avenra_logo_mark),
                        contentDescription = "Avenra Logo",
                        modifier = Modifier.requiredSize(28.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WhiteColor)
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
                is AccountUiState.Loading -> {
                    LoadingState(
                        message = "Loading profile...",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is AccountUiState.Error -> {
                    ErrorState(
                        title = "Failed to Load Profile",
                        message = state.message,
                        onRetry = { viewModel.loadUserProfile() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is AccountUiState.Unauthenticated -> {
                    UnauthenticatedAccountContent(
                        onNavigateToSignIn = onNavigateToSignIn,
                        onNavigateToSignUp = onNavigateToSignUp,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is AccountUiState.Success -> {
                    AccountProfileContent(
                        profile = state.profile,
                        onSignOut = { viewModel.signOut() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun UnauthenticatedAccountContent(
    onNavigateToSignIn: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = Spacing.large, vertical = Spacing.xLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = SurfaceVariant,
            modifier = Modifier.size(88.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.large))

        Text(
            text = "Welcome to Avenra",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            color = DarkNavy
        )

        Spacer(modifier = Modifier.height(Spacing.small))

        Text(
            text = "Sign in to view your profile, manage orders, and checkout faster.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.medium)
        )

        Spacer(modifier = Modifier.height(Spacing.xLarge))

        PrimaryButton(
            text = "Sign In",
            onClick = onNavigateToSignIn,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        OutlinedButton(
            onClick = onNavigateToSignUp,
            shape = RoundedCornerShape(40.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Primary
            )
        }
    }
}

@Composable
private fun AccountProfileContent(
    profile: UserProfile,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstName = profile.fullName.split(" ").firstOrNull() ?: profile.fullName

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.medium, vertical = Spacing.small)
    ) {
        // Greeting Header
        Text(
            text = "Welcome, $firstName",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = Primary
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = profile.email,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(Spacing.large))

        // Profile Fields
        ProfileFieldCard(
            label = "Your full name",
            value = profile.fullName.ifBlank { "Not set" }
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        ProfileFieldCard(
            label = "Your E-mail",
            value = profile.email
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        ProfileFieldCard(
            label = "Your password",
            value = profile.passwordMasked
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        ProfileFieldCard(
            label = "Your mobile number",
            value = profile.mobileNumber.ifBlank { "Not set" }
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        ProfileFieldCard(
            label = "Your Address",
            value = profile.address.ifBlank { "Not set" }
        )

        Spacer(modifier = Modifier.height(Spacing.large))

        // Sign Out Button
        OutlinedButton(
            onClick = onSignOut,
            shape = RoundedCornerShape(40.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                contentDescription = "Sign Out",
                tint = ErrorColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "  Sign Out",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = ErrorColor
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xLarge))
    }
}

@Composable
private fun ProfileFieldCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            ),
            color = DarkNavy,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
