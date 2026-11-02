package com.bertschell.immichcontacts.ui.screens

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
import com.bertschell.immichcontacts.data.SettingsRepository
import com.bertschell.immichcontacts.ui.viewmodel.LibraryViewModel
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onAssetSelected: (assetId: String, contactId: Long, contactName: String) -> Unit,
    onBack: () -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val context = LocalContext.current
    val state = viewModel.state

    var serverUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val settings = SettingsRepository(context)
        serverUrl = settings.serverUrl.first()
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
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        if (state.error != null) {
            Text(
                text = state.error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(padding).padding(16.dp),
            )
            return@Scaffold
        }

        if (state.selectedBucket == null) {
            // Show month buckets as horizontal scrollable chips
            Column(modifier = Modifier.padding(padding)) {
                LazyRow(
                    contentPadding = PaddingValues(16.dp),
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
                Text(
                    "Tippe auf einen Monat um die Bilder zu sehen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        } else {
            // Show assets in grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(padding),
            ) {
                items(state.currentBucketAssets, key = { it.id }) { asset ->
                    AsyncImage(
                        model = if (serverUrl.isNotBlank()) {
                            "${serverUrl.trimEnd('/')}/api/assets/${asset.id}/thumbnail"
                        } else null,
                        contentDescription = asset.originalFileName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }
}