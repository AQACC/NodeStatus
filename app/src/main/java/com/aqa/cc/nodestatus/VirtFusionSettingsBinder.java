package com.aqa.cc.nodestatus;

import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.aqa.cc.nodestatus.core.model.ProviderFamily;
import com.google.android.material.materialswitch.MaterialSwitch;

final class VirtFusionSettingsBinder implements ProviderSettingsBinder {
    @Override
    public @NonNull ProviderFamily providerFamily() {
        return ProviderFamily.VIRT_FUSION;
    }

    @Override
    public int layoutResId() {
        return R.layout.provider_settings_virtfusion;
    }

    @Override
    public void bind(@NonNull View rootView, @NonNull ProviderSessionConfig config, boolean debugBuild) {
        EditText baseUrlInput = rootView.findViewById(R.id.baseUrlInput);
        EditText loginEntryUrlInput = rootView.findViewById(R.id.loginEntryUrlInput);
        EditText cookieHeaderInput = rootView.findViewById(R.id.cookieHeaderInput);
        EditText xsrfHeaderInput = rootView.findViewById(R.id.xsrfHeaderInput);
        EditText userAgentInput = rootView.findViewById(R.id.userAgentInput);
        MaterialSwitch allowInsecureTlsSwitch = rootView.findViewById(R.id.allowInsecureTlsSwitch);
        MaterialSwitch notificationsEnabledSwitch = rootView.findViewById(R.id.notificationsEnabledSwitch);
        EditText trafficThresholdInput = rootView.findViewById(R.id.trafficThresholdInput);

        baseUrlInput.setText(config.getBaseUrl());
        loginEntryUrlInput.setText(config.getLoginEntryUrl());
        cookieHeaderInput.setText(config.getCookieHeader());
        xsrfHeaderInput.setText(config.getXsrfHeader());
        userAgentInput.setText(config.getUserAgent());
        allowInsecureTlsSwitch.setChecked(config.isAllowInsecureTls());
        allowInsecureTlsSwitch.setVisibility(debugBuild ? View.VISIBLE : View.GONE);
        notificationsEnabledSwitch.setChecked(config.isNotificationsEnabled());
        trafficThresholdInput.setText(String.valueOf(config.getLowTrafficThresholdPercent()));
        NotificationSettingsSupport.bindNotificationControls(rootView, notificationsEnabledSwitch, trafficThresholdInput);
        bindCollapsibleSection(
                rootView,
                R.id.optionalToggle,
                R.id.optionalContent,
                R.id.optionalState,
                !readTrimmed(loginEntryUrlInput).isEmpty()
                        || !readTrimmed(xsrfHeaderInput).isEmpty()
                        || !readTrimmed(userAgentInput).isEmpty()
                        || config.isAllowInsecureTls()
        );
    }

    @Override
    public @NonNull ProviderSessionConfig read(@NonNull View rootView, boolean debugBuild) {
        EditText baseUrlInput = rootView.findViewById(R.id.baseUrlInput);
        EditText loginEntryUrlInput = rootView.findViewById(R.id.loginEntryUrlInput);
        EditText cookieHeaderInput = rootView.findViewById(R.id.cookieHeaderInput);
        EditText xsrfHeaderInput = rootView.findViewById(R.id.xsrfHeaderInput);
        EditText userAgentInput = rootView.findViewById(R.id.userAgentInput);
        MaterialSwitch allowInsecureTlsSwitch = rootView.findViewById(R.id.allowInsecureTlsSwitch);
        MaterialSwitch notificationsEnabledSwitch = rootView.findViewById(R.id.notificationsEnabledSwitch);
        EditText trafficThresholdInput = rootView.findViewById(R.id.trafficThresholdInput);

        return new ProviderSessionConfig(
                readTrimmed(baseUrlInput),
                readTrimmed(loginEntryUrlInput),
                readTrimmed(cookieHeaderInput),
                readTrimmed(xsrfHeaderInput),
                readTrimmed(userAgentInput),
                debugBuild && allowInsecureTlsSwitch.isChecked(),
                notificationsEnabledSwitch.isChecked(),
                NotificationSettingsSupport.parseThresholdPercent(trafficThresholdInput)
        );
    }

    private String readTrimmed(EditText input) {
        return input.getText().toString().trim();
    }

    private void bindCollapsibleSection(
            @NonNull View rootView,
            int toggleId,
            int contentId,
            int stateId,
            boolean expanded
    ) {
        View toggle = rootView.findViewById(toggleId);
        View content = rootView.findViewById(contentId);
        TextView state = rootView.findViewById(stateId);
        ViewGroup transitionRoot = rootView instanceof ViewGroup ? (ViewGroup) rootView : null;

        applyExpandedState(content, state, expanded);
        toggle.setOnClickListener(view -> {
            boolean nextExpanded = content.getVisibility() != View.VISIBLE;
            if (transitionRoot != null) {
                TransitionManager.beginDelayedTransition(transitionRoot, new AutoTransition());
            }
            applyExpandedState(content, state, nextExpanded);
        });
    }

    private void applyExpandedState(@NonNull View content, @NonNull TextView state, boolean expanded) {
        content.setVisibility(expanded ? View.VISIBLE : View.GONE);
        state.setText(expanded ? R.string.settings_section_collapse : R.string.settings_section_expand);
    }
}
