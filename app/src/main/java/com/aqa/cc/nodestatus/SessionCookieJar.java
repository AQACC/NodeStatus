package com.aqa.cc.nodestatus;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aqa.cc.nodestatus.adapter.virtfusion.VirtFusionHttpRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

final class SessionCookieJar {
    private final LinkedHashMap<String, String> cookies;
    private final String initialCookieHeader;
    private final String initialXsrfHeader;
    private String currentXsrfHeader;

    SessionCookieJar(@NonNull String cookieHeader, @NonNull String xsrfHeader) {
        this.cookies = parseCookieHeader(cookieHeader);
        this.initialCookieHeader = buildCookieHeader(cookies);
        this.initialXsrfHeader = xsrfHeader;
        this.currentXsrfHeader = resolveXsrfHeader(xsrfHeader, cookies);
    }

    @NonNull
    synchronized VirtFusionHttpRequest apply(@NonNull VirtFusionHttpRequest request) {
        LinkedHashMap<String, String> headers = new LinkedHashMap<>(request.getHeaders());
        String cookieHeader = getCookieHeader();
        if (!cookieHeader.isBlank()) {
            headers.put("Cookie", cookieHeader);
        } else {
            headers.remove("Cookie");
        }
        if (!currentXsrfHeader.isBlank()) {
            headers.put("X-XSRF-TOKEN", currentXsrfHeader);
        } else {
            headers.remove("X-XSRF-TOKEN");
        }
        return new VirtFusionHttpRequest(request.getUri(), headers);
    }

    synchronized void mergeResponseHeaders(@Nullable Map<String, List<String>> responseHeaders) {
        if (responseHeaders == null || responseHeaders.isEmpty()) {
            return;
        }
        boolean updated = false;
        for (Map.Entry<String, List<String>> entry : responseHeaders.entrySet()) {
            String headerName = entry.getKey();
            if (headerName == null || !"set-cookie".equalsIgnoreCase(headerName)) {
                continue;
            }
            List<String> values = entry.getValue();
            if (values == null) {
                continue;
            }
            for (String headerValue : values) {
                SetCookie update = parseSetCookie(headerValue);
                if (update == null) {
                    continue;
                }
                if (update.isDeletion()) {
                    updated |= cookies.remove(update.getName()) != null;
                    continue;
                }
                String previousValue = cookies.put(update.getName(), update.getValue());
                updated |= !Objects.equals(previousValue, update.getValue());
            }
        }
        if (updated) {
            currentXsrfHeader = resolveXsrfHeader(currentXsrfHeader, cookies);
        }
    }

    synchronized boolean hasChanged() {
        return !initialCookieHeader.equals(getCookieHeader())
                || !initialXsrfHeader.equals(currentXsrfHeader);
    }

    synchronized boolean isCookieHeaderChanged() {
        return !initialCookieHeader.equals(getCookieHeader());
    }

    synchronized boolean isXsrfHeaderChanged() {
        return !initialXsrfHeader.equals(currentXsrfHeader);
    }

    @NonNull
    synchronized String getCookieHeader() {
        return buildCookieHeader(cookies);
    }

    @NonNull
    synchronized String getXsrfHeader() {
        return currentXsrfHeader;
    }

    @NonNull
    private String resolveXsrfHeader(
            @NonNull String fallbackXsrfHeader,
            @NonNull LinkedHashMap<String, String> currentCookies
    ) {
        String xsrfCookie = currentCookies.get("XSRF-TOKEN");
        if (xsrfCookie == null || xsrfCookie.isBlank()) {
            return fallbackXsrfHeader;
        }
        return SessionCookieParser.decodeCookieValue(xsrfCookie);
    }

    @NonNull
    private static LinkedHashMap<String, String> parseCookieHeader(@Nullable String cookieHeader) {
        LinkedHashMap<String, String> parsed = new LinkedHashMap<>();
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return parsed;
        }
        for (String rawPart : cookieHeader.split(";")) {
            String part = rawPart.trim();
            int separatorIndex = part.indexOf('=');
            if (separatorIndex <= 0) {
                continue;
            }
            parsed.put(
                    part.substring(0, separatorIndex).trim(),
                    part.substring(separatorIndex + 1).trim()
            );
        }
        return parsed;
    }

    @NonNull
    private static String buildCookieHeader(@NonNull LinkedHashMap<String, String> cookies) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            parts.add(entry.getKey() + "=" + entry.getValue());
        }
        return String.join("; ", parts);
    }

    @Nullable
    private static SetCookie parseSetCookie(@Nullable String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        String[] parts = headerValue.split(";");
        if (parts.length == 0) {
            return null;
        }
        String nameValue = parts[0].trim();
        int separatorIndex = nameValue.indexOf('=');
        if (separatorIndex <= 0) {
            return null;
        }
        String name = nameValue.substring(0, separatorIndex).trim();
        String value = nameValue.substring(separatorIndex + 1).trim();
        boolean deletion = value.isEmpty();
        for (int index = 1; index < parts.length; index++) {
            String attribute = parts[index].trim();
            int attributeSeparator = attribute.indexOf('=');
            String attributeName = attributeSeparator >= 0
                    ? attribute.substring(0, attributeSeparator).trim()
                    : attribute;
            String attributeValue = attributeSeparator >= 0
                    ? attribute.substring(attributeSeparator + 1).trim()
                    : "";
            if ("max-age".equalsIgnoreCase(attributeName) && "0".equals(attributeValue)) {
                deletion = true;
            }
            if ("expires".equalsIgnoreCase(attributeName)
                    && attributeValue.toLowerCase(Locale.US).contains("1970")) {
                deletion = true;
            }
        }
        return new SetCookie(name, value, deletion);
    }

    private static final class SetCookie {
        private final String name;
        private final String value;
        private final boolean deletion;

        private SetCookie(@NonNull String name, @NonNull String value, boolean deletion) {
            this.name = name;
            this.value = value;
            this.deletion = deletion;
        }

        @NonNull
        String getName() {
            return name;
        }

        @NonNull
        String getValue() {
            return value;
        }

        boolean isDeletion() {
            return deletion;
        }
    }
}
