plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
    // org.jetbrains.kotlin.android jest celowo pominięty - AGP 9.x ma wbudowaną obsługę Kotlina.
}

android {
    namespace = "com.topseven.fakty.baselineprofile"
    compileSdk = 36

    defaultConfig {
        minSdk = 28 // BaselineProfileRule wymaga API 28+
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    targetProjectPath = ":app"
}

// Generowanie profilu działa także na emulatorze - inaczej wymagałoby fizycznego urządzenia.
baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
