package com.loukatech.mbote

import com.loukatech.mbote.service.api.MboteBackendConfig
import com.loukatech.mbote.service.api.PublicationApiService
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PublicationApiServiceTest {
    private lateinit var server: HttpServer
    private lateinit var previousUrl: String
    private var previousToken: String? = null
    private val api = PublicationApiService()

    @Before fun start() {
        previousUrl = MboteBackendConfig.baseUrl
        previousToken = MboteBackendConfig.authToken
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.start()
        MboteBackendConfig.baseUrl = "http://127.0.0.1:${server.address.port}"
        MboteBackendConfig.authToken = "local-test-session"
    }

    @After fun stop() {
        server.stop(0)
        MboteBackendConfig.baseUrl = previousUrl
        MboteBackendConfig.authToken = previousToken
    }

    @Test fun mapsCanonicalJobEnvelopeWithoutInventedCounts() = runBlocking {
        server.createContext("/jobs") { exchange ->
            val bytes = """{"jobs":[{"id":"remote-42","title":"Technicien","company":"Entreprise","location":"Pointe-Noire","activityDomain":"Industrie","url":"https://example.org/apply"}]}""".toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        val offer = api.fetchJobs().getOrThrow().single()
        assertEquals("remote-42", offer.id)
        assertEquals("Industrie", offer.domain)
        assertEquals("https://example.org/apply", offer.applyUrl)
        assertEquals(0, offer.likesCount)
        assertEquals(0, offer.applicantsCount)
    }

    @Test fun applicationFailureIsNotReportedAsSuccess() = runBlocking {
        server.createContext("/jobs/mbote-42/apply") { exchange ->
            assertEquals("POST", exchange.requestMethod)
            assertEquals("Bearer local-test-session", exchange.requestHeaders.getFirst("Authorization"))
            val bytes = """{"error":"Offre expirée"}""".toByteArray()
            exchange.sendResponseHeaders(409, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        val result = api.applyToJob("mbote-42")
        assertTrue(result.isFailure)
        assertEquals("Offre expirée", result.exceptionOrNull()?.message)
    }

    @Test fun refusesUnauthenticatedJobCreation() = runBlocking {
        MboteBackendConfig.authToken = null
        assertTrue(api.createJob(mapOf("title" to "Technicien")).isFailure)
    }
}
