package com.bertschell.immichcontacts.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bertschell.immichcontacts.data.ContactsRepository
import com.bertschell.immichcontacts.data.ImmichApi
import com.bertschell.immichcontacts.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class CropState(
    val isLoading: Boolean = false,
    val isApplying: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val tempInputPath: String? = null,
    val readyForCrop: Boolean = false,
)

class CropViewModel : ViewModel() {
    var state by mutableStateOf(CropState())
        private set

    fun downloadAndPrepare(context: Context, assetId: String) {
        state = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val settings = SettingsRepository(context)
                val url = settings.serverUrl.first()
                val k = settings.apiKey.first()
                val api = ImmichApi(url, k)

                val imageBytes = withContext(Dispatchers.IO) {
                    api.getAssetOriginal(assetId)
                }
                api.close()

                val tempFile = File(context.cacheDir, "immich_" + assetId + ".jpg")
                withContext(Dispatchers.IO) {
                    tempFile.writeBytes(imageBytes)
                }

                state = state.copy(
                    isLoading = false,
                    tempInputPath = tempFile.absolutePath,
                    readyForCrop = true,
                )
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = "Download fehlgeschlagen: " + (e.message ?: ""))
            }
        }
    }

    fun cropLaunched() {
        state = state.copy(readyForCrop = false)
    }

    fun applyCroppedPhoto(context: Context, contactId: Long, resultUri: Uri, onDone: () -> Unit) {
        state = state.copy(isApplying = true, error = null)
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(resultUri)?.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                } ?: throw IllegalStateException("Konnte zugeschnittenes Bild nicht laden")

                val contactsRepo = ContactsRepository(context)
                val success = withContext(Dispatchers.IO) {
                    contactsRepo.setContactPhoto(contactId, bitmap)
                }

                if (success) {
                    com.bertschell.immichcontacts.data.PhotoDirtyFlag.isDirty = true
                    com.bertschell.immichcontacts.data.PhotoDirtyFlag.dirtyContactId = contactId
                    state = state.copy(isApplying = false, isSuccess = true)
                    onDone()
                } else {
                    state = state.copy(isApplying = false, error = "Konnte Kontaktfoto nicht speichern")
                }

                state.tempInputPath?.let { File(it).delete() }
                File(context.cacheDir, "cropped_" + contactId + ".jpg").delete()
            } catch (e: Exception) {
                state = state.copy(isApplying = false, error = "Fehler: " + (e.message ?: ""))
            }
        }
    }

    fun setError(message: String) {
        state = state.copy(error = message, isLoading = false, isApplying = false)
    }
}
