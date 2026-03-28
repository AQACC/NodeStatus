plugins {
    alias(libs.plugins.android.application)
}

fun optionalTextConfig(gradleKey: String, envKey: String): String? {
    return providers.gradleProperty(gradleKey).orNull
        ?: providers.environmentVariable(envKey).orNull
}

fun intConfig(gradleKey: String, envKey: String, defaultValue: Int): Int {
    val rawValue = optionalTextConfig(gradleKey, envKey) ?: return defaultValue
    return rawValue.toIntOrNull()
        ?: error("$gradleKey / $envKey must be an integer, got '$rawValue'")
}

val releaseVersionName = optionalTextConfig("nodestatusVersionName", "NODESTATUS_VERSION_NAME")
    ?.removePrefix("v")
    ?: "1.0"
val releaseVersionCode = intConfig("nodestatusVersionCode", "NODESTATUS_VERSION_CODE", 1)

android {
    namespace = "com.aqa.cc.nodestatus"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.aqa.cc.nodestatus"
        minSdk = 34
        targetSdk = 36
        versionCode = releaseVersionCode
        versionName = releaseVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":adapter:virtfusion"))
    implementation(project(":core:storage"))
    implementation(project(":core:widget"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.work.runtime)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
