package com.bertschell.immichcontacts.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.asImageBitmap
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

    // Reload on every resume (e.g. return from crop) so new profile photos
    // show up immediately. Re-load also rewrites the contacts list — avatars
    // are keyed by contact.id + photoRev, which bumps on refresh.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var photoRev by remember { mutableStateOf(0) }
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (hasContactPermission && com.bertschell.immichcontacts.data.PhotoDirtyFlag.isDirty) {
                    com.bertschell.immichcontacts.data.PhotoDirtyFlag.isDirty = false
                    viewModel.loadData(context)
                    photoRev++
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
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

            // Filter chips: Immich-Bilder vorhanden / alle anzeigen
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.showOnlyWithImmich,
                    onClick = { viewModel.onFilterChange(true) },
                    label = { Text("Mit Immich-Bild") },
                )
                FilterChip(
                    selected = !state.showOnlyWithImmich,
                    onClick = { viewModel.onFilterChange(false) },
                    label = { Text("Alle anzeigen") },
                )
            }

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

            val dupNames = remember(contacts) {
                contacts.groupingBy { it.displayName.trim().lowercase() }.eachCount()
                    .filterValues { it > 1 }.keys
            }
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(contacts, key = { it.id }) { contact ->
                    val person = viewModel.getPersonForContact(contact.id)
                    ContactRow(
                        contact = contact,
                        isDuplicate = contact.displayName.trim().lowercase() in dupNames,
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
    isDuplicate: Boolean = false,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(contact.displayName, fontWeight = FontWeight.Medium)
                if (isDuplicate) {
                    Text(
                        "  ⚠ Duplikat",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (contact.accountTypes.isNotEmpty()) {
                    Text(
                        "  [" + contact.accountTypes.joinToString() + "]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
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
        leadingContent = { ContactAvatar(contact = contact) },
        trailingContent = if (matchedPersonName != null) {
            { Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        } else null,
        modifier = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider()
}

@Composable
private fun ContactAvatar(contact: ContactInfo, sizeDp: Int = 40) {
    val contextForAvatars = androidx.compose.ui.platform.LocalContext.current
    var bitmap by remember(contact.id, contact.photoUri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(contact.id, contact.photoUri) {
        if (bitmap != null) return@LaunchedEffect
        val bmp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                when {
                    contact.photoUri != null ->
                        contextForAvatars.contentResolver.openInputStream(
                            android.net.Uri.parse(contact.photoUri)
                        )?.use { android.graphics.BitmapFactory.decodeStream(it) }
                    else ->
                        // Fallback: full-res photo via contact id
                        android.provider.ContactsContract.Contacts.openContactPhotoInputStream(
                            contextForAvatars.contentResolver,
                            android.content.ContentUris.withAppendedId(
                                android.provider.ContactsContract.Contacts.CONTENT_URI, contact.id),
                            true
                        )?.use { android.graphics.BitmapFactory.decodeStream(it) }
                }
            } catch (e: Exception) {
                android.util.Log.w("ContactAvatar", "photo load failed for ${'$'}{contact.id}", e)
                null
            }
        }
        bitmap = bmp
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(androidx.compose.foundation.shape.CircleShape),
        )
    } else {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Text(
                contact.displayName.trim().take(1).uppercase().ifBlank { "?" },
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
