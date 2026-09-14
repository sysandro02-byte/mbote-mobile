package com.loukatech.mbote

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.loukatech.mbote.data.DiscoveryProfilesData
import com.loukatech.mbote.data.MastaData
import com.loukatech.mbote.data.MboteRepository
import com.loukatech.mbote.model.NavigationTab
import com.loukatech.mbote.ui.viewmodel.MboteViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MboteRobolectricTest {
    @Test fun applicationContextUsesMbotePackage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(context)
        assertTrue(context.packageName.startsWith("com.aistudio.mbote.krtwvx"))
        assertTrue(context.getString(R.string.app_name).isNotBlank())
    }

    @Test fun navigationWorksWithAnEmptyProductionRepository() {
        val viewModel = MboteViewModel(MboteRepository())
        assertEquals(NavigationTab.MESSAGES, viewModel.currentTab.value)
        viewModel.setTab(NavigationTab.CALLS)
        assertEquals(NavigationTab.CALLS, viewModel.currentTab.value)
        viewModel.setTab(NavigationTab.ACTUS)
        assertEquals(NavigationTab.ACTUS, viewModel.currentTab.value)
        viewModel.setTab(NavigationTab.SETTINGS)
        assertEquals(NavigationTab.SETTINGS, viewModel.currentTab.value)
    }

    @Test fun legacyCatalogsNeverExposeFictitiousUsers() {
        assertTrue(MastaData.getInitialMastaUsers().isEmpty())
        assertTrue(MastaData.sampleAvatars.isEmpty())
        assertTrue(DiscoveryProfilesData.initialProfiles.isEmpty())
    }

    @Test fun repositoryStartsWithoutFabricatedActivity() {
        val repository = MboteRepository()
        assertTrue(repository.chats.value.isEmpty())
        assertTrue(repository.calls.value.isEmpty())
        assertTrue(repository.statuses.value.isEmpty())
        assertTrue(repository.meetings.value.isEmpty())
        assertTrue(repository.notifications.value.isEmpty())
        assertTrue(repository.reports.value.isEmpty())
    }
}
