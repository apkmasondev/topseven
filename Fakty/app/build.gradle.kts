import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.androidx.baselineprofile)
}

// Dane keystore trzymamy poza repozytorium (keystore.properties jest w .gitignore).
// Gdy pliku brak - build release nadal działa, tylko produkuje APK niepodpisany,
// więc projekt da się zbudować bez dostępu do klucza.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
  if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { load(it) }
  }
}
val hasReleaseSigning = keystoreProperties.getProperty("storeFile")?.let {
  rootProject.file(it).exists()
} == true

android {
    namespace = "com.topseven.fakty"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.topseven.fakty"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "2.1.4"
        // Bez tego `connectedAndroidTest` nie miał czym uruchomić testów z androidTest/ -
        // zestaw testów instrumentalnych istniał, ale nie dawało się go wykonać.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

  signingConfigs {
    if (hasReleaseSigning) {
      create("release") {
        storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
        storePassword = keystoreProperties.getProperty("storePassword")
        keyAlias = keystoreProperties.getProperty("keyAlias")
        keyPassword = keystoreProperties.getProperty("keyPassword")
        enableV1Signing = false
        enableV2Signing = true
        enableV3Signing = true
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
      if (hasReleaseSigning) {
        signingConfig = signingConfigs.getByName("release")
      }
    }
  }

  compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }

    testOptions {
      unitTests {
        // Warstwa danych loguje przez android.util.Log, który w testach JVM domyślnie
        // rzuca wyjątkiem "not mocked".
        isReturnDefaultValues = true
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  implementation("androidx.compose.material:material-icons-core")
  implementation("androidx.compose.material:material-icons-extended")
  implementation(libs.androidx.navigation.compose)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.coil.compose)

  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Profile installer to load baseline profiles automatically
  implementation(libs.androidx.profileinstaller)

  // Wpina wygenerowany profil do APK. Bez tej zależności moduł :baselineprofile
  // produkował profil, którego nikt nie konsumował - profileinstaller nie miał czego wczytać.
  baselineProfile(project(":baselineprofile"))
}
