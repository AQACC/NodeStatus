package com.aqa.cc.nodestatus

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class NodeStatusRefreshCoordinatorInstrumentedTest {
    private lateinit var context: Context
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        workManager = WorkManager.getInstance(context)
        clearWork()
    }

    @After
    fun tearDown() {
        clearWork()
    }

    @Test
    fun reconcile_refresh_work_enqueues_periodic_work_for_runnable_auth() {
        NodeStatusRefreshCoordinator.reconcileRefreshWork(context, runnableSessionConfig())

        val infos = workManager
            .getWorkInfosForUniqueWork(NodeStatusRefreshCoordinator.UNIQUE_WORK_NAME)
            .get(10, TimeUnit.SECONDS)

        assertFalse(infos.isEmpty())
        assertTrue(infos.any { !it.state.isFinished })
    }

    @Test
    fun enqueue_immediate_refresh_adds_one_time_work() {
        NodeStatusRefreshCoordinator.enqueueImmediateRefresh(context, runnableSessionConfig())

        val infos = workManager
            .getWorkInfosForUniqueWork(NodeStatusRefreshCoordinator.UNIQUE_ONE_TIME_WORK_NAME)
            .get(10, TimeUnit.SECONDS)

        assertFalse(infos.isEmpty())
    }

    @Test
    fun reconcile_refresh_work_cancels_when_auth_is_not_runnable() {
        NodeStatusRefreshCoordinator.reconcileRefreshWork(context, runnableSessionConfig())
        NodeStatusRefreshCoordinator.reconcileRefreshWork(context, emptyConfig())

        Thread.sleep(300)

        val infos = workManager
            .getWorkInfosForUniqueWork(NodeStatusRefreshCoordinator.UNIQUE_WORK_NAME)
            .get(10, TimeUnit.SECONDS)

        assertTrue(infos.isEmpty() || infos.all { it.state == WorkInfo.State.CANCELLED || it.state.isFinished })
    }

    private fun clearWork() {
        NodeStatusRefreshCoordinator.cancelRefreshWork(context)
        workManager.cancelAllWork().result.get(10, TimeUnit.SECONDS)
        workManager.pruneWork()
    }

    private fun runnableSessionConfig() = ProviderSessionConfig(
        "https://127.0.0.1:9",
        "",
        "cookie=value",
        "xsrf-token",
        "agent/1.0",
        false,
        false,
        20,
    )

    private fun emptyConfig() = ProviderSessionConfig(
        "",
        "",
        "",
        "",
        "agent/1.0",
        false,
        false,
        20,
    )
}
