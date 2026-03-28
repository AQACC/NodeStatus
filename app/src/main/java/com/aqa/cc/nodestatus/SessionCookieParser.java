package com.aqa.cc.nodestatus;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

final class SessionCookieParser {
    private SessionCookieParser() {
    }

    static boolean containsCookie(@Nullable String cookieHeader, @NonNull String cookieName) {
        return findCookieValue(cookieHeader, cookieName) != null;
    }

    @Nullable
    static String findCookieValue(@Nullable String cookieHeader, @NonNull String cookieName) {
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return null;
        }
        String[] segments = cookieHeader.split(";");
        for (String segment : segments) {
            String trimmed = segment.trim();
            int separatorIndex = trimmed.indexOf('=');
            if (separatorIndex <= 0) {
                continue;
            }
            String name = trimmed.substring(0, separatorIndex).trim();
            if (!cookieName.equals(name)) {
                continue;
            }
            return trimmed.substring(separatorIndex + 1).trim();
        }
        return null;
    }

    @NonNull
    static String decodeCookieValue(@Nullable String encodedValue) {
        if (encodedValue == null || encodedValue.isBlank()) {
            return "";
        }
        return URLDecoder.decode(encodedValue, StandardCharsets.UTF_8);
    }
}
