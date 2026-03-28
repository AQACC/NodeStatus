package com.aqa.cc.nodestatus;

import com.aqa.cc.nodestatus.core.model.ProviderFamily;
import com.aqa.cc.nodestatus.core.model.SiteProfile;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public class RefreshDebugLogComposerTest {
    @Test
    public void compose_includes_sanitized_session_config_and_status() {
        ProviderSessionConfig config = new ProviderSessionConfig(
                "https://panel.example.com",
                "",
                "cookie=value",
                "xsrf-value",
                "agent/1.0",
                false,
                true,
                15
        );
        NodeStatusRefreshStatusStore.RefreshStatus status = new NodeStatusRefreshStatusStore.RefreshStatus(
                "manual",
                "Refresh failed",
                "2026-03-26T08:00:00Z",
                false,
                false,
                true,
                "java.lang.IllegalStateException: boom"
        );

        String report = RefreshDebugLogComposer.compose(
                Instant.parse("2026-03-26T08:01:00Z"),
                new SiteProfile("site-a", "Alpha", "https://panel.example.com", ProviderFamily.VIRT_FUSION),
                config,
                "Refreshing...",
                status,
                true
        );

        Assert.assertTrue(report.contains("capturedAt=2026-03-26T08:01:00Z"));
        Assert.assertTrue(report.contains("cookieHeaderLength=12"));
        Assert.assertTrue(report.contains("refreshStatusValue=Refreshing..."));
        Assert.assertTrue(report.contains("message=Refresh failed"));
        Assert.assertTrue(report.contains("hasCompatibilitySession=true"));
        Assert.assertFalse(report.contains("cookie=value"));
    }

    @Test
    public void formatThrowable_returns_stack_trace_text() {
        String report = RefreshDebugLogComposer.formatThrowable(new IllegalStateException("boom"));

        Assert.assertTrue(report.contains("IllegalStateException"));
        Assert.assertTrue(report.contains("boom"));
    }

    @Test
    public void appendSessionRenewalSummary_includes_dedicated_section_when_present() throws IOException {
        Path debugDirectory = Files.createTempDirectory("nodestatus-refresh-debug");
        Files.writeString(
                debugDirectory.resolve("session-renewal.txt"),
                "setCookieObserved=true\nsummary=Detected Set-Cookie response headers and applied a renewed session.\n",
                StandardCharsets.UTF_8
        );

        String report = RefreshDebugLogComposer.appendSessionRenewalSummary(
                "base-report",
                debugDirectory
        );

        Assert.assertTrue(report.contains("[session-renewal]"));
        Assert.assertTrue(report.contains("setCookieObserved=true"));
        Assert.assertTrue(report.contains("applied a renewed session"));
    }
}
