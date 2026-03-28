package com.aqa.cc.nodestatus;

import androidx.annotation.NonNull;

import com.aqa.cc.nodestatus.adapter.virtfusion.VirtFusionCapturedResponseParser;
import com.aqa.cc.nodestatus.adapter.virtfusion.VirtFusionLocalSessionAuth;
import com.aqa.cc.nodestatus.adapter.virtfusion.VirtFusionSessionClient;
import com.aqa.cc.nodestatus.core.model.ResourceSnapshot;
import com.aqa.cc.nodestatus.core.model.SiteProfile;

import java.time.Instant;
import java.util.List;

final class VirtFusionProviderRefreshAdapter implements ProviderRefreshAdapter {
    @Override
    public @NonNull List<ResourceSnapshot> fetchSnapshots(
            @NonNull SiteProfile siteProfile,
            @NonNull ProviderRefreshConfig config,
            @NonNull Instant collectedAt
    ) {
        ProviderSessionConfig sessionConfig = requireVirtFusionConfig(config);
        VirtFusionDebugCaptureTransport transport = createTransport(sessionConfig);
        List<ResourceSnapshot> snapshots = new VirtFusionSessionClient(
                createAuth(sessionConfig),
                new VirtFusionCapturedResponseParser(),
                transport
        ).fetchSnapshots(8, collectedAt);
        persistRenewedSession(siteProfile, sessionConfig, transport);
        return snapshots;
    }

    @NonNull
    private VirtFusionLocalSessionAuth createAuth(@NonNull ProviderSessionConfig config) {
        return new VirtFusionLocalSessionAuth(
                config.getBaseUrl(),
                config.getCookieHeader(),
                config.getXsrfHeader(),
                config.getUserAgent(),
                config.isAllowInsecureTls()
        );
    }

    @NonNull
    private VirtFusionDebugCaptureTransport createTransport(@NonNull ProviderSessionConfig config) {
        return new VirtFusionDebugCaptureTransport(
                config.getCookieHeader(),
                config.getXsrfHeader(),
                config.isAllowInsecureTls(),
                BuildConfig.DEBUG
        );
    }

    private void persistRenewedSession(
            @NonNull SiteProfile siteProfile,
            @NonNull ProviderSessionConfig config,
            @NonNull VirtFusionDebugCaptureTransport transport
    ) {
        if (!transport.hasSessionUpdates()) {
            return;
        }
        new ActiveSiteSessionRepository(NodeStatusApp.appContext()).saveSiteSessionAuthIfUnchanged(
                siteProfile.getId(),
                config,
                transport.getLatestCookieHeader(),
                transport.getLatestXsrfHeader()
        );
    }

    @NonNull
    private ProviderSessionConfig requireVirtFusionConfig(@NonNull ProviderRefreshConfig config) {
        if (!(config instanceof ProviderSessionConfig)) {
            throw new IllegalStateException("VirtFusion adapter requires ProviderSessionConfig.");
        }
        return (ProviderSessionConfig) config;
    }
}
