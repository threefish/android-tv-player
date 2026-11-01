plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

android {
    namespace = "com.tvplayer.player"
    compileSdk = 34

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    implementation(project(":core"))

    // Media3 (ExoPlayer)
    implementation("androidx.media3:media3-exoplayer:${rootProject.extra["media3_version"]}")
    implementation("androidx.media3:media3-ui:${rootProject.extra["media3_version"]}")
    implementation("androidx.media3:media3-ui-leanback:${rootProject.extra["media3_version"]}")
    implementation("androidx.media3:media3-session:${rootProject.extra["media3_version"]}")

    // libVLC：ExoPlayer 不支持的 RealMedia/AVI/WMV 等旧格式回退引擎
    api("org.videolan.android:libvlc-all:3.6.5")

    // Hilt
    implementation("com.google.dagger:hilt-android:${rootProject.extra["hilt_version"]}")
    kapt("com.google.dagger:hilt-android-compiler:${rootProject.extra["hilt_version"]}")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
}
