package com.loukatech.mbote.model

import java.util.UUID

data class SyncedContact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phoneNumber: String,
    val avatarUrl: String? = null,
    val isMboteUser: Boolean = true,
    val statusText: String = "Utilise MBoté chiffré",
    val isSynced: Boolean = true,
    val lastSyncedTimestamp: Long = System.currentTimeMillis()
)

data class ContactsSyncState(
    val isSyncing: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val totalSynced: Int = 0,
    val mboteUsersCount: Int = 0,
    val lastSyncTime: String? = null,
    val error: String? = null
)
