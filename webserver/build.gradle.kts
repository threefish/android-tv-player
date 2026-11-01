plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

android {
    namespace = "com.tvplayer.webserver"
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
}

dependencies {
    implementation(project(":core"))
    implementation(project(":ai"))
    implementation(project(":webdav"))

    // Ktor
    implementation("io.ktor:ktor-server-core:${rootProject.extra["ktor_version"]}")
    implementation("io.ktor:ktor-server-netty:${rootProject.extra["ktor_version"]}")
    implementation("io.ktor:ktor-server-content-negotiation:${rootProject.extra["ktor_version"]}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${rootProject.extra["ktor_version"]}")
    implementation("io.ktor:ktor-server-websockets:${rootProject.extra["ktor_version"]}")
    implementation("io.ktor:ktor-server-cors:${rootProject.extra["ktor_version"]}")

    // Hilt
    implementation("com.google.dagger:hilt-android:${rootProject.extra["hilt_version"]}")
    kapt("com.google.dagger:hilt-android-compiler:${rootProject.extra["hilt_version"]}")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
