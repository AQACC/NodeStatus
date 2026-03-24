package com.aqa.cc.nodestatus.adapter.testkit

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.readText

class RulePackValidationTest {
    private val yaml = Yaml()

    @Test
    fun every_rule_pack_has_required_files_and_fields() {
        val repoRoot = System.getProperty("nodestatus.repoRoot")
        assertFalse("Missing nodestatus.repoRoot system property", repoRoot.isNullOrBlank())

        val packsRoot = Path.of(repoRoot, "rule-packs", "packs")
        assertTrue("Missing rule-packs directory", packsRoot.exists())
        assertTrue("rule-packs/packs must be a directory", packsRoot.isDirectory())

        val packDirs = Files.list(packsRoot).use { stream ->
            stream.filter(Files::isDirectory).toList()
        }
        assertFalse("At least one rule pack should exist", packDirs.isEmpty())

        for (packDir in packDirs) {
            validatePack(packDir)
        }
    }

    private fun validatePack(packDir: Path) {
        val manifestPath = packDir.resolve("manifest.yaml")
        val capabilitiesPath = packDir.resolve("capabilities.yaml")
        val fixturesDir = packDir.resolve("fixtures")
        val fixturesReadme = fixturesDir.resolve("README.md")

        assertTrue("Missing manifest: $manifestPath", manifestPath.exists())
        assertTrue("Missing capabilities file: $capabilitiesPath", capabilitiesPath.exists())
        assertTrue("Missing fixtures directory: $fixturesDir", fixturesDir.exists())
        assertTrue("Missing fixture notes: $fixturesReadme", fixturesReadme.exists())

        val manifest = loadMap(manifestPath)
        val capabilities = loadMap(capabilitiesPath)

        val packId = requiredString(manifest, "id", manifestPath)
        requiredString(manifest, "family", manifestPath)
        requiredString(manifest, "displayName", manifestPath)
        requiredString(manifest, "version", manifestPath)

        val supportedAuth = manifest["supportedAuth"]
        assertTrue("supportedAuth must be a non-empty list in $manifestPath", supportedAuth is List<*> && supportedAuth.isNotEmpty())

        val declaredCapabilities = manifest["capabilities"]
        assertTrue(
            "capabilities must be a non-empty list in $manifestPath",
            declaredCapabilities is List<*> && declaredCapabilities.isNotEmpty(),
        )

        val fixtureDirectory = requiredString(manifest, "fixtureDirectory", manifestPath)
        assertTrue(
            "fixtureDirectory should point to this pack's fixture folder in $manifestPath",
            fixtureDirectory.endsWith("${packDir.name}/fixtures"),
        )
        assertTrue(
            "Pack id should match directory name for $manifestPath",
            packId == packDir.name,
        )

        assertTrue("capabilities.yaml must declare at least one capability in $capabilitiesPath", capabilities.isNotEmpty())
        for ((capability, metadata) in capabilities) {
            assertTrue("Capability key must be non-blank in $capabilitiesPath", capability.isNotBlank())
            assertTrue("Capability metadata must be a map for $capabilitiesPath", metadata is Map<*, *>)
            requiredNestedString(metadata as Map<*, *>, "status", capabilitiesPath, capability)
            requiredNestedString(metadata, "note", capabilitiesPath, capability)
        }

        val fixturesText = fixturesReadme.readText()
        assertTrue("Fixture README should mention redaction in $fixturesReadme", fixturesText.contains("redacted", ignoreCase = true))
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadMap(path: Path): Map<String, Any?> {
        val loaded = yaml.load<Any?>(Files.newBufferedReader(path))
        assertNotNull("YAML file is empty: $path", loaded)
        assertTrue("YAML root must be a mapping: $path", loaded is Map<*, *>)
        return loaded as Map<String, Any?>
    }

    private fun requiredString(map: Map<String, Any?>, key: String, path: Path): String {
        val value = map[key]
        assertTrue("Missing or blank '$key' in $path", value is String && value.isNotBlank())
        return value as String
    }

    private fun requiredNestedString(map: Map<*, *>, key: String, path: Path, capability: Any?): String {
        val value = map[key]
        assertTrue("Missing or blank '$key' for capability '$capability' in $path", value is String && value.isNotBlank())
        return value as String
    }
}
