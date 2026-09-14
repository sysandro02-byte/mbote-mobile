package com.loukatech.mbote.data

import com.loukatech.mbote.model.MastaUser

/**
 * Legacy compatibility entry point. User discovery must be loaded from the
 * authenticated backend; production builds never seed fictitious accounts.
 */
object MastaData {
    val sampleAvatars: List<String> = emptyList()
    fun getInitialMastaUsers(): List<MastaUser> = emptyList()
}
