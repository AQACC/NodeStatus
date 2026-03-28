package com.aqa.cc.nodestatus;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.aqa.cc.nodestatus.core.model.ProviderFamily;
import com.aqa.cc.nodestatus.core.model.SiteProfile;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

final class ProviderSiteController {
    interface Listener {
        void beforeSiteMutation();

        void onSiteChanged();

        void onSiteCreated();
    }

    private final AppCompatActivity activity;
    private final ActiveSiteSessionRepository repository;
    private final Listener listener;
    private final ProviderDescriptorRegistry descriptorRegistry;
    private final ChipGroup providerChipGroup;
    private final MaterialButton addProviderButton;

    private boolean suppressSelectionEvents;

    ProviderSiteController(
            AppCompatActivity activity,
            ActiveSiteSessionRepository repository,
            Listener listener
    ) {
        this(activity, repository, listener, new AppProviderDescriptorRegistry());
    }

    ProviderSiteController(
            AppCompatActivity activity,
            ActiveSiteSessionRepository repository,
            Listener listener,
            ProviderDescriptorRegistry descriptorRegistry
    ) {
        this.activity = activity;
        this.repository = repository;
        this.listener = listener;
        this.descriptorRegistry = descriptorRegistry;
        this.providerChipGroup = activity.findViewById(R.id.providerChipGroup);
        this.addProviderButton = activity.findViewById(R.id.addProviderButton);
    }

    void bind() {
        providerChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (suppressSelectionEvents || checkedIds.isEmpty()) {
                return;
            }
            View checkedView = group.findViewById(checkedIds.get(0));
            Object tag = checkedView == null ? null : checkedView.getTag();
            if (!(tag instanceof String)) {
                return;
            }
            String siteId = (String) tag;
            if (siteId.equals(repository.loadActiveSiteId())) {
                return;
            }
            listener.beforeSiteMutation();
            repository.switchActiveSite(siteId);
            listener.onSiteChanged();
        });

        addProviderButton.setOnClickListener(view -> showAddProviderDialog());
    }

    void render() {
        List<SiteProfile> sites = repository.loadSites();
        String activeSiteId = repository.loadActiveSiteId();

        suppressSelectionEvents = true;
        providerChipGroup.removeAllViews();

        int selectedChipId = View.NO_ID;
        for (SiteProfile site : sites) {
            Chip chip = createProviderChip(site);
            chip.setId(View.generateViewId());
            providerChipGroup.addView(chip);
            if (site.getId().equals(activeSiteId)) {
                selectedChipId = chip.getId();
            }
        }

        if (selectedChipId != View.NO_ID) {
            providerChipGroup.check(selectedChipId);
        }
        suppressSelectionEvents = false;
    }

    private void showAddProviderDialog() {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_add_provider, null, false);
        TextInputEditText nameInput = dialogView.findViewById(R.id.newProviderNameInput);
        RadioGroup providerFamilyGroup = dialogView.findViewById(R.id.newProviderFamilyGroup);
        TextView helperText = dialogView.findViewById(R.id.newProviderHelperText);
        List<ProviderDescriptor> descriptors = descriptorRegistry.allDescriptors();
        bindProviderChoices(providerFamilyGroup, helperText, descriptors);

        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.provider_add_dialog_title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.provider_add_confirm, (dialog, which) -> {
                    listener.beforeSiteMutation();
                    ProviderDescriptor selectedDescriptor = resolveSelectedDescriptor(providerFamilyGroup);
                    ProviderFamily selectedFamily = selectedDescriptor == null
                            ? ProviderFamily.VIRT_FUSION
                            : selectedDescriptor.providerFamily();
                    repository.createSite(
                            nameInput.getText() == null ? null : nameInput.getText().toString(),
                            selectedFamily
                    );
                    listener.onSiteCreated();
                })
                .show();
    }

    private void bindProviderChoices(
            RadioGroup providerFamilyGroup,
            TextView helperText,
            List<ProviderDescriptor> descriptors
    ) {
        providerFamilyGroup.removeAllViews();
        providerFamilyGroup.setOnCheckedChangeListener(null);
        if (descriptors.isEmpty()) {
            helperText.setText("");
            return;
        }

        int checkedId = View.NO_ID;
        for (ProviderDescriptor descriptor : descriptors) {
            MaterialRadioButton radioButton = createProviderChoice(descriptor);
            providerFamilyGroup.addView(radioButton);
            if (checkedId == View.NO_ID) {
                checkedId = radioButton.getId();
            }
        }

        providerFamilyGroup.setOnCheckedChangeListener((group, selectedId) -> {
            ProviderDescriptor descriptor = resolveSelectedDescriptor(group);
            if (descriptor != null) {
                helperText.setText(descriptor.resolveCreateHelper(activity));
            }
        });
        providerFamilyGroup.check(checkedId);
        helperText.setText(descriptors.get(0).resolveCreateHelper(activity));
    }

    private ProviderDescriptor resolveSelectedDescriptor(RadioGroup providerFamilyGroup) {
        View checkedView = providerFamilyGroup.findViewById(providerFamilyGroup.getCheckedRadioButtonId());
        Object tag = checkedView == null ? null : checkedView.getTag();
        return tag instanceof ProviderDescriptor ? (ProviderDescriptor) tag : null;
    }

    private MaterialRadioButton createProviderChoice(ProviderDescriptor descriptor) {
        MaterialRadioButton radioButton = new MaterialRadioButton(activity);
        radioButton.setId(View.generateViewId());
        radioButton.setTag(descriptor);
        radioButton.setText(descriptor.resolveLabel(activity));
        radioButton.setLayoutParams(new RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.MATCH_PARENT,
                RadioGroup.LayoutParams.WRAP_CONTENT
        ));
        radioButton.setMinHeight(dp(52));
        radioButton.setPaddingRelative(dp(2), dp(8), dp(2), dp(8));
        radioButton.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        return radioButton;
    }

    private Chip createProviderChip(SiteProfile site) {
        return SelectionChipFactory.createSelectableChip(
                activity,
                site.getId(),
                site.getDisplayName()
        );
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
