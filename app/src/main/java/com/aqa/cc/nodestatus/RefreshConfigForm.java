package com.aqa.cc.nodestatus;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.aqa.cc.nodestatus.core.model.ProviderFamily;
import com.aqa.cc.nodestatus.core.model.SiteProfile;
final class RefreshConfigForm {
    static final class Draft {
        private final SiteProfile siteProfile;
        private final ProviderSessionConfig config;

        Draft(SiteProfile siteProfile, ProviderSessionConfig config) {
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

    private final AppCompatActivity activity;
    private final ProviderDescriptorRegistry descriptorRegistry;
    private final ViewGroup providerSettingsContainer;
    private ProviderFamily renderedProviderFamily;
    private View providerSettingsRoot;

    RefreshConfigForm(AppCompatActivity activity) {
        this(activity, new AppProviderDescriptorRegistry());
    }

    RefreshConfigForm(AppCompatActivity activity, ProviderDescriptorRegistry descriptorRegistry) {
        this.activity = activity;
        this.descriptorRegistry = descriptorRegistry;
        this.providerSettingsContainer = activity.findViewById(R.id.providerSettingsContainer);
    }

    void load(SiteProfile siteProfile, ProviderSessionConfig config, boolean debugBuild) {
        View rootView = ensureProviderSettingsRoot(
                siteProfile.getProviderFamily(),
                descriptorRegistry.getDescriptor(siteProfile.getProviderFamily())
        );
        EditText siteDisplayNameInput = requireSiteDisplayNameInput(rootView);
        TextView providerFamilyValue = requireProviderFamilyValue(rootView);
        siteDisplayNameInput.setText(siteProfile.getDisplayName());
        ProviderDescriptor descriptor = descriptorRegistry.getDescriptor(siteProfile.getProviderFamily());
        providerFamilyValue.setText(descriptor.resolveLabel(activity));
        descriptor.settingsBinder().bind(
                rootView,
                config,
                debugBuild
        );
    }

    Draft read(String siteId, ProviderFamily providerFamily, boolean debugBuild) {
        ProviderDescriptor descriptor = descriptorRegistry.getDescriptor(providerFamily);
        View rootView = ensureProviderSettingsRoot(providerFamily, descriptor);
        ProviderSessionConfig config = descriptor.settingsBinder().read(rootView, debugBuild);

        SiteProfile siteProfile = new SiteProfile(
                siteId,
                readTrimmed(requireSiteDisplayNameInput(rootView)),
                deriveSiteBaseUrl(config),
                providerFamily
        );
        return new Draft(siteProfile, config);
    }

    private View ensureProviderSettingsRoot(ProviderFamily providerFamily, ProviderDescriptor descriptor) {
        if (providerSettingsRoot != null && renderedProviderFamily == providerFamily) {
            return providerSettingsRoot;
        }

        providerSettingsContainer.removeAllViews();
        providerSettingsRoot = LayoutInflater.from(activity)
                .inflate(descriptor.settingsBinder().layoutResId(), providerSettingsContainer, false);
        providerSettingsContainer.addView(providerSettingsRoot);
        renderedProviderFamily = providerFamily;
        return providerSettingsRoot;
    }

    private String deriveSiteBaseUrl(ProviderSessionConfig config) {
        return config.getBaseUrl();
    }

    private EditText requireSiteDisplayNameInput(View rootView) {
        return rootView.findViewById(R.id.siteDisplayNameInput);
    }

    private TextView requireProviderFamilyValue(View rootView) {
        return rootView.findViewById(R.id.providerFamilyValue);
    }

    private String readTrimmed(EditText input) {
        return input.getText().toString().trim();
    }
}
