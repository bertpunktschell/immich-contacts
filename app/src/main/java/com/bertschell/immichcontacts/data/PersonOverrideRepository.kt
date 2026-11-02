package com.bertschell.immichcontacts.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.overrideDataStore: DataStore<Preferences> by preferencesDataStore(name = "person_overrides")

/** Persisted manual mappings android-contact-id -> immich-person-id. Always beats auto-matching. */
class PersonOverrideRepository(private val context: Context) {

    private fun keyFor(contactId: Long) = stringPreferencesKey("override_$contactId")

    val overrides: Flow<Map<Long, String>> = context.overrideDataStore.data.map { prefs ->
        val map = mutableMapOf<Long, String>()
        for ((key, value) in prefs.asMap()) {
            if (key.name.startsWith("override_")) {
                val contactId = key.name.removePrefix("override_").toLongOrNull()
                if (contactId != null && value is String) map[contactId] = value
            }
        }
        map
    }

    suspend fun setOverride(contactId: Long, personId: String) {
        context.overrideDataStore.edit { it[keyFor(contactId)] = personId }
    }

    suspend fun clearOverride(contactId: Long) {
        context.overrideDataStore.edit { it.remove(keyFor(contactId)) }
    }
}
