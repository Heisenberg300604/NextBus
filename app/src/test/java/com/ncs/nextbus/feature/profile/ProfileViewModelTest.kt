package com.ncs.nextbus.feature.profile

import app.cash.turbine.test
import com.ncs.nextbus.core.model.ThemeMode
import com.ncs.nextbus.testing.FakePreferencesRepository
import com.ncs.nextbus.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferences = FakePreferencesRepository()
    // Created lazily so viewModelScope binds to the test Main dispatcher installed by the rule.
    private val viewModel by lazy { ProfileViewModel(preferences) }

    @Test
    fun `starts loading then shows stored preferences`() = runTest {
        viewModel.uiState.test {
            val ready = awaitItem() as? ProfileUiState.Ready ?: awaitItem() as ProfileUiState.Ready
            assertEquals(ThemeMode.SYSTEM, ready.preferences.themeMode)
        }
    }

    @Test
    fun `theme and dynamic colour changes are persisted`() = runTest {
        viewModel.setThemeMode(ThemeMode.DARK)
        viewModel.setDynamicColor(true)
        assertEquals(ThemeMode.DARK, preferences.state.value.themeMode)
        assertTrue(preferences.state.value.useDynamicColor)
    }

    @Test
    fun `display name is trimmed and length-validated`() = runTest {
        assertTrue(viewModel.setDisplayName("  Asha  "))
        assertEquals("Asha", preferences.state.value.displayName)

        assertFalse(viewModel.setDisplayName("x".repeat(ProfileViewModel.MAX_NAME_LENGTH + 1)))
        assertEquals("Asha", preferences.state.value.displayName)
    }
}
