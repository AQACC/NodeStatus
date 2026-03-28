package com.aqa.cc.nodestatus;

import androidx.annotation.NonNull;
import androidx.annotation.LayoutRes;

import com.aqa.cc.nodestatus.core.model.ProviderFamily;

interface ProviderSettingsBinder {
    @NonNull
    ProviderFamily providerFamily();

    @LayoutRes
    int layoutResId();

    void bind(@NonNull android.view.View rootView, @NonNull ProviderSessionConfig config, boolean debugBuild);

    @NonNull
    ProviderSessionConfig read(@NonNull android.view.View rootView, boolean debugBuild);
}
