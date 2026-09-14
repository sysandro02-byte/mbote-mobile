package com.loukatech.mbote

import com.loukatech.mbote.data.MboteRepository
import com.loukatech.mbote.model.NavigationTab
import com.loukatech.mbote.ui.viewmodel.MboteViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MboteViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: MboteRepository
    private lateinit var viewModel: MboteViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = MboteRepository()
        viewModel = MboteViewModel(repository)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test fun productionStateDoesNotContainSeededUserData() {
        assertEquals(NavigationTab.MESSAGES, viewModel.currentTab.value)
        assertTrue(viewModel.chats.value.isEmpty())
        assertTrue(viewModel.calls.value.isEmpty())
        assertTrue(viewModel.statuses.value.isEmpty())
        assertTrue(viewModel.meetings.value.isEmpty())
        assertTrue(viewModel.notifications.value.isEmpty())
        assertTrue(viewModel.mastaUsers.value.isEmpty())
        assertNull(viewModel.activeChatId.value)
        assertNull(viewModel.activeCall.value)
        assertNull(viewModel.activeMeetingRoom.value)
    }

    @Test fun searchAndFiltersRemainReactiveWithoutSeedData() {
        viewModel.setSearchQuery("Brazzaville")
        assertEquals("Brazzaville", viewModel.searchQuery.value)
        viewModel.setChatFilter("Non lus")
        assertEquals("Non lus", viewModel.chatFilter.value)
        viewModel.setChatFilter("Groupes")
        assertEquals("Groupes", viewModel.chatFilter.value)
    }

    @Test fun transientUiStateIsStillControllable() {
        assertFalse(viewModel.showNewChatDialog.value)
        viewModel.setShowNewChatDialog(true)
        assertTrue(viewModel.showNewChatDialog.value)
        assertFalse(viewModel.showNewMeetingDialog.value)
        viewModel.setShowNewMeetingDialog(true)
        assertTrue(viewModel.showNewMeetingDialog.value)
        assertFalse(viewModel.showNotificationsSheet.value)
        viewModel.setShowNotificationsSheet(true)
        assertTrue(viewModel.showNotificationsSheet.value)
    }

    @Test fun operationsOnUnknownRemoteEntitiesDoNotInventRecords() {
        val jobs = viewModel.jobs.value
        val chats = viewModel.chats.value
        viewModel.toggleJobBookmark("missing-job")
        viewModel.applyToJob("missing-job")
        assertEquals(jobs, viewModel.jobs.value)
        assertEquals(chats, viewModel.chats.value)
    }
}
