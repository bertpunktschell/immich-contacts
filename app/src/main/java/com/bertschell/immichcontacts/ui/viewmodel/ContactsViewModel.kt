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
    val error: String? = null,
    val searchQuery: String = "",
    val showOnlyWithImmich: Boolean = false,
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
                val (contacts, people, matches) = withContext(Dispatchers.IO) {
                    val c = contactsRepo.getAllContacts()
                    val pr = api.getPeople()
                    val ppl = pr.people.filter { !it.isHidden }
                    val m = contactsRepo.matchContactsToPeople(c, ppl)
                    Triple(c, ppl, m)
                }

                val contactToPerson = mutableMapOf<Long, String>()
                for ((personId, matchedContacts) in matches) {
                    for (contact in matchedContacts) {
                        contactToPerson[contact.id] = personId
                    }
                }

                state = state.copy(
                    isLoading = false,
                    contacts = contacts,
                    people = people,
                    matchedPersonIds = matches,
                    contactToPersonMap = contactToPerson,
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
