package com.loukatech.mbote.data

import com.loukatech.mbote.model.DiscoverProfile

/**
 * Production discovery starts empty and is populated only with server-provided
 * users. No invented identity is displayed while loading or offline.
 */
object DiscoveryProfilesData {
    val initialProfiles: List<DiscoverProfile> = emptyList()
}
