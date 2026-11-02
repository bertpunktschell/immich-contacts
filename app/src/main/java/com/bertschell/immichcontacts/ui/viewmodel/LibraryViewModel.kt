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
import com.bertschell.immichcontacts.data.model.ImmichTimelineBucket
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class LibraryState(
    val isLoading: Boolean = false,
    val buckets: List<ImmichTimelineBucket> = emptyList(),
    val currentBucketAssets: List<ImmichAsset> = emptyList(),
    val allAssets: List<ImmichAsset> = emptyList(),
    val selectedBucket: String? = null,
    val error: String? = null,
)

class LibraryViewModel : ViewModel() {
    var state by mutableStateOf(LibraryState())
        private set
    private val TAG = "LibraryViewModel"

    fun loadBuckets(context: Context) {
        state = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val settings = SettingsRepository(context)
                val url = settings.serverUrl.first()
                val k = settings.apiKey.first()
                val api = ImmichApi(url, k)

                // Load buckets for the month chips
                val buckets = try {
                    api.getTimelineBuckets()
                } catch (e: Exception) {
                    Log.w(TAG, "Buckets failed, falling back to all images", e)
                    emptyList()
                }

                // Also load first page of all images via search/metadata
                val searchResp = api.getAllImages(page = 1, size = 100)
                val assets = searchResp.assets.items
                Log.d(TAG, "Loaded ${assets.size} assets, total=${searchResp.assets.total}")
                Log.d(TAG, "Loaded ${buckets.size} buckets")

                state = state.copy(
                    isLoading = false,
                    buckets = buckets,
                    allAssets = assets,
                    currentBucketAssets = assets,  // Show all images by default
                )
                api.close()
            } catch (e: Exception) {
                Log.e(TAG, "loadBuckets error", e)
                state = state.copy(isLoading = false, error = "Fehler: ${e.message ?: e::class.simpleName}")
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

                // Use search/metadata with takenAfter/takenBefore for the month
                // bucket format is "2026-10-01" — filter by that month
                val parts = bucket.split("-")
                val year = parts[0].toInt()
                val month = parts[1].toInt()

                // Calculate next month for takenBefore
                val nextMonthYear = if (month == 12) year + 1 else year
                val nextMonth = if (month == 12) 1 else month + 1
                val takenAfter = "${year}-${month.toString().padStart(2, '0')}-01T00:00:00.000Z"
                val takenBefore = "${nextMonthYear}-${nextMonth.toString().padStart(2, '0')}-01T00:00:00.000Z"

                val response = api.getAllImages(page = 1, size = 500)
                // Filter by date client-side as fallback
                val assets = response.assets.items.filter { asset ->
                    asset.fileCreatedAt.startsWith("${year}-${month.toString().padStart(2, '0')}")
                }
                Log.d(TAG, "Bucket $bucket: ${assets.size} assets filtered from ${response.assets.items.size}")

                state = state.copy(isLoading = false, currentBucketAssets = assets)
                api.close()
            } catch (e: Exception) {
                Log.e(TAG, "loadBucketAssets error", e)
                state = state.copy(isLoading = false, error = "Fehler: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    fun backToBuckets() {
        state = state.copy(selectedBucket = null, currentBucketAssets = state.allAssets)
    }
}
