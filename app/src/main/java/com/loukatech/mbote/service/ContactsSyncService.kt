package com.loukatech.mbote.service

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.loukatech.mbote.model.ContactsSyncState
import com.loukatech.mbote.model.SyncedContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ContactsSyncService(private val context: Context? = null) {

    private val timeFormat = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())

    private val _syncState = MutableStateFlow(ContactsSyncState())
    val syncState: StateFlow<ContactsSyncState> = _syncState.asStateFlow()

    private val _syncedContacts = MutableStateFlow<List<SyncedContact>>(emptyList())
    val syncedContacts: StateFlow<List<SyncedContact>> = _syncedContacts.asStateFlow()

    init {
        checkPermissionAndLoadCached()
    }

    fun hasContactsPermission(): Boolean {
        val ctx = context ?: return false
        return try {
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    private fun checkPermissionAndLoadCached() {
        val granted = hasContactsPermission()
        _syncState.update { it.copy(isPermissionGranted = granted) }
        if (granted) {
            // Load initial
            loadDeviceContacts()
        } else {
            _syncedContacts.value = emptyList()
            _syncState.update {
                it.copy(
                    totalSynced = _syncedContacts.value.size,
                    mboteUsersCount = _syncedContacts.value.count { c -> c.isMboteUser },
                    lastSyncTime = "Prêt à synchroniser"
                )
            }
        }
    }

    suspend fun syncContacts(): Result<List<SyncedContact>> = withContext(Dispatchers.IO) {
        _syncState.update { it.copy(isSyncing = true, error = null) }

        val ctx = context
        if (ctx == null || !hasContactsPermission()) {
            val contacts = emptyList<SyncedContact>()
            _syncedContacts.value = contacts
            _syncState.update {
                it.copy(
                    isSyncing = false,
                    isPermissionGranted = hasContactsPermission(),
                    totalSynced = 0,
                    mboteUsersCount = 0,
                    lastSyncTime = timeFormat.format(Date()),
                    error = if (ctx == null) null else "Permission 'Contacts' non accordée. Veuillez l'activer pour synchroniser."
                )
            }
            return@withContext Result.success(contacts)
        }

        try {
            val contactsList = mutableListOf<SyncedContact>()
            val contentResolver: ContentResolver = ctx.contentResolver
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            )

            val cursor: Cursor? = contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            val seenNumbers = mutableSetOf<String>()

            cursor?.use {
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)

                while (it.moveToNext()) {
                    val contactId = if (idIdx >= 0) it.getString(idIdx) else ""
                    val name = if (nameIdx >= 0) it.getString(nameIdx) ?: "Contact sans nom" else "Contact"
                    val rawNumber = if (numberIdx >= 0) it.getString(numberIdx) ?: "" else ""
                    val photoUri = if (photoIdx >= 0) it.getString(photoIdx) else null

                    val cleanNumber = rawNumber.replace("\\s+".toRegex(), "").replace("-", "")
                    if (cleanNumber.isNotBlank() && !seenNumbers.contains(cleanNumber)) {
                        seenNumbers.add(cleanNumber)

                        contactsList.add(
                            SyncedContact(
                                id = "contact_$contactId",
                                name = name,
                                phoneNumber = rawNumber,
                                avatarUrl = photoUri,
                                isMboteUser = false,
                                statusText = "Vérification serveur requise",
                                isSynced = true,
                                lastSyncedTimestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }

            _syncedContacts.value = contactsList
            val nowStr = timeFormat.format(Date())
            _syncState.update {
                it.copy(
                    isSyncing = false,
                    isPermissionGranted = true,
                    totalSynced = contactsList.size,
                    mboteUsersCount = contactsList.count { c -> c.isMboteUser },
                    lastSyncTime = nowStr,
                    error = null
                )
            }

            Result.success(contactsList)
        } catch (e: Exception) {
            Log.e("ContactsSyncService", "Error querying contacts", e)
            _syncedContacts.value = emptyList()
            _syncState.update {
                it.copy(
                    isSyncing = false,
                    error = "Erreur de lecture du carnet: ${e.localizedMessage}",
                    totalSynced = 0,
                    mboteUsersCount = 0
                )
            }
            Result.failure(e)
        }
    }

    private fun loadDeviceContacts() {
        val ctx = context ?: return
        try {
            val contentResolver: ContentResolver = ctx.contentResolver
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            )

            val cursor: Cursor? = contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            val list = mutableListOf<SyncedContact>()
            val seenNumbers = mutableSetOf<String>()

            cursor?.use {
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)

                while (it.moveToNext()) {
                    val contactId = if (idIdx >= 0) it.getString(idIdx) else ""
                    val name = if (nameIdx >= 0) it.getString(nameIdx) ?: "Contact" else "Contact"
                    val rawNumber = if (numberIdx >= 0) it.getString(numberIdx) ?: "" else ""
                    val photoUri = if (photoIdx >= 0) it.getString(photoIdx) else null

                    val cleanNumber = rawNumber.replace("\\s+".toRegex(), "").replace("-", "")
                    if (cleanNumber.isNotBlank() && !seenNumbers.contains(cleanNumber)) {
                        seenNumbers.add(cleanNumber)
                        list.add(
                            SyncedContact(
                                id = "contact_$contactId",
                                name = name,
                                phoneNumber = rawNumber,
                                avatarUrl = photoUri,
                                isMboteUser = false,
                                statusText = "Vérification serveur requise",
                                isSynced = true
                            )
                        )
                    }
                }
            }

            _syncedContacts.value = list
            _syncState.update {
                it.copy(
                    totalSynced = list.size,
                    mboteUsersCount = list.count { c -> c.isMboteUser },
                    lastSyncTime = timeFormat.format(Date())
                )
            }
        } catch (e: Exception) {
            _syncedContacts.value = emptyList()
        }
    }

    fun addContact(name: String, phoneNumber: String, isMboteUser: Boolean = false): SyncedContact {
        val newContact = SyncedContact(
            name = name,
            phoneNumber = phoneNumber,
            isMboteUser = isMboteUser,
            statusText = if (isMboteUser) "Disponible sur MBoté" else "Invité à rejoindre MBoté"
        )
        _syncedContacts.update { listOf(newContact) + it }
        _syncState.update {
            it.copy(
                totalSynced = _syncedContacts.value.size,
                mboteUsersCount = _syncedContacts.value.count { c -> c.isMboteUser }
            )
        }
        return newContact
    }
}
