package com.aqa.cc.nodestatus.adapter.virtfusion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

class VirtFusionSessionClientTest {
    @Test
    fun session_client_fetches_list_and_combines_detail_and_state_requests() {
        val transport = RecordingVirtFusionTransport(
            responses = mapOf(
                "/servers/_list?limit=8" to fixtureText("rule-packs", "packs", "virtfusion.vmvm", "fixtures", "server-list.capture.sanitized.json"),
                "/server/192/resource/server.json" to fixtureText("rule-packs", "packs", "virtfusion.vmvm", "fixtures", "server-details.capture.sanitized.json"),
                "/server/192/resource/state.json" to fixtureText("rule-packs", "packs", "virtfusion.vmvm", "fixtures", "server-state.capture.sanitized.json"),
            ),
        )
        val client = VirtFusionSessionClient(
            auth = VirtFusionLocalSessionAuth(
                baseUrl = "https://panel.example.invalid",
                cookieHeader = "XSRF-TOKEN=test-cookie; virtfusion_session=test-session",
                xsrfHeader = "test-xsrf",
                userAgent = "NodeStatusTest/1.0",
            ),
            transport = transport,
        )

        val snapshots = client.fetchSnapshots(
            limit = 8,
            collectedAt = Instant.parse("2026-03-24T15:10:21Z"),
        )

        assertEquals(1, snapshots.size)
        val snapshot = snapshots.single()
        val metrics = snapshot.metrics.associateBy { it.key }

        assertEquals("11111111-2222-3333-4444-555555555555", snapshot.resourceId)
        assertEquals("VMVM", snapshot.displayName)
        assertEquals("running", metrics.getValue("state.power").value?.raw)
        assertEquals("5181442543", metrics.getValue("usage.traffic_total_bytes").value?.raw)

        assertEquals(3, transport.requests.size)
        assertEquals("https://panel.example.invalid/dashboard", transport.requests[0].headers["Referer"])
        assertEquals(
            "https://panel.example.invalid/server/11111111-2222-3333-4444-555555555555",
            transport.requests[1].headers["Referer"],
        )
        assertEquals(
            "https://panel.example.invalid/server/11111111-2222-3333-4444-555555555555",
            transport.requests[2].headers["Referer"],
        )
        assertTrue(transport.requests.all { it.headers["X-XSRF-TOKEN"] == "test-xsrf" })
    }

    private fun fixtureText(vararg parts: String): String {
        val repoRoot = System.getProperty("nodestatus.repoRoot")
        require(!repoRoot.isNullOrBlank()) { "Missing nodestatus.repoRoot system property" }
        return Files.readString(Path.of(repoRoot, *parts))
    }
}

private class RecordingVirtFusionTransport(
    private val responses: Map<String, String>,
) : VirtFusionHttpTransport {
    val requests = mutableListOf<VirtFusionHttpRequest>()

    override fun get(request: VirtFusionHttpRequest): String {
        requests += request
        val key = request.uri.path + request.uri.query?.let { "?$it" }.orEmpty()
        return responses[key] ?: error("Missing fake response for $key")
    }
}
