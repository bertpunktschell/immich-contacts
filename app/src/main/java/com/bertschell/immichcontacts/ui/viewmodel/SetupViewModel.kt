package com.bertschell.immichcontacts.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bertschell.immichcontacts.data.ImmichApi
import com.bertschell.immichcontacts.data.SettingsRepository
import kotlinx.coroutines.launch

data class SetupState(
    val url: String = "",
    val apiKey: String = "",
    val showApiKey: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class SetupViewModel : ViewModel() {
    var state by mutableStateOf(SetupState())
        private set

    fun onUrlChange(value: String) {
        state = state.copy(url = value, error = null)
    }

    fun onApiKeyChange(value: String) {
        state = state.copy(apiKey = value, error = null)
    }

    fun toggleShowApiKey() {
        state = state.copy(showApiKey = !state.showApiKey)
    }

    fun testAndSave(context: Context, onSuccess: () -> Unit) {
        state = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val repo = SettingsRepository(context)
                val url = state.url.trimEnd('/')
                val api = ImmichApi(url, state.apiKey)
                val user = api.testConnection()
                api.close()
                repo.saveConnection(url, state.apiKey)
                state = state.copy(isLoading = false)
                onSuccess()
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    error = "Verbindung fehlgeschlagen: ${e.message ?: "Unbekannter Fehler"}"
                )
            }
        }
    }
}