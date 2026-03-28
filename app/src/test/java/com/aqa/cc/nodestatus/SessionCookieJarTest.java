package com.aqa.cc.nodestatus;

import com.aqa.cc.nodestatus.adapter.virtfusion.VirtFusionHttpRequest;

import org.junit.Test;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SessionCookieJarTest {
    @Test
    public void apply_overrides_request_auth_headers_with_latest_session_values() {
        SessionCookieJar jar = new SessionCookieJar(
                "XSRF-TOKEN=old-xsrf; virtfusion_session=old-session",
                "old-xsrf"
        );

        VirtFusionHttpRequest request = jar.apply(
                new VirtFusionHttpRequest(
                        URI.create("https://panel.example.invalid/servers/_list?limit=8"),
                        Map.of("Cookie", "placeholder", "X-XSRF-TOKEN", "placeholder")
                )
        );

        assertEquals("XSRF-TOKEN=old-xsrf; virtfusion_session=old-session", request.getHeaders().get("Cookie"));
        assertEquals("old-xsrf", request.getHeaders().get("X-XSRF-TOKEN"));
    }

    @Test
    public void mergeResponseHeaders_updates_cookie_header_and_decoded_xsrf() {
        SessionCookieJar jar = new SessionCookieJar(
                "XSRF-TOKEN=old-xsrf; virtfusion_session=old-session",
                "old-xsrf"
        );

        Map<String, List<String>> responseHeaders = new LinkedHashMap<>();
        responseHeaders.put("Set-Cookie", List.of(
                "virtfusion_session=new-session; path=/; secure; httponly",
                "XSRF-TOKEN=new%3Dxsrf; path=/; secure"
        ));

        jar.mergeResponseHeaders(responseHeaders);

        assertTrue(jar.hasChanged());
        assertEquals(
                "XSRF-TOKEN=new%3Dxsrf; virtfusion_session=new-session",
                jar.getCookieHeader()
        );
        assertEquals("new=xsrf", jar.getXsrfHeader());
    }

    @Test
    public void mergeResponseHeaders_removes_deleted_cookies() {
        SessionCookieJar jar = new SessionCookieJar(
                "XSRF-TOKEN=old-xsrf; virtfusion_session=old-session; theme=light",
                "old-xsrf"
        );

        Map<String, List<String>> responseHeaders = new LinkedHashMap<>();
        responseHeaders.put("set-cookie", List.of("theme=; Max-Age=0; path=/"));

        jar.mergeResponseHeaders(responseHeaders);

        assertTrue(jar.hasChanged());
        assertFalse(jar.getCookieHeader().contains("theme="));
    }
}
