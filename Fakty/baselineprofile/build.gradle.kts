plugins {
    alias(libs.plugins.android.test)
    // NOTE: org.jetbrains.kotlin.android is intentionally omitted because AGP 9.2 doesn't require it
    // and androidx.baselineprofile is omitted due to AGP 9.2 compatibility issues (TestExtension).
    // We will generate the baseline profile using Macrobenchmark natively.
}

android {
    namespace = "com.topseven.fakty.baselineprofile"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        create("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                file("proguard-rules.pro")
            )
            testProguardFiles(file("proguard-rules.pro"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    targetProjectPath = ":app"
}

dependencies {
    implementation(libs.androidx.test.core)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.espresso.core)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)

    // Dependencies to satisfy R8 in the test module
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation("androidx.startup:startup-runtime:1.2.0")
    implementation("androidx.arch.core:core-runtime:2.2.0")
}
