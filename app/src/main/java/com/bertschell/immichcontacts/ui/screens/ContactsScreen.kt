package com.bertschell.immichcontacts.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bertschell.immichcontacts.data.ContactInfo
import com.bertschell.immichcontacts.ui.viewmodel.ContactsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onContactWithPerson: (ContactInfo, String, String) -> Unit,
    onContactBrowseLibrary: (ContactInfo) -> Unit,
    onSettings: () -> Unit,
    viewModel: ContactsViewModel = viewModel()
) {
    val context = LocalContext.current
    val state = viewModel.state

    var hasContactPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasContactPermission = permissions[Manifest.permission.READ_CONTACTS] == true ||
                               permissions[Manifest.permission.WRITE_CONTACTS] == true
        if (hasContactPermission) {
            viewModel.loadData(context)
        }
    }

    LaunchedEffect(hasContactPermission) {
        if (hasContactPermission && state.contacts.isEmpty() && !state.isLoading) {
            viewModel.loadData(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kontakte") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        if (!hasContactPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Kontakt-Berechtigung benötigt",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Die App braucht Zugriff auf deine Kontakte, um Profilfotos zu setzen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = {
                    permissionLauncher.launch(arrayOf(
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS
                    ))
                }) {
                    Text("Berechtigung erteilen")
                }
            }
            return@Scaffold
        }

        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchChange,
                placeholder = { Text("Kontakte suchen...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
            )

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    com.bertschell.immichcontacts.ui.PulsingLoader()
                }
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

            val contacts = viewModel.filteredContacts
            Text(
                text = "${contacts.size} Kontakte" +
                    if (state.matchedPersonIds.isNotEmpty()) " · ${state.matchedPersonIds.size} Immich-Personen zugeordnet" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(contacts, key = { it.id }) { contact ->
                    val person = viewModel.getPersonForContact(contact.id)
                    ContactRow(
                        contact = contact,
                        matchedPersonName = person?.name,
                        onClick = {
                            if (person != null) {
                                onContactWithPerson(contact, person.id, person.name)
                            } else {
                                onContactBrowseLibrary(contact)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactRow(
    contact: ContactInfo,
    matchedPersonName: String?,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(contact.displayName, fontWeight = FontWeight.Medium) },
        supportingContent = {
            if (matchedPersonName != null) {
                Text(
                    "Immich: $matchedPersonName · Tippe für Bilder",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else if (contact.hasPhoto) {
                Text(
                    "Profilfoto vorhanden · Tippe für Bibliothek",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "Kein Foto · Tippe für Bibliothek",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        leadingContent = {
            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(40.dp))
        },
        trailingContent = if (matchedPersonName != null) {
            { Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        } else null,
        modifier = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider()
}