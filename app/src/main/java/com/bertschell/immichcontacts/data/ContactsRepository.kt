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
    val phoneNumbers: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
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
                contacts.add(ContactInfo(
                    id = id,
                    displayName = name,
                    hasPhoto = photoUri != null,
                    phoneNumbers = phones,
                    emails = emails,
                ))
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
            // Find the raw contact ID for this contact
            val rawContactId = getRawContactId(contactId) ?: run {
                Log.e("ContactsRepository", "No raw contact found for contact $contactId")
                return false
            }

            // Delete existing photo if any
            context.contentResolver.delete(
                ContactsContract.Data.CONTENT_URI,
                "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
            )

            // Insert new photo
            val op = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                .build()

            val results = context.contentResolver.applyBatch(ContactsContract.AUTHORITY, arrayListOf(op))
            results.isNotEmpty()
        } catch (e: Exception) {
            Log.e("ContactsRepository", "Failed to set contact photo", e)
            false
        }
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