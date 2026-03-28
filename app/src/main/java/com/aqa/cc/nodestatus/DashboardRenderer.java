package com.aqa.cc.nodestatus;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aqa.cc.nodestatus.core.widget.WidgetSummary;

import java.util.List;

final class DashboardRenderer {
    private final DashboardContentRenderer contentRenderer;
    private final ServerChipSelector serverChipSelector;
    private final LatestSnapshotRepository latestSnapshotRepository;

    private LatestSnapshotRepository.SiteSnapshots currentSiteSnapshots =
            LatestSnapshotRepository.SiteSnapshots.empty();

    DashboardRenderer(AppCompatActivity activity, DashboardSelectionStore selectionStore) {
        this.contentRenderer = new DashboardContentRenderer(activity);
        this.latestSnapshotRepository = new LatestSnapshotRepository(activity);
        this.serverChipSelector = new ServerChipSelector(
                activity,
                selectionStore,
                activity.findViewById(R.id.serverContextSection),
                activity.findViewById(R.id.serverContextTitle),
                activity.findViewById(R.id.serverEmptyHint),
                activity.findViewById(R.id.serverSingleChip),
                activity.findViewById(R.id.serverSelectorScroll),
                activity.findViewById(R.id.serverChipGroup)
        );
    }

    void bindServerSelector() {
        serverChipSelector.bind(summary -> {
            serverChipSelector.selectSummary(summary);
            renderSelection(summary);
        });
    }

    void renderLatestSummary(String siteId) {
        currentSiteSnapshots = latestSnapshotRepository.loadSiteSnapshots(siteId);
        List<WidgetSummary> currentSummaries = currentSiteSnapshots.getSummaries();
        if (currentSummaries.isEmpty()) {
            serverChipSelector.clear();
            contentRenderer.renderEmptyState();
            return;
        }

        serverChipSelector.render(currentSummaries, siteId);
        WidgetSummary selectedSummary = serverChipSelector.resolveSelectedSummary();
        if (selectedSummary != null) {
            serverChipSelector.selectSummary(selectedSummary);
            renderSelection(selectedSummary);
        }
    }

    void renderBackgroundStatus() {
        contentRenderer.renderBackgroundStatus();
    }

    @Nullable
    WidgetSummary getSelectedSummary() {
        if (currentSiteSnapshots.getSummaries().isEmpty()) {
            return null;
        }
        return serverChipSelector.resolveSelectedSummary();
    }

    private void renderSelection(WidgetSummary summary) {
        contentRenderer.renderSelectedSummary(
                summary,
                currentSiteSnapshots.findSnapshot(summary.getScopedResourceId())
        );
    }
}
