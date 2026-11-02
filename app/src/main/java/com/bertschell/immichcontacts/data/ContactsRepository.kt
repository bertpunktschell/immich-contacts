package com.bertschell.immichcontacts.data

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.ContactsContract
import android.util.Log
import java.io.ByteArrayOutputStream

data class ContactInfo(
    val id: Long,
    val displayName: String,
    val hasPhoto: Boolean,
    val photoUri: String? = null,
    val phoneNumbers: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val accountTypes: List<String> = emptyList(), // debug: which accounts hold this contact
)

class ContactsRepository(private val context: Context) {

    fun getAllContacts(): List<ContactInfo> {
        val contacts = mutableListOf<ContactInfo>()
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Contacts.HAS_PHONE_NUMBER,
        )

        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            "${ContactsContract.Contacts.DISPLAY_NAME} IS NOT NULL",
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val photoIdx = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val name = cursor.getString(nameIdx) ?: continue
                val photoUri = cursor.getString(photoIdx)
                val phones = getPhoneNumbers(id)
                val emails = getEmails(id)
                val accounts = getAccountTypes(id)
                // Skip aggregates that ONLY come from messaging apps (WhatsApp/Signal/Telegram).
                // These are duplicate copies Android didn't merge with the "real" contact;
                // Google Contacts hides them too, and their raw contacts are read-only.
                val messagingOnly = accounts.isNotEmpty() && accounts.all {
                    it in setOf("com.whatsapp", "org.thoughtcrime.securesms",
                                "org.telegram.messenger", "jp.naver.line.android",
                                "com.linkedin.android", "com.viber.voip")
                }
                if (messagingOnly) {
                    android.util.Log.d("ContactsRepoDebug", "skip messaging-only aggregate id=$id name=$name accounts=${accounts.joinToString()}")
                    continue
                }
                contacts.add(ContactInfo(
                    id = id,
                    displayName = name,
                    hasPhoto = photoUri != null,
                    photoUri = photoUri,
                    phoneNumbers = phones,
                    emails = emails,
                    accountTypes = accounts,
                ))
                android.util.Log.d("ContactsRepoDebug", "contact id=$id name=$name accounts=${accounts.joinToString()} phone=${phones.firstOrNull() ?: "-"} photo=${photoUri != null}")
            }
        }
        return contacts
    }

    private fun getPhoneNumbers(contactId: Long): List<String> {
        val phones = mutableListOf<String>()
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val selection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"
        val selectionArgs = arrayOf(contactId.toString())

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                cursor.getString(numberIdx)?.let { phones.add(it) }
            }
        }
        return phones
    }

    private fun getEmails(contactId: Long): List<String> {
        val emails = mutableListOf<String>()
        val projection = arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS)
        val selection = "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?"
        val selectionArgs = arrayOf(contactId.toString())

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val addrIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            while (cursor.moveToNext()) {
                cursor.getString(addrIdx)?.let { emails.add(it) }
            }
        }
        return emails
    }

    fun setContactPhoto(contactId: Long, bitmap: Bitmap): Boolean {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val photoBytes = stream.toByteArray()
        return setContactPhotoBytes(contactId, photoBytes)
    }

    fun setContactPhotoBytes(contactId: Long, photoBytes: ByteArray): Boolean {
        return try {
            // Write to ALL raw contacts of this aggregate (local account,
            // Outlook/Exchange, etc.) so every app shows the new photo.
            val rawContactIds = getAllRawContactIds(contactId)
            if (rawContactIds.isEmpty()) {
                Log.e("ContactsRepository", "No raw contact found for contact $contactId")
                return false
            }
            var anySuccess = false
            for (rawContactId in rawContactIds) {
                try {
                    context.contentResolver.delete(
                        ContactsContract.Data.CONTENT_URI,
                        "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                        arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                    )
                    val op = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                        .build()
                    val results = context.contentResolver.applyBatch(ContactsContract.AUTHORITY, arrayListOf(op))
                    if (results.isNotEmpty()) anySuccess = true
                    else Log.w("ContactsRepository", "Photo write rejected for raw contact $rawContactId (account may be read-only)")
                } catch (e: Exception) {
                    Log.w("ContactsRepository", "Photo write failed for raw contact $rawContactId", e)
                }
            }
            anySuccess
        } catch (e: Exception) {
            Log.e("ContactsRepository", "Failed to set contact photo", e)
            false
        }
    }

    /** ALL raw contact ids of an aggregate contact (local + outlook + whatsapp etc.) */
    private fun getAllRawContactIds(contactId: Long): List<Long> {
        val ids = mutableListOf<Long>()
        context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            "${ContactsContract.RawContacts.CONTACT_ID} = ? AND ${ContactsContract.RawContacts.DELETED} = 0",
            arrayOf(contactId.toString()),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) ids.add(cursor.getLong(0))
        }
        return ids
    }

    /** Debug: which account types hold raw contacts for this aggregate */
    private fun getAccountTypes(contactId: Long): List<String> {
        val types = mutableListOf<String>()
        try {
            context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts.ACCOUNT_TYPE_AND_DATA_SET, ContactsContract.RawContacts._ID),
                "${ContactsContract.RawContacts.CONTACT_ID} = ? AND ${ContactsContract.RawContacts.DELETED} = 0",
                arrayOf(contactId.toString()),
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    types.add(cursor.getString(0) ?: "unknown")
                }
            }
        } catch (e: Exception) { /* ignore */ }
        return types
    }

    private fun getRawContactId(contactId: Long): Long? {
        val projection = arrayOf(ContactsContract.RawContacts._ID)
        val selection = "${ContactsContract.RawContacts.CONTACT_ID} = ?"
        val selectionArgs = arrayOf(contactId.toString())

        context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }
        return null
    }

    fun matchContactsToPeople(
        contacts: List<ContactInfo>,
        people: List<com.bertschell.immichcontacts.data.model.ImmichPerson>
    ): Map<String, List<ContactInfo>> {
        val result = mutableMapOf<String, MutableList<ContactInfo>>()
        val peopleByName = people.filter { it.name.isNotBlank() }
            .associateBy { it.name.lowercase().trim() }

        for (contact in contacts) {
            val contactName = contact.displayName.lowercase().trim()
            // Exact name match
            peopleByName[contactName]?.let { person ->
                result.getOrPut(person.id) { mutableListOf() }.add(contact)
            }
            // Also try first name match
            val firstName = contactName.substringBefore(" ")
            for (person in people.filter { it.name.isNotBlank() }) {
                val personFirstName = person.name.lowercase().trim().substringBefore(" ")
                if (firstName.isNotEmpty() && firstName == personFirstName && person.id !in result) {
                    result.getOrPut(person.id) { mutableListOf() }.add(contact)
                }
            }
        }
        return result
    }
}