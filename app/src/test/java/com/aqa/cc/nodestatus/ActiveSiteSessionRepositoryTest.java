package com.aqa.cc.nodestatus;

import com.aqa.cc.nodestatus.core.model.ProviderFamily;
import com.aqa.cc.nodestatus.core.model.SiteProfile;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ActiveSiteSessionRepositoryTest {
    @Test
    public void loadActiveSession_bootstraps_default_site_from_root_config() {
        FakeSiteCatalogStore siteCatalogStore = new FakeSiteCatalogStore();
        ProviderSessionConfig rootConfig = config("https://panel.example.com");
        FakeSiteConfigStore configStore = new FakeSiteConfigStore(rootConfig);

        ActiveSiteSessionRepository repository = new ActiveSiteSessionRepository(siteCatalogStore, configStore);

        ActiveSiteSessionRepository.ActiveSiteSession session = repository.loadActiveSession();

        Assert.assertEquals(SiteCatalogStore.DEFAULT_SITE_ID, session.getSiteProfile().getId());
        Assert.assertEquals("VirtFusion", session.getSiteProfile().getDisplayName());
        Assert.assertEquals("https://panel.example.com", session.getSiteProfile().getBaseUrl());
        Assert.assertEquals(rootConfig.getBaseUrl(), session.getConfig().getBaseUrl());
        Assert.assertEquals(SiteCatalogStore.DEFAULT_SITE_ID, siteCatalogStore.activeSiteId);
        Assert.assertEquals(rootConfig.getBaseUrl(), configStore.scopedConfigs.get(SiteCatalogStore.DEFAULT_SITE_ID).getBaseUrl());
    }

    @Test
    public void switchActiveSite_promotes_selected_site_config_to_root_scope() {
        FakeSiteCatalogStore siteCatalogStore = new FakeSiteCatalogStore();
        siteCatalogStore.saveSite(new SiteCatalogStore.StoredSite(
                SiteCatalogStore.DEFAULT_SITE_ID,
                "VirtFusion",
                ProviderFamily.VIRT_FUSION
        ));
        siteCatalogStore.saveSite(new SiteCatalogStore.StoredSite(
                "site-b",
                "Tokyo",
                ProviderFamily.VIRT_FUSION
        ));
        siteCatalogStore.saveActiveSiteId(SiteCatalogStore.DEFAULT_SITE_ID);

        ProviderSessionConfig defaultConfig = config("https://panel-a.example.com");
        ProviderSessionConfig siteBConfig = config("https://panel-b.example.com");
        FakeSiteConfigStore configStore = new FakeSiteConfigStore(defaultConfig);
        configStore.scopedConfigs.put(SiteCatalogStore.DEFAULT_SITE_ID, defaultConfig);
        configStore.scopedConfigs.put("site-b", siteBConfig);

        ActiveSiteSessionRepository repository = new ActiveSiteSessionRepository(siteCatalogStore, configStore);

        repository.switchActiveSite("site-b");

        Assert.assertEquals("site-b", siteCatalogStore.activeSiteId);
        Assert.assertEquals(siteBConfig.getBaseUrl(), configStore.rootConfig.getBaseUrl());
        Assert.assertEquals(siteBConfig.getBaseUrl(), repository.loadActiveConfig().getBaseUrl());
    }

    @Test
    public void saveActiveDraft_updates_scoped_and_root_config_together() {
        FakeSiteCatalogStore siteCatalogStore = new FakeSiteCatalogStore();
        siteCatalogStore.saveSite(new SiteCatalogStore.StoredSite(
                SiteCatalogStore.DEFAULT_SITE_ID,
                "VirtFusion",
                ProviderFamily.VIRT_FUSION
        ));
        siteCatalogStore.saveActiveSiteId(SiteCatalogStore.DEFAULT_SITE_ID);

        ProviderSessionConfig initialConfig = config("https://panel-a.example.com");
        FakeSiteConfigStore configStore = new FakeSiteConfigStore(initialConfig);
        configStore.scopedConfigs.put(SiteCatalogStore.DEFAULT_SITE_ID, initialConfig);

        ActiveSiteSessionRepository repository = new ActiveSiteSessionRepository(siteCatalogStore, configStore);

        ProviderSessionConfig updatedConfig = config("https://panel-b.example.com");
        SiteProfile updatedSite = new SiteProfile(
                SiteCatalogStore.DEFAULT_SITE_ID,
                "Renamed site",
                updatedConfig.getBaseUrl(),
                ProviderFamily.VIRT_FUSION
        );
        repository.saveActiveDraft(new RefreshConfigForm.Draft(updatedSite, updatedConfig));

        Assert.assertEquals("Renamed site", siteCatalogStore.loadStoredSite(SiteCatalogStore.DEFAULT_SITE_ID).getDisplayName());
        Assert.assertEquals(updatedConfig.getBaseUrl(), configStore.scopedConfigs.get(SiteCatalogStore.DEFAULT_SITE_ID).getBaseUrl());
        Assert.assertEquals(updatedConfig.getBaseUrl(), configStore.rootConfig.getBaseUrl());
    }

    @Test
    public void saveActiveSessionAuth_updates_scoped_and_root_auth_together() {
        FakeSiteCatalogStore siteCatalogStore = new FakeSiteCatalogStore();
        siteCatalogStore.saveSite(new SiteCatalogStore.StoredSite(
                SiteCatalogStore.DEFAULT_SITE_ID,
                "VirtFusion",
                ProviderFamily.VIRT_FUSION
        ));
        siteCatalogStore.saveActiveSiteId(SiteCatalogStore.DEFAULT_SITE_ID);

        ProviderSessionConfig initialConfig = config("https://panel-a.example.com");
        FakeSiteConfigStore configStore = new FakeSiteConfigStore(initialConfig);
        configStore.scopedConfigs.put(SiteCatalogStore.DEFAULT_SITE_ID, initialConfig);

        ActiveSiteSessionRepository repository = new ActiveSiteSessionRepository(siteCatalogStore, configStore);

        ProviderSessionConfig updatedConfig = repository.saveActiveSessionAuth(
                "virtfusion_session=session-123",
                "xsrf-123"
        );

        Assert.assertEquals("virtfusion_session=session-123", updatedConfig.getCookieHeader());
        Assert.assertEquals("xsrf-123", updatedConfig.getXsrfHeader());
        Assert.assertEquals("virtfusion_session=session-123", configStore.rootConfig.getCookieHeader());
        Assert.assertEquals("xsrf-123", configStore.scopedConfigs.get(SiteCatalogStore.DEFAULT_SITE_ID).getXsrfHeader());
    }

    @Test
    public void saveSiteSessionAuthIfUnchanged_skips_when_auth_changed_during_refresh() {
        FakeSiteCatalogStore siteCatalogStore = new FakeSiteCatalogStore();
        siteCatalogStore.saveSite(new SiteCatalogStore.StoredSite(
                SiteCatalogStore.DEFAULT_SITE_ID,
                "VirtFusion",
                ProviderFamily.VIRT_FUSION
        ));
        siteCatalogStore.saveActiveSiteId(SiteCatalogStore.DEFAULT_SITE_ID);

        ProviderSessionConfig initialConfig = new ProviderSessionConfig(
                "https://panel-a.example.com",
                "",
                "cookie=old",
                "xsrf-old",
                "agent/1.0",
                false,
                false,
                20
        );
        ProviderSessionConfig changedConfig = new ProviderSessionConfig(
                "https://panel-a.example.com",
                "",
                "cookie=newer",
                "xsrf-newer",
                "agent/1.0",
                false,
                false,
                20
        );
        FakeSiteConfigStore configStore = new FakeSiteConfigStore(initialConfig);
        configStore.scopedConfigs.put(SiteCatalogStore.DEFAULT_SITE_ID, changedConfig);
        configStore.rootConfig = changedConfig;

        ActiveSiteSessionRepository repository = new ActiveSiteSessionRepository(siteCatalogStore, configStore);

        ProviderSessionConfig result = repository.saveSiteSessionAuthIfUnchanged(
                SiteCatalogStore.DEFAULT_SITE_ID,
                initialConfig,
                "cookie=rotated",
                "xsrf-rotated"
        );

        Assert.assertEquals("cookie=newer", result.getCookieHeader());
        Assert.assertEquals("xsrf-newer", configStore.scopedConfigs.get(SiteCatalogStore.DEFAULT_SITE_ID).getXsrfHeader());
    }

    private ProviderSessionConfig config(String baseUrl) {
        return new ProviderSessionConfig(
                baseUrl,
                "",
                "",
                "",
                "agent/1.0",
                false,
                false,
                20
        );
    }

    private static final class FakeSiteCatalogStore implements SiteCatalogStore {
        private final List<StoredSite> sites = new ArrayList<>();
        private String activeSiteId;

        @Override
        public List<StoredSite> loadSites() {
            return new ArrayList<>(sites);
        }

        @Override
        public String loadActiveSiteId() {
            return activeSiteId;
        }

        @Override
        public void saveActiveSiteId(String siteId) {
            activeSiteId = siteId;
        }

        @Override
        public boolean containsSite(String siteId) {
            return findSite(siteId) != null;
        }

        @Override
        public StoredSite ensureDefaultSite() {
            StoredSite defaultSite = new StoredSite(DEFAULT_SITE_ID, "VirtFusion", ProviderFamily.VIRT_FUSION);
            saveSite(defaultSite);
            return defaultSite;
        }

        @Override
        public StoredSite createSite(String displayName, ProviderFamily providerFamily) {
            StoredSite site = new StoredSite("site-created", displayName, providerFamily);
            saveSite(site);
            return site;
        }

        @Override
        public void saveSite(StoredSite site) {
            StoredSite existing = findSite(site.getId());
            if (existing != null) {
                sites.remove(existing);
            }
            sites.add(site);
        }

        @Override
        public StoredSite updateSiteDisplayName(String siteId, String displayName) {
            StoredSite current = loadStoredSite(siteId);
            StoredSite updated = new StoredSite(current.getId(), displayName, current.getProviderFamily());
            saveSite(updated);
            return updated;
        }

        @Override
        public StoredSite loadStoredSite(String siteId) {
            StoredSite site = findSite(siteId);
            if (site == null) {
                throw new IllegalStateException("Missing site " + siteId);
            }
            return site;
        }

        private StoredSite findSite(String siteId) {
            for (StoredSite site : sites) {
                if (site.getId().equals(siteId)) {
                    return site;
                }
            }
            return null;
        }
    }

    private static final class FakeSiteConfigStore implements SiteConfigStore {
        private ProviderSessionConfig rootConfig;
        private final Map<String, ProviderSessionConfig> scopedConfigs = new LinkedHashMap<>();

        private FakeSiteConfigStore(ProviderSessionConfig rootConfig) {
            this.rootConfig = rootConfig;
        }

        @Override
        public ProviderSessionConfig load() {
            return rootConfig;
        }

        @Override
        public ProviderSessionConfig load(String siteId) {
            ProviderSessionConfig config = scopedConfigs.get(siteId);
            if (config == null) {
                throw new IllegalStateException("Missing scoped config for " + siteId);
            }
            return config;
        }

        @Override
        public void save(ProviderSessionConfig config) {
            rootConfig = config;
        }

        @Override
        public void save(String siteId, ProviderSessionConfig config) {
            scopedConfigs.put(siteId, config);
        }

        @Override
        public boolean hasStoredConfig(String siteId) {
            return siteId != null && scopedConfigs.containsKey(siteId);
        }
    }
}
