package com.avenra.app.presentation.home

import com.avenra.app.domain.model.HomeData

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val homeData: HomeData) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
