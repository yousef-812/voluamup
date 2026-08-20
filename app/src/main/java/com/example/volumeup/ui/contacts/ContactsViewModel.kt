package com.example.volumeup.ui.contacts

import android.app.Application
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.volumeup.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val _contacts = MutableLiveData<List<Contact>>()
    val contacts: LiveData<List<Contact>> = _contacts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadContacts() {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val fetchedContacts = queryContactsFromProvider()
            withContext(Dispatchers.Main) {
                _contacts.value = fetchedContacts
                _isLoading.value = false
            }
        }
    }

    private fun queryContactsFromProvider(): List<Contact> {
        val contactsMap = mutableMapOf<String, Contact>()
        val contentResolver = getApplication<Application>().contentResolver

        // Query Phone Numbers & Names
        val phoneCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            ),
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )

        phoneCursor?.use { cursor ->
            val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

            while (cursor.moveToNext()) {
                val id = if (idIndex >= 0) cursor.getString(idIndex) else ""
                val name = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "" else ""
                val number = if (numberIndex >= 0) cursor.getString(numberIndex) ?: "" else ""
                val photo = if (photoIndex >= 0) cursor.getString(photoIndex) else null

                if (id.isNotEmpty() && !contactsMap.containsKey(id)) {
                    contactsMap[id] = Contact(
                        id = id,
                        name = name,
                        phoneNumber = number,
                        photoUri = photo
                    )
                }
            }
        }

        // Query Email Addresses
        val emailCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                ContactsContract.CommonDataKinds.Email.ADDRESS
            ),
            null,
            null,
            null
        )

        emailCursor?.use { cursor ->
            val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
            val addressIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)

            while (cursor.moveToNext()) {
                val id = if (idIndex >= 0) cursor.getString(idIndex) else ""
                val email = if (addressIndex >= 0) cursor.getString(addressIndex) else null

                if (id.isNotEmpty() && contactsMap.containsKey(id) && email != null) {
                    val existing = contactsMap[id]!!
                    contactsMap[id] = existing.copy(emailAddress = email)
                }
            }
        }

        return contactsMap.values.toList()
    }
}
