// Top-level build file
buildscript {
    extra.apply {
        set("kotlin_version", "1.9.22")
        set("compose_version", "1.6.0")
        set("media3_version", "1.3.0")
        set("room_version", "2.6.1")
        set("ktor_version", "2.3.8")
        set("hilt_version", "2.50")
    }
}

plugins {
    id("com.android.application") version "8.2.2" apply false
    id("com.android.library") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
}
