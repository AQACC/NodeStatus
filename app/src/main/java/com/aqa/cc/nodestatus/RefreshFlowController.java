package com.aqa.cc.nodestatus;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.aqa.cc.nodestatus.core.model.SiteProfile;

import java.util.concurrent.ExecutorService;

final class RefreshFlowController {
    interface Listener {
        void renderDashboard();

        void renderBackgroundStatus();
    }

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    private final AppCompatActivity activity;
    private final ActiveSiteSessionRepository siteSessionRepository;
    private final ProviderDescriptorRegistry providerDescriptorRegistry;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final Listener listener;
    private final RefreshConfigForm configForm;
    private final Button quickRefreshButton;
    private final ActivityResultLauncher<Intent> importSessionLauncher;

    RefreshFlowController(
            AppCompatActivity activity,
            ActiveSiteSessionRepository siteSessionRepository,
            ProviderDescriptorRegistry providerDescriptorRegistry,
            ExecutorService executorService,
            Handler mainHandler,
            Listener listener
    ) {
        this.activity = activity;
        this.siteSessionRepository = siteSessionRepository;
        this.providerDescriptorRegistry = providerDescriptorRegistry;
        this.executorService = executorService;
        this.mainHandler = mainHandler;
        this.listener = listener;
        this.configForm = new RefreshConfigForm(activity, providerDescriptorRegistry);
        this.quickRefreshButton = activity.findViewById(R.id.quickRefreshButton);
        this.importSessionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleImportSessionResult(result.getResultCode(), result.getData())
        );
    }

    SiteProfile loadActiveSiteIntoViews() {
        ActiveSiteSessionRepository.ActiveSiteSession activeSession = siteSessionRepository.loadActiveSession();
        configForm.load(activeSession.getSiteProfile(), activeSession.getConfig(), BuildConfig.DEBUG);
        bindProviderActionButtons();
        return activeSession.getSiteProfile();
    }

    SiteProfile saveDraft() {
        SiteProfile activeSite = siteSessionRepository.loadActiveSite();
        RefreshConfigForm.Draft draft = configForm.read(
                activeSite.getId(),
                activeSite.getProviderFamily(),
                BuildConfig.DEBUG
        );
        return siteSessionRepository.saveActiveDraft(draft);
    }

    void showIdleStatus() {
        TextView refreshStatusValue = findRefreshStatusValue();
        if (refreshStatusValue != null) {
            refreshStatusValue.setText(R.string.refresh_status_idle);
        }
    }

    void triggerManualRefresh() {
        SiteProfile siteProfile = saveDraft();
        ProviderSessionConfig config = siteSessionRepository.loadActiveConfig();
        NodeStatusRefreshCoordinator.reconcileRefreshWork(activity, config);

        String validationMessage = resolveRefreshValidationMessage(siteProfile, config);
        if (validationMessage != null) {
            listener.renderBackgroundStatus();
            TextView refreshStatusValue = findRefreshStatusValue();
            if (refreshStatusValue != null) {
                refreshStatusValue.setText(validationMessage);
            }
            return;
        }

        maybeRequestNotificationPermission(config);
        setRefreshControlsEnabled(false);
        TextView refreshStatusValue = findRefreshStatusValue();
        if (refreshStatusValue != null) {
            refreshStatusValue.setText(R.string.refresh_status_running);
        }

        executorService.execute(() -> {
            try {
                NodeStatusRefreshCoordinator.RefreshOutcome outcome = NodeStatusRefreshCoordinator.refresh(
                        activity.getApplicationContext(),
                        siteProfile,
                        config,
                        NodeStatusRefreshCoordinator.SOURCE_MANUAL
                );
                mainHandler.post(() -> handleRefreshSuccess(outcome));
            } catch (Throwable throwable) {
                mainHandler.post(() -> handleRefreshFailure(throwable));
            }
        });
    }

    private void handleRefreshSuccess(NodeStatusRefreshCoordinator.RefreshOutcome outcome) {
        listener.renderDashboard();
        listener.renderBackgroundStatus();
        TextView refreshStatusValue = findRefreshStatusValue();
        if (refreshStatusValue != null) {
            refreshStatusValue.setText(buildSuccessMessage(outcome));
        }
        setRefreshControlsEnabled(true);
    }

    private void handleRefreshFailure(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            message = throwable.getClass().getSimpleName();
        }
        new NodeStatusRefreshStatusStore(activity.getApplicationContext()).saveFailure(
                NodeStatusRefreshCoordinator.SOURCE_MANUAL,
                message,
                java.time.Instant.now().toString(),
                RefreshDebugLogComposer.formatThrowable(throwable)
        );
        NodeStatusWidgetProvider.refreshAll(activity.getApplicationContext());
        listener.renderBackgroundStatus();
        TextView refreshStatusValue = findRefreshStatusValue();
        if (refreshStatusValue != null) {
            refreshStatusValue.setText(activity.getString(R.string.refresh_status_failure, message));
        }
        setRefreshControlsEnabled(true);
    }

    private String resolveRefreshValidationMessage(SiteProfile siteProfile, ProviderSessionConfig config) {
        ProviderDescriptor descriptor = providerDescriptorRegistry.getDescriptor(siteProfile.getProviderFamily());
        if (!descriptor.supportsRefresh()) {
            return activity.getString(
                    R.string.refresh_status_provider_not_supported,
                    descriptor.resolveLabel(activity)
            );
        }
        if (!config.hasAnyAuth()) {
            return activity.getString(R.string.refresh_status_missing_auth);
        }
        if (config.hasRunnableAuth()) {
            return null;
        }

        boolean sessionMissingBaseUrl = config.hasCompatibilitySession() && config.getBaseUrl().isBlank();
        if (sessionMissingBaseUrl) {
            return activity.getString(R.string.refresh_status_missing_base_url);
        }
        return activity.getString(R.string.refresh_status_missing_auth);
    }

    private String buildSuccessMessage(NodeStatusRefreshCoordinator.RefreshOutcome outcome) {
        return activity.getResources().getQuantityString(
                R.plurals.refresh_status_success,
                outcome.getSnapshotCount(),
                outcome.getSnapshotCount()
        );
    }

    private void setRefreshControlsEnabled(boolean enabled) {
        quickRefreshButton.setEnabled(enabled);
        Button refreshButton = findRefreshButton();
        if (refreshButton != null) {
            refreshButton.setEnabled(enabled);
        }
    }

    private void maybeRequestNotificationPermission(ProviderSessionConfig config) {
        if (!config.isNotificationsEnabled()) {
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                NOTIFICATION_PERMISSION_REQUEST_CODE
        );
    }

    private void bindProviderActionButtons() {
        Button refreshButton = findRefreshButton();
        if (refreshButton != null) {
            refreshButton.setOnClickListener(view -> triggerManualRefresh());
        }
        Button importSessionButton = findImportSessionButton();
        if (importSessionButton != null) {
            importSessionButton.setOnClickListener(view -> launchImportSession());
        }
        Button copyDebugLogButton = findCopyDebugLogButton();
        if (copyDebugLogButton != null) {
            copyDebugLogButton.setOnClickListener(view -> copyDebugLogToClipboard());
        }
    }

    private void launchImportSession() {
        saveDraft();
        ProviderSessionConfig config = siteSessionRepository.loadActiveConfig();
        if (config.getBaseUrl().isBlank()) {
            TextView refreshStatusValue = findRefreshStatusValue();
            if (refreshStatusValue != null) {
                refreshStatusValue.setText(R.string.session_import_requires_base_url);
            }
            return;
        }
        importSessionLauncher.launch(VirtFusionWebLoginActivity.createIntent(activity, config));
    }

    private void handleImportSessionResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            String statusMessage = VirtFusionWebLoginActivity.extractStatusMessage(data);
            if (statusMessage != null && !statusMessage.isBlank()) {
                TextView refreshStatusValue = findRefreshStatusValue();
                if (refreshStatusValue != null) {
                    refreshStatusValue.setText(statusMessage);
                }
            }
            return;
        }

        String cookieHeader = VirtFusionWebLoginActivity.extractCookieHeader(data);
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return;
        }
        ProviderSessionConfig updatedConfig = siteSessionRepository.saveActiveSessionAuth(
                cookieHeader,
                VirtFusionWebLoginActivity.extractXsrfHeader(data)
        );
        NodeStatusRefreshCoordinator.reconcileRefreshWork(activity, updatedConfig);
        listener.renderDashboard();
        listener.renderBackgroundStatus();
        TextView refreshStatusValue = findRefreshStatusValue();
        if (refreshStatusValue != null) {
            refreshStatusValue.setText(R.string.session_import_success);
        }
    }

    private void copyDebugLogToClipboard() {
        SiteProfile siteProfile = siteSessionRepository.loadActiveSite();
        ProviderSessionConfig config = siteSessionRepository.loadActiveConfig();
        TextView refreshStatusValue = findRefreshStatusValue();
        CharSequence currentStatus = refreshStatusValue == null ? null : refreshStatusValue.getText();
        String report = RefreshDebugLogComposer.buildReport(
                activity.getApplicationContext(),
                siteProfile,
                config,
                currentStatus
        );

        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("NodeStatus refresh debug log", report));
        Toast.makeText(activity, R.string.refresh_debug_log_copied, Toast.LENGTH_SHORT).show();
    }

    private Button findRefreshButton() {
        return activity.findViewById(R.id.refreshButton);
    }

    private Button findCopyDebugLogButton() {
        return activity.findViewById(R.id.copyDebugLogButton);
    }

    private Button findImportSessionButton() {
        return activity.findViewById(R.id.importSessionButton);
    }

    private TextView findRefreshStatusValue() {
        return activity.findViewById(R.id.refreshStatusValue);
    }
}
