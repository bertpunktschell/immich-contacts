package com.bertschell.immichcontacts.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
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
import com.bertschell.immichcontacts.ui.viewmodel.PersonAssetsViewModel
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonAssetsScreen(
    personId: String,
    personName: String,
    contactId: Long,
    contactName: String,
    onAssetSelected: (assetId: String) -> Unit,
    onBrowseLibrary: () -> Unit,
    onBack: () -> Unit,
    viewModel: PersonAssetsViewModel = viewModel()
) {
    val context = LocalContext.current
    val state = viewModel.state

    var serverUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }

    LaunchedEffect(personId) {
        val settings = SettingsRepository(context)
        serverUrl = settings.serverUrl.first()
        apiKey = settings.apiKey.first()
        viewModel.loadAssets(context, personId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (personName.isNotBlank()) personName else "Immich Person") },
                actions = {
                    TextButton(onClick = onBrowseLibrary) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Bibliothek")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text(
                "Bilder von ${personName.ifBlank { "dieser Person" }} — tippe ein Bild an um es für \"$contactName\" zuzuschneiden",
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

            if (state.assets.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Keine Bilder für diese Person gefunden.")
                }
                return@Column
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(state.assets, key = { it.id }) { asset ->
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
