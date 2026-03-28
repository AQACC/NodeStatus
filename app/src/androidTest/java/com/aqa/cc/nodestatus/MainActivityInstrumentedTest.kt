package com.aqa.cc.nodestatus

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aqa.cc.nodestatus.core.model.Freshness
import com.aqa.cc.nodestatus.core.model.Metric
import com.aqa.cc.nodestatus.core.model.MetricValue
import com.aqa.cc.nodestatus.core.model.MetricValueType
import com.aqa.cc.nodestatus.core.model.ProviderFamily
import com.aqa.cc.nodestatus.core.model.ResourceKind
import com.aqa.cc.nodestatus.core.model.ResourceSnapshot
import com.aqa.cc.nodestatus.core.storage.FileSnapshotHistoryStore
import com.aqa.cc.nodestatus.core.storage.FileSnapshotStore
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentedTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        clearAppState()
        seedSnapshots()
    }

    @After
    fun tearDown() {
        clearAppState()
    }

    @Test
    fun dashboard_renders_server_chips_and_settings_button_switches_tabs() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val activity = instrumentation.startActivitySync(intent)
        instrumentation.waitForIdleSync()

        val chipGroup = activity.findViewById<ChipGroup>(R.id.serverChipGroup)
        val titleView = activity.findViewById<TextView>(R.id.snapshotStatusValue)
        val openSettingsButton = activity.findViewById<View>(R.id.openSettingsButton)
        val dashboardSection = activity.findViewById<View>(R.id.dashboardSection)
        val settingsSection = activity.findViewById<View>(R.id.settingsSection)

        assertEquals(2, chipGroup.childCount)
        assertEquals("Beta", titleView.text.toString())

        val alphaChip = (0 until chipGroup.childCount)
            .map { chipGroup.getChildAt(it) as Chip }
            .first { it.text.toString() == "Alpha" }

        instrumentation.runOnMainSync { chipGroup.check(alphaChip.id) }
        instrumentation.waitForIdleSync()
        assertEquals("Alpha", titleView.text.toString())

        instrumentation.runOnMainSync { openSettingsButton.performClick() }
        instrumentation.waitForIdleSync()
        assertTrue(settingsSection.visibility == View.VISIBLE)
        assertTrue(dashboardSection.visibility == View.GONE)

        activity.finish()
    }

    private fun seedSnapshots() {
        val snapshots = listOf(
            snapshot("server-alpha", "Alpha", "2026-03-25T10:00:00Z", "2147483648"),
            snapshot("server-beta", "Beta", "2026-03-25T10:05:00Z", "3221225472"),
        )
        FileSnapshotStore(AppSnapshotFiles.latestSnapshotPath(context)).saveAll(snapshots)
        FileSnapshotHistoryStore(AppSnapshotFiles.historySnapshotPath(context), 240).appendAll(snapshots)
    }

    private fun snapshot(
        resourceId: String,
        displayName: String,
        collectedAt: String,
        memoryUsedBytes: String,
    ): ResourceSnapshot {
        val instant = Instant.parse(collectedAt)
        return ResourceSnapshot(
            resourceId = resourceId,
            displayName = displayName,
            providerFamily = ProviderFamily.VIRT_FUSION,
            resourceKind = ResourceKind.VIRTUAL_MACHINE,
            collectedAt = instant,
            metrics = listOf(
                metric("state.power", "Power state", "running", MetricValueType.TEXT, null, instant),
                metric("usage.traffic_total_bytes", "Traffic total", "5181442543", MetricValueType.INTEGER, "bytes", instant),
                metric("usage.memory_used_bytes", "Memory used", memoryUsedBytes, MetricValueType.INTEGER, "bytes", instant),
                metric("usage.disk_used_bytes", "Disk used", "6442450944", MetricValueType.INTEGER, "bytes", instant),
            ),
        )
    }

    private fun metric(
        key: String,
        label: String,
        raw: String,
        type: MetricValueType,
        unit: String?,
        collectedAt: Instant,
    ): Metric = Metric(
        key = key,
        label = label,
        value = MetricValue(raw, type),
        unit = unit,
        supported = true,
        freshness = Freshness.CURRENT,
        source = "test",
        collectedAt = collectedAt,
    )

    private fun clearAppState() {
        context.getSharedPreferences("virtfusion_session_config", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("nodestatus_dashboard_state", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("nodestatus_refresh_status", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("nodestatus_alert_state", Context.MODE_PRIVATE).edit().clear().commit()
        AppSnapshotFiles.latestSnapshotPath(context).toFile().delete()
        AppSnapshotFiles.historySnapshotPath(context).toFile().delete()
    }
}
