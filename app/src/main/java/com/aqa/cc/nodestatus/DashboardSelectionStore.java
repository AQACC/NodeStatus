package com.aqa.cc.nodestatus;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

public final class DashboardSelectionStore {
    private static final String PREFS_NAME = "nodestatus_dashboard_state";
    private static final String KEY_SELECTED_RESOURCE_ID = "selected_resource_id";

    private final SharedPreferences preferences;

    public DashboardSelectionStore(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveSelectedResourceId(@Nullable String resourceId) {
        preferences.edit().putString(KEY_SELECTED_RESOURCE_ID, resourceId).apply();
    }

    @Nullable
    public String loadSelectedResourceId() {
        return preferences.getString(KEY_SELECTED_RESOURCE_ID, null);
    }
}
