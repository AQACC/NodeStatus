package com.aqa.cc.nodestatus.adapter.virtfusion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class VirtFusionLocalSessionAuthTest {
    @Test
    fun loader_reads_local_auth_properties_and_builds_requests() {
        val authFile = Files.createTempFile("nodestatus-virtfusion-auth", ".properties")
        Files.writeString(
            authFile,
            """
            virtfusion.base_url=https://panel.example.invalid
            virtfusion.cookie_header=XSRF-TOKEN=test-cookie; virtfusion_session=test-session
            virtfusion.x_xsrf_token=test-xsrf
            virtfusion.user_agent=NodeStatusTest/1.0
            virtfusion.allow_insecure_tls=true
            """.trimIndent(),
        )

        val auth = VirtFusionLocalSessionAuthLoader.load(authFile)
        val request = auth.newAuthorizedGet(
            uri = auth.serverStateUri(192),
            referer = auth.serverReferer("11111111-2222-3333-4444-555555555555"),
        )

        assertEquals("https://panel.example.invalid", auth.baseUrl)
        assertEquals(
            "https://panel.example.invalid/servers/_list?limit=8",
            auth.serverListUri().toString(),
        )
        assertEquals(
            "https://panel.example.invalid/server/192/resource/server.json",
            auth.serverDetailsUri(192).toString(),
        )
        assertEquals(
            "https://panel.example.invalid/server/192/resource/state.json",
            auth.serverStateUri(192).toString(),
        )
        assertEquals(
            "https://panel.example.invalid/queue/list?server_id=192&results=6&page=1",
            auth.queueUri(192).toString(),
        )
        assertEquals(
            "XSRF-TOKEN=test-cookie; virtfusion_session=test-session",
            request.headers["Cookie"],
        )
        assertEquals("test-xsrf", request.headers["X-XSRF-TOKEN"])
        assertEquals("XMLHttpRequest", request.headers["X-Requested-With"])
        assertEquals(
            "https://panel.example.invalid/server/11111111-2222-3333-4444-555555555555",
            request.headers["Referer"],
        )
        assertTrue(request.uri.toString().endsWith("/server/192/resource/state.json"))
        assertTrue(auth.allowInsecureTls)
    }

    @Test
    fun loader_defaults_insecure_tls_to_false() {
        val authFile = Files.createTempFile("nodestatus-virtfusion-auth", ".properties")
        Files.writeString(
            authFile,
            """
            virtfusion.base_url=https://panel.example.invalid
            virtfusion.cookie_header=XSRF-TOKEN=test-cookie; virtfusion_session=test-session
            virtfusion.x_xsrf_token=test-xsrf
            """.trimIndent(),
        )

        val auth = VirtFusionLocalSessionAuthLoader.load(authFile)

        assertFalse(auth.allowInsecureTls)
    }
}
