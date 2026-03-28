package com.aqa.cc.nodestatus;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aqa.cc.nodestatus.core.model.ProviderFamily;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class ProviderSiteCatalogStore implements SiteCatalogStore {
    private static final String DEFAULT_SITE_NAME = "VirtFusion";
    private static final String KEY_SITE_ORDER = "site_order";
    private static final String KEY_ACTIVE_SITE_ID = "active_site_id";
    private static final String KEY_SITE_DISPLAY_NAME = "display_name";
    private static final String KEY_SITE_PROVIDER_FAMILY = "provider_family";

    private final SharedPreferences preferences;

    ProviderSiteCatalogStore(Context context) {
        preferences = context.getSharedPreferences(ProviderSessionConfigStore.PREFS_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    @Override
    public List<StoredSite> loadSites() {
        List<String> siteIds = parseSiteIds(preferences.getString(KEY_SITE_ORDER, ""));
        List<StoredSite> sites = new ArrayList<>();
        for (String siteId : siteIds) {
            sites.add(loadStoredSite(siteId));
        }
        return sites;
    }

    @Nullable
    @Override
    public String loadActiveSiteId() {
        return preferences.getString(KEY_ACTIVE_SITE_ID, null);
    }

    @Override
    public void saveActiveSiteId(@NonNull String siteId) {
        preferences.edit().putString(KEY_ACTIVE_SITE_ID, siteId).apply();
    }

    @Override
    public boolean containsSite(@NonNull String siteId) {
        return parseSiteIds(preferences.getString(KEY_SITE_ORDER, "")).contains(siteId);
    }

    @NonNull
    @Override
    public StoredSite ensureDefaultSite() {
        if (containsSite(DEFAULT_SITE_ID)) {
            return loadStoredSite(DEFAULT_SITE_ID);
        }
        StoredSite defaultSite = new StoredSite(
                DEFAULT_SITE_ID,
                DEFAULT_SITE_NAME,
                ProviderFamily.VIRT_FUSION
        );
        saveSite(defaultSite);
        return defaultSite;
    }

    @NonNull
    @Override
    public StoredSite createSite(@Nullable String displayName, @NonNull ProviderFamily providerFamily) {
        String siteId = "site-" + UUID.randomUUID();
        StoredSite site = new StoredSite(
                siteId,
                normalizeDisplayName(displayName, providerFamily),
                providerFamily
        );
        saveSite(site);
        return site;
    }

    @Override
    public void saveSite(@NonNull StoredSite site) {
        List<String> siteIds = parseSiteIds(preferences.getString(KEY_SITE_ORDER, ""));
        if (!siteIds.contains(site.getId())) {
            siteIds.add(site.getId());
        }

        preferences.edit()
                .putString(KEY_SITE_ORDER, String.join(",", siteIds))
                .putString(siteMetadataKey(site.getId(), KEY_SITE_DISPLAY_NAME), site.getDisplayName())
                .putString(siteMetadataKey(site.getId(), KEY_SITE_PROVIDER_FAMILY), site.getProviderFamily().name())
                .apply();
    }

    @NonNull
    @Override
    public StoredSite updateSiteDisplayName(@NonNull String siteId, @Nullable String displayName) {
        StoredSite current = loadStoredSite(siteId);
        StoredSite updated = new StoredSite(
                current.getId(),
                normalizeDisplayName(displayName, current.getProviderFamily()),
                current.getProviderFamily()
        );
        saveSite(updated);
        return updated;
    }

    @NonNull
    @Override
    public StoredSite loadStoredSite(@NonNull String siteId) {
        String rawProviderFamily = preferences.getString(siteMetadataKey(siteId, KEY_SITE_PROVIDER_FAMILY), null);
        ProviderFamily providerFamily;
        try {
            providerFamily = rawProviderFamily == null
                    ? ProviderFamily.VIRT_FUSION
                    : ProviderFamily.valueOf(rawProviderFamily);
        } catch (IllegalArgumentException ignored) {
            providerFamily = ProviderFamily.VIRT_FUSION;
        }

        String displayName = preferences.getString(siteMetadataKey(siteId, KEY_SITE_DISPLAY_NAME), null);
        return new StoredSite(
                siteId,
                normalizeDisplayName(displayName, providerFamily),
                providerFamily
        );
    }

    @NonNull
    private List<String> parseSiteIds(@Nullable String rawSiteOrder) {
        List<String> siteIds = new ArrayList<>();
        if (rawSiteOrder == null || rawSiteOrder.isBlank()) {
            return siteIds;
        }

        String[] parts = rawSiteOrder.split(",");
        for (String part : parts) {
            String siteId = part.trim();
            if (!siteId.isEmpty() && !siteIds.contains(siteId)) {
                siteIds.add(siteId);
            }
        }
        return siteIds;
    }

    private String siteMetadataKey(@NonNull String siteId, @NonNull String key) {
        return "site." + siteId + ".meta." + key;
    }

    private String normalizeDisplayName(@Nullable String displayName, @NonNull ProviderFamily providerFamily) {
        if (displayName != null && !displayName.isBlank()) {
            return displayName.trim();
        }
        if (providerFamily == ProviderFamily.VIRT_FUSION) {
            return DEFAULT_SITE_NAME;
        }
        return providerFamily.name();
    }
}
