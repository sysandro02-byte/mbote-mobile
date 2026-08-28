package com.loukatech.mbote

import com.loukatech.mbote.data.MboteRepository
import com.loukatech.mbote.model.MediaType
import com.loukatech.mbote.model.MessageStatus
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

@OptIn(ExperimentalCoroutinesApi::class)
class MboteViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: MboteRepository
    private lateinit var viewModel: MboteViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = MboteRepository()
        viewModel = MboteViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() {
        assertEquals(NavigationTab.MESSAGES, viewModel.currentTab.value)
        assertNull(viewModel.activeChatId.value)
        assertNull(viewModel.activeCall.value)
        assertNull(viewModel.activeMeetingRoom.value)
        assertEquals("", viewModel.searchQuery.value)
        assertEquals("Tous", viewModel.chatFilter.value)
        assertTrue(viewModel.chats.value.isNotEmpty())
        assertTrue(viewModel.calls.value.isNotEmpty())
        assertTrue(viewModel.statuses.value.isNotEmpty())
        assertTrue(viewModel.newsPosts.value.isNotEmpty())
        assertTrue(viewModel.meetings.value.isNotEmpty())
        assertTrue(viewModel.jobs.value.isNotEmpty())
    }

    @Test
    fun testSearchAndFilter() {
        viewModel.setSearchQuery("Grace")
        assertEquals("Grace", viewModel.searchQuery.value)

        viewModel.setChatFilter("Non lus")
        assertEquals("Non lus", viewModel.chatFilter.value)

        viewModel.setChatFilter("Groupes")
        assertEquals("Groupes", viewModel.chatFilter.value)

        viewModel.setChatFilter("Canaux")
        assertEquals("Canaux", viewModel.chatFilter.value)
    }

    @Test
    fun testAddReactionAndRecallMessage() = runTest(testDispatcher) {
        val chatId = "chat_grace"
        val messageId = "m_g_1"

        viewModel.addReaction(chatId, messageId, "❤️")
        advanceUntilIdle()

        val chat = viewModel.chats.value.first { it.id == chatId }
        val msg = chat.messages.first { it.id == messageId }
        assertEquals(1, msg.reactions["❤️"])

        // Delete (recall) message
        viewModel.deleteMessage(chatId, messageId)
        advanceUntilIdle()

        val updatedChat = viewModel.chats.value.first { it.id == chatId }
        val updatedMsg = updatedChat.messages.first { it.id == messageId }
        assertTrue(updatedMsg.isRecalled)
        assertEquals("Ce message a été supprimé", updatedMsg.text)
    }

    @Test
    fun testCreateNewChat() = runTest(testDispatcher) {
        val initialSize = viewModel.chats.value.size
        viewModel.createChat("Groupe Projet Android", "Bienvenue à tous !", isGroup = true)
        advanceUntilIdle()

        assertEquals(initialSize + 1, viewModel.chats.value.size)
        val created = viewModel.chats.value.first()
        assertEquals("Groupe Projet Android", created.name)
        assertTrue(created.isGroup)
        assertEquals("Bienvenue à tous !", created.lastMessage)
    }

    @Test
    fun testJobLikeToggle() {
        val firstJob = viewModel.jobs.value.first()
        val initialLiked = firstJob.isLiked
        val initialLikes = firstJob.likesCount

        viewModel.toggleJobLike(firstJob.id)
        val toggledJob = viewModel.jobs.value.first { it.id == firstJob.id }
        assertEquals(!initialLiked, toggledJob.isLiked)
        val expectedLikes = if (!initialLiked) initialLikes + 1 else initialLikes - 1
        assertEquals(expectedLikes, toggledJob.likesCount)

        viewModel.toggleJobLike(firstJob.id)
        val restoredJob = viewModel.jobs.value.first { it.id == firstJob.id }
        assertEquals(initialLiked, restoredJob.isLiked)
        assertEquals(initialLikes, restoredJob.likesCount)
    }

    @Test
    fun testJobBookmarkAndApply() {
        val job = viewModel.jobs.value.first()
        val initialSaved = job.isSaved
        val initialApplicants = job.applicantsCount

        viewModel.toggleJobBookmark(job.id)
        val savedJob = viewModel.jobs.value.first { it.id == job.id }
        assertEquals(!initialSaved, savedJob.isSaved)

        val applied = viewModel.applyToJob(job.id)
        assertTrue(applied)
        val appliedJob = viewModel.jobs.value.first { it.id == job.id }
        assertEquals(initialApplicants + 1, appliedJob.applicantsCount)
    }

    @Test
    fun testPostJobOffer() {
        val initialCount = viewModel.jobs.value.size
        val newJob = viewModel.postJobOffer(
            title = "Architecte Cloud",
            company = "LoukaTech",
            location = "Brazzaville",
            domain = "Ingénierie Logicielle",
            contractType = "CDI",
            workMode = "Hybride",
            salary = "2 000 000 FCFA",
            description = "Concevoir nos architectures résilientes"
        )
        assertEquals(initialCount + 1, viewModel.jobs.value.size)
        assertEquals("Architecte Cloud", newJob.title)
    }

    @Test
    fun testDialogStateToggles() {
        assertFalse(viewModel.showNewChatDialog.value)
        viewModel.setShowNewChatDialog(true)
        assertTrue(viewModel.showNewChatDialog.value)

        assertFalse(viewModel.showAddStatusDialog.value)
        viewModel.setShowAddStatusDialog(true)
        assertTrue(viewModel.showAddStatusDialog.value)

        assertFalse(viewModel.showNewMeetingDialog.value)
        viewModel.setShowNewMeetingDialog(true)
        assertTrue(viewModel.showNewMeetingDialog.value)

        assertFalse(viewModel.showEditProfileDialog.value)
        viewModel.setShowEditProfileDialog(true)
        assertTrue(viewModel.showEditProfileDialog.value)

        assertFalse(viewModel.showJobsScreen.value)
        viewModel.setShowJobsScreen(true)
        assertTrue(viewModel.showJobsScreen.value)

        assertFalse(viewModel.showContactsSyncSheet.value)
        viewModel.setShowContactsSyncSheet(true)
        assertTrue(viewModel.showContactsSyncSheet.value)

        assertFalse(viewModel.showQuickActionsMenu.value)
        viewModel.setShowQuickActionsMenu(true)
        assertTrue(viewModel.showQuickActionsMenu.value)

        assertFalse(viewModel.showCreateGroupDialog.value)
        viewModel.setShowCreateGroupDialog(true)
        assertTrue(viewModel.showCreateGroupDialog.value)

        assertFalse(viewModel.showCreateChannelDialog.value)
        viewModel.setShowCreateChannelDialog(true)
        assertTrue(viewModel.showCreateChannelDialog.value)

        assertFalse(viewModel.showMastaSheet.value)
        viewModel.setShowMastaSheet(true)
        assertTrue(viewModel.showMastaSheet.value)
    }

    @Test
    fun testCreateGroupAndChannel() {
        val initialChatsCount = viewModel.chats.value.size

        // Test create group
        val groupContact = viewModel.syncedContacts.value.first()
        viewModel.createGroup("Dev Congo", "Communauté Tech", listOf(groupContact), "Bienvenue !")
        assertEquals(initialChatsCount + 1, viewModel.chats.value.size)
        val createdGroup = viewModel.chats.value.first()
        assertEquals("Dev Congo", createdGroup.name)
        assertTrue(createdGroup.isGroup)

        // Test create channel
        viewModel.createChannel("MBoté Actualités", "Canal officiel d'annonces", true, "Premier post !")
        assertEquals(initialChatsCount + 2, viewModel.chats.value.size)
        val createdChannel = viewModel.chats.value.first()
        assertEquals("MBoté Actualités", createdChannel.name)
        assertTrue(createdChannel.isChannel)
    }

    @Test
    fun testAddNewMasta() {
        val initialContactsCount = viewModel.syncedContacts.value.size
        val newMasta = viewModel.addNewMasta("Rodrigue Makosso", "+242 06 777 8899", true)
        assertEquals(initialContactsCount + 1, viewModel.syncedContacts.value.size)
        assertEquals("Rodrigue Makosso", newMasta.name)
        assertTrue(newMasta.isMboteUser)
    }

    @Test
    fun testNotificationsManagement() {
        assertFalse(viewModel.showNotificationsSheet.value)
        viewModel.setShowNotificationsSheet(true)
        assertTrue(viewModel.showNotificationsSheet.value)

        val notifications = viewModel.notifications.value
        assertTrue(notifications.isNotEmpty())

        viewModel.markAllNotificationsAsRead()
        assertTrue(viewModel.notifications.value.all { it.isRead })

        viewModel.clearAllNotifications()
        assertTrue(viewModel.notifications.value.isEmpty())
    }

    @Test
    fun testSendVoiceMessage() = runTest(testDispatcher) {
        val chatId = "chat_grace"
        val initialMessagesCount = viewModel.chats.value.first { it.id == chatId }.messages.size

        viewModel.sendVoiceMessage(chatId, "/tmp/sample_audio.m4a", 15)
        advanceUntilIdle()

        val updatedChat = viewModel.chats.value.first { it.id == chatId }
        assertEquals(initialMessagesCount + 1, updatedChat.messages.size)
        val lastMsg = updatedChat.messages.last()
        assertEquals(MediaType.AUDIO, lastMsg.mediaType)
        assertEquals(15, lastMsg.audioDurationSec)
        assertTrue(lastMsg.isMine)
    }
}
