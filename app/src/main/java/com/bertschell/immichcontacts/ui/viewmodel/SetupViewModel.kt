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
                val url = state.url.trim().trimEnd('/')
                val key = state.apiKey.trim()
                if (url.isBlank() || state.apiKey.isBlank()) {
                    state = state.copy(isLoading = false, error = "URL und API Key duerfen nicht leer sein")
                    return@launch
                }
                val repo = SettingsRepository(context)
                val api = ImmichApi(url, key)
                val user = api.testConnection()
                api.close()
                repo.saveConnection(url, key)
                state = state.copy(isLoading = false)
                onSuccess()
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("Unknown host") == true -> "Server nicht erreichbar: URL korrekt?"
                    e.message?.contains("timeout") == true -> "Timeout: Server zu langsam oder nicht erreichbar"
                    e.message?.contains("401") == true || e.message?.contains("403") == true -> "API Key unguelig"
                    e.message?.contains("SSL") == true -> "SSL/TLS Fehler: Zertifikat Problem?"
                    else -> "Fehler: " + (e.message ?: e::class.simpleName ?: "Unbekannt")
                }
                state = state.copy(isLoading = false, error = msg)
            }
        }
    }
}
