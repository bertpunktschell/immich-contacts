package com.bertschell.immichcontacts.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.ui.text.font.FontWeight
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
        viewModel.loadAllAssets(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Bibliothek")
                },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
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
                ) { com.bertschell.immichcontacts.ui.PulsingLoader() }
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

            // Immich-style timeline: continuous grid with month headers
            // Assets arrive date-desc from search/metadata; group by month.
            val timelineItems = remember(state.currentBucketAssets) {
                val fmt = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.GERMANY)
                buildList {
                    var lastMonth = ""
                    for (asset in state.currentBucketAssets) {
                        val date = try {
                            java.time.LocalDateTime.parse(asset.localDateTime.ifBlank { asset.fileCreatedAt }.take(19))
                        } catch (e: Exception) { null }
                        val month = date?.let { it.format(fmt) } ?: "Unbekannt"
                        if (month != lastMonth) {
                            add(TimelineEntry.Header(month, date))
                            lastMonth = month
                        }
                        add(TimelineEntry.AssetEntry(asset))
                    }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(timelineItems, key = { entry ->
                    when (entry) {
                        is TimelineEntry.Header -> "h-" + entry.label + "-" + entry.date
                        is TimelineEntry.AssetEntry -> entry.asset.id
                    }
                }, span = { entry ->
                    if (entry is TimelineEntry.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1)
                }) { entry ->
                    when (entry) {
                        is TimelineEntry.Header -> {
                            // Sticky-style month header
                            Text(
                                text = if (entry.date != null && entry.date.year == java.time.LocalDate.now().year) {
                                    entry.label.substringBefore(" ") + " " + entry.date.year
                                } else entry.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                        }
                        is TimelineEntry.AssetEntry -> {
                            val asset = entry.asset
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .fillMaxWidth()
                                    .clickable { if (asset.id.isNotBlank()) onAssetSelected(asset.id) },
                            ) {
                                com.bertschell.immichcontacts.ui.ThumbnailBox(
                                    serverUrl = serverUrl,
                                    apiKey = apiKey,
                                    assetId = asset.id,
                                    contentDescription = asset.originalFileName,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed interface TimelineEntry {
    data class Header(val label: String, val date: java.time.LocalDateTime?) : TimelineEntry
    data class AssetEntry(val asset: com.bertschell.immichcontacts.data.model.ImmichAsset) : TimelineEntry
}
