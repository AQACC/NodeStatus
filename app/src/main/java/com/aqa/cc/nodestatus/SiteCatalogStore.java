package com.aqa.cc.nodestatus;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aqa.cc.nodestatus.core.model.ProviderFamily;

import java.util.List;

interface SiteCatalogStore {
    String DEFAULT_SITE_ID = "site-default";

    final class StoredSite {
        private final String id;
        private final String displayName;
        private final ProviderFamily providerFamily;

        StoredSite(String id, String displayName, ProviderFamily providerFamily) {
            this.id = id;
            this.displayName = displayName;
            this.providerFamily = providerFamily;
        }

        String getId() {
            return id;
        }

        String getDisplayName() {
            return displayName;
        }

        ProviderFamily getProviderFamily() {
            return providerFamily;
        }
    }

    @NonNull
    List<StoredSite> loadSites();

    @Nullable
    String loadActiveSiteId();

    void saveActiveSiteId(@NonNull String siteId);

    boolean containsSite(@NonNull String siteId);

    @NonNull
    StoredSite ensureDefaultSite();

    @NonNull
    StoredSite createSite(@Nullable String displayName, @NonNull ProviderFamily providerFamily);

    void saveSite(@NonNull StoredSite site);

    @NonNull
    StoredSite updateSiteDisplayName(@NonNull String siteId, @Nullable String displayName);

    @NonNull
    StoredSite loadStoredSite(@NonNull String siteId);
}
