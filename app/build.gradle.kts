plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Set by GitHub Actions: build number becomes the app version (build 7 -> version 1.7)
val buildNumber = (System.getenv("BUILD_NUMBER") ?: "1").toInt()
// Set by GitHub Actions from your repository secrets
val keystorePath: String? = System.getenv("KEYSTORE_PATH")

android {
    namespace = "com.plantscout.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.plantscout.app"
        minSdk = 26
        targetSdk = 34
        versionCode = buildNumber
        versionName = "1.$buildNumber"
    }

    signingConfigs {
        create("release") {
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
