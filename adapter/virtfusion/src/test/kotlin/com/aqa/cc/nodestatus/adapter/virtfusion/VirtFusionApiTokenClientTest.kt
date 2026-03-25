package com.aqa.cc.nodestatus.adapter.virtfusion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VirtFusionApiTokenClientTest {
    @Test
    fun api_token_client_uses_bearer_auth_on_official_endpoints() {
        val transport = RecordingApiTransport(
            mapOf(
                "/api/account" to "{\"data\":{\"name\":\"Jon Doe\"}}",
                "/api/server?results=8" to "{\"data\":[]}"
            )
        )
        val client = VirtFusionApiTokenClient(
            auth = VirtFusionApiTokenAuth(
                apiBaseUrl = "https://panel.example.invalid/api",
                apiToken = "test-token",
                userAgent = "NodeStatusTest/1.0",
                allowInsecureTls = true,
            ),
            transport = transport,
        )

        assertEquals("{\"data\":{\"name\":\"Jon Doe\"}}", client.probeConnection())
        assertEquals("{\"data\":[]}", client.fetchServersRaw())
        assertEquals(2, transport.requests.size)
        assertTrue(transport.requests.all { it.headers["Authorization"] == "Bearer test-token" })
        assertEquals("https://panel.example.invalid/api/account", transport.requests[0].uri.toString())
        assertEquals("https://panel.example.invalid/api/server?results=8", transport.requests[1].uri.toString())
    }
}

private class RecordingApiTransport(
    private val responses: Map<String, String>,
) : VirtFusionHttpTransport {
    val requests = mutableListOf<VirtFusionHttpRequest>()

    override fun get(request: VirtFusionHttpRequest): String {
        requests += request
        val key = request.uri.path + request.uri.query?.let { "?$it" }.orEmpty()
        return responses[key] ?: error("Missing fake API response for $key")
    }
}
