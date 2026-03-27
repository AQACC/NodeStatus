package com.aqa.cc.nodestatus;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aqa.cc.nodestatus.core.model.ProviderFamily;
import com.aqa.cc.nodestatus.core.model.SiteProfile;
import com.aqa.cc.nodestatus.core.widget.WidgetSummary;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ActiveSiteSessionRepository siteSessionRepository;
    private ProviderDescriptorRegistry providerDescriptorRegistry;
    private TextView foundationScopeValue;
    private Button openSettingsButton;
    private LinearLayout dashboardSection;
    private LinearLayout settingsSection;
    private MaterialButtonToggleGroup topTabGroup;
    private DashboardRenderer dashboardRenderer;
    private RefreshFlowController refreshFlowController;
    private LanguageButtonController languageButtonController;
    private ProviderSiteController providerSiteController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        siteSessionRepository = new ActiveSiteSessionRepository(this);
        providerDescriptorRegistry = new AppProviderDescriptorRegistry();
        DashboardSelectionStore dashboardSelectionStore = new DashboardSelectionStore(this);

        bindRootViews();

        dashboardRenderer = new DashboardRenderer(this, dashboardSelectionStore);
        refreshFlowController = new RefreshFlowController(
                this,
                siteSessionRepository,
                providerDescriptorRegistry,
                executorService,
                mainHandler,
                new RefreshFlowController.Listener() {
                    @Override
                    public void renderDashboard() {
                        renderActiveSite();
                    }

                    @Override
                    public void renderBackgroundStatus() {
                        dashboardRenderer.renderBackgroundStatus();
                    }
                }
        );
        languageButtonController = new LanguageButtonController(
                this,
                new AppLocaleStore(this),
                () -> refreshFlowController.saveDraft()
        );
        providerSiteController = new ProviderSiteController(
                this,
                siteSessionRepository,
                new ProviderSiteController.Listener() {
                    @Override
                    public void beforeSiteMutation() {
                        refreshFlowController.saveDraft();
                    }

                    @Override
                    public void onSiteChanged() {
                        renderActiveSite();
                    }

                    @Override
                    public void onSiteCreated() {
                        renderActiveSite();
                        showSettingsTab();
                    }
                },
                providerDescriptorRegistry
        );

        setupTabs();
        setupActions();
        languageButtonController.bind();
        providerSiteController.bind();
        dashboardRenderer.bindServerSelector();
        renderActiveSite();
        refreshFlowController.showIdleStatus();
        NodeStatusRefreshCoordinator.reconcileRefreshWork(this, siteSessionRepository.loadActiveConfig());
    }

    @Override
    protected void onPause() {
        refreshFlowController.saveDraft();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        executorService.shutdownNow();
        super.onDestroy();
    }

    private void bindRootViews() {
        topTabGroup = findViewById(R.id.topTabGroup);
        foundationScopeValue = findViewById(R.id.foundationScopeValue);
        openSettingsButton = findViewById(R.id.openSettingsButton);
        dashboardSection = findViewById(R.id.dashboardSection);
        settingsSection = findViewById(R.id.settingsSection);
    }

    private void setupTabs() {
        topTabGroup.check(R.id.dashboardTabButton);
        setDashboardVisible(true);
        topTabGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            setDashboardVisible(checkedId == R.id.dashboardTabButton);
        });
    }

    private void setupActions() {
        findViewById(R.id.quickRefreshButton).setOnClickListener(view -> triggerManualRefresh());
        openSettingsButton.setOnClickListener(view -> showSettingsTab());
        findViewById(R.id.openHistoryDetailButton).setOnClickListener(view -> openHistoryDetail());
    }

    private void triggerManualRefresh() {
        refreshFlowController.triggerManualRefresh();
        providerSiteController.render();
    }

    private void renderActiveSite() {
        SiteProfile activeSite = refreshFlowController.loadActiveSiteIntoViews();
        foundationScopeValue.setText(
                providerDescriptorRegistry.getDescriptor(activeSite.getProviderFamily()).resolveLabel(this)
        );
        languageButtonController.render();
        providerSiteController.render();
        dashboardRenderer.renderLatestSummary(activeSite.getId());
        dashboardRenderer.renderBackgroundStatus();
    }

    private void setDashboardVisible(boolean dashboardVisible) {
        dashboardSection.setVisibility(dashboardVisible ? View.VISIBLE : View.GONE);
        settingsSection.setVisibility(dashboardVisible ? View.GONE : View.VISIBLE);
    }

    private void showSettingsTab() {
        topTabGroup.check(R.id.settingsTabButton);
        setDashboardVisible(false);
    }

    private void openHistoryDetail() {
        WidgetSummary summary = dashboardRenderer.getSelectedSummary();
        if (summary == null) {
            return;
        }

        Intent intent = new Intent(this, HistoryDetailActivity.class);
        intent.putExtra(HistoryDetailActivity.EXTRA_RESOURCE_ID, summary.getScopedResourceId());
        intent.putExtra(HistoryDetailActivity.EXTRA_DISPLAY_NAME, summary.getDisplayName());
        startActivity(intent);
    }
}
