package com.bertschell.immichcontacts.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import com.bertschell.immichcontacts.data.ImmichImageLoader
import com.bertschell.immichcontacts.data.SettingsRepository
import com.bertschell.immichcontacts.ui.viewmodel.LibraryViewModel
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    contactId: Long,
    contactName: String,
    onAssetSelected: (assetId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val context = LocalContext.current
    val state = viewModel.state

    var serverUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val settings = SettingsRepository(context)
        serverUrl = settings.serverUrl.first()
        apiKey = settings.apiKey.first()
        viewModel.loadBuckets(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.selectedBucket != null) "Bilder ${state.selectedBucket}" else "Bibliothek")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.selectedBucket != null) viewModel.backToBuckets()
                        else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text(
                "Wähle ein Bild für \"$contactName\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
                return@Column
            }

            if (state.error != null) {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
                return@Column
            }

            if (state.currentBucketAssets.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Keine Bilder gefunden.")
                }
                return@Column
            }

            // Show month buckets as chips at top
            if (state.buckets.isNotEmpty() && state.selectedBucket == null) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.buckets) { bucket ->
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.loadBucketAssets(context, bucket.timeBucket) },
                            label = { Text("${bucket.timeBucket.take(7)} (${bucket.count})") },
                        )
                    }
                }
            }

            // Show assets in grid — tappable
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(state.currentBucketAssets, key = { it.id }) { asset ->
                    AsyncImage(
                        model = ImmichImageLoader.thumbnailRequest(context, serverUrl, apiKey, asset.id),
                        contentDescription = asset.originalFileName,
                        contentScale = ContentScale.Crop,
                        error = painterResource(android.R.drawable.ic_menu_report_image),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxWidth()
                            .clickable { if (asset.id.isNotBlank()) onAssetSelected(asset.id) },
                    )
                }
            }
        }
    }
}
