package com.bertschell.immichcontacts.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bertschell.immichcontacts.data.ContactsRepository
import com.bertschell.immichcontacts.data.ImmichApi
import com.bertschell.immichcontacts.data.SettingsRepository
import com.bertschell.immichcontacts.ui.viewmodel.CropViewModel
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(
    assetId: String,
    contactId: Long,
    contactName: String,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: CropViewModel = viewModel()
) {
    val context = LocalContext.current
    val state = viewModel.state
    val scope = rememberCoroutineScope()

    // Download original image and launch uCrop
    LaunchedEffect(assetId) {
        viewModel.downloadAndPrepare(context, assetId)
    }

    // Listen for uCrop result
    val cropResultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data ?: Intent())
            if (resultUri != null) {
                viewModel.applyCroppedPhoto(context, contactId, resultUri, onDone)
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val error = UCrop.getError(result.data ?: Intent())
            viewModel.setError(error?.message ?: "Crop fehlgeschlagen")
        }
    }

    // Launch uCrop when image is ready
    LaunchedEffect(state.readyForCrop, state.tempInputPath) {
        if (state.readyForCrop && state.tempInputPath != null) {
            val inputUri = Uri.fromFile(File(state.tempInputPath))
            val outputUri = Uri.fromFile(File(context.cacheDir, "cropped_${assetId}.jpg"))

            val uCropIntent = UCrop.of(inputUri, outputUri)
                .withAspectRatio(1f, 1f)  // Square for contact photos
                .withMaxResultSize(512, 512)
                .withOptions(UCrop.Options().apply {
                    setCompressionFormat(Bitmap.CompressFormat.JPEG)
                    setCompressionQuality(90)
                    setHideBottomControls(false)
                    setFreeStyleCropEnabled(true)
                    setToolbarTitle("Zuschneiden für $contactName")
                })
                .getIntent(context)

            cropResultLauncher.launch(uCropIntent)
            viewModel.cropLaunched()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Foto für $contactName") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Lade Originalbild von Immich...")
                }
                state.error != null -> {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBack) { Text("Zurück") }
                }
                state.isApplying -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Speichere Kontaktfoto...")
                }
                state.isSuccess -> {
                    Text("✅ Profilfoto gesetzt für $contactName", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onDone) { Text("Fertig") }
                }
                else -> {
                    Text("Bereite Bild vor...")
                }
            }
        }
    }
}