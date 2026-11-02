package com.bertschell.immichcontacts.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bertschell.immichcontacts.data.ImmichApi
import com.bertschell.immichcontacts.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SettingsState(
    val isLoading: Boolean = false,
    val serverUrl: String = "",
    val apiKeyPreview: String = "",
    val userName: String? = null,
    val error: String? = null,
)

class SettingsViewModel : ViewModel() {
    var state by mutableStateOf(SettingsState())
        private set

    fun loadSettings(context: Context) {
        state = state.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val repo = SettingsRepository(context)
                val url = repo.serverUrl.first()
                val k = repo.apiKey.first()
                val api = ImmichApi(url, k)
                val user = api.testConnection()
                api.close()
                state = state.copy(
                    isLoading = false,
                    serverUrl = url,
                    apiKeyPreview = k.take(6) + "--------" + k.takeLast(4),
                    userName = user.name,
                )
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun disconnect(context: Context, onDone: () -> Unit) {
        viewModelScope.launch {
            val repo = SettingsRepository(context)
            repo.clearConnection()
            onDone()
        }
    }
}
