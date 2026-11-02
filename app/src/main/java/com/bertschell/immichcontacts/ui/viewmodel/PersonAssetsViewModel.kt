package com.bertschell.immichcontacts.ui.viewmodel

import android.content.Context
import android.util.Log
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
    private val TAG = "PersonAssetsVM"

    fun loadAssets(context: Context, personId: String) {
        state = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val settings = SettingsRepository(context)
                val url = settings.serverUrl.first()
                val k = settings.apiKey.first()
                val api = ImmichApi(url, k)
                val response = api.getAssetsForPerson(personId, size = 100)
                val assets = response.assets.items
                state = state.copy(isLoading = false, assets = assets)
                api.close()
            } catch (e: Exception) {
                Log.e(TAG, "loadAssets error for person $personId", e)
                state = state.copy(isLoading = false, error = "Fehler: ${e.message ?: e::class.simpleName}")
            }
        }
    }
}
