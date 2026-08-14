package com.avenra.app.domain.model

import com.avenra.app.presentation.account.AccountUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountTest {

    @Test
    fun userProfile_creation_preservesRequiredFields() {
        val profile = UserProfile(
            id = "usr_123",
            fullName = "Mohamed Mohamed Nabil",
            email = "mohamed.N@gmail.com",
            mobileNumber = "01122118855",
            address = "6th October, street 11.....",
            isLoggedIn = true
        )
        assertEquals("usr_123", profile.id)
        assertEquals("Mohamed Mohamed Nabil", profile.fullName)
        assertEquals("mohamed.N@gmail.com", profile.email)
        assertEquals("01122118855", profile.mobileNumber)
        assertEquals("6th October, street 11.....", profile.address)
        assertEquals("******************", profile.passwordMasked)
        assertTrue(profile.isLoggedIn)
    }

    @Test
    fun accountUiState_success_containsProfile() {
        val profile = UserProfile(
            fullName = "Test User",
            email = "test@example.com",
            mobileNumber = "01000000000",
            address = "Test Address"
        )
        val state: AccountUiState = AccountUiState.Success(profile)
        assertTrue(state is AccountUiState.Success)
        assertEquals("Test User", (state as AccountUiState.Success).profile.fullName)
    }

    @Test
    fun accountUiState_unauthenticated_isHandled() {
        val state: AccountUiState = AccountUiState.Unauthenticated
        assertTrue(state is AccountUiState.Unauthenticated)
    }
}
