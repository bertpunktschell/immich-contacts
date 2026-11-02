package com.bertschell.immichcontacts.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.bertschell.immichcontacts.data.ImmichApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manual thumbnail loading via ImmichApi (Ktor) with visible per-tile states.
 * Coil was failing silently for Immich thumbnails; this logs the real HTTP status.
 */
@Composable
fun ThumbnailBox(
    serverUrl: String,
    apiKey: String,
    assetId: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    var bitmap by remember(assetId) { mutableStateOf<Bitmap?>(null) }
    var state by remember(assetId) { mutableStateOf("loading") } // loading | ok | error

    val scope = rememberCoroutineScope()
    LaunchedEffect(assetId) {
        if (serverUrl.isBlank() || apiKey.isBlank() || assetId.isBlank()) {
            state = "error"; return@LaunchedEffect
        }
        scope.launch {
            try {
                val api = ImmichApi(serverUrl, apiKey)
                val bytes = withContext(Dispatchers.IO) { api.getAssetThumbnail(assetId) }
                api.close()
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) { bitmap = bmp; state = "ok" } else state = "error"
            } catch (e: Exception) {
                android.util.Log.e("ThumbnailBox", "thumb failed for $assetId", e)
                state = "error"
            }
        }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        when {
            bitmap != null -> Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            state == "loading" -> CircularProgressIndicator()
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("⚠")
            }
        }
    }
}
