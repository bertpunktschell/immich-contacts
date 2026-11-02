package com.bertschell.immichcontacts.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.bertschell.immichcontacts.ui.viewmodel.PersonAssetsViewModel
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonAssetsScreen(
    personId: String,
    personName: String,
    onAssetSelected: (assetId: String, contactId: Long, contactName: String) -> Unit,
    onBack: () -> Unit,
    viewModel: PersonAssetsViewModel = viewModel()
) {
    val context = LocalContext.current
    val state = viewModel.state

    // TODO: contactId/contactName should be passed from ContactsScreen
    // For now using placeholder — will be wired through navigation args
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
                navigationIcon = {
                    IconButton(onClick = onBack) {
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

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(padding),
        ) {
            items(state.assets, key = { it.id }) { asset ->
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