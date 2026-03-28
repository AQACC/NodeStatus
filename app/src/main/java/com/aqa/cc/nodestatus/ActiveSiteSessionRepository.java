package com.aqa.cc.nodestatus;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aqa.cc.nodestatus.core.model.ProviderFamily;
import com.aqa.cc.nodestatus.core.model.SiteProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class ActiveSiteSessionRepository {
    static final class ActiveSiteSession {
        private final SiteProfile siteProfile;
        private final ProviderSessionConfig config;

        ActiveSiteSession(SiteProfile siteProfile, ProviderSessionConfig config) {
            this.siteProfile = siteProfile;
            this.config = config;
        }

        SiteProfile getSiteProfile() {
            return siteProfile;
        }

        ProviderSessionConfig getConfig() {
            return config;
        }
    }

    private final SiteCatalogStore siteCatalogStore;
    private final SiteConfigStore configStore;

    ActiveSiteSessionRepository(Context context) {
        this(new ProviderSiteCatalogStore(context), new ProviderSessionConfigStore(context));
    }

    ActiveSiteSessionRepository(
            SiteCatalogStore siteCatalogStore,
            SiteConfigStore configStore
    ) {
        this.siteCatalogStore = siteCatalogStore;
        this.configStore = configStore;
    }

    @NonNull
    ActiveSiteSession loadActiveSession() {
        InitializedState state = ensureInitialized();
        SiteCatalogStore.StoredSite activeSite = state.getActiveSite();
        return new ActiveSiteSession(
                toSiteProfile(activeSite),
                configStore.load(activeSite.getId())
        );
    }

    @NonNull
    SiteProfile loadActiveSite() {
        return toSiteProfile(ensureInitialized().getActiveSite());
    }

    @NonNull
    ProviderSessionConfig loadActiveConfig() {
        return configStore.load(ensureInitialized().getActiveSite().getId());
    }

    @NonNull
    String loadActiveSiteId() {
        return ensureInitialized().getActiveSite().getId();
    }

    @NonNull
    List<SiteProfile> loadSites() {
        InitializedState state = ensureInitialized();
        List<SiteProfile> sites = new ArrayList<>();
        for (SiteCatalogStore.StoredSite site : state.getSites()) {
            sites.add(toSiteProfile(site));
        }
        return sites;
    }

    @NonNull
    SiteProfile saveActiveDraft(@NonNull RefreshConfigForm.Draft draft) {
        InitializedState state = ensureInitialized();
        SiteCatalogStore.StoredSite activeSite = state.getActiveSite();
        SiteCatalogStore.StoredSite updatedSite = siteCatalogStore.updateSiteDisplayName(
                activeSite.getId(),
                draft.getSiteProfile().getDisplayName()
        );
        persistSiteConfigAndMirrorRoot(updatedSite.getId(), draft.getConfig());
        return toSiteProfile(updatedSite);
    }

    @NonNull
    ProviderSessionConfig saveActiveSessionAuth(@NonNull String cookieHeader, @NonNull String xsrfHeader) {
        InitializedState state = ensureInitialized();
        return saveSiteSessionAuthInternal(
                state,
                state.getActiveSite().getId(),
                cookieHeader,
                xsrfHeader,
                true
        );
    }

    @NonNull
    ProviderSessionConfig saveSiteSessionAuthIfUnchanged(
            @NonNull String siteId,
            @NonNull ProviderSessionConfig expectedConfig,
            @NonNull String cookieHeader,
            @NonNull String xsrfHeader
    ) {
        InitializedState state = ensureInitialized();
        ProviderSessionConfig currentConfig = ensureSiteConfig(siteId, configStore.load());
        boolean unchanged = Objects.equals(currentConfig.getBaseUrl(), expectedConfig.getBaseUrl())
                && Objects.equals(currentConfig.getLoginEntryUrl(), expectedConfig.getLoginEntryUrl())
                && Objects.equals(currentConfig.getCookieHeader(), expectedConfig.getCookieHeader())
                && Objects.equals(currentConfig.getXsrfHeader(), expectedConfig.getXsrfHeader());
        if (!unchanged) {
            return currentConfig;
        }
        return saveSiteSessionAuthInternal(
                state,
                siteId,
                cookieHeader,
                xsrfHeader,
                siteId.equals(state.getActiveSite().getId())
        );
    }

    @NonNull
    SiteProfile switchActiveSite(@NonNull String siteId) {
        InitializedState state = ensureInitialized();
        SiteCatalogStore.StoredSite selectedSite = state.findSite(siteId);
        if (selectedSite == null) {
            return toSiteProfile(state.getActiveSite());
        }
        siteCatalogStore.saveActiveSiteId(siteId);
        ProviderSessionConfig selectedConfig = ensureSiteConfig(siteId, configStore.load());
        mirrorRootConfig(selectedConfig);
        return toSiteProfile(selectedSite);
    }

    @NonNull
    SiteProfile createSite(@Nullable String displayName, @NonNull ProviderFamily providerFamily) {
        ensureInitialized();
        SiteCatalogStore.StoredSite createdSite = siteCatalogStore.createSite(displayName, providerFamily);
        ProviderSessionConfig emptyConfig = emptyConfig();
        persistSiteConfig(createdSite.getId(), emptyConfig);
        siteCatalogStore.saveActiveSiteId(createdSite.getId());
        mirrorRootConfig(emptyConfig);
        return toSiteProfile(createdSite);
    }

    @NonNull
    private synchronized InitializedState ensureInitialized() {
        List<SiteCatalogStore.StoredSite> sites = siteCatalogStore.loadSites();
        SiteCatalogStore.StoredSite activeSite;

        if (sites.isEmpty()) {
            activeSite = siteCatalogStore.ensureDefaultSite();
            sites = List.of(activeSite);
        }

        String activeSiteId = siteCatalogStore.loadActiveSiteId();
        activeSite = findSite(sites, activeSiteId);
        if (activeSite == null) {
            activeSite = sites.get(0);
            siteCatalogStore.saveActiveSiteId(activeSite.getId());
        }

        ProviderSessionConfig rootConfig = configStore.load();
        ProviderSessionConfig activeConfig = ensureSiteConfig(activeSite.getId(), rootConfig);
        if (!rootConfig.equals(activeConfig)) {
            mirrorRootConfig(activeConfig);
        }

        return new InitializedState(sites, activeSite);
    }

    @NonNull
    private ProviderSessionConfig ensureSiteConfig(
            @NonNull String siteId,
            @NonNull ProviderSessionConfig fallbackConfig
    ) {
        if (!configStore.hasStoredConfig(siteId)) {
            persistSiteConfig(siteId, fallbackConfig);
            return fallbackConfig;
        }
        return configStore.load(siteId);
    }

    private void persistSiteConfigAndMirrorRoot(
            @NonNull String siteId,
            @NonNull ProviderSessionConfig config
    ) {
        persistSiteConfig(siteId, config);
        mirrorRootConfig(config);
    }

    @NonNull
    private ProviderSessionConfig saveSiteSessionAuthInternal(
            @NonNull InitializedState state,
            @NonNull String siteId,
            @NonNull String cookieHeader,
            @NonNull String xsrfHeader,
            boolean mirrorRoot
    ) {
        ProviderSessionConfig currentConfig = ensureSiteConfig(siteId, configStore.load());
        ProviderSessionConfig updatedConfig = new ProviderSessionConfig(
                currentConfig.getBaseUrl(),
                currentConfig.getLoginEntryUrl(),
                cookieHeader,
                xsrfHeader,
                currentConfig.getUserAgent(),
                currentConfig.isAllowInsecureTls(),
                currentConfig.isNotificationsEnabled(),
                currentConfig.getLowTrafficThresholdPercent()
        );
        persistSiteConfig(siteId, updatedConfig);
        if (mirrorRoot && siteId.equals(state.getActiveSite().getId())) {
            mirrorRootConfig(updatedConfig);
        }
        return updatedConfig;
    }

    private void persistSiteConfig(@NonNull String siteId, @NonNull ProviderSessionConfig config) {
        configStore.save(siteId, config);
    }

    private void mirrorRootConfig(@NonNull ProviderSessionConfig config) {
        configStore.save(config);
    }

    @NonNull
    private SiteProfile toSiteProfile(@NonNull SiteCatalogStore.StoredSite site) {
        ProviderSessionConfig config = configStore.load(site.getId());
        return new SiteProfile(
                site.getId(),
                site.getDisplayName(),
                deriveSiteBaseUrl(config),
                site.getProviderFamily()
        );
    }

    @Nullable
    private SiteCatalogStore.StoredSite findSite(
            @NonNull List<SiteCatalogStore.StoredSite> sites,
            @Nullable String siteId
    ) {
        if (siteId == null || siteId.isBlank()) {
            return null;
        }
        for (SiteCatalogStore.StoredSite site : sites) {
            if (site.getId().equals(siteId)) {
                return site;
            }
        }
        return null;
    }

    private String deriveSiteBaseUrl(@NonNull ProviderSessionConfig config) {
        return config.getBaseUrl();
    }

    @NonNull
    private ProviderSessionConfig emptyConfig() {
        return new ProviderSessionConfig(
                "",
                "",
                "",
                "",
                ProviderSessionConfigStore.DEFAULT_USER_AGENT,
                false,
                false,
                NotificationSettingsSupport.DEFAULT_TRAFFIC_THRESHOLD_PERCENT
        );
    }

    private static final class InitializedState {
        private final List<SiteCatalogStore.StoredSite> sites;
        private final SiteCatalogStore.StoredSite activeSite;

        InitializedState(
                List<SiteCatalogStore.StoredSite> sites,
                SiteCatalogStore.StoredSite activeSite
        ) {
            this.sites = sites;
            this.activeSite = activeSite;
        }

        List<SiteCatalogStore.StoredSite> getSites() {
            return sites;
        }

        SiteCatalogStore.StoredSite getActiveSite() {
            return activeSite;
        }

        @Nullable
        SiteCatalogStore.StoredSite findSite(@NonNull String siteId) {
            for (SiteCatalogStore.StoredSite site : sites) {
                if (site.getId().equals(siteId)) {
                    return site;
                }
            }
            return null;
        }
    }
}
