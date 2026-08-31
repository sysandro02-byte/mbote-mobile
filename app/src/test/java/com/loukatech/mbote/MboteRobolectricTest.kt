package com.loukatech.mbote

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.loukatech.mbote.data.MboteRepository
import com.loukatech.mbote.model.CallType
import com.loukatech.mbote.model.NavigationTab
import com.loukatech.mbote.ui.viewmodel.MboteViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MboteRobolectricTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testAppContextAndStrings() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(context)
        assertEquals("com.aistudio.mbote.krtwvx", context.packageName)
    }

    @Test
    fun testNavigationTabSwitching() {
        val viewModel = MboteViewModel(MboteRepository())
        assertEquals(NavigationTab.MESSAGES, viewModel.currentTab.value)

        viewModel.setTab(NavigationTab.CALLS)
        assertEquals(NavigationTab.CALLS, viewModel.currentTab.value)

        viewModel.setTab(NavigationTab.ACTUS)
        assertEquals(NavigationTab.ACTUS, viewModel.currentTab.value)

        viewModel.setTab(NavigationTab.MEETINGS)
        assertEquals(NavigationTab.MEETINGS, viewModel.currentTab.value)

        viewModel.setTab(NavigationTab.SETTINGS)
        assertEquals(NavigationTab.SETTINGS, viewModel.currentTab.value)
    }

    @Test
    fun testChatOpeningAndMarkAsRead() = runTest(testDispatcher) {
        val viewModel = MboteViewModel(MboteRepository())
        val initialChats = viewModel.chats.value
        val unreadChat = initialChats.firstOrNull { it.unreadCount > 0 }
        assertNotNull("Should have initial unread chat", unreadChat)

        viewModel.openChat(unreadChat!!.id)
        assertEquals(unreadChat.id, viewModel.activeChatId.value)

        val updatedChat = viewModel.chats.value.find { it.id == unreadChat.id }
        assertEquals(0, updatedChat?.unreadCount)

        viewModel.closeChat()
        assertNull(viewModel.activeChatId.value)
    }

    @Test
    fun testSendMessageAndAiReply() = runTest(testDispatcher) {
        val viewModel = MboteViewModel(MboteRepository())
        val lunaChatId = "chat_luna"

        val initialMessagesCount = viewModel.chats.value.find { it.id == lunaChatId }?.messages?.size ?: 0

        viewModel.sendMessage(lunaChatId, "Bonjour Luna, comment vas-tu ?")
        advanceUntilIdle()

        val updatedChat = viewModel.chats.value.find { it.id == lunaChatId }
        assertNotNull(updatedChat)
        // User message + AI response
        assertEquals(initialMessagesCount + 2, updatedChat!!.messages.size)
        assertTrue(updatedChat.messages.last().text.contains("Mbote", ignoreCase = true))
        assertFalse(updatedChat.messages.last().isMine)
    }

    @Test
    fun testCallsFlow() {
        val viewModel = MboteViewModel(MboteRepository())
        assertNull(viewModel.activeCall.value)

        viewModel.startCall("Grace Makiese", "https://avatar.url", true)
        val activeCall = viewModel.activeCall.value
        assertNotNull(activeCall)
        assertEquals("Grace Makiese", activeCall?.name)
        assertTrue(activeCall?.isVideo == true)
        assertEquals(CallType.OUTGOING, activeCall?.type)

        viewModel.endCall()
        assertNull(viewModel.activeCall.value)
    }

    @Test
    fun testMeetingsFlow() {
        val viewModel = MboteViewModel(MboteRepository())
        assertNull(viewModel.activeMeetingRoom.value)

        val existingMeeting = viewModel.meetings.value.first()
        viewModel.startMeeting(existingMeeting)
        assertEquals(existingMeeting.id, viewModel.activeMeetingRoom.value?.id)

        assertFalse(viewModel.isMutedInMeeting.value)
        viewModel.toggleMeetingMute()
        assertTrue(viewModel.isMutedInMeeting.value)

        assertFalse(viewModel.isVideoOffInMeeting.value)
        viewModel.toggleMeetingVideo()
        assertTrue(viewModel.isVideoOffInMeeting.value)

        viewModel.leaveMeeting()
        assertNull(viewModel.activeMeetingRoom.value)

        // Create new meeting
        val countBefore = viewModel.meetings.value.size
        viewModel.createMeeting("Sprint Retrospective 2026", 45)
        assertEquals(countBefore + 1, viewModel.meetings.value.size)
        assertNotNull(viewModel.activeMeetingRoom.value)
        assertEquals("Sprint Retrospective 2026", viewModel.activeMeetingRoom.value?.title)
    }

    @Test
    fun testUserProfileAndSettings() {
        val viewModel = MboteViewModel(MboteRepository())
        val profile = viewModel.userProfile.value
        assertEquals("Marc Loutala", profile.name)

        viewModel.updateUserProfile("Marc Loutala Senior", "Tech enthusiast", "+242 06 999 8877", "Pointe-Noire")
        val updatedProfile = viewModel.userProfile.value
        assertEquals("Marc Loutala Senior", updatedProfile.name)
        assertEquals("Pointe-Noire", updatedProfile.city)
        assertEquals("Tech enthusiast", updatedProfile.bio)

        val darkModeBefore = updatedProfile.darkModeEnabled
        viewModel.toggleDarkMode()
        assertEquals(!darkModeBefore, viewModel.userProfile.value.darkModeEnabled)
    }

    @Test
    fun testExportChatHistoryToJson() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = MboteViewModel(MboteRepository())
        val testChat = viewModel.chats.value.first()

        val exportResult = com.loukatech.mbote.service.ChatExportManager.exportChatHistory(context, testChat)
        assertTrue(exportResult.isSuccess)
        val resultData = exportResult.getOrNull()
        assertNotNull(resultData)
        assertTrue(resultData!!.file.exists())
        assertTrue(resultData.jsonString.contains("MBoté Messenger"))
        assertTrue(resultData.jsonString.contains(testChat.id))
        assertEquals(testChat.messages.size, resultData.totalMessages)
    }

    @Test
    fun testSearchableContactsSync() = runTest(testDispatcher) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val syncService = com.loukatech.mbote.service.ContactsSyncService(context)
        syncService.syncContacts()
        advanceUntilIdle()

        val contacts = syncService.syncedContacts.value
        assertTrue(contacts.isNotEmpty())

        // Test search filter
        val filtered = contacts.filter {
            it.name.contains("Grace", ignoreCase = true) || it.phoneNumber.contains("Grace")
        }
        assertTrue(filtered.any { it.name.contains("Grace") })
    }

    @Test
    fun testVoiceMessageSending() = runTest(testDispatcher) {
        val viewModel = MboteViewModel(MboteRepository())
        val chat = viewModel.chats.value.first { !it.isAI }
        val initialCount = chat.messages.size

        viewModel.sendVoiceMessage(chat.id, "/cache/voice_notes/test.m4a", 12, null)
        advanceUntilIdle()

        val updatedChat = viewModel.chats.value.find { it.id == chat.id }
        assertNotNull(updatedChat)
        assertEquals(initialCount + 1, updatedChat!!.messages.size)
        val lastMsg = updatedChat.messages.last()
        assertEquals(com.loukatech.mbote.model.MediaType.AUDIO, lastMsg.mediaType)
        assertEquals(12, lastMsg.audioDurationSec)
        assertTrue(lastMsg.isMine)
    }

    @Test
    fun testShareLocationMessage() = runTest(testDispatcher) {
        val viewModel = MboteViewModel(MboteRepository())
        val chat = viewModel.chats.value.first { !it.isAI }
        val initialCount = chat.messages.size

        viewModel.sendLocation(chat.id, "Brazzaville Centre", -4.2634, 15.2429, false, 0)
        advanceUntilIdle()

        val updatedChat = viewModel.chats.value.find { it.id == chat.id }
        assertNotNull(updatedChat)
        assertEquals(initialCount + 1, updatedChat!!.messages.size)
        val lastMsg = updatedChat.messages.last()
        assertEquals(com.loukatech.mbote.model.MediaType.LOCATION, lastMsg.mediaType)
        assertNotNull(lastMsg.locationData)
        assertEquals("Brazzaville Centre", lastMsg.locationData?.placeName)
        assertEquals(-4.2634, lastMsg.locationData?.latitude ?: 0.0, 0.0001)
        assertEquals(15.2429, lastMsg.locationData?.longitude ?: 0.0, 0.0001)
    }
}
