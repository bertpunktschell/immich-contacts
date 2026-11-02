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
import com.bertschell.immichcontacts.data.model.ImmichTimelineBucket
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class LibraryState(
    val isLoading: Boolean = false,
    val buckets: List<ImmichTimelineBucket> = emptyList(),
    val currentBucketAssets: List<ImmichAsset> = emptyList(),
    val selectedBucket: String? = null,
    val error: String? = null,
)

class LibraryViewModel : ViewModel() {
    var state by mutableStateOf(LibraryState())
        private set

    fun loadBuckets(context: Context) {
        state = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val settings = SettingsRepository(context)
                val url = settings.serverUrl.first()
                val k = settings.apiKey.first()
                val api = ImmichApi(url, k)
                val buckets = api.getTimelineBuckets()
                state = state.copy(isLoading = false, buckets = buckets)
                api.close()
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message ?: "Fehler")
            }
        }
    }

    fun loadBucketAssets(context: Context, bucket: String) {
        state = state.copy(isLoading = true, selectedBucket = bucket, error = null)
        viewModelScope.launch {
            try {
                val settings = SettingsRepository(context)
                val url = settings.serverUrl.first()
                val k = settings.apiKey.first()
                val api = ImmichApi(url, k)
                val assets = api.getTimelineBucket(bucket)
                state = state.copy(isLoading = false, currentBucketAssets = assets)
                api.close()
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message ?: "Fehler")
            }
        }
    }

    fun backToBuckets() {
        state = state.copy(selectedBucket = null, currentBucketAssets = emptyList())
    }
}
