package com.aqa.cc.nodestatus;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProviderSessionConfigTest {
    @Test
    public void emptyConfigurationIsNotRunnable() {
        ProviderSessionConfig config = new ProviderSessionConfig(
                "",
                "",
                "",
                "",
                "agent",
                false,
                false,
                20
        );

        assertFalse(config.hasAnyAuth());
        assertFalse(config.hasRunnableAuth());
        assertFalse(config.canUseCompatibilitySessionFlow());
    }

    @Test
    public void compatibilitySessionNeedsBaseUrlToBeRunnable() {
        ProviderSessionConfig config = new ProviderSessionConfig(
                "",
                "",
                "cookie=value",
                "xsrf-value",
                "agent",
                false,
                false,
                20
        );

        assertTrue(config.hasCompatibilitySession());
        assertFalse(config.canUseCompatibilitySessionFlow());
        assertFalse(config.hasRunnableAuth());
    }

    @Test
    public void cookieOnlySessionIsRunnableWhenBaseUrlIsPresent() {
        ProviderSessionConfig config = new ProviderSessionConfig(
                "https://panel.example.com",
                "",
                "cookie=value",
                "",
                "agent",
                false,
                false,
                20
        );

        assertTrue(config.hasCompatibilitySession());
        assertTrue(config.canUseCompatibilitySessionFlow());
        assertTrue(config.hasRunnableAuth());
    }
}
