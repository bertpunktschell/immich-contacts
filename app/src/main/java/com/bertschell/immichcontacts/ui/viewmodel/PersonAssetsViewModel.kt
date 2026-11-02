package com.bertschell.immichcontacts.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bertschell.immichcontacts.data.ImmichApi
import com.bertschell.immichcontacts.data.SettingsRepository
import com.bertschell.immichcontacts.data.model.ImmichAsset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PersonAssetsState(
    val isLoading: Boolean = false,
    val assets: List<ImmichAsset> = emptyList(),
    val error: String? = null,
)

class PersonAssetsViewModel : ViewModel() {
    var state by mutableStateOf(PersonAssetsState())
        private set

    fun loadAssets(context: Context, personId: String) {
        state = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val settings = SettingsRepository(context)
                val url = settings.serverUrl.first()
                val apiKey = settings.apiKey.first()
                val api = ImmichApi(url, apiKey)
                val response = api.getAssetsForPerson(personId, size = 100)
                state = state.copy(isLoading = false, assets = response.assets.items)
                api.close()
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message ?: "Fehler")
            }
        }
    }
}