plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    systemProperty("nodestatus.repoRoot", rootDir.absolutePath)
}

tasks.register<JavaExec>("runLiveFetch") {
    group = "verification"
    description = "Fetches live VirtFusion snapshots using local.auth.properties"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.aqa.cc.nodestatus.adapter.virtfusion.VirtFusionLiveFetchMainKt")
    systemProperty("nodestatus.repoRoot", rootDir.absolutePath)
}

dependencies {
    implementation(project(":adapter:engine"))
    implementation(project(":core:model"))
    implementation(project(":core:storage"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
