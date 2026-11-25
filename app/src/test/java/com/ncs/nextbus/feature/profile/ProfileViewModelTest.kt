package com.ncs.nextbus.feature.profile

import app.cash.turbine.test
import com.ncs.nextbus.core.model.ThemeMode
import com.ncs.nextbus.core.model.EmergencyContact
import com.ncs.nextbus.testing.FakePreferencesRepository
import com.ncs.nextbus.testing.FakeUserDataRepository
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
    private val viewModel by lazy { ProfileViewModel(preferences, FakeUserDataRepository()) }

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

    @Test
    fun `contact validation rejects blank names, bad numbers, duplicates and overflow`() {
        val existing = listOf(EmergencyContact("Asha", "9876543210"))
        assertEquals(ContactError.NAME_REQUIRED, ProfileViewModel.validateContact(" ", "9876543211", existing))
        assertEquals(ContactError.INVALID_PHONE, ProfileViewModel.validateContact("Ravi", "12345", existing))
        assertEquals(ContactError.DUPLICATE, ProfileViewModel.validateContact("Ravi", "98765 43210", existing))
        val full = (1..ProfileViewModel.MAX_CONTACTS).map { EmergencyContact("C$it", "98765432%02d".format(it)) }
        assertEquals(ContactError.LIMIT_REACHED, ProfileViewModel.validateContact("Ravi", "9123456789", full))
        assertEquals(null, ProfileViewModel.validateContact("Ravi", "+91 91234-56789", existing))
    }

    @Test
    fun `valid contact is saved normalised`() = runTest {
        viewModel.uiState.test {
            while (awaitItem() !is ProfileUiState.Ready) Unit
            assertEquals(null, viewModel.addEmergencyContact("Ravi", "+91 (912) 345-6789"))
            assertEquals(listOf(EmergencyContact("Ravi", "+919123456789")), preferences.state.value.emergencyContacts)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
