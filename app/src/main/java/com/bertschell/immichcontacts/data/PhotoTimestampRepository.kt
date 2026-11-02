package com.bertschell.immichcontacts.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.timestampDataStore: DataStore<Preferences> by preferencesDataStore(name = "photo_timestamps")

/** Records when OUR app last set a contact's photo (contactId -> epoch millis). */
class PhotoTimestampRepository(private val context: Context) {

    private fun keyFor(contactId: Long) = longPreferencesKey("photots_$contactId")

    val timestamps: Flow<Map<Long, Long>> = context.timestampDataStore.data.map { prefs ->
        val map = mutableMapOf<Long, Long>()
        for ((key, value) in prefs.asMap()) {
            if (key.name.startsWith("photots_")) {
                val contactId = key.name.removePrefix("photots_").toLongOrNull()
                if (contactId != null && value is Long) map[contactId] = value
            }
        }
        map
    }

    suspend fun recordNow(contactId: Long) {
        context.timestampDataStore.edit { it[keyFor(contactId)] = System.currentTimeMillis() }
    }
}
