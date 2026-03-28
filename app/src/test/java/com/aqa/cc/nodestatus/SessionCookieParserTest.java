package com.aqa.cc.nodestatus;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SessionCookieParserTest {
    @Test
    public void findCookieValue_reads_trimmed_cookie_segments() {
        String cookieHeader = "foo=bar; XSRF-TOKEN=hello%3Dworld; virtfusion_session=session-123";

        assertEquals("session-123", SessionCookieParser.findCookieValue(cookieHeader, "virtfusion_session"));
        assertEquals("hello%3Dworld", SessionCookieParser.findCookieValue(cookieHeader, "XSRF-TOKEN"));
        assertTrue(SessionCookieParser.containsCookie(cookieHeader, "virtfusion_session"));
    }

    @Test
    public void decodeCookieValue_decodes_percent_encoded_values() {
        assertEquals("hello=world", SessionCookieParser.decodeCookieValue("hello%3Dworld"));
    }

    @Test
    public void findCookieValue_returns_null_when_cookie_is_missing() {
        assertNull(SessionCookieParser.findCookieValue("foo=bar", "virtfusion_session"));
    }
}
