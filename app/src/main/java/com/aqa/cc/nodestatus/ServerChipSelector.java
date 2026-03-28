package com.aqa.cc.nodestatus;

import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aqa.cc.nodestatus.core.widget.WidgetSummary;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.Collections;
import java.util.List;

final class ServerChipSelector {
    interface Listener {
        void onSummarySelected(WidgetSummary summary);
    }

    private final AppCompatActivity activity;
    private final DashboardSelectionStore selectionStore;
    private final View sectionView;
    private final TextView titleView;
    private final TextView emptyHintView;
    private final Chip singleChip;
    private final HorizontalScrollView chipScrollView;
    private final ChipGroup chipGroup;

    private List<WidgetSummary> currentSummaries = Collections.emptyList();
    private String selectedResourceId;
    private String currentSiteId;
    private boolean suppressSelectionEvents;

    ServerChipSelector(
            AppCompatActivity activity,
            DashboardSelectionStore selectionStore,
            View sectionView,
            TextView titleView,
            TextView emptyHintView,
            Chip singleChip,
            HorizontalScrollView chipScrollView,
            ChipGroup chipGroup
    ) {
        this.activity = activity;
        this.selectionStore = selectionStore;
        this.sectionView = sectionView;
        this.titleView = titleView;
        this.emptyHintView = emptyHintView;
        this.singleChip = singleChip;
        this.chipScrollView = chipScrollView;
        this.chipGroup = chipGroup;
    }

    void bind(Listener listener) {
        chipGroup.setOnCheckedStateChangeListener(null);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (suppressSelectionEvents || checkedIds.isEmpty()) {
                return;
            }
            WidgetSummary selectedSummary = resolveSummaryForCheckedChip(
                    group.findViewById(checkedIds.get(0))
            );
            if (selectedSummary == null) {
                return;
            }
            selectSummary(selectedSummary);
            listener.onSummarySelected(selectedSummary);
        });
    }

    void render(List<WidgetSummary> summaries, String siteId) {
        currentSiteId = siteId;
        selectedResourceId = selectionStore.loadSelectedResourceId(siteId);
        currentSummaries = summaries;
        suppressSelectionEvents = true;
        chipGroup.removeAllViews();

        if (summaries.isEmpty()) {
            sectionView.setVisibility(View.VISIBLE);
            titleView.setText(R.string.server_switcher_title);
            emptyHintView.setVisibility(View.VISIBLE);
            singleChip.setVisibility(View.GONE);
            chipScrollView.setVisibility(View.GONE);
            suppressSelectionEvents = false;
            return;
        }

        sectionView.setVisibility(View.VISIBLE);
        emptyHintView.setVisibility(View.GONE);

        if (summaries.size() == 1) {
            WidgetSummary onlySummary = summaries.get(0);
            titleView.setText(R.string.server_switcher_title);
            singleChip.setText(onlySummary.getDisplayName());
            singleChip.setVisibility(View.VISIBLE);
            chipScrollView.setVisibility(View.GONE);
            selectSummary(onlySummary);
            suppressSelectionEvents = false;
            return;
        }

        titleView.setText(R.string.server_switcher_title_multi);
        singleChip.setVisibility(View.GONE);
        chipScrollView.setVisibility(View.VISIBLE);

        int selectedChipId = View.NO_ID;
        for (WidgetSummary summary : summaries) {
            Chip chip = createChip(summary);
            chip.setId(View.generateViewId());
            chipGroup.addView(chip);
            if (summary.getScopedResourceId().equals(selectedResourceId)) {
                selectedChipId = chip.getId();
            }
        }

        if (selectedChipId == View.NO_ID && chipGroup.getChildCount() > 0) {
            selectedChipId = chipGroup.getChildAt(0).getId();
        }
        if (selectedChipId != View.NO_ID) {
            chipGroup.check(selectedChipId);
        }
        suppressSelectionEvents = false;
    }

    void selectSummary(WidgetSummary summary) {
        selectedResourceId = summary.getScopedResourceId();
        selectionStore.saveSelectedResourceId(currentSiteId, selectedResourceId);
    }

    void clear() {
        chipGroup.removeAllViews();
        sectionView.setVisibility(View.VISIBLE);
        titleView.setText(R.string.server_switcher_title);
        emptyHintView.setVisibility(View.VISIBLE);
        singleChip.setVisibility(View.GONE);
        chipScrollView.setVisibility(View.GONE);
    }

    @Nullable
    WidgetSummary resolveSelectedSummary() {
        if (selectedResourceId != null) {
            for (WidgetSummary summary : currentSummaries) {
                if (selectedResourceId.equals(summary.getScopedResourceId())) {
                    return summary;
                }
            }
        }
        if (currentSummaries.isEmpty()) {
            return null;
        }
        WidgetSummary summary = currentSummaries.get(0);
        selectSummary(summary);
        return summary;
    }

    @Nullable
    private WidgetSummary resolveSummaryForCheckedChip(
            @Nullable View checkedView
    ) {
        if (!(checkedView instanceof Chip)) {
            return null;
        }
        Object tag = checkedView.getTag();
        if (!(tag instanceof String)) {
            return null;
        }
        String resourceId = (String) tag;
        for (WidgetSummary summary : currentSummaries) {
            if (summary.getScopedResourceId().equals(resourceId)) {
                return summary;
            }
        }
        return null;
    }

    private Chip createChip(WidgetSummary summary) {
        return SelectionChipFactory.createSelectableChip(
                activity,
                summary.getScopedResourceId(),
                summary.getDisplayName()
        );
    }
}
