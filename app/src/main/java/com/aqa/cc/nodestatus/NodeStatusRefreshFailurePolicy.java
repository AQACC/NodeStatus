package com.aqa.cc.nodestatus;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class NodeStatusRefreshFailurePolicy {
    enum Disposition {
        RETRY,
        FAIL,
    }

    private static final Pattern HTTP_STATUS_PATTERN = Pattern.compile("HTTP\\s+(\\d{3})");

    private NodeStatusRefreshFailurePolicy() {
    }

    static Disposition classify(Throwable throwable) {
        if (throwable instanceof IllegalArgumentException || throwable instanceof IllegalStateException) {
            return Disposition.FAIL;
        }

        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return Disposition.RETRY;
        }

        Integer statusCode = parseHttpStatusCode(message);
        if (statusCode == null) {
            return Disposition.RETRY;
        }
        if (statusCode == 408 || statusCode == 429) {
            return Disposition.RETRY;
        }
        if (statusCode >= 400 && statusCode < 500) {
            return Disposition.FAIL;
        }
        return Disposition.RETRY;
    }

    private static Integer parseHttpStatusCode(String message) {
        Matcher matcher = HTTP_STATUS_PATTERN.matcher(message);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
