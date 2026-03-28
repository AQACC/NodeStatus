package com.aqa.cc.nodestatus

import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class NodeStatusWidgetProviderInstrumentedTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        clearState()
    }

    @After
    fun tearDown() {
        clearState()
    }

    @Test
    fun widget_manual_refresh_queues_status_and_work() {
        ProviderSessionConfigStore(context).save(
            ProviderSessionConfig(
                "https://127.0.0.1:9",
                "",
                "cookie=value",
                "xsrf-token",
                "agent/1.0",
                false,
                false,
                20,
            ),
        )

        val provider = NodeStatusWidgetProvider()
        provider.onReceive(
            context,
            Intent(context, NodeStatusWidgetProvider::class.java).setAction(NodeStatusWidgetProvider.ACTION_MANUAL_REFRESH),
        )

        val status = NodeStatusRefreshStatusStore(context).load()
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(NodeStatusRefreshCoordinator.UNIQUE_ONE_TIME_WORK_NAME)
            .get(10, TimeUnit.SECONDS)

        assertEquals(NodeStatusRefreshCoordinator.SOURCE_WIDGET, status?.getSource())
        assertFalse(workInfos.isEmpty())
    }

    private fun clearState() {
        context.getSharedPreferences(ProviderSessionConfigStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        context.getSharedPreferences("nodestatus_refresh_status", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        NodeStatusRefreshCoordinator.cancelRefreshWork(context)
        WorkManager.getInstance(context).cancelAllWork().result.get(10, TimeUnit.SECONDS)
        WorkManager.getInstance(context).pruneWork()
    }
}
