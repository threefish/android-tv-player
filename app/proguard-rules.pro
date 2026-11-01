# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Kotlin serialization
-keepclassmembers class * {
    companion object;
}
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class kotlin.Unit

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Netty
-dontwarn io.netty.**
-keep class io.netty.** { *; }

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Ktor
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }

# General
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.protobuf.** { *; }

# XmlPull
-dontwarn org.xmlpull.v1.**
-keep class org.xmlpull.v1.** { *; }
