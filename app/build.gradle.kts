plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "id.kostgoldyz.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "id.kostgoldyz.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "1.3"
    }

    // Kunci tanda tangan tetap, supaya APK baru bisa dipasang menimpa yang lama.
    signingConfigs {
        create("kost") {
            storeFile = file("kostgoldyz.jks")
            storePassword = "kostgoldyz"
            keyAlias = "kostgoldyz"
            keyPassword = "kostgoldyz"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("kost")
        }
        release {
            signingConfig = signingConfigs.getByName("kost")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.webkit:webkit:1.12.1")
}
