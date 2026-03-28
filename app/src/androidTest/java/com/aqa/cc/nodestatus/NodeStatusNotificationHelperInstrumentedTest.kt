package com.aqa.cc.nodestatus

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aqa.cc.nodestatus.core.model.Freshness
import com.aqa.cc.nodestatus.core.model.Metric
import com.aqa.cc.nodestatus.core.model.MetricValue
import com.aqa.cc.nodestatus.core.model.MetricValueType
import com.aqa.cc.nodestatus.core.model.ProviderFamily
import com.aqa.cc.nodestatus.core.model.ResourceKind
import com.aqa.cc.nodestatus.core.model.ResourceSnapshot
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class NodeStatusNotificationHelperInstrumentedTest {
    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        notificationManager = context.getSystemService(NotificationManager::class.java)
        clearState()
    }

    @After
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.dropShellPermissionIdentity()
        clearState()
    }

    @Test
    fun low_traffic_notification_creates_channel_and_marks_resource_as_below_threshold() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .adoptShellPermissionIdentity(Manifest.permission.POST_NOTIFICATIONS)

        val config = ProviderSessionConfig(
            "https://panel.example.com",
            "",
            "",
            "",
            "agent/1.0",
            false,
            true,
            20,
        )

        NodeStatusNotificationHelper.maybeNotifyLowTraffic(context, config, listOf(snapshot()))

        val channel = notificationManager.getNotificationChannel("nodestatus_alerts")
        val belowThreshold = context.getSharedPreferences("nodestatus_alert_state", Context.MODE_PRIVATE)
            .getBoolean("site-a::server-a", false)
        val activeIds = notificationManager.activeNotifications.map { it.id }

        assertNotNull(channel)
        assertTrue(belowThreshold)
        assertTrue(activeIds.contains("site-a::server-a".hashCode()))
    }

    private fun snapshot(): ResourceSnapshot {
        val instant = Instant.parse("2026-03-25T10:00:00Z")
        return ResourceSnapshot(
            "server-a",
            "Alpha",
            ProviderFamily.VIRT_FUSION,
            ResourceKind.VIRTUAL_MACHINE,
            instant,
            listOf(
                metric("quota.traffic_bytes", "Traffic quota", "1000", instant),
                metric("usage.traffic_remaining_bytes", "Traffic remaining", "100", instant),
            ),
            "site-a",
            "Alpha site",
        )
    }

    private fun metric(key: String, label: String, rawValue: String, collectedAt: Instant): Metric =
        Metric(
            key = key,
            label = label,
            value = MetricValue(rawValue, MetricValueType.INTEGER),
            unit = "bytes",
            supported = true,
            freshness = Freshness.CURRENT,
            source = "test",
            collectedAt = collectedAt,
        )

    private fun clearState() {
        notificationManager.cancelAll()
        context.getSharedPreferences("nodestatus_alert_state", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}
