package com.aqa.cc.nodestatus;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aqa.cc.nodestatus.core.model.SiteProfile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;

final class RefreshDebugLogComposer {
    private RefreshDebugLogComposer() {
    }

    @NonNull
    static String buildReport(
            @NonNull Context context,
            @NonNull SiteProfile siteProfile,
            @NonNull ProviderSessionConfig config,
            @Nullable CharSequence currentStatus
    ) {
        String report = compose(
                Instant.now(),
                siteProfile,
                config,
                currentStatus == null ? null : currentStatus.toString(),
                new NodeStatusRefreshStatusStore(context).load(),
                BuildConfig.DEBUG
        );
        Path debugDirectory = AppSnapshotFiles.virtFusionRefreshDebugDirectoryPath(context);
        report = appendSessionRenewalSummary(report, debugDirectory);
        return appendHttpCaptures(report, debugDirectory);
    }

    @NonNull
    static String compose(
            @NonNull Instant capturedAt,
            @NonNull SiteProfile siteProfile,
            @NonNull ProviderSessionConfig config,
            @Nullable String currentStatus,
            @Nullable NodeStatusRefreshStatusStore.RefreshStatus status,
            boolean debugBuild
    ) {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, "capturedAt", capturedAt.toString());
        appendLine(builder, "debugBuild", Boolean.toString(debugBuild));
        appendLine(builder, "siteId", siteProfile.getId());
        appendLine(builder, "siteDisplayName", siteProfile.getDisplayName());
        appendLine(builder, "providerFamily", siteProfile.getProviderFamily().name());

        builder.append("\n[ui]\n");
        appendLine(builder, "refreshStatusValue", currentStatus);

        builder.append("\n[config]\n");
        appendLine(builder, "baseUrl", config.getBaseUrl());
        appendLine(builder, "loginEntryUrl", config.getLoginEntryUrl());
        appendLine(builder, "hasCompatibilitySession", Boolean.toString(config.hasCompatibilitySession()));
        appendLine(builder, "cookieHeaderLength", Integer.toString(config.getCookieHeader().length()));
        appendLine(builder, "xsrfHeaderLength", Integer.toString(config.getXsrfHeader().length()));
        appendLine(builder, "canUseCompatibilitySessionFlow", Boolean.toString(config.canUseCompatibilitySessionFlow()));
        appendLine(builder, "allowInsecureTls", Boolean.toString(config.isAllowInsecureTls()));
        appendLine(builder, "notificationsEnabled", Boolean.toString(config.isNotificationsEnabled()));
        appendLine(builder, "lowTrafficThresholdPercent", Integer.toString(config.getLowTrafficThresholdPercent()));
        appendLine(builder, "userAgent", config.getUserAgent());

        builder.append("\n[stored-status]\n");
        if (status == null) {
            builder.append("status=<none>\n");
        } else {
            appendLine(builder, "source", status.getSource());
            appendLine(builder, "updatedAt", status.getUpdatedAt());
            appendLine(builder, "pending", Boolean.toString(status.isPending()));
            appendLine(builder, "success", Boolean.toString(status.isSuccess()));
            appendLine(builder, "usedCompatibilitySession", Boolean.toString(status.isUsedCompatibilitySession()));
            appendLine(builder, "message", status.getMessage());
            appendMultiline(builder, "details", status.getDetails());
        }
        return builder.toString().trim();
    }

    @NonNull
    static String formatThrowable(@NonNull Throwable throwable) {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        throwable.printStackTrace(printWriter);
        printWriter.flush();
        return writer.toString().trim();
    }

    private static void appendLine(@NonNull StringBuilder builder, @NonNull String key, @Nullable String value) {
        builder.append(key)
                .append('=')
                .append(value == null || value.isBlank() ? "<blank>" : value)
                .append('\n');
    }

    private static void appendMultiline(@NonNull StringBuilder builder, @NonNull String key, @Nullable String value) {
        if (value == null || value.isBlank()) {
            builder.append(key).append("=<blank>\n");
            return;
        }
        builder.append(key).append(":\n").append(value).append('\n');
    }

    @NonNull
    static String appendSessionRenewalSummary(@NonNull String report, @NonNull Path debugDirectory) {
        if (!BuildConfig.DEBUG) {
            return report;
        }

        java.io.File summaryFile = debugDirectory.resolve("session-renewal.txt").toFile();
        if (!summaryFile.isFile()) {
            return report;
        }

        StringBuilder builder = new StringBuilder(report);
        builder.append("\n\n[session-renewal]\n");
        try {
            builder.append(readUtf8(summaryFile).trim());
        } catch (IOException exception) {
            builder.append("error=").append(exception);
        }
        return builder.toString().trim();
    }

    @NonNull
    private static String appendHttpCaptures(@NonNull String report, @NonNull Path debugDirectory) {
        if (!BuildConfig.DEBUG) {
            return report;
        }

        java.io.File[] captureFiles = debugDirectory.toFile().listFiles(file ->
                file.isFile() && !"session-renewal.txt".equals(file.getName())
        );
        if (captureFiles == null || captureFiles.length == 0) {
            return report;
        }

        Arrays.sort(captureFiles, Comparator.comparing(java.io.File::getName));
        StringBuilder builder = new StringBuilder(report);
        builder.append("\n\n[virtfusion-http-captures]\n");
        for (java.io.File captureFile : captureFiles) {
            builder.append('\n')
                    .append('[')
                    .append(captureFile.getName())
                    .append("]\n");
            try {
                builder.append(readUtf8(captureFile).trim());
            } catch (IOException exception) {
                builder.append("error=").append(exception);
            }
            builder.append('\n');
        }
        return builder.toString().trim();
    }

    @NonNull
    private static String readUtf8(@NonNull java.io.File file) throws IOException {
        try (InputStream input = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(line);
            }
            return builder.toString();
        }
    }
}
