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
    implementation(project(":adapter:engine"))
    implementation(project(":core:model"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
