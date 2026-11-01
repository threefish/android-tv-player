import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

// 签名口令不入库：从本机 local.properties 读取，也允许 -P 或 ~/.gradle/gradle.properties 覆盖
val signingProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.reader()?.use { load(it) }
}

fun signingProp(key: String): String? = signingProps.getProperty(key) ?: findProperty(key) as String?

android {
    namespace = "com.tvplayer.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tvplayer.app"
        minSdk = 23
        targetSdk = 34
        versionCode = 2
        versionName = "1.1.0"

        ndk {
            // 电视为 32 位 armv7；只保留该 ABI 可省掉约 52MB 的 arm64 libVLC
            abiFilters += listOf("armeabi-v7a")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = signingProp("RELEASE_STORE_FILE")?.let { rootProject.file(it) }
            storePassword = signingProp("RELEASE_STORE_PASSWORD")
            keyAlias = signingProp("RELEASE_KEY_ALIAS")
            keyPassword = signingProp("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // 本机未配置签名时回落 debug 签名，保证 clone 后仍能装 Release 包
            signingConfig = signingConfigs.getByName("release")
                .takeIf { it.storeFile != null }
                ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
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

    packaging {
        resources {
            excludes += listOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
                "META-INF/io.netty.versions.properties",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/*.kotlin_module",
                "META-INF/native-image/**",
                "META-INF/kotlinx_coroutines_*.properties"
            )
        }
    }
}

dependencies {
    // java.time 等 API 26+ 类在 Android 6 上不存在，靠核心库脱糖补齐
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation(project(":core"))
    implementation(project(":webdav"))
    implementation(project(":player"))
    implementation(project(":ai"))
    implementation(project(":webserver"))

    // Android TV
    implementation("androidx.leanback:leanback:1.0.0")

    // Activity Compose (setContent)
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Hilt
    implementation("com.google.dagger:hilt-android:${rootProject.extra["hilt_version"]}")
    kapt("com.google.dagger:hilt-android-compiler:${rootProject.extra["hilt_version"]}")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Media3 (ExoPlayer) for in-app playback screens
    implementation("androidx.media3:media3-exoplayer:${rootProject.extra["media3_version"]}")
    implementation("androidx.media3:media3-ui:${rootProject.extra["media3_version"]}")

    // OkHttp (QR code / remote calls in UI layer)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Coil：图片宫格与全屏预览，配合自定义 WebDAV Fetcher
    implementation("io.coil-kt:coil-compose:2.6.0")

    // ZXing (QR code generation)
    implementation("com.google.zxing:core:3.5.3")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
}
