package com.aqa.cc.nodestatus

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NodeStatusRefreshStatusStoreInstrumentedTest {
    private lateinit var context: Context
    private lateinit var store: NodeStatusRefreshStatusStore

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        store = NodeStatusRefreshStatusStore(context)
        clearPrefs()
    }

    @After
    fun tearDown() {
        clearPrefs()
    }

    @Test
    fun loadReturnsNullWhenNoStatusWasSaved() {
        assertNull(store.load())
    }

    @Test
    fun pendingSuccessAndFailureStatesRoundTripOnDevice() {
        store.savePending("widget", "Queued", "2026-03-25T10:00:00Z")
        val pending = store.load()
        assertNotNull(pending)
        assertEquals("widget", pending!!.getSource())
        assertEquals("Queued", pending.getMessage())
        assertTrue(pending.isPending())
        assertFalse(pending.isSuccess())

        store.saveSuccess("worker", "Saved 2 snapshot(s).", "2026-03-25T10:01:00Z", true)
        val success = store.load()
        assertNotNull(success)
        assertEquals("worker", success!!.getSource())
        assertEquals("Saved 2 snapshot(s).", success.getMessage())
        assertTrue(success.isSuccess())
        assertFalse(success.isPending())
        assertTrue(success.isUsedCompatibilitySession())

        store.saveFailure("manual", "boom", "2026-03-25T10:02:00Z")
        val failure = store.load()
        assertNotNull(failure)
        assertEquals("manual", failure!!.getSource())
        assertEquals("boom", failure.getMessage())
        assertFalse(failure.isSuccess())
        assertFalse(failure.isPending())
    }

    private fun clearPrefs() {
        context.getSharedPreferences("nodestatus_refresh_status", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}
