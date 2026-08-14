package com.avenra.app.presentation.account

import com.avenra.app.domain.model.UserProfile

sealed interface AccountUiState {
    data object Loading : AccountUiState
    data class Success(val profile: UserProfile) : AccountUiState
    data object Unauthenticated : AccountUiState
    data class Error(val message: String) : AccountUiState
}

