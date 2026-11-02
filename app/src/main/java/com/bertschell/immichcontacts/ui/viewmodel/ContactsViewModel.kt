package com.bertschell.immichcontacts.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bertschell.immichcontacts.data.ContactInfo
import com.bertschell.immichcontacts.data.ContactsRepository
import com.bertschell.immichcontacts.data.ImmichApi
import com.bertschell.immichcontacts.data.SettingsRepository
import com.bertschell.immichcontacts.data.model.ImmichPerson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ContactsState(
    val isLoading: Boolean = false,
    val contacts: List<ContactInfo> = emptyList(),
    val people: List<ImmichPerson> = emptyList(),
    val matchedPersonIds: Map<String, List<ContactInfo>> = emptyMap(),
    val contactToPersonMap: Map<Long, String> = emptyMap(),
    val ambiguous: Map<Long, List<ImmichPerson>> = emptyMap(), // contact -> candidates needing user decision
    val photoTimestamps: Map<Long, Long> = emptyMap(), // contact -> epoch millis when OUR app set the photo
    val error: String? = null,
    val searchQuery: String = "",
    val showOnlyWithImmich: Boolean = true,
)

class ContactsViewModel : ViewModel() {
    var state by mutableStateOf(ContactsState())
        private set

    fun loadData(context: Context) {
        state = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val settings = SettingsRepository(context)
                val url = settings.serverUrl.first()
                val k = settings.apiKey.first()
                val api = ImmichApi(url, k)
                val contactsRepo = ContactsRepository(context)

                // Contacts query blocks heavily — keep it OFF the main thread
                // so the loading spinner ticks fluently.
                val overrideRepo = com.bertschell.immichcontacts.data.PersonOverrideRepository(context)
                val overrides = overrideRepo.overrides.first()
                val photoTs = com.bertschell.immichcontacts.data.PhotoTimestampRepository(context)
                    .timestamps.first()

                val (contacts, people, matchResult) = withContext(Dispatchers.IO) {
                    val c = contactsRepo.getAllContacts()
                    val pr = api.getPeople()
                    val ppl = pr.people.filter { !it.isHidden }
                    Triple(c, ppl, contactsRepo.matchConservative(c, ppl))
                }

                // Overrides always win, then auto matches.
                // An override also REMOVES the contact from a wrong auto match.
                val autoMatches = matchResult.auto.toMutableMap()
                val contactToPerson = mutableMapOf<Long, String>()
                for ((contactId, personId) in overrides) {
                    contactToPerson[contactId] = personId
                }
                for ((personId, matchedContacts) in autoMatches) {
                    for (contact in matchedContacts) {
                        if (contact.id !in contactToPerson) contactToPerson[contact.id] = personId
                    }
                }

                // Ambiguous candidates only for contacts still unmapped
                val amb = matchResult.ambiguous
                    .filter { (contact, _) -> contact.id !in contactToPerson }
                    .associate { (contact, candidates) -> contact.id to candidates }

                state = state.copy(
                    isLoading = false,
                    contacts = contacts,
                    people = people,
                    matchedPersonIds = autoMatches,
                    contactToPersonMap = contactToPerson,
                    ambiguous = amb,
                    photoTimestamps = photoTs,
                )
                api.close()
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message ?: "Unbekannter Fehler")
            }
        }
    }

    fun onSearchChange(query: String) {
        state = state.copy(searchQuery = query)
    }

    fun getPersonForContact(contactId: Long): ImmichPerson? {
        val personId = state.contactToPersonMap[contactId] ?: return null
        return state.people.find { it.id == personId }
    }

    /** Targeted refresh: re-read ONE contact after its photo changed (no full list rebuild). */
    fun refreshSingleContact(contactId: Long, context: Context) {
        viewModelScope.launch {
            try {
                val refreshed = withContext(Dispatchers.IO) {
                    ContactsRepository(context).getContact(contactId)
                } ?: return@launch
                val ts = withContext(Dispatchers.IO) {
                    com.bertschell.immichcontacts.data.PhotoTimestampRepository(context)
                        .timestamps.first()[contactId]
                }
                val newList = state.contacts.map { if (it.id == contactId) refreshed else it }
                state = state.copy(
                    contacts = newList,
                    photoTimestamps = if (ts != null) state.photoTimestamps + (contactId to ts) else state.photoTimestamps,
                )
            } catch (e: Exception) {
                // Fallback: full reload on failure
                loadData(context)
            }
        }
    }

    fun setPersonOverride(contactId: Long, personId: String, context: Context) {
        // update state immediately
        val newMap = state.contactToPersonMap + (contactId to personId)
        state = state.copy(contactToPersonMap = newMap, ambiguous = state.ambiguous - contactId)
        viewModelScope.launch {
            com.bertschell.immichcontacts.data.PersonOverrideRepository(context).setOverride(contactId, personId)
        }
    }

    fun clearPersonOverride(contactId: Long, context: Context) {
        state = state.copy(contactToPersonMap = state.contactToPersonMap - contactId)
        viewModelScope.launch {
            com.bertschell.immichcontacts.data.PersonOverrideRepository(context).clearOverride(contactId)
        }
    }

    fun onFilterChange(showOnlyWithImmich: Boolean) {
        state = state.copy(showOnlyWithImmich = showOnlyWithImmich)
    }

    val filteredContacts: List<ContactInfo>
        get() {
            var list = state.contacts
            if (state.showOnlyWithImmich) {
                list = list.filter { state.contactToPersonMap.containsKey(it.id) }
            }
            if (!state.searchQuery.isBlank()) {
                list = list.filter { it.displayName.contains(state.searchQuery, ignoreCase = true) }
            }
            return list
        }
}
