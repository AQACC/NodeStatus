plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    systemProperty("nodestatus.repoRoot", rootDir.absolutePath)
}

dependencies {
    testImplementation(libs.snakeyaml)
    testImplementation(libs.junit)
}
