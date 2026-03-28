package com.aqa.cc.nodestatus;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.aqa.cc.nodestatus.core.model.ProviderFamily;

final class ProviderDescriptor {
    private final ProviderFamily providerFamily;
    private final int labelResId;
    private final int createHelperResId;
    private final ProviderSettingsBinder settingsBinder;
    private final ProviderRefreshAdapter refreshAdapter;

    private ProviderDescriptor(
            @NonNull ProviderFamily providerFamily,
            @StringRes int labelResId,
            @StringRes int createHelperResId,
            @NonNull ProviderSettingsBinder settingsBinder,
            ProviderRefreshAdapter refreshAdapter
    ) {
        this.providerFamily = providerFamily;
        this.labelResId = labelResId;
        this.createHelperResId = createHelperResId;
        this.settingsBinder = settingsBinder;
        this.refreshAdapter = refreshAdapter;
    }

    @NonNull
    static ProviderDescriptor refreshable(
            @NonNull ProviderFamily providerFamily,
            @StringRes int labelResId,
            @StringRes int createHelperResId,
            @NonNull ProviderSettingsBinder settingsBinder,
            @NonNull ProviderRefreshAdapter refreshAdapter
    ) {
        return new ProviderDescriptor(
                providerFamily,
                labelResId,
                createHelperResId,
                settingsBinder,
                refreshAdapter
        );
    }

    @NonNull
    static ProviderDescriptor settingsOnly(
            @NonNull ProviderFamily providerFamily,
            @StringRes int labelResId,
            @StringRes int createHelperResId,
            @NonNull ProviderSettingsBinder settingsBinder
    ) {
        return new ProviderDescriptor(
                providerFamily,
                labelResId,
                createHelperResId,
                settingsBinder,
                null
        );
    }

    @NonNull
    ProviderFamily providerFamily() {
        return providerFamily;
    }

    @NonNull
    String resolveLabel(@NonNull Context context) {
        return context.getString(labelResId);
    }

    @NonNull
    String resolveCreateHelper(@NonNull Context context) {
        return context.getString(createHelperResId);
    }

    boolean supportsRefresh() {
        return refreshAdapter != null;
    }

    @NonNull
    ProviderSettingsBinder settingsBinder() {
        return settingsBinder;
    }

    @NonNull
    ProviderRefreshAdapter requireRefreshAdapter() {
        if (refreshAdapter == null) {
            throw new IllegalStateException("No refresh adapter is registered for provider family " + providerFamily);
        }
        return refreshAdapter;
    }
}
