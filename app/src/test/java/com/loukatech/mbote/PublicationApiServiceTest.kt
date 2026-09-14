package com.loukatech.mbote

import com.loukatech.mbote.service.api.MboteBackendConfig
import com.loukatech.mbote.service.api.PublicationApiService
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PublicationApiServiceTest {
    private lateinit var server: MockWebServer
    private lateinit var previousUrl: String
    private var previousToken: String? = null
    private val api = PublicationApiService()

    @Before fun start() {
        previousUrl = MboteBackendConfig.baseUrl
        previousToken = MboteBackendConfig.authToken
        server = MockWebServer()
        server.start()
        MboteBackendConfig.baseUrl = server.url("/").toString().trimEnd('/')
        MboteBackendConfig.authToken = "local-test-session"
    }

    @After fun stop() {
        server.shutdown()
        MboteBackendConfig.baseUrl = previousUrl
        MboteBackendConfig.authToken = previousToken
    }

    @Test fun mapsCanonicalJobEnvelopeWithoutInventedCounts() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"jobs":[{"id":"remote-42","title":"Technicien","company":"Entreprise","location":"Pointe-Noire","activityDomain":"Industrie","url":"https://example.org/apply"}]}"""
            )
        )
        val offer = api.fetchJobs().getOrThrow().single()
        assertEquals("/jobs", server.takeRequest().path)
        assertEquals("remote-42", offer.id)
        assertEquals("Industrie", offer.domain)
        assertEquals("https://example.org/apply", offer.applyUrl)
        assertEquals(0, offer.likesCount)
        assertEquals(0, offer.applicantsCount)
    }

    @Test fun applicationFailureIsNotReportedAsSuccess() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"error":"Offre expirée"}"""))
        val result = api.applyToJob("mbote-42")
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/jobs/mbote-42/apply", request.path)
        assertEquals("Bearer local-test-session", request.getHeader("Authorization"))
        assertTrue(result.isFailure)
        assertEquals("Offre expirée", result.exceptionOrNull()?.message)
    }

    @Test fun refusesUnauthenticatedJobCreation() = runBlocking {
        MboteBackendConfig.authToken = null
        assertTrue(api.createJob(mapOf("title" to "Technicien")).isFailure)
        assertEquals(0, server.requestCount)
    }
}
