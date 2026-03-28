package com.aqa.cc.nodestatus

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProviderSessionConfigStoreInstrumentedTest {
    private lateinit var context: Context
    private lateinit var store: ProviderSessionConfigStore

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        store = ProviderSessionConfigStore(context)
        clearPrefs()
    }

    @After
    fun tearDown() {
        clearPrefs()
    }

    @Test
    fun saveEncryptsSensitiveFieldsAndLoadReturnsOriginalValues() {
        val config = ProviderSessionConfig(
            "https://panel.example.com",
            "",
            "cookie=value",
            "xsrf-value",
            "agent/1.0",
            false,
            true,
            25,
        )

        store.save(config)

        val loaded = store.load()
        assertEquals(config.getBaseUrl(), loaded.getBaseUrl())
        assertEquals(config.getCookieHeader(), loaded.getCookieHeader())
        assertEquals(config.getXsrfHeader(), loaded.getXsrfHeader())
        assertEquals(config.getUserAgent(), loaded.getUserAgent())
        assertTrue(loaded.isNotificationsEnabled())
        assertEquals(25, loaded.getLowTrafficThresholdPercent())

        assertStoredEncrypted(ProviderSessionConfigStore.KEY_BASE_URL, config.getBaseUrl())
        assertStoredEncrypted(ProviderSessionConfigStore.KEY_COOKIE_HEADER, config.getCookieHeader())
        assertStoredEncrypted(ProviderSessionConfigStore.KEY_XSRF_HEADER, config.getXsrfHeader())
        assertStoredEncrypted(ProviderSessionConfigStore.KEY_USER_AGENT, config.getUserAgent())
    }

    @Test
    fun loadMigratesLegacyPlaintextValuesToEncryptedStorage() {
        val prefs = prefs()
        prefs.edit()
            .putString(ProviderSessionConfigStore.KEY_BASE_URL, "https://legacy-panel.example.com")
            .putString(ProviderSessionConfigStore.LEGACY_KEY_API_BASE_URL, "https://legacy-panel.example.com/api/v1")
            .putString(ProviderSessionConfigStore.LEGACY_KEY_API_TOKEN, "legacy-token")
            .putString(ProviderSessionConfigStore.KEY_COOKIE_HEADER, "legacy-cookie")
            .putString(ProviderSessionConfigStore.KEY_XSRF_HEADER, "legacy-xsrf")
            .putString(ProviderSessionConfigStore.KEY_USER_AGENT, "legacy-agent")
            .putBoolean(ProviderSessionConfigStore.KEY_NOTIFICATIONS_ENABLED, true)
            .commit()

        val loaded = store.load()

        assertEquals("https://legacy-panel.example.com", loaded.getBaseUrl())
        assertEquals("legacy-cookie", loaded.getCookieHeader())
        assertEquals("legacy-xsrf", loaded.getXsrfHeader())
        assertEquals("legacy-agent", loaded.getUserAgent())
        assertTrue(loaded.isNotificationsEnabled())

        assertStoredEncrypted(ProviderSessionConfigStore.KEY_COOKIE_HEADER, "legacy-cookie")
        assertStoredEncrypted(ProviderSessionConfigStore.KEY_XSRF_HEADER, "legacy-xsrf")
        assertNull(prefs.getString(ProviderSessionConfigStore.LEGACY_KEY_API_BASE_URL, null))
        assertNull(prefs.getString(ProviderSessionConfigStore.LEGACY_KEY_API_TOKEN, null))
    }

    private fun assertStoredEncrypted(key: String, plaintext: String) {
        val stored = prefs().getString(key, null)
        assertNotNull(stored)
        assertNotEquals(plaintext, stored)
        assertTrue(stored!!.startsWith("enc:v1:"))
        assertFalse(stored.contains(plaintext))
    }

    private fun prefs() =
        context.getSharedPreferences(ProviderSessionConfigStore.PREFS_NAME, Context.MODE_PRIVATE)

    private fun clearPrefs() {
        prefs().edit().clear().commit()
    }
}
