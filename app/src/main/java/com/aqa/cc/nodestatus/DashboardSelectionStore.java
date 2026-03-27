package com.aqa.cc.nodestatus;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

public final class DashboardSelectionStore {
    private static final String PREFS_NAME = "nodestatus_dashboard_state";
    private static final String KEY_SELECTED_RESOURCE_ID_PREFIX = "selected_resource_id.";

    private final SharedPreferences preferences;

    public DashboardSelectionStore(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveSelectedResourceId(@Nullable String siteId, @Nullable String resourceId) {
        preferences.edit().putString(resolveSelectedResourceKey(siteId), resourceId).apply();
    }

    @Nullable
    public String loadSelectedResourceId(@Nullable String siteId) {
        return preferences.getString(resolveSelectedResourceKey(siteId), null);
    }

    private String resolveSelectedResourceKey(@Nullable String siteId) {
        if (siteId == null || siteId.isBlank()) {
            return KEY_SELECTED_RESOURCE_ID_PREFIX + SiteCatalogStore.DEFAULT_SITE_ID;
        }
        return KEY_SELECTED_RESOURCE_ID_PREFIX + siteId;
    }
}
